package com.tgdownloader.service;

import com.tgdownloader.entity.ChatConfig;
import com.tgdownloader.entity.DownloadTask;
import com.tgdownloader.model.DownloadStatus;
import com.tgdownloader.repository.ChatConfigRepository;
import com.tgdownloader.repository.DownloadTaskRepository;
import com.tgdownloader.repository.TelegramConfigRepository;
import com.tgdownloader.util.TelegramUtils;
import it.tdlight.jni.TdApi;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Service
public class SavedMessagesService {
    private static final Logger log = LoggerFactory.getLogger(SavedMessagesService.class);
    @Autowired
    private DownloadCoreService downloadCore;
    @Autowired
    private TelegramClientService telegramClient;
    @Autowired
    private DownloadTaskRepository taskRepo;
    @Autowired
    private ChatConfigRepository chatConfigRepo;
    @Autowired
    private TelegramConfigRepository configRepo;

    private static final int BATCH = 100;
    // 内部状态（监听开关）
    private final AtomicBoolean monitoring = new AtomicBoolean(false);

    @Transactional
    public int scanAll(long fromMsgId) {
        int found = 0;
        List<TdApi.Message> allMessages = new ArrayList<>();
        int limit = 100; // 最大值

        while (true) {
            // 使用 SearchSavedMessages 替代 GetSavedMessagesTopicHistory
            TdApi.SearchSavedMessages request = new TdApi.SearchSavedMessages(
                    0,              // savedMessagesTopicId: 0 代表所有主题
                    null,           // tag: 搜索的标签，null 代表不限
                    "",             // query: 搜索关键词，空字符串代表不限
                    fromMsgId,  // 从哪条消息开始
                    0,              // offset: 偏移量，通常设为0
                    limit           // 每次获取数量
            );

            TdApi.FoundChatMessages response = null;
            try {
                response = telegramClient.getClient().send(request).get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            TdApi.Message[] messages = response.messages;

            if (messages.length == 0) {
                break;
            }

            for (TdApi.Message msg : messages) {
                if (!hasMedia(msg)) continue;
                if (taskRepo.existsByChatIdAndMessageId(String.valueOf(msg.chatId), msg.id)) {
                    continue;
                }
                createTask(msg);
                found++;
            }

            if (messages.length < limit) {
                break;
            }

            // 更新 fromMsgId 为最后一条消息的 ID，用于获取下一页
            fromMsgId = messages[messages.length - 1].id;
        }
        return found;
    }

    private boolean hasMedia(TdApi.Message msg) {
        if (msg == null || msg.content == null) return false;
        // 检查媒体类型是否在配置的下载类型中
        String downloadTypes = null;
        try {
            var cfg = configRepo.findByConfigName("default").orElse(null);
            if (cfg != null) {
                downloadTypes = cfg.getDownloadTypes();
            }
        } catch (Exception e) {
            log.warn("获取下载类型配置失败: {}", e.getMessage());
        }
        return TelegramUtils.isAllowedDownloadType(msg, downloadTypes);
    }

    private void createTask(TdApi.Message msg) {
        try {
            ChatConfig cfg = chatConfigRepo.findByChatId(String.valueOf(msg.chatId))
                    .orElseGet(() -> {
                        ChatConfig c = new ChatConfig();
                        c.setChatId(String.valueOf(msg.chatId));
                        c.setTitle("Saved Messages");
                        c.setEnabled(true);
                        return chatConfigRepo.save(c);
                    });
            DownloadTask task = new DownloadTask();
            task.setFileName(Objects.requireNonNull(TelegramUtils.getFileRef(msg)).fileName());
            task.setChatId(String.valueOf(msg.chatId));
            task.setMessageId(msg.id);
            task.setTelegramMessage(msg);
            task.setChatConfig(cfg);
            task.setStatus(DownloadStatus.PENDING.name());
            task.setTotalTask(1);
            taskRepo.save(task);
            log.info("创建任务: msgId={}", msg.id);
        } catch (Exception e) {
            log.error("创建任务失败: {}", e.getMessage());
        }
    }

    private void startPending() {
        for (DownloadTask t : taskRepo.findByStatusIn(List.of(DownloadStatus.DOWNLOADING.name(), DownloadStatus.PENDING.name(), DownloadStatus.PAUSED.name()))) {
            if (!DownloadStatus.DOWNLOADING.name().equals(t.getStatus()) && !Boolean.TRUE.equals(t.getIsStopTransmission())) {
                downloadCore.startDownload(t);
            }
        }
    }

    // TelegramClientService 连接成功后回调
    public void onConnected() {
        if (monitoring.get()) performInitScan();
    }

    // TelegramClientService 收到 SavedMessagesTopicUpdate 回调
    public void onTopicUpdate(TdApi.SavedMessagesTopic topic) {
        if (!monitoring.get() || topic == null || topic.lastMessage == null) return;
        log.info("收到 SavedMessagesTopicUpdate: topicId={}, lastMessageId={}", topic.id, topic.lastMessage.id);
        TdApi.Message msg = topic.lastMessage;
        if (hasMedia(msg) && !taskRepo.existsByChatIdAndMessageId(String.valueOf(msg.chatId), msg.id)) {
            createTask(msg);
            startPending();
        }
    }

    public void performInitScan() {
        var cfg = configRepo.findByConfigName("default").orElse(null);
        if (cfg == null || !Boolean.TRUE.equals(cfg.getSavedMessagesEnabled())) return;
        scanAll(0);
    }
}
