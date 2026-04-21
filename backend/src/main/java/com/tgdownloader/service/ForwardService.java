package com.tgdownloader.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgdownloader.entity.ForwardTask;
import com.tgdownloader.repository.ForwardTaskRepository;
import com.tgdownloader.repository.TelegramConfigRepository;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 转发服务：合并了 ForwardService + ForwardListenerService
 */
@Service
public class ForwardService {

    private static final Logger log = LoggerFactory.getLogger(ForwardService.class);

    @Autowired
    private ForwardTaskRepository taskRepo;
    @Autowired
    private TelegramConfigRepository configRepo;
    @Autowired
    @Lazy
    private TelegramClientService telegramClient;

    private final ObjectMapper json = new ObjectMapper();
    private final ExecutorService executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> new Thread(r, "ForwardWorker-" + System.nanoTime()),
            new ThreadPoolExecutor.CallerRunsPolicy());

    // --- 转发监听状态 ---
    private final Set<Long> sourceChatIds = ConcurrentHashMap.newKeySet();
    private volatile Long targetChatId = null;
    private final AtomicBoolean listening = new AtomicBoolean(false);

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    // ==================== 转发监听 ====================

    public void startListening() {
        if (!listening.compareAndSet(false, true)) return;
        var cfg = configRepo.findByConfigName("default").orElse(null);
        if (cfg == null || !Boolean.TRUE.equals(cfg.getForwardListenerEnabled())) {
            log.info("转发监听未启用");
            listening.set(false);
            return;
        }
        try {
            var ids = json.readValue(cfg.getForwardListenerSourceChatIds(), new TypeReference<List<Long>>() {
            });
            sourceChatIds.clear();
            sourceChatIds.addAll(ids);
            targetChatId = cfg.getForwardListenerTargetChatId();
        } catch (Exception e) {
            log.error("解析监听配置失败: {}", e.getMessage());
            listening.set(false);
            return;
        }
        if (sourceChatIds.isEmpty() || targetChatId == null) {
            log.warn("转发监听配置不完整");
            listening.set(false);
            return;
        }
        log.info("转发监听已启动，源: {} -> 目标: {}", sourceChatIds, targetChatId);
    }

    public void stopListening() {
        if (!listening.get()) return;
        listening.set(false);
        sourceChatIds.clear();
        targetChatId = null;
        log.info("转发监听已停止");
    }

    public boolean isListening() {
        return listening.get();
    }

    public Set<Long> getSourceChatIds() {
        return Set.copyOf(sourceChatIds);
    }

    public Long getTargetChatId() {
        return targetChatId;
    }

    /**
     * TDLib 新消息回调（由 TelegramClientService 调用）
     */
    public void onNewMessage(TdApi.Message msg) {
        if (!listening.get()) return;
        if (msg.isOutgoing) return;
        if (!sourceChatIds.contains(msg.chatId)) return;
        createAutoTask(msg.chatId, msg.id, targetChatId);
        log.info("监听消息，已创建转发任务: chatId={} msgId={}", msg.chatId, msg.id);
    }

    // ==================== 转发任务 ====================

    @Transactional
    public ForwardTask createTask(long srcChat, long msgId, long dstChat, boolean isAuto) {
        if (taskRepo.existsBySourceChatIdAndMessageIdAndTargetChatId(srcChat, msgId, dstChat)) {
            log.debug("转发任务已存在");
            return null;
        }
        ForwardTask task = ForwardTask.builder()
                .sourceChatId(srcChat).messageId(msgId).targetChatId(dstChat)
                .status("PENDING").isAutoForward(isAuto).build();
        try {
            var src = telegramClient.getChatSync(srcChat);
            if (src != null) task.setSourceChatTitle(src.title);
            var dst = telegramClient.getChatSync(dstChat);
            if (dst != null) task.setTargetChatTitle(dst.title);
        } catch (Exception e) {
            log.warn("获取聊天标题失败: {}", e.getMessage());
        }
        return taskRepo.save(task);
    }

    private ForwardTask createAutoTask(long srcChat, long msgId, long dstChat) {
        return createTask(srcChat, msgId, dstChat, true);
    }

    @Transactional
    public ForwardTask executeTask(Long taskId) {
        var task = taskRepo.findById(taskId).orElse(null);
        if (task == null) return null;
        if ("SUCCESS".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) return task;
        task.setStatus("FORWARDING");
        taskRepo.save(task);
        try {
            telegramClient.forwardMessageSync(task.getSourceChatId(), task.getMessageId(), task.getTargetChatId());
            task.setStatus("SUCCESS");
            task.setErrorMessage(null);
            log.info("转发成功: {} -> {} msg:{}", task.getSourceChatId(), task.getTargetChatId(), task.getMessageId());
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            log.error("转发失败: {}", e.getMessage());
        }
        return taskRepo.save(task);
    }

    public void processPending() {
        if (!telegramClient.isConnected()) return;
        var pending = taskRepo.findByStatusIn(List.of("PENDING", "FORWARDING"));
        pending.forEach(t -> executor.submit(() -> executeTask(t.getId())));
        if (!pending.isEmpty()) log.info("处理 {} 个待转发任务", pending.size());
    }

    @Transactional
    public void retryTask(Long taskId) {
        taskRepo.findById(taskId).ifPresent(task -> {
            task.setStatus("PENDING");
            task.setErrorMessage(null);
            taskRepo.save(task);
            executor.submit(() -> executeTask(taskId));
        });
    }

    @Transactional
    public void stopTask(Long taskId) {
        taskRepo.findById(taskId).ifPresent(task -> {
            task.setStatus("STOPPED");
            taskRepo.save(task);
        });
    }

    // ==================== 查询 ====================

    public Page<ForwardTask> list(Pageable p) {
        return taskRepo.findAll(p);
    }

    public Map<String, Long> stats() {
        return Map.of(
                "total", taskRepo.count(),
                "pending", taskRepo.countByStatus("PENDING"),
                "success", taskRepo.countByStatus("SUCCESS"),
                "failed", taskRepo.countByStatus("FAILED"));
    }

    public long countByStatus(String s) {
        return taskRepo.countByStatus(s);
    }
}
