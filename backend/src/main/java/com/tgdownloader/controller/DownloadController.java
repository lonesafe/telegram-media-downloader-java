package com.tgdownloader.controller;

import com.tgdownloader.dto.ApiResponse;
import com.tgdownloader.dto.DownloadProgressDto;
import com.tgdownloader.dto.DownloadTaskRequest;
import com.tgdownloader.entity.DownloadTask;
import com.tgdownloader.model.DownloadStatus;
import com.tgdownloader.repository.DownloadTaskRepository;
import com.tgdownloader.service.DownloadCoreService;
import com.tgdownloader.service.TelegramClientService;
import com.tgdownloader.util.ByteFormatUtil;
import com.tgdownloader.util.TelegramUtils;
import it.tdlight.jni.TdApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 涓嬭浇绠＄悊 Controller - 鏃?Lombok 鐗堟湰
 */
@RestController
@RequestMapping("/api/download")
public class DownloadController {

    @Autowired
    private DownloadTaskRepository downloadTaskRepository;
    @Autowired
    private DownloadCoreService downloadCoreService;
    @Autowired
    private TelegramClientService telegramService;

    private volatile boolean isDownloading = true;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getList(
            @RequestParam(defaultValue = "downloading") String alreadyDown,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        org.springframework.data.domain.Page<DownloadTask> taskPage;
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        taskPage = switch (alreadyDown) {
            case "downloading" ->
                // 正在下载：status = DOWNLOADING（在运行池中）
                    downloadTaskRepository.findByStatusIn(
                            List.of(DownloadStatus.DOWNLOADING.name()),
                            pageRequest);
            case "waiting" ->
                // 等待下载：PENDING + PAUSED
                    downloadTaskRepository.findByStatusIn(
                            List.of(DownloadStatus.PENDING.name(), DownloadStatus.PAUSED.name(), DownloadStatus.FAILED_DOWNLOAD.name(), DownloadStatus.SKIP_DOWNLOAD.name()),
                            pageRequest);
            case "completed" ->
                // 下载完成：SUCCESS + SKIP + FAILED
                    downloadTaskRepository.findByStatusIn(
                            List.of(DownloadStatus.SUCCESS_DOWNLOAD.name()),
                            pageRequest);
            default ->
                // all: 全部任务
                    downloadTaskRepository.findAll(pageRequest);
        };

        // 获取实时速度
        Map<Long, Long> taskSpeeds = downloadCoreService.getAllTaskSpeeds();

        List<Map<String, Object>> result = taskPage.getContent().stream()
                .map(task -> toMap(task, taskSpeeds.get(task.getId())))
                .collect(Collectors.toList());

        // 统计各选项卡数量
        long downloadingCount = downloadTaskRepository.countByStatusIn(List.of(DownloadStatus.DOWNLOADING.name()));
        long waitingCount = downloadTaskRepository.countByStatusIn(
                List.of(DownloadStatus.PENDING.name(), DownloadStatus.PAUSED.name()));
        long completedCount = downloadTaskRepository.countByStatusIn(
                List.of(DownloadStatus.SUCCESS_DOWNLOAD.name(), DownloadStatus.SKIP_DOWNLOAD.name(), DownloadStatus.FAILED_DOWNLOAD.name()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("list", result);
        response.put("total", taskPage.getTotalElements());
        response.put("page", page);
        response.put("size", size);
        response.put("pages", taskPage.getTotalPages());
        // 各选项卡计数
        response.put("downloadingCount", downloadingCount);
        response.put("waitingCount", waitingCount);
        response.put("completedCount", completedCount);

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
        speed.put("task_speeds", taskSpeeds); // 每个任务的实时速度

        return ApiResponse.success(speed);
    }

    // ===== 以下方法前端未使用，已注释 =====
    /*
    @PostMapping("/state")
    public ApiResponse<String> setState(@RequestParam String state) {
        isDownloading = "continue".equalsIgnoreCase(state);

        if (isDownloading) {
            downloadCoreService.resumeAll();
        } else {
            downloadCoreService.pauseAll();
        }

        return ApiResponse.success(isDownloading ? "continue" : "pause");
    }

    @PostMapping("/task")
    public ApiResponse<Map<String, Object>> createTask(@RequestBody DownloadTaskRequest request) throws Exception {
        TdApi.Message msg = telegramService.getMessageByLink(request.getChatLink());
        DownloadTask task = new DownloadTask();
        task.setChatId(String.valueOf(msg.chatId));
        task.setStatus(DownloadStatus.PENDING.name());
        task.setFileName(TelegramUtils.getFileName(msg));
        task.setMessageId(msg.id);
        task.setTelegramMessage(msg);

        DownloadTask saved = downloadTaskRepository.save(task);
        downloadCoreService.startDownload(saved);

        return ApiResponse.success(toMap(saved));
    }
    */

    @PostMapping("/stop/{taskId}")
    public ApiResponse<Void> stopTask(@PathVariable Long taskId) {
        DownloadTask task = downloadTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        task.setIsStopTransmission(true);
        task.setStatus(DownloadStatus.PAUSED.name());
        downloadTaskRepository.save(task);

        downloadCoreService.stopDownload(taskId);

        return ApiResponse.success();
    }

    @PostMapping("/resume/{taskId}")
    public ApiResponse<Void> resumeTask(@PathVariable Long taskId) {
        downloadCoreService.resumeTask(taskId);
        return ApiResponse.success();
    }

    @GetMapping("/status/{taskId}")
    public ApiResponse<Map<String, Object>> getStatus(@PathVariable Long taskId) {
        DownloadTask task = downloadTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        return ApiResponse.success(toMap(task));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> getAllTasks() {
        List<DownloadTask> tasks = downloadTaskRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        List<Map<String, Object>> result = tasks.stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    @DeleteMapping("/completed")
    public ApiResponse<Void> clearCompleted() {
        List<DownloadTask> completed = downloadTaskRepository.findByStatusIn(
                List.of(DownloadStatus.SUCCESS_DOWNLOAD.name(),
                        DownloadStatus.SKIP_DOWNLOAD.name(),
                        DownloadStatus.FAILED_DOWNLOAD.name())
        );
        downloadTaskRepository.deleteAll(completed);
        return ApiResponse.success();
    }

    @DeleteMapping("/task/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable Long taskId) {
        downloadTaskRepository.deleteById(taskId);
        return ApiResponse.success();
    }

    private Map<String, Object> toMap(DownloadTask task) {
        return toMap(task, null);
    }

    /**
     * 转换任务为 Map，包含实时速度
     *
     * @param task          任务
     * @param realtimeSpeed 实时速度（bytes/s），如果为 null 则使用数据库中的速度
     */
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

        // 优先使用实时速度，否则使用数据库中的速度
        long speed = realtimeSpeed != null ? realtimeSpeed
                : (task.getDownloadSpeed() != null ? task.getDownloadSpeed().longValue() : 0L);
        map.put("downloadSpeed", ByteFormatUtil.format(speed) + "/s");
        map.put("downloadSpeedRaw", speed); // 原始数值，供前端使用

        int progress = 0;
        if (task.getFileSize() != null && task.getFileSize() > 0
                && task.getDownloadedSize() != null) {
            progress = (int) (task.getDownloadedSize() * 100 / task.getFileSize());
        }
        map.put("downloadProgress", progress);
        map.put("status", task.getStatus());
        map.put("isRunning", DownloadStatus.DOWNLOADING.name().equals(task.getStatus())); // 实时计算
        map.put("isStopTransmission", task.getIsStopTransmission());
        map.put("createdAt", task.getCreatedAt());
        map.put("updatedAt", task.getUpdatedAt());

        return map;
    }
}
