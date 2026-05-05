package com.tgdownloader.service;

import com.tgdownloader.entity.TelegramConfig;
import com.tgdownloader.mapper.TelegramConfigMapper;
import com.tgdownloader.util.TelegramUtils;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * Telegram 用户客户端服务
 * <p>
 * 认证流程（参考 Example.java）：
 * 1. connect() → PhoneAuthSupplier 提供手机号 → TDLib 发送验证码
 * 2. setCode() → PhoneAuthSupplier 填写验证码 → 验证通过（或需要密码）
 * 3. setPassword() → PhoneAuthSupplier 填写两步密码 → 登录完成
 * <p>
 * 授权状态通过 UpdateAuthorizationState 回调更新，前端通过 /auth/state 查询当前状态。
 */
@Data
@Service
public class TelegramClientService {

    private static final Logger log = LoggerFactory.getLogger(TelegramClientService.class);

    @Autowired
    private TelegramConfigMapper telegramConfigMapper;

    @Autowired
    @Lazy
    private ForwardService forwardService;


    @Autowired
    @Lazy
    private SavedMessagesService savedMessagesService;

    /**
     * 单例 ClientFactory（全局只能有一个）
     */
    private static SimpleTelegramClientFactory clientFactoryInstance;

    private SimpleTelegramClient client;
    private final PhoneAuthSupplier phoneAuthSupplier = new PhoneAuthSupplier();

    // ==================== 连接状态 ====================
    private volatile boolean connected = false;
    private volatile boolean connecting = false;
    private volatile long userId = 0;
    private volatile String userName = "";
    private volatile int authState = STATE_NONE;

    // ==================== 认证状态（由 onAuthorizationStateUpdate 更新）====================
    /**
     * 等待手机号输入
     */
    public static final int STATE_WAIT_PHONE = 0;
    /**
     * 等待验证码输入
     */
    public static final int STATE_WAIT_CODE = 1;
    /**
     * 等待两步验证密码
     */
    public static final int STATE_WAIT_PASSWORD = 2;
    /**
     * 已连接（登录完成）
     */
    public static final int STATE_READY = 3;
    /**
     * 未初始化（未发起连接）
     */
    public static final int STATE_NONE = -1;

    public boolean isConnected() { return connected; }
    public boolean isConnecting() { return connecting; }


    /**
     * 是否需要输入手机号
     */
    public boolean isNeedPhoneNumber() {
        return authState == STATE_NONE || authState == STATE_WAIT_PHONE;
    }

    /**
     * 是否需要输入验证码
     */
    public boolean isNeedCode() {
        return authState == STATE_WAIT_CODE;
    }

    /**
     * 是否需要输入两步密码
     */
    public boolean isNeedPassword() {
        return authState == STATE_WAIT_PASSWORD;
    }

    // ==================== 生命周期 ====================

    @PostConstruct
    public void init() {
        log.info("TelegramClientService 初始化");
    }

    @PreDestroy
    public void destroy() {
        disconnect();
    }

    // ==================== 连接 / 断开 ====================

