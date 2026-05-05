package com.tgdownloader.handler.bot;

import com.tgdownloader.entity.ChatConfig;
import com.tgdownloader.entity.DownloadTask;
import com.tgdownloader.model.DownloadStatus;
import com.tgdownloader.mapper.ChatConfigMapper;
import com.tgdownloader.mapper.DownloadTaskMapper;
import com.tgdownloader.mapper.TelegramConfigMapper;
import com.tgdownloader.service.DownloadCoreService;
import com.tgdownloader.service.TelegramClientService;
import com.tgdownloader.util.TelegramUtils;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bot 命令处理器
 */
@Component
public class BotCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(BotCommandHandler.class);

    @Autowired
    private ChatConfigMapper chatConfigMapper;
    @Autowired
    private DownloadTaskMapper downloadTaskMapper;
    @Autowired
    private TelegramConfigMapper configMapper;
    @Autowired
    @Lazy
    private DownloadCoreService downloadCoreService;
    @Autowired
    private TelegramClientService telegramClientService;
    @Autowired
    private TelegramUtils telegramUtils;

    @Setter
    private SimpleTelegramClient botClient;
    private final Map<Long, ListenTask> listenTasks = new ConcurrentHashMap<>();
    private final AtomicLong taskIdSeq = new AtomicLong(0);
    private volatile String language = "zh";
    private final Map<String, Long> usernameCache = new ConcurrentHashMap<>();

    public void handleMessage(TdApi.Message message) {
        if (!(message.content instanceof TdApi.MessageText mt)) return;
        String text = mt.text.text.trim();
        if (text.isEmpty()) return;
        if (text.startsWith("/")) dispatchCommand(message.chatId, text);
        else if (text.startsWith("https://t.me/")) downloadByLink(message.chatId, text);
    }

    public void handleForwardCheck(TdApi.Message message) {
        for (ListenTask task : listenTasks.values()) {
            if (message.chatId == task.sourceChatId && matchFilter(message, task.filter)) {
                try {
                    forwardMsg(task.sourceChatId, message.id, task.targetChatId);
                } catch (Exception e) {
                    log.error("转发失败 msg={}", message.id, e);
                }
            }
        }
    }

    private void dispatchCommand(long chatId, String text) {
        String[] p = text.split("\s+");
        String cmd = p[0].toLowerCase();
        try {
            switch (cmd) {
                case "/start" -> sendTextMessage(chatId, WELCOME_MSG);
                case "/help" -> sendTextMessage(chatId, HELP_MSG);
                case "/download" -> cmdDownload(chatId, p);
                case "/forward" -> cmdForward(chatId, p);
                case "/listen_forward" -> cmdListenForward(chatId, p);
                case "/stop" -> cmdStop(chatId, p);
                case "/status" -> cmdStatus(chatId);
                case "/set_chat" -> cmdSetChat(chatId, p);
                case "/list_chats" -> cmdListChats(chatId);
                case "/set_language" -> cmdSetLanguage(chatId, p);
                case "/add_filter" -> cmdAddFilter(chatId, p);
                case "/get_info" -> cmdGetInfo(chatId, p);
                default -> sendTextMessage(chatId, "未知命令: " + cmd + "\n输入 /help 查看帮助");
            }
        } catch (Exception e) {
            log.error("命令执行失败: {}", text, e);
            sendTextMessage(chatId, "命令执行失败: " + e.getMessage());
        }
    }

    // ========== /download ==========
    private void cmdDownload(long chatId, String[] args) {
        if (args.length < 2) {
            sendTextMessage(chatId, "格式:\n/download <链接>\n/download <链接> <起始ID>\n/download <链接> <起始ID> <结束ID>");
            return;
        }
        String link = args[1];
        long startId = args.length > 2 ? parseMsgId(args[2]) : 0;
        long endId = args.length > 3 ? parseMsgId(args[3]) : 0;
        CompletableFuture.runAsync(() -> {
            try {
                if (endId == 0 && startId == 0) {
                    TdApi.Message msg = resolveMessageFromLink(link);
                    if (msg == null) {
                        sendTextMessage(chatId, "解析链接失败，请确保 Bot 有权限访问该群组");
                        return;
                    }
                    ChatConfig cfg = chatConfigMapper.findByChatId(String.valueOf(msg.chatId))
                            .orElseGet(() -> {
                                ChatConfig c = new ChatConfig();
                                c.setChatId(String.valueOf(msg.chatId));
                                c.setEnabled(true);
                                return chatConfigMapper.save(c);
                            });
                    DownloadTask task = telegramUtils.buildTask(cfg, msg);
                    if (task == null) {
                        sendTextMessage(chatId, "该消息类型不在下载类型配置中，已跳过:\n链接: " + link);
                        return;
                    }
                    downloadCoreService.startDownload(task);
                    String type = msg.content.getClass().getSimpleName().replace("Message", "");
                    sendTextMessage(chatId, "已加入下载队列:\n链接: " + link + "\n消息ID: " + msg.id + "\n类型: " + type);
                } else {
                    String baseUrl = extractBaseUrl(link);
                    if (baseUrl == null) {
                        sendTextMessage(chatId, "无法解析链接: " + link);
                        return;
                    }
                    long finalEndId = (endId == 0) ? startId + 100 : endId;
                    sendTextMessage(chatId, "正在下载 " + startId + " 到 " + finalEndId + " ...");
                    int ok = 0, fail = 0;
                    for (long id = startId; id <= finalEndId; id++) {
                        try {
                            TdApi.Message msg = resolveMessageFromLink(baseUrl + "/" + id);
                            if (msg != null) {
                                ChatConfig cfg = chatConfigMapper.findByChatId(String.valueOf(msg.chatId))
                                    .orElseGet(() -> {
                                        ChatConfig c = new ChatConfig();
                                        c.setChatId(String.valueOf(msg.chatId));
                                        c.setEnabled(true);
                                        return chatConfigMapper.save(c);
                                    });
                                DownloadTask task = telegramUtils.buildTask(cfg, msg);
                                if (task != null) {
                                    downloadCoreService.startDownload(task);
                                    ok++;
                                } else {
                                    fail++; // 类型不在允许范围内
                                }
                            } else {
                                fail++;
                            }
                        } catch (Exception e) {
                            log.debug("跳过 msgId={}: {}", id, e.getMessage());
                            fail++;
                        }
                    }
                    sendTextMessage(chatId, "下载完成\n成功: " + ok + "\n失败: " + fail);
                }
            } catch (Exception e) {
                log.error("下载失败", e);
                sendTextMessage(chatId, "下载失败: " + e.getMessage());
            }
        });
    }

    private String extractBaseUrl(String link) {
        int lastSlash = link.lastIndexOf("/");
        if (lastSlash > 0) {
            String maybeId = link.substring(lastSlash + 1);
            if (maybeId.matches("\\d+")) return link.substring(0, lastSlash);
        }
        return null;
    }

    // ========== /forward ==========
    private void cmdForward(long chatId, String[] args) {
        if (args.length < 3) {
            sendTextMessage(chatId, "格式:\n/forward <源链接> <目标链接>\n/forward <源链接> <目标链接> <起始ID> <结束ID>");
            return;
        }
        String srcLink = args[1], dstLink = args[2];
        long startId = args.length > 3 ? parseMsgId(args[3]) : 0;
        long endId = args.length > 4 ? parseMsgId(args[4]) : 0;
        CompletableFuture.runAsync(() -> {
            ResolvedLink src = resolveLinkSync(srcLink);
            ResolvedLink dst = resolveLinkSync(dstLink);
            if (src == null || dst == null) {
                sendTextMessage(chatId, "解析链接失败");
                return;
            }
            if (endId == 0 && startId == 0) {
                TdApi.Message msg = resolveMessageFromLink(srcLink);
                if (msg == null) {
                    sendTextMessage(chatId, "无效的消息链接");
                    return;
                }
                try {
                    forwardMsg(src.realChatId, msg.id, dst.realChatId);
                    sendTextMessage(chatId, "已转发消息ID: " + msg.id + " -> " + dstLink);
                } catch (Exception e) {
                    log.error("转发失败", e);
                    sendTextMessage(chatId, "转发失败: " + e.getMessage());
                }
            } else {
                if (startId == 0 || endId == 0) {
                    sendTextMessage(chatId, "批量转发必须指定起始ID和结束ID");
                    return;
                }
                String srcBaseUrl = extractBaseUrl(srcLink);
                if (srcBaseUrl == null) {
                    sendTextMessage(chatId, "无法解析链接");
                    return;
                }
                sendTextMessage(chatId, "正在转发 " + startId + " 到 " + endId + " ...");
                int ok = 0, fail = 0;
                for (long id = startId; id <= endId; id++) {
                    try {
                        TdApi.Message m = resolveMessageFromLink(srcBaseUrl + "/" + id);
                        if (m != null) {
                            forwardMsg(src.realChatId, m.id, dst.realChatId);
                            ok++;
                        } else {
                            fail++;
                        }
                    } catch (Exception e) {
                        log.debug("转发失败 msgId={}: {}", id, e.getMessage());
                        fail++;
                    }
                }
                sendTextMessage(chatId, "转发完成\n成功: " + ok + "\n失败: " + fail);
            }
        });
    }

    // ========== /listen_forward ==========
    private void cmdListenForward(long chatId, String[] args) {
        if (args.length < 3) {
            sendTextMessage(chatId, "格式:\n/listen_forward <源链接> <目标链接>\n/listen_forward <源链接> <目标链接> [过滤器]");
            return;
        }
        CompletableFuture.runAsync(() -> {
            ResolvedLink src = resolveLinkSync(args[1]);
            ResolvedLink dst = resolveLinkSync(args[2]);
            if (src == null || dst == null) {
                sendTextMessage(chatId, "解析链接失败");
                return;
            }
            String filter = args.length > 3 ? args[3].toUpperCase() : "ALL";
            long taskId = taskIdSeq.incrementAndGet();
            listenTasks.put(taskId, new ListenTask(taskId, src.realChatId, dst.realChatId, filter));
            sendTextMessage(chatId, "已启动监听任务 #" + taskId + "\n源: " + src.realChatId + "\n目标: " + dst.realChatId + "\n过滤: " + filter);
        });
    }

    // ========== /stop ==========
    private void cmdStop(long chatId, String[] args) {
        if (args.length > 1) {
            try {
                long taskId = Long.parseLong(args[1].replace("#", ""));
                ListenTask r = listenTasks.remove(taskId);
                sendTextMessage(chatId, r != null ? "已停止监听任务 #" + taskId : "未找到监听任务 #" + taskId);
            } catch (NumberFormatException e) {
                sendTextMessage(chatId, "无效的任务ID: " + args[1]);
            }
        } else {
            int n = listenTasks.size();
            if (n > 0) {
                listenTasks.clear();
                sendTextMessage(chatId, "已停止所有监听任务 (" + n + " 个)");
            } else {
                sendTextMessage(chatId, "当前没有正在监听的转发任务");
            }
        }
    }

    // ========== /status ==========
    private void cmdStatus(long chatId) {
        int running = (int) downloadTaskMapper.countByStatusIn(List.of(DownloadStatus.DOWNLOADING.name()));
        String botStatus = botClient != null ? "在线" : "离线";
        sendTextMessage(chatId, String.format("Bot: %s\n下载中: %d\n监听任务: %d\n配置群组: %d\n语言: %s",
                botStatus, running, listenTasks.size(), chatConfigMapper.countAll(), language));
    }

    // ========== /set_chat ==========
    private void cmdSetChat(long chatId, String[] args) {
        if (args.length < 2) {
            sendTextMessage(chatId, "格式:\n/set_chat <ID或链接>");
            return;
        }
        long targetId = resolveChatId(args[1]);
        if (targetId == 0) {
            sendTextMessage(chatId, "无法解析 Chat ID，请检查输入");
            return;
        }
        ChatConfig cfg = chatConfigMapper.findByChatId(String.valueOf(targetId))
                .orElseGet(() -> {
                    ChatConfig c = new ChatConfig();
                    c.setChatId(String.valueOf(targetId));
                    c.setEnabled(true);
                    return chatConfigMapper.save(c);
                });
        sendTextMessage(chatId, "已设置当前群组: " + targetId + "\n标题: " + nvl(cfg.getTitle(), "(无)"));
    }

    // ========== /list_chats ==========
    private void cmdListChats(long chatId) {
        List<ChatConfig> all = chatConfigMapper.findAll();
        if (all.isEmpty()) {
            sendTextMessage(chatId, "还没有配置任何群组，请先使用 /set_chat 设置");
            return;
        }
        StringBuilder sb = new StringBuilder("已配置的群组:\n");
        for (ChatConfig cfg : all) {
            sb.append(cfg.getChatId()).append(" - ").append(nvl(cfg.getTitle(), "(无标题)")).append("\n");
        }
        sendTextMessage(chatId, sb.toString());
    }

    // ========== /set_language ==========
    private void cmdSetLanguage(long chatId, String[] args) {
        if (args.length < 2) {
            sendTextMessage(chatId, "格式:\n/set_language <zh|en>");
            return;
        }
        String lang = args[1].toLowerCase();
        if (!lang.equals("zh") && !lang.equals("en")) {
            sendTextMessage(chatId, "仅支持 zh 或 en");
            return;
        }
        language = lang;
        configMapper.findByConfigName("default").ifPresent(cfg -> {
            cfg.setLanguageCode(lang);
            configMapper.save(cfg);
        });
        sendTextMessage(chatId, "语言已设置为: " + (lang.equals("zh") ? "中文" : "English"));
    }

    // ========== /add_filter ==========
    private void cmdAddFilter(long chatId, String[] args) {
        if (args.length < 2) {
            sendTextMessage(chatId, "格式:\n/add_filter <过滤器>\n过滤器: ALL, PHOTO, VIDEO, DOCUMENT, AUDIO");
            return;
        }
        String filter = args[1].toUpperCase();
        if (!isValidFilter(filter)) {
            sendTextMessage(chatId, "无效的过滤器: " + filter + "\n可用: ALL, PHOTO, VIDEO, DOCUMENT, AUDIO");
            return;
        }
        ChatConfig cfg = chatConfigMapper.findByChatId(String.valueOf(chatId)).orElseGet(() -> {
            ChatConfig c = new ChatConfig();
            c.setChatId(String.valueOf(chatId));
            return c;
        });
        cfg.setFilterType(filter);
        chatConfigMapper.save(cfg);
        sendTextMessage(chatId, "已更新过滤器为: " + filter);
    }

    // ========== /get_info ==========
    private void cmdGetInfo(long chatId, String[] args) {
        if (args.length < 2) {
            sendTextMessage(chatId, "格式:\n/get_info <链接>");
            return;
        }
        CompletableFuture.runAsync(() -> {
            TdApi.Message msg = resolveMessageFromLink(args[1]);
            if (msg == null) {
                sendTextMessage(chatId, "无法获取消息信息，请检查链接是否有效");
                return;
            }
            sendTextMessage(chatId, buildMessageInfoText(msg));
        });
    }

    // ========== 直接链接下载 ==========
    private void downloadByLink(long chatId, String link) {
        CompletableFuture.runAsync(() -> {
            TdApi.Message msg = resolveMessageFromLink(link);
            if (msg == null) {
                sendTextMessage(chatId, "解析链接失败");
                return;
            }
            ChatConfig cfg = chatConfigMapper.findByChatId(String.valueOf(msg.chatId))
                    .orElseGet(() -> {
                        ChatConfig c = new ChatConfig();
                        c.setChatId(String.valueOf(msg.chatId));
                        c.setEnabled(true);
                        return chatConfigMapper.save(c);
                    });
            DownloadTask task = telegramUtils.buildTask(cfg, msg);
            if (task == null) {
                sendTextMessage(chatId, "该消息类型不在下载类型配置中，已跳过:\n链接: " + link);
                return;
            }
            downloadCoreService.startDownload(task);
            String type = msg.content.getClass().getSimpleName().replace("Message", "");
            sendTextMessage(chatId, "已加入下载队列:\n链接: " + link + "\n消息ID: " + msg.id + "\n类型: " + type);
        });
    }

    // ========== 工具方法 ==========
    private TdApi.Message resolveMessageFromLink(String link) {
        try {
            return telegramClientService.getMessageByLink(link);
        } catch (Exception e) {
            log.error("解析消息链接失败: {}", link, e);
            return null;
        }
    }

    private ResolvedLink resolveLinkSync(String link) {
        try {
            ParsedLink p = parseLinkRaw(link);
            if (p == null) return null;
            return resolveParsedLink(p);
        } catch (Exception e) {
            log.error("解析链接失败: {}", link, e);
            return null;
        }
    }

    private ParsedLink parseLinkRaw(String link) {
        try {
            String s = link.replaceFirst("https://t.me/", "");
            String[] p = s.split("/");
            if (p[0].equals("c") && p.length >= 3)
                return new ParsedLink(ChatType.PRIVATE, Long.parseLong(p[1]), null, Long.parseLong(p[2]));
            if (p.length >= 2) return new ParsedLink(ChatType.PUBLIC, 0, p[0], Long.parseLong(p[1]));
            if (p.length == 1 && p[0].matches("\\d+"))
                return new ParsedLink(ChatType.PRIVATE, Long.parseLong(p[0]), null, 0);
            return null;
        } catch (Exception e) {
            log.error("解析链接格式失败: {}", link, e);
            return null;
        }
    }

    private ResolvedLink resolveParsedLink(ParsedLink p) {
        if (p == null) return null;
        try {
            if (p.type == ChatType.PRIVATE) {
                return new ResolvedLink(-100000000000L + p.rawChatId * 1000, p.msgId);
            } else {
                Long cached = usernameCache.get(p.username);
                if (cached != null) return new ResolvedLink(cached, p.msgId);
                TdApi.Chat chat = telegramClientService.searchChatSync(p.username);
                if (chat != null) {
                    usernameCache.put(p.username, chat.id);
                    return new ResolvedLink(chat.id, p.msgId);
                }
                return null;
            }
        } catch (Exception e) {
            log.error("解析链接失败: {}", e.getMessage());
            return null;
        }
    }

    private long resolveChatId(String input) {
        if (input.startsWith("https://t.me/")) {
            ParsedLink p = parseLinkRaw(input);
            if (p != null) {
                ResolvedLink r = resolveParsedLink(p);
                return r != null ? r.realChatId : 0;
            }
        }
        try {
            long raw = Long.parseLong(input.replace("-", ""));
            if (raw > 0 && raw < 1_000_000_000L) return -100_000_000_000L + raw * 1000;
            return raw;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseMsgId(String s) {
        try {
            return Long.parseLong(s.replace("#", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void sendTextMessage(long chatId, String text) {
        if (botClient == null) {
            log.warn("Bot 未连接");
            return;
        }
        try {
            TdApi.SendMessage req = new TdApi.SendMessage();
            req.chatId = chatId;
            TdApi.InputMessageText txtMsg = new TdApi.InputMessageText();
            txtMsg.text = new TdApi.FormattedText(text, null);
            txtMsg.linkPreviewOptions = null;
            txtMsg.clearDraft = false;
            req.inputMessageContent = txtMsg;
            botClient.send(req, result -> {
                if (result.isError()) log.warn("Bot 发送失败: {}", result.getError().message);
            });
        } catch (Exception e) {
            log.error("发送消息失败 chatId={}: {}", chatId, e.getMessage());
        }
    }

    private void forwardMsg(long fromChatId, long msgId, long toChatId) {
        if (botClient == null) throw new IllegalStateException("Bot 未连接");
        TdApi.ForwardMessages req = new TdApi.ForwardMessages();
        req.chatId = toChatId;
        req.fromChatId = fromChatId;
        req.messageIds = new long[]{msgId};
        req.sendCopy = true;
        botClient.send(req, result -> {
            if (result.isError()) log.warn("转发失败: {}", result.getError().message);
        });
    }

    private String buildMessageInfoText(TdApi.Message msg) {
        String type = msg.content.getClass().getSimpleName().replace("Message", "");
        long size = 0;
        String caption = "";
        if (msg.content instanceof TdApi.MessageText mt) {
            caption = mt.text.text;
            size = caption.length();
        } else if (msg.content instanceof TdApi.MessageVideo mv) size = mv.video.video.size;
        else if (msg.content instanceof TdApi.MessagePhoto mp) size = mp.photo.sizes[0].photo.size;
        else if (msg.content instanceof TdApi.MessageDocument md) size = md.document.document.size;
        else if (msg.content instanceof TdApi.MessageAudio ma) size = ma.audio.audio.size;
        else if (msg.content instanceof TdApi.MessageVoiceNote vn) size = vn.voiceNote.voice.size;
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(msg.date * 1000L));
        String cap = caption.isEmpty() ? "" : "\n描述: " + caption.substring(0, Math.min(caption.length(), 100));
        return "消息ID: " + msg.id + "\n类型: " + type + "\n大小: " + fmtSize(size) + "\n日期: " + date + cap;
    }

    private boolean matchFilter(TdApi.Message msg, String filter) {
        if (filter == null || filter.equals("ALL")) return true;
        String msgType = msg.content.getClass().getSimpleName().replace("Message", "").toUpperCase();
        for (String f : filter.split(",")) if (f.trim().equals(msgType)) return true;
        return false;
    }

    private boolean isValidFilter(String filter) {
        if (filter == null || filter.equals("ALL")) return true;
        String[] valid = {"ALL", "PHOTO", "VIDEO", "DOCUMENT", "AUDIO", "VOICE"};
        for (String f : filter.split(",")) {
            boolean ok = false;
            for (String v : valid) if (f.trim().equals(v)) ok = true;
            if (!ok) return false;
        }
        return true;
    }

    private static String fmtSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024 * 1024 * 1024) return String.format("%.1f MB", b / (1024.0 * 1024));
        return String.format("%.2f GB", b / (1024.0 * 1024 * 1024));
    }

    private static String nvl(String s, String def) {
        return s == null || s.isEmpty() ? def : s;
    }

    // ========== 常量 ==========
    private static final String WELCOME_MSG = "欢迎使用 Telegram Media Downloader Bot!\n\n发送 t.me 链接即可自动下载媒体文件\n发送 /help 获取完整命令帮助";
    private static final String HELP_MSG =
            "Telegram Media Downloader 帮助\n\n" +
                    "下载命令:\n" +
                    "/download <链接> [起始ID] [结束ID]\n" +
                    "/get_info <链接>\n\n" +
                    "转发命令:\n" +
                    "/forward <源链接> <目标链接> [起始ID] [结束ID]\n" +
                    "/listen_forward <源链接> <目标链接> [过滤器]\n" +
                    "/stop [任务ID]\n\n" +
                    "配置命令:\n" +
                    "/set_chat <ID/链接>\n" +
                    "/list_chats\n" +
                    "/set_language <zh|en>\n" +
                    "/add_filter <过滤器>\n\n" +
                    "基础命令:\n" +
                    "/start /help /status";

    // ========== 内部类 ==========
    private record ResolvedLink(long realChatId, long msgId) {
    }

    private enum ChatType {PRIVATE, PUBLIC}

    private record ParsedLink(ChatType type, long rawChatId, String username, long msgId) {
    }

    private record ListenTask(long id, long sourceChatId, long targetChatId, String filter) {
    }
}