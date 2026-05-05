package com.tgdownloader.controller;

import com.tgdownloader.dto.ApiResponse;
import com.tgdownloader.entity.DownloadTask;
import com.tgdownloader.entity.TelegramConfig;
import com.tgdownloader.mapper.TelegramConfigMapper;
import com.tgdownloader.service.BotClientService;
import com.tgdownloader.service.DownloadCoreService;
import com.tgdownloader.service.SavedMessagesService;
import com.tgdownloader.service.TelegramClientService;
import com.tgdownloader.util.TelegramUtils;
import it.tdlight.jni.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Telegram Controller
 */
@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    private static final Logger log = LoggerFactory.getLogger(TelegramController.class);

    @Autowired
    private TelegramConfigMapper telegramConfigMapper;

    @Autowired
    private TelegramClientService telegramClientService;

    @Autowired
    private BotClientService botClientService;

    @Autowired
    private SavedMessagesService savedMessagesService;

    @Autowired
    private TelegramUtils telegramUtils;

    @Autowired
    private DownloadCoreService downloadCoreService;

    @GetMapping("/config")
    public ApiResponse<TelegramConfig> getConfig() {
        TelegramConfig config = telegramConfigMapper.findByConfigName("default")
                .orElseGet(() -> {
                    TelegramConfig newConfig = new TelegramConfig();
                    newConfig.setConfigName("default");
                    return telegramConfigMapper.save(newConfig);
                });
        return ApiResponse.success(config);
    }

    @PostMapping("/config")
    public ApiResponse<TelegramConfig> saveConfig(@RequestBody TelegramConfig config) {
        TelegramConfig existing = telegramConfigMapper.findByConfigName("default")
                .orElse(new TelegramConfig());

        existing.setConfigName("default");
        if (config.getApiId() != null) existing.setApiId(config.getApiId());
        if (config.getApiHash() != null) existing.setApiHash(config.getApiHash());
        if (config.getBotToken() != null) existing.setBotToken(config.getBotToken());
        if (config.getDatabaseDirectory() != null) existing.setDatabaseDirectory(config.getDatabaseDirectory());
        if (config.getFilesDirectory() != null) existing.setFilesDirectory(config.getFilesDirectory());
        if (config.getLanguageCode() != null) existing.setLanguageCode(config.getLanguageCode());
        if (config.getMaxConcurrentTasks() != null) existing.setMaxConcurrentTasks(config.getMaxConcurrentTasks());
        if (config.getProxyEnabled() != null) existing.setProxyEnabled(config.getProxyEnabled());
        if (config.getProxyScheme() != null) existing.setProxyScheme(config.getProxyScheme());
        if (config.getProxyHostname() != null) existing.setProxyHostname(config.getProxyHostname());
        if (config.getProxyPort() != null) existing.setProxyPort(config.getProxyPort());
        if (config.getProxyUsername() != null) existing.setProxyUsername(config.getProxyUsername());
        if (config.getProxyPassword() != null) existing.setProxyPassword(config.getProxyPassword());
        if (config.getEnableUploadFile() != null) existing.setEnableUploadFile(config.getEnableUploadFile());
        if (config.getUploadAdapter() != null) existing.setUploadAdapter(config.getUploadAdapter());
        if (config.getRemoteDir() != null) existing.setRemoteDir(config.getRemoteDir());
        if (config.getRclonePath() != null) existing.setRclonePath(config.getRclonePath());
        if (config.getBeforeUploadFileZip() != null) existing.setBeforeUploadFileZip(config.getBeforeUploadFileZip());
        if (config.getAfterUploadFileDelete() != null) existing.setAfterUploadFileDelete(config.getAfterUploadFileDelete());
        if (config.getUseTestDc() != null) existing.setUseTestDc(config.getUseTestDc());
        if (config.getSavedMessagesEnabled() != null) existing.setSavedMessagesEnabled(config.getSavedMessagesEnabled());
        if (config.getSavedMessagesLastScanMessageId() != null) existing.setSavedMessagesLastScanMessageId(config.getSavedMessagesLastScanMessageId());
        if (config.getForwardListenerEnabled() != null) existing.setForwardListenerEnabled(config.getForwardListenerEnabled());
        if (config.getForwardListenerSourceChatIds() != null) existing.setForwardListenerSourceChatIds(config.getForwardListenerSourceChatIds());
        if (config.getForwardListenerTargetChatId() != null) existing.setForwardListenerTargetChatId(config.getForwardListenerTargetChatId());
        if (config.getForwardListenerLastMessageId() != null) existing.setForwardListenerLastMessageId(config.getForwardListenerLastMessageId());

        boolean wasEnabled = existing.getSavedMessagesEnabled() != null && existing.getSavedMessagesEnabled();
        boolean nowEnabled = config.getSavedMessagesEnabled() != null && config.getSavedMessagesEnabled();

        TelegramConfig saved = telegramConfigMapper.save(existing);

        // 并发数变更时动态调整线程池
        if (saved.getMaxConcurrentTasks() != null) {
            downloadCoreService.updateConcurrency(saved.getMaxConcurrentTasks());
        }

        if (!wasEnabled && nowEnabled) {
            log.info("启用收藏夹扫秒..");
            try {
                savedMessagesService.scanAll(0);
            } catch (Exception e) {
                log.error("扫描收藏夹失败 {}", e.getMessage());
            }
        }

        return ApiResponse.success(saved);
    }

    @PostMapping("/test-connection")
    public ApiResponse<Map<String, Object>> testConnection(@RequestBody TelegramConfig config) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (config.getApiId() == null || config.getApiId().isEmpty()) {
                throw new Exception("API ID 不能为空");
            }
            if (config.getApiHash() == null || config.getApiHash().isEmpty()) {
                throw new Exception("API Hash 不能为空");
            }
            result.put("success", true);
            result.put("message", "连接成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", telegramClientService.isConnected());
        status.put("connecting", telegramClientService.isConnecting());
        status.put("userId", telegramClientService.getUserId());
        status.put("userName", telegramClientService.getUserName());
        return ApiResponse.success(status);
    }

    @PostMapping("/connect")
    public ApiResponse<String> connect() {
        try {
            telegramClientService.connect();
            return ApiResponse.success("正在连接...");
        } catch (Exception e) {
            log.error("连接失败", e);
            return ApiResponse.error("连接失败: " + e.getMessage());
        }
    }

    @PostMapping("/disconnect")
    public ApiResponse<String> disconnect() {
        try {
            telegramClientService.disconnect();
            return ApiResponse.success("已断开连接");
        } catch (Exception e) {
            log.error("断开连接失败: {}", e.getMessage());
            return ApiResponse.error("断开连接失败: " + e.getMessage());
        }
    }



    @PostMapping("/auth/phone")
    public ApiResponse<String> sendPhone(@RequestBody Map<String, String> body) {
        try {
            String phone = body.get("phone");
            if (phone == null || phone.isEmpty()) {
                return ApiResponse.error("手机号不能为空");
            }
            // 如果还没连接，先发起连接（TDLib 初始化后进入 WaitPhoneNumber 状态）
            if (!telegramClientService.isConnected() && !telegramClientService.isConnecting()) {
                telegramClientService.connect();
            }
            // 设置手机号-> PhoneAuthSupplier.get() 返回后交后TDLib -> TDLib 发送验证码
            telegramClientService.setPhoneNumber(phone);
            return ApiResponse.success("手机号已提交，请在Telegram 中查看验证码");
        } catch (Exception e) {
            return ApiResponse.error("验证码错误" + e.getMessage());
        }
    }

    @PostMapping("/auth/code")
    public ApiResponse<String> verifyCode(@RequestBody Map<String, String> body) {
        try {
            String code = body.get("code");
            if (code == null || code.isEmpty()) {
                return ApiResponse.error("验证码不能为空");
            }
            telegramClientService.setCode(code);
            return ApiResponse.success("验证码已提交");
        } catch (Exception e) {
            return ApiResponse.error("验证码错误" + e.getMessage());
        }
    }

    @PostMapping("/auth/password")
    public ApiResponse<String> verifyPassword(@RequestBody Map<String, String> body) {
        try {
            String password = body.get("password");
            if (password == null) password = "";
            telegramClientService.setPassword(password);
            return ApiResponse.success("密码已提交");
        } catch (Exception e) {
            return ApiResponse.error("密码验证失败: " + e.getMessage());
        }
    }

    @GetMapping("/auth/state")
    public ApiResponse<Map<String, Object>> getAuthState() {
        Map<String, Object> state = new HashMap<>();
        int authState = telegramClientService.getAuthState();
        state.put("authState", authState);
        state.put("needPhoneNumber", telegramClientService.isNeedPhoneNumber());
        state.put("needCode", telegramClientService.isNeedCode());
        state.put("needPassword", telegramClientService.isNeedPassword());
        state.put("isConnected", telegramClientService.isConnected());
        return ApiResponse.success(state);
    }

    @GetMapping("/chats")
    public ApiResponse<Map<String, Object>> getChats(@RequestParam String chatId) {
        try {
            Map<String, Object> chat = telegramClientService.getChatInfo(Long.parseLong(chatId));
            return ApiResponse.success(chat);
        } catch (Exception e) {
            log.error("获取聊天信息失败: {}", e.getMessage());
            return ApiResponse.error("获取聊天信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/messages")
    public ApiResponse<Map<String, Object>> getMessages(
            @RequestParam String chatId,
            @RequestParam(defaultValue = "0") long offset,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Map<String, Object> result = telegramClientService.getMessages(Long.parseLong(chatId), offset, limit);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取消息失败: {}", e.getMessage());
            return ApiResponse.error("获取消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/download")
    public ApiResponse<Map<String, Object>> downloadMedia(@RequestParam String link) {
        try {
            TdApi.Message msg = telegramClientService.getMessageByLink(link);
            if (msg == null) return ApiResponse.error("消息不存在");
            DownloadTask task = telegramUtils.buildTask(null, msg);
            if (task == null) return ApiResponse.error("该消息类型不在下载类型配置中，或无法解析媒体文件");
            downloadCoreService.startDownload(task);
            return ApiResponse.success(Map.of(
                    "taskId", task.getId(),
                    "messageId", msg.id,
                    "status", task.getStatus(),
                    "fileName", task.getFileName() != null ? task.getFileName() : ""));
        } catch (Exception e) {
            log.error("加入下载队列失败: {}", e.getMessage());
            return ApiResponse.error("加入下载队列失败: " + e.getMessage());
        }
    }

    @PostMapping("/forward")
    public ApiResponse<String> forwardMessage(
            @RequestParam String fromChatId,
            @RequestParam Long messageId,
            @RequestParam String toChatId) {
        try {
            telegramClientService.forwardMessageSync(Long.parseLong(fromChatId), messageId, Long.parseLong(toChatId));
            return ApiResponse.success("转发成功");
        } catch (Exception e) {
            log.error("转发失败: {}", e.getMessage());
            return ApiResponse.error("转发失败: " + e.getMessage());
        }
    }



    @GetMapping("/bot/status")
    public ApiResponse<Map<String, Object>> getBotStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", botClientService.isConnected());
        return ApiResponse.success(status);
    }

    @PostMapping("/bot/connect")
    public ApiResponse<Map<String, Object>> connectBot(@RequestBody(required = false) Map<String, String> body) {
        try {
            String token = body != null ? body.get("token") : null;
            if (token == null || token.isEmpty()) {
                TelegramConfig config = telegramConfigMapper.findByConfigName("default").orElse(null);
                if (config != null) {
                    token = config.getBotToken();
                }
            }
            if (token == null || token.isEmpty()) {
                return ApiResponse.error("Bot Token 不能为空");
            }
            Map<String, Object> result = botClientService.connect(token);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Bot 连接失败", e);
            return ApiResponse.error("Bot 连接失败: " + e.getMessage());
        }
    }

    @PostMapping("/bot/disconnect")
    public ApiResponse<String> disconnectBot() {
        try {
            botClientService.disconnect();
            return ApiResponse.success("Bot 已断开连接");
        } catch (Exception e) {
            return ApiResponse.error("Bot 断开连接失败: " + e.getMessage());
        }
    }

    @GetMapping("/bot/info")
    public ApiResponse<Map<String, Object>> getBotInfo() {
        try {
            Map<String, Object> info = botClientService.getBotInfo();
            return ApiResponse.success(info);
        } catch (Exception e) {
            log.error("获取Bot信息失败: {}", e.getMessage());
            return ApiResponse.error("获取Bot信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/bot/check-token")
    public ApiResponse<Map<String, Object>> checkBotToken(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            if (token == null || token.isEmpty()) {
                return ApiResponse.error("Token 不能为空");
            }
            Map<String, Object> result = botClientService.checkToken(token);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("验证Token失败: {}", e.getMessage());
            return ApiResponse.error("验证Token失败: " + e.getMessage());
        }
    }
}