    /**
     * 发起 Telegram 连接。
     * <p>
     * 会先检查配置（API ID / Hash），然后：
     * 1. 设置 PhoneAuthSupplier 为交互处理器（处理验证码 / 密码）
     * 2. 使用 PhoneAuthSupplier 作为认证供应器（TDLib 会回调 get() 获取手机号）
     * 3. 发起连接后进入等待状态，直到 setPhone() 提供手机号
     */
    public synchronized void connect() throws Exception {
        if (connected || connecting) {
            log.warn("已连接或正在连接中，跳过");
            return;
        }
        connecting = true;
        // 注意：authState 由 onAuthorizationStateUpdate 回调更新，不在这里预设

        try {
            TelegramConfig config = telegramConfigMapper.findByConfigName("default")
                    .orElseThrow(() -> new IllegalStateException("请先保存 Telegram 配置（API ID / Hash）"));

            if (config.getApiId() == null || config.getApiId().isEmpty()
                    || config.getApiHash() == null || config.getApiHash().isEmpty()) {
                throw new IllegalStateException("请先配置 API ID 和 API Hash");
            }

            int apiId = Integer.parseInt(config.getApiId());
            String apiHash = config.getApiHash();

            // ---- 参考 Example.java：初始化 APIToken 和 TDLibSettings ----
            APIToken apiToken = new APIToken(apiId, apiHash);
            TDLibSettings settings = TDLibSettings.create(apiToken);

            // 会话目录配置
            String baseDir = System.getProperty("user.dir");
            String dbDir = config.getDatabaseDirectory();
            if (dbDir == null || dbDir.isEmpty()) dbDir = baseDir + File.separator + "tdlib_db";
            // 优先使用 tempPath，其次使用 filesDirectory
            String tempDir = config.getTempPath();
            if (tempDir == null || tempDir.isEmpty()) tempDir = config.getFilesDirectory();
            if (tempDir == null || tempDir.isEmpty()) tempDir = baseDir + File.separator + "temp";
            new File(dbDir).mkdirs();
            new File(tempDir).mkdirs();
            settings.setDatabaseDirectoryPath(Path.of(dbDir));
            settings.setDownloadedFilesDirectoryPath(Path.of(tempDir));

            // 测试数据中心（示例用）
            if (config.getUseTestDc() != null && config.getUseTestDc()) {
                settings.setUseTestDatacenter(true);
            }

            // ---- ClientFactory（全局单例）----
            if (clientFactoryInstance == null) {
                clientFactoryInstance = new SimpleTelegramClientFactory();
            }

            // ---- 参考 Example.java：设置 UpdateHandler ----
            SimpleTelegramClientBuilder clientBuilder = clientFactoryInstance.builder(settings);

            // 授权状态回调（更新 authState 并分发事件）
            clientBuilder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
                onAuthorizationStateUpdate(update.authorizationState);
            });

            // 新消息回调 → 转发监听服务
            clientBuilder.addUpdateHandler(TdApi.UpdateNewMessage.class, update -> {
                try {
                    forwardService.onNewMessage(update.message);
                } catch (Exception e) {
                    log.error("转发监听处理新消息异常: {}", e.getMessage(), e);
                }
            });

            // 收藏夹 Topic 变更回调 → 收藏夹服务
            clientBuilder.addUpdateHandler(TdApi.UpdateSavedMessagesTopic.class, update -> {
                try {
                    savedMessagesService.onTopicUpdate(update.topic);
                } catch (Exception e) {
                    log.error("收藏夹处理 Topic 更新异常: {}", e.getMessage(), e);
                }
            });

            // ---- 参考 Example.java：设置 ClientInteraction（验证码 / 密码交互）----
            // PhoneAuthSupplier 同时实现了 ClientInteraction.onParameterRequest
            clientBuilder.setClientInteraction(phoneAuthSupplier);

            // ---- 参考 Example.java：build(AuthenticationSupplier) 发起连接 ----
            // PhoneAuthSupplier.get() 会提供手机号；会回调 onParameterRequest 处理验证码/密码
            this.client = clientBuilder.build(phoneAuthSupplier);

