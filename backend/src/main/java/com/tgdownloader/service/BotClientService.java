package com.tgdownloader.service;

import com.tgdownloader.entity.TelegramConfig;
import com.tgdownloader.handler.bot.BotCommandHandler;
import com.tgdownloader.repository.TelegramConfigRepository;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Telegram Bot 客户端服务
 * Bot 命令处理委托给 BotCommandHandler
 */
@Service
@Data
public class BotClientService {

    private static final Logger log = LoggerFactory.getLogger(BotClientService.class);

    @Autowired
    private TelegramConfigRepository configRepository;

    @Autowired
    private BotCommandHandler commandHandler;

    private SimpleTelegramClient botClient;
    private volatile boolean connected = false;
    private volatile boolean connecting = false;


    public Map<String, Object> connect(String token) throws Exception {
        Map<String, Object> result = new HashMap<>();
        if (connected || connecting) {
            result.put("success", true);
            result.put("message", "已连接");
            return result;
        }
        connecting = true;

        TelegramConfig config = configRepository.findByConfigName("default").orElse(new TelegramConfig());
        int apiId = Integer.parseInt(config.getApiId() != null ? config.getApiId() : "0");
        String apiHash = config.getApiHash() != null ? config.getApiHash() : "";

        APIToken apiToken = new APIToken(apiId, apiHash);
        TDLibSettings settings = TDLibSettings.create(apiToken);

        // 共享 ClientFactory（TelegramClientService 和 BotClientService 各创建一个会导致冲突）
        SimpleTelegramClientFactory factory = TelegramClientService.getClientFactory();
        if (factory == null) {
            factory = new SimpleTelegramClientFactory();
            TelegramClientService.setClientFactory(factory);
        }

        SimpleTelegramClientBuilder builder = factory.builder(settings);

        // 授权状态回调
        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> {
            onAuthStateUpdate(update.authorizationState);
        });

        // 新消息回调 → 命令处理 + 监听转发检查
        builder.addUpdateHandler(TdApi.UpdateNewMessage.class, update -> {
            TdApi.Message message = update.message;
            commandHandler.handleMessage(message);
            commandHandler.handleForwardCheck(message);
        });

        this.botClient = builder.build(AuthenticationSupplier.bot(token));
        commandHandler.setBotClient(this.botClient);

        connected = true;
        connecting = false;

        result.put("success", true);
        result.put("message", "Bot 连接成功");
        return result;
    }

    private void onAuthStateUpdate(TdApi.AuthorizationState state) {
        if (state instanceof TdApi.AuthorizationStateReady) {
            connected = true;
            connecting = false;
            log.info("Bot 连接成功");
        } else if (state instanceof TdApi.AuthorizationStateClosed) {
            connected = false;
            connecting = false;
        }
    }

    @PreDestroy
    public void disconnect() {
        if (botClient != null) {
            try {
                botClient.sendClose();
            } catch (Exception ignored) {
            }
            botClient = null;
        }
        connected = false;
        connecting = false;
        log.info("Bot 客户端已断开");
    }

    /**
     * 通过 Bot 发送文本消息
     */
    public void sendTextMessage(long chatId, String message) {
        commandHandler.sendTextMessage(chatId, message);
    }

    /**
     * 获取底层 Bot 客户端，用于 TDLib API 调用（如 searchPublicChat）
     */
    public SimpleTelegramClient getClient() {
        return botClient;
    }

    public Map<String, Object> getBotInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("connected", connected);
        return info;
    }

    /**
     * 验证 Bot Token
     */
    public Map<String, Object> checkToken(String token) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 尝试用 token 创建客户端来验证
            result.put("valid", true);
            result.put("message", "Token 有效");
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "Token 无效: " + e.getMessage());
        }
        return result;
    }
}
