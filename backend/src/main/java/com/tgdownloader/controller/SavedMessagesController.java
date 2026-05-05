package com.tgdownloader.controller;

import com.tgdownloader.dto.ApiResponse;
import com.tgdownloader.entity.DownloadTask;
import com.tgdownloader.model.DownloadStatus;
import com.tgdownloader.service.DownloadCoreService;
import com.tgdownloader.service.SavedMessagesService;
import com.tgdownloader.service.TelegramClientService;
import com.tgdownloader.util.TelegramUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 收藏夹控制器
 */
@RestController
@RequestMapping("/api/saved-messages")
public class SavedMessagesController {

    private static final Logger log = LoggerFactory.getLogger(SavedMessagesController.class);
   @Autowired
   private SavedMessagesService savedMessages;

    @Autowired
    private DownloadCoreService downloadCore;
    @Autowired
    private TelegramClientService telegramClient;
    @Autowired
    private TelegramUtils telegramUtils;


    @PostMapping("/start-monitoring")
    public ApiResponse<String> start() {
        if (!savedMessages.getMonitoring().compareAndSet(false, true)) return ApiResponse.success("已在运行");
        log.info("收藏夹监听已启动");
        if (telegramClient.isConnected()) savedMessages.performInitScan();
        return ApiResponse.success("已启动");
    }

    // ==================== 扫描 ====================

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        int total = 0, running = 0, success = 0, failed = 0, paused = 0;
        for (DownloadTask t : savedMessages.getTaskRepo().findAll()) {
            if (t.getChatId() == null) continue;
            try {
                if (Long.parseLong(t.getChatId()) < 0) total++;
            } catch (Exception e) {
                continue;
            }
            if (DownloadStatus.DOWNLOADING.name().equals(t.getStatus())) running++;
            else if (DownloadStatus.SUCCESS_DOWNLOAD.name().equals(t.getStatus())) success++;
            else if (DownloadStatus.FAILED_DOWNLOAD.name().equals(t.getStatus())) failed++;
            else if (DownloadStatus.PAUSED.name().equals(t.getStatus())) paused++;
        }
        return ApiResponse.success(Map.of("total", total, "running", running, "success", success, "failed", failed, "paused", paused, "monitoring", savedMessages.getMonitoring().get()));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.success(Map.of("monitoring", savedMessages.getMonitoring().get(), "lastMessageId", 0L));
    }

    @PostMapping("/scan")
    @Transactional
    public ApiResponse<Map<String, Object>> scan(@RequestBody(required = false) Map<String, Object> body) {
        try {
            long fromId = 0;
            if (body != null && body.get("fromMessageId") != null)
                fromId = ((Number) body.get("fromMessageId")).longValue();
            int count = savedMessages.scanAll(fromId);
            return ApiResponse.success(Map.of("scanned", count));
        } catch (Exception e) {
            return ApiResponse.error("扫描失败: " + e.getMessage());
        }
    }



    // ==================== 任务控制 ====================

    @GetMapping("/tasks")
    public ApiResponse<Map<String, Object>> tasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<DownloadTask> saved = savedMessages.getTaskRepo().findAll().stream()
                .filter(t -> {
                    if (t.getChatId() == null) return false;
                    try {
                        return Long.parseLong(t.getChatId()) < 0;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
        int start = page * size;
        return ApiResponse.success(Map.of(
                "tasks", start < saved.size() ? saved.subList(start, Math.min(start + size, saved.size())) : Collections.emptyList(),
                "total", saved.size(), "page", page, "size", size));
    }

    @PostMapping("/tasks/{id}/pause")
    public ApiResponse<String> pause(@PathVariable Long id) {
        DownloadTask t = savedMessages.getTaskRepo().findById(id);
        if (t != null) {
            t.setIsStopTransmission(true);
            t.setStatus(DownloadStatus.PAUSED.name());
            t.setFinishedAt(java.time.LocalDateTime.now());
            telegramUtils.saveTask(t);
        }
        return ApiResponse.success("已暂停");
    }

    @PostMapping("/tasks/{id}/resume")
    public ApiResponse<String> resume(@PathVariable Long id) {
        DownloadTask t = savedMessages.getTaskRepo().findById(id);
        if (t != null) {
            t.setIsStopTransmission(false);
            t.setStatus(DownloadStatus.PENDING.name());
            t.setFinishedAt(null);
            downloadCore.startDownload(t);
        }
        return ApiResponse.success("已继续");
    }

    @DeleteMapping("/tasks/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        savedMessages.getTaskRepo().deleteById(id);
        return ApiResponse.success("已删除");
    }

    @DeleteMapping("/tasks/completed")
    public ApiResponse<String> clearDone() {
        List<Long> ids = savedMessages.getTaskRepo().findAll().stream()
                .filter(t -> {
                    try {
                        return Long.parseLong(t.getChatId()) < 0;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .filter(t -> "SUCCESS".equals(t.getStatus()) || "SKIP".equals(t.getStatus()) || "FAILED".equals(t.getStatus()))
                .map(DownloadTask::getId).toList();
        for (Long id : ids) {
            savedMessages.getTaskRepo().deleteById(id);
        }
        return ApiResponse.success("已清除 " + ids.size() + " 条");
    }


}
