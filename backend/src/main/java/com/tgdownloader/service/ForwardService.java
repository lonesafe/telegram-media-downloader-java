package com.tgdownloader.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgdownloader.entity.ForwardTask;
import com.tgdownloader.entity.TelegramConfig;
import com.tgdownloader.mapper.ForwardTaskMapper;
import com.tgdownloader.mapper.TelegramConfigMapper;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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
    private ForwardTaskMapper taskMapper;

    @Autowired
    private TelegramConfigMapper configMapper;

    @Autowired
    @Lazy
    private TelegramClientService telegramClient;

    private final ObjectMapper json = new ObjectMapper();
    private final ExecutorService executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> new Thread(r, "ForwardWorker-" + System.nanoTime()),
            new ThreadPoolExecutor.CallerRunsPolicy());

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
        TelegramConfig cfg = configMapper.findByConfigName("default").orElse(null);
        if (cfg == null || !Boolean.TRUE.equals(cfg.getForwardListenerEnabled())) {
            log.info("转发监听未启用");
            listening.set(false);
            return;
        }
        try {
            var ids = json.readValue(cfg.getForwardListenerSourceChatIds(), new TypeReference<List<Long>>() {});
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

    public boolean isListening() { return listening.get(); }

    public Set<Long> getSourceChatIds() { return Set.copyOf(sourceChatIds); }

    public Long getTargetChatId() { return targetChatId; }

    public void onNewMessage(TdApi.Message msg) {
        if (!listening.get()) return;
        if (msg.isOutgoing) return;
        if (!sourceChatIds.contains(msg.chatId)) return;
        createAutoTask(msg.chatId, msg.id, targetChatId);
        log.info("监听消息，已创建转发任务: chatId={} msgId={}", msg.chatId, msg.id);
    }

    // ==================== 转发任务 ====================

    public ForwardTask createTask(long srcChat, long msgId, long dstChat, boolean isAuto) {
        if (taskMapper.existsBySourceChatIdAndMessageIdAndTargetChatId(srcChat, msgId, dstChat)) {
            log.debug("转发任务已存在");
            return null;
        }
        ForwardTask task = new ForwardTask();
        task.setSourceChatId(srcChat);
        task.setMessageId(msgId);
        task.setTargetChatId(dstChat);
        task.setStatus("PENDING");
        task.setIsAutoForward(isAuto);
        try {
            var src = telegramClient.getChatSync(srcChat);
            if (src != null) task.setSourceChatTitle(src.title);
            var dst = telegramClient.getChatSync(dstChat);
            if (dst != null) task.setTargetChatTitle(dst.title);
        } catch (Exception e) {
            log.warn("获取聊天标题失败: {}", e.getMessage());
        }
        taskMapper.insertSelective(task);
        return task;
    }

    private ForwardTask createAutoTask(long srcChat, long msgId, long dstChat) {
        return createTask(srcChat, msgId, dstChat, true);
    }

    public ForwardTask executeTask(Long taskId) {
        ForwardTask task = taskMapper.selectById(taskId);
        if (task == null) return null;
        if ("SUCCESS".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) return task;
        task.setStatus("FORWARDING");
        taskMapper.update(task);
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
        taskMapper.update(task);
        return task;
    }

    public void processPending() {
        if (telegramClient == null || !telegramClient.isConnected()) return;
        List<ForwardTask> pending = taskMapper.selectAll().stream()
                .filter(t -> List.of("PENDING", "FORWARDING").contains(t.getStatus()))
                .toList();
        pending.forEach(t -> executor.submit(() -> executeTask(t.getId())));
        if (!pending.isEmpty()) log.info("处理 {} 个待转发任务", pending.size());
    }

    public void retryTask(Long taskId) {
        ForwardTask task = taskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus("PENDING");
            task.setErrorMessage(null);
            taskMapper.update(task);
            executor.submit(() -> executeTask(taskId));
        }
    }

    public void stopTask(Long taskId) {
        ForwardTask task = taskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus("STOPPED");
            taskMapper.update(task);
        }
    }

    // ==================== 查询 ====================

    public Map<String, Object> list(int page, int size) {
        List<ForwardTask> tasks = taskMapper.findAll(page * size, size);
        long total = taskMapper.selectCount();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("tasks", tasks);
        result.put("total", total);
        result.put("pages", (total + size - 1) / size);
        return result;
    }

    public Map<String, Long> stats() {
        return Map.of(
                "total", (long) taskMapper.selectCount(),
                "pending", taskMapper.countByStatus("PENDING"),
                "success", taskMapper.countByStatus("SUCCESS"),
                "failed", taskMapper.countByStatus("FAILED"));
    }

    public long countByStatus(String s) {
        return taskMapper.countByStatus(s);
    }
}
