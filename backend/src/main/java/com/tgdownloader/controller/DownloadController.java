package com.tgdownloader.controller;

import com.tgdownloader.dto.ApiResponse;
import com.tgdownloader.dto.DownloadProgressDto;
import com.tgdownloader.dto.DownloadTaskRequest;
import com.tgdownloader.entity.DownloadTask;
import com.tgdownloader.model.DownloadStatus;
import com.tgdownloader.mapper.DownloadTaskMapper;
import com.tgdownloader.service.DownloadCoreService;
import com.tgdownloader.service.TelegramClientService;
import com.tgdownloader.util.ByteFormatUtil;
import com.tgdownloader.util.TelegramUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 下载管理 Controller
 */
@RestController
@RequestMapping("/api/download")
public class DownloadController {

    private static final Logger log = LoggerFactory.getLogger(DownloadController.class);

    @Autowired
    private DownloadTaskMapper downloadTaskMapper;
    @Autowired
    private DownloadCoreService downloadCoreService;
    @Autowired
    private TelegramUtils telegramUtils;
    @Autowired
    private TelegramClientService telegramClientService;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getList(
            @RequestParam(defaultValue = "downloading") String tab,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<DownloadTask> tasks;
        long total;

        switch (tab) {
            case "downloading" -> {
                tasks = downloadTaskMapper.findByStatuses(List.of(DownloadStatus.DOWNLOADING.name()), page, size);
                total = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.DOWNLOADING.name()));
            }
            case "waiting" -> {
                tasks = downloadTaskMapper.findByStatuses(List.of(DownloadStatus.QUEUED.name(), DownloadStatus.PENDING.name()), page, size);
                total = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.QUEUED.name(), DownloadStatus.PENDING.name()));
            }
            case "paused" -> {
                tasks = downloadTaskMapper.findByStatuses(List.of(DownloadStatus.PAUSED.name()), page, size);
                total = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.PAUSED.name()));
            }
            case "skipped" -> {
                tasks = downloadTaskMapper.findByStatuses(List.of(DownloadStatus.SKIP_DOWNLOAD.name()), page, size);
                total = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.SKIP_DOWNLOAD.name()));
            }
            case "completed" -> {
                tasks = downloadTaskMapper.findByStatuses(List.of(DownloadStatus.SUCCESS_DOWNLOAD.name()), page, size);
                total = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.SUCCESS_DOWNLOAD.name()));
            }
            case "failed" -> {
                tasks = downloadTaskMapper.findByStatuses(List.of(DownloadStatus.FAILED_DOWNLOAD.name()), page, size);
                total = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.FAILED_DOWNLOAD.name()));
            }
            default -> {
                tasks = downloadTaskMapper.findAll();
                total = downloadTaskMapper.countAll();
            }
        }

        // 获取实时速度
        Map<Long, Long> taskSpeeds = downloadCoreService.getAllTaskSpeeds();

        List<Map<String, Object>> result = tasks.stream()
                .map(task -> toMap(task, taskSpeeds.get(task.getId())))
                .collect(Collectors.toList());

        // 统计各选项卡数量
        long downloadingCount = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.DOWNLOADING.name()));
        long waitingCount = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.QUEUED.name(), DownloadStatus.PENDING.name()));
        long pausedCount = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.PAUSED.name()));
        long skippedCount = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.SKIP_DOWNLOAD.name()));
        long completedCount = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.SUCCESS_DOWNLOAD.name()));
        long failedCount = downloadTaskMapper.countByStatuses(List.of(DownloadStatus.FAILED_DOWNLOAD.name()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("list", result);
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);
        response.put("pages", (total + size - 1) / size);
        response.put("downloadingCount", downloadingCount);
        response.put("waitingCount", waitingCount);
        response.put("pausedCount", pausedCount);
        response.put("skippedCount", skippedCount);
        response.put("completedCount", completedCount);
        response.put("failedCount", failedCount);

        return ApiResponse.success(response);
    }

    @GetMapping("/speed")
    public ApiResponse<Map<String, Object>> getSpeed() {
        DownloadProgressDto progress = downloadCoreService.getGlobalProgress();
        Map<Long, Long> taskSpeeds = downloadCoreService.getAllTaskSpeeds();

        Map<String, Object> speed = new LinkedHashMap<>();
        speed.put("download_speed", progress.getDownloadSpeed());
        speed.put("upload_speed", progress.getUploadSpeed());
        speed.put("total_downloaded", progress.getTotalDownloaded());
        speed.put("total_uploaded", progress.getTotalUploaded());
        speed.put("task_speeds", taskSpeeds);

        return ApiResponse.success(speed);
    }

    @PostMapping("/stop/{taskId}")
    public ApiResponse<Void> stopTask(@PathVariable Long taskId) {
        DownloadTask task = downloadTaskMapper.findById(taskId);
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        task.setIsStopTransmission(true);
        task.setStatus(DownloadStatus.PAUSED.name());
        telegramUtils.saveTask(task);

        downloadCoreService.stopDownload(taskId);

        return ApiResponse.success();
    }

    @PostMapping("/resume/{taskId}")
    public ApiResponse<Void> resumeTask(@PathVariable Long taskId) {
        DownloadTask task = downloadTaskMapper.findById(taskId);
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        task.setIsStopTransmission(false);
        task.setFinishedAt(null);
        task.setStatus(DownloadStatus.PENDING.name());
        telegramUtils.saveTask(task);

        downloadCoreService.startDownload(task);
        return ApiResponse.success();
    }

    @GetMapping("/status/{taskId}")
    public ApiResponse<Map<String, Object>> getStatus(@PathVariable Long taskId) {
        DownloadTask task = downloadTaskMapper.findById(taskId);
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        return ApiResponse.success(toMap(task));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> getAllTasks() {
        List<DownloadTask> tasks = downloadTaskMapper.findAll();

        List<Map<String, Object>> result = tasks.stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    @DeleteMapping("/completed")
    public ApiResponse<Void> clearCompleted() {
        List<DownloadTask> completed = downloadTaskMapper.findByStatuses(
                List.of(DownloadStatus.SUCCESS_DOWNLOAD.name(),
                        DownloadStatus.SKIP_DOWNLOAD.name(),
                        DownloadStatus.FAILED_DOWNLOAD.name()));
        for (DownloadTask t : completed) {
            downloadTaskMapper.deleteById(t.getId());
        }
        return ApiResponse.success();
    }

    @PostMapping("/state")
    public ApiResponse<Void> setState(@RequestParam String state) {
        try {
            if ("pause".equals(state)) {
                downloadCoreService.pauseAll();
            } else if ("continue".equals(state)) {
                downloadCoreService.resumeAll();
            }
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("设置下载状态失败: {}", e.getMessage());
            return ApiResponse.error("设置下载状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/task")
    public ApiResponse<Map<String, Object>> createTask(@RequestBody DownloadTaskRequest request) {
        try {
            String chatLink = request.getChatLink();
            if (chatLink == null || chatLink.isEmpty()) {
                return ApiResponse.error("chatLink 不能为空");
            }
            
            Map<String, Object> parsed = telegramClientService.parseMessageLink(chatLink);
            long chatId = (long) parsed.get("chatId");
            long messageId = (long) parsed.get("messageId");
            
            DownloadTask task = new DownloadTask();
            task.setChatId(String.valueOf(chatId));
            task.setMessageId(messageId);
            task.setStatus("PENDING");
            task.setCreatedAt(java.time.LocalDateTime.now());
            task = downloadTaskMapper.save(task);
            
            downloadCoreService.startDownload(task);
            return ApiResponse.success(Map.of("taskId", task.getId(), "message", "任务已创建"));
        } catch (Exception e) {
            log.error("创建下载任务失败: {}", e.getMessage());
            return ApiResponse.error("创建下载任务失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/task/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable Long taskId) {
        downloadTaskMapper.deleteById(taskId);
        return ApiResponse.success();
    }

    private Map<String, Object> toMap(DownloadTask task) {
        return toMap(task, null);
    }

    private Map<String, Object> toMap(DownloadTask task, Long realtimeSpeed) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.getId());
        map.put("messageId", task.getMessageId());
        map.put("chatId", task.getChatId());
        map.put("chatTitle", task.getChatTitle());
        map.put("fileName", task.getFileName());
        map.put("filePath", task.getLocalPath());
        map.put("savePath", task.getLocalPath());
        map.put("fileSize", task.getFileSize());
        map.put("downloadedSize", task.getDownloadedSize());

        long speed = realtimeSpeed != null ? realtimeSpeed
                : (task.getDownloadSpeed() != null ? task.getDownloadSpeed().longValue() : 0L);
        map.put("downloadSpeed", ByteFormatUtil.format(speed) + "/s");
        map.put("downloadSpeedRaw", speed);

        int progress = 0;
        if (task.getFileSize() != null && task.getFileSize() > 0
                && task.getDownloadedSize() != null) {
            progress = (int) (task.getDownloadedSize() * 100 / task.getFileSize());
        }
        map.put("downloadProgress", progress);
        map.put("status", task.getStatus());
        map.put("isRunning", DownloadStatus.DOWNLOADING.name().equals(task.getStatus()));
        map.put("isStopTransmission", task.getIsStopTransmission());
        map.put("createdAt", task.getCreatedAt());
        map.put("updatedAt", task.getUpdatedAt());

        return map;
    }
}