            log.info("TDLight 客户端已启动，等待手机号...");

        } catch (Exception e) {
            connecting = false;
            authState = STATE_NONE;
            throw e;
        }
    }

    /**
     * 主动断开 Telegram 连接
     */
    public synchronized void disconnect() {
        connected = false;
        connecting = false;
        authState = STATE_NONE;
        userId = 0;
        userName = "";

        if (client != null) {
            try {
                client.sendClose();
            } catch (Exception ignored) {
            }
            client = null;
        }
        log.info("Telegram 用户客户端已断开");
    }

    // ==================== 认证输入（供 Controller 调用）====================

    /**
     * 设置手机号 - 供 TelegramController.auth/phone 调用
     * PhoneAuthSupplier.waitFor() 会收到此值并返回给 TDLib
     */
    public void setPhoneNumber(String phone) {
        log.info("设置手机号: {}", maskPhone(phone));
        phoneAuthSupplier.setPhone(phone);
    }

    /**
     * 设置验证码 - 供 TelegramController.auth/code 调用
     */
    public void setCode(String code) {
        log.info("设置验证码: ***");
        phoneAuthSupplier.setCode(code);
    }

    /**
     * 设置两步验证密码 - 供 TelegramController.auth/password 调用
     */
    public void setPassword(String password) {
        log.info("设置两步密码: ***");
        phoneAuthSupplier.setPassword(password);
    }

    // ==================== 授权状态回调 ====================

    private void onAuthorizationStateUpdate(TdApi.AuthorizationState state) {
        String stateName = state == null ? "null" : state.getClass().getSimpleName();
        log.info("[TDLight] 授权状态: {}", stateName);

        if (state instanceof TdApi.AuthorizationStateReady) {
            connected = true;
            connecting = false;
            authState = STATE_READY;
            log.info("Telegram 登录成功！");
            fetchMeAsync();
            savedMessagesService.onConnected();

        } else if (state instanceof TdApi.AuthorizationStateClosed) {
            connected = false;
            connecting = false;
            authState = STATE_NONE;
            log.info("Telegram 已断开连接");

        } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
            authState = STATE_WAIT_CODE;
            log.info("等待验证码输入（调用 setCode）");

        } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
            authState = STATE_WAIT_PASSWORD;
            log.info("等待两步密码输入（调用 setPassword）");

        } else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            authState = STATE_WAIT_PHONE;
            log.info("等待手机号输入（调用 setPhone）");
        }
    }

    private void fetchMeAsync() {
        if (client == null) return;
        client.getMeAsync().whenComplete((me, err) -> {
            if (err != null) {
                log.error("获取用户信息失败", err);
            } else {
                userId = me.id;
                userName = (me.firstName != null ? me.firstName : "")
                        + (me.lastName != null ? " " + me.lastName : "");
                log.info("当前用户: {} (ID: {})", userName, userId);
            }
        });
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // ==================== 静态工厂（供 BotClientService 共享 ClientFactory）====================

    public static SimpleTelegramClientFactory getClientFactory() {
        return clientFactoryInstance;
    }

    public static void setClientFactory(SimpleTelegramClientFactory factory) {
        clientFactoryInstance = factory;
    }

    // ==================== 文件下载 ====================

    /**
     * 通过 t.me 链接获取消息对象
     */
    public TdApi.Message getMessageByLink(String link) throws Exception {
        if (client == null) throw new IllegalStateException("用户客户端未连接");
        TdApi.MessageLinkInfo info = client.send(new TdApi.GetMessageLinkInfo(link)).get(30, TimeUnit.SECONDS);
        if (info != null) {
            if (info.message != null) {
                return info.message;
            }
        }

        log.warn("GetMessageLinkInfo 返回的 message 为空: link={}", link);
        return null;
    }


    /**
     * 直接从 TdApi.Message 下载媒体，带进度回调
     *
     * @param onProgress 回调 (downloadedSize, fileSize)，可为 null
     */
    public DownloadResult downloadMessageFile(TdApi.Message msg, BiConsumer<Long, Long> onProgress) throws Exception {
        if (client == null) throw new IllegalStateException("用户客户端未连接");
        if (msg == null) throw new IllegalArgumentException("消息不能为空");

        TdApi.GetMessage getMsg = new TdApi.GetMessage(msg.chatId, msg.id);
        msg = client.send(getMsg).get(30, TimeUnit.SECONDS);

        return doDownloadFile(Objects.requireNonNull(TelegramUtils.getFileRef(msg)), onProgress);
    }

    /**
     * 执行文件下载（内部方法）
     */
    private DownloadResult doDownloadFile(TelegramUtils.FileRef fileRef, BiConsumer<Long, Long> onProgress) throws Exception {
        int fileId = fileRef.fileId();
        log.info("开始下载: fileId={}, fileName={}, size={}", fileId, fileRef.fileName(), fileRef.fileSize());

        client.send(new TdApi.DownloadFile(fileId, 16, 0, 0, false)).get(10, TimeUnit.SECONDS);

        long lastReported = 0;
        while (true) {
            Thread.sleep(500);
            TdApi.File file = client.send(new TdApi.GetFile(fileId)).get(10, TimeUnit.SECONDS);

            long downloaded = file.local.downloadedSize;
            long total = file.size;

            // 回调通知外部进度更新
            if (onProgress != null && total > 0) {
                onProgress.accept(downloaded, total);
            }

            // 日志（每 1MB 报告一次）
            if (downloaded - lastReported >= 1024 * 1024 || file.local.isDownloadingCompleted) {
                if (total > 0) {
                    int pct = (int) (downloaded * 100 / total);
                    log.info("下载进度: {}% ({}/{})", pct, fmtSize(downloaded), fmtSize(total));
                }
                lastReported = downloaded;
            }

            if (file.local.isDownloadingCompleted) {
                log.info("下载完成: {}", file.local.path);
                return new DownloadResult(file.local.path, fileRef.fileName(), file.size);
            }

            if (file.local.canBeDownloaded && !file.local.isDownloadingActive) {
                // TDLib 可能因为错误停止了下载
                log.warn("下载意外停止，重新发起: fileId={}", fileId);
                client.send(new TdApi.DownloadFile(fileId, 16, 0, 0, false)).get(10, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * 下载结果
     */
    public record DownloadResult(String localPath, String fileName, long fileSize) {
    }

    // ==================== 聊天操作 ====================

    /**
     * 获取 Chat 对象（返回 TDLib 原生对象）
     */
    public TdApi.Chat getChatSync(long chatId) throws Exception {
        if (client == null) throw new IllegalStateException("客户端未连接");
        return client.send(new TdApi.GetChat(chatId)).get(30, TimeUnit.SECONDS);
    }

    public void forwardMessageSync(long fromChatId, long messageId, long toChatId) throws Exception {
        if (client == null) throw new IllegalStateException("客户端未连接");
        TdApi.ForwardMessages req = new TdApi.ForwardMessages();
        req.chatId = toChatId;
        req.fromChatId = fromChatId;
        req.messageIds = new long[]{messageId};
        req.options = new TdApi.MessageSendOptions();
        req.sendCopy = true;
        req.removeCaption = false;
        client.send(req).get(30, TimeUnit.SECONDS);
        log.info("消息转发: {} from {} to {}", messageId, fromChatId, toChatId);
    }

    private static String fmtSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 搜索公开聊天（按用户名）
     */
    public TdApi.Chat searchChatSync(String username) throws Exception {
        if (client == null) throw new IllegalStateException("客户端未连接");
        return client.send(new TdApi.SearchPublicChat(username)).get(10, TimeUnit.SECONDS);
    }

    /**
     * 解析消息链接，获取 chatId 和 messageId
     * 支持格式: t.me/c/数字/消息ID, t.me/用户名/消息ID
     */
    public Map<String, Object> parseMessageLink(String link) throws Exception {
        if (client == null) throw new IllegalStateException("客户端未连接");
        TdApi.MessageLinkInfo linkResult = client.send(new TdApi.GetMessageLinkInfo(link)).get(10, TimeUnit.SECONDS);
        
        Map<String, Object> result = new HashMap<>();
        if (linkResult != null && linkResult.message != null) {
            result.put("chatId", linkResult.message.chatId);
            result.put("messageId", linkResult.message.id);
        }
        return result;
    }

    /**
     * 获取聊天信息
     */
    public Map<String, Object> getChatInfo(long chatId) throws Exception {
        if (client == null) throw new IllegalStateException("客户端未连接");
        TdApi.Chat chat = getChatSync(chatId);
        
        Map<String, Object> info = new HashMap<>();
        info.put("id", chat.id);
        info.put("title", chat.title);
        info.put("type", chat.type.getClass().getSimpleName());
        info.put("lastMessage", chat.lastMessage != null ? chat.lastMessage.id : null);
        info.put("unreadCount", chat.unreadCount);
        return info;
    }

    /**
     * 获取消息列表
     */
    public Map<String, Object> getMessages(long chatId, long offset, int limit) throws Exception {
        if (client == null) throw new IllegalStateException("客户端未连接");
        TdApi.Chat chat = getChatSync(chatId);
        
        // 从最新消息往前获取
        TdApi.Messages messages = client.send(new TdApi.GetChatHistory(chatId, offset, 0, limit, false)).get(30, TimeUnit.SECONDS);
        
        List<Map<String, Object>> messageList = new ArrayList<>();
        for (TdApi.Message msg : messages.messages) {
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("id", msg.id);
            msgMap.put("date", msg.date);
            msgMap.put("contentType", msg.content.getClass().getSimpleName());
            if (msg.content instanceof TdApi.MessageText) {
                msgMap.put("text", ((TdApi.MessageText) msg.content).text.text);
            }
            messageList.add(msgMap);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("messages", messageList);
        result.put("totalCount", messages.totalCount);
        return result;
    }

}
