package com.tgdownloader.service;

import com.tgdownloader.dto.DownloadProgressDto;
import com.tgdownloader.entity.DownloadTask;
import com.tgdownloader.entity.TelegramConfig;
import com.tgdownloader.model.DownloadStatus;
import com.tgdownloader.repository.DownloadTaskRepository;
import com.tgdownloader.repository.TelegramConfigRepository;
import it.tdlight.jni.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 核心下载服务
 * 管理下载任务的生命周期：创建、执行、停止、暂停、恢复
 * 通过 TelegramClientService 调用 TDLib 下载文件
 */
@Service
public class DownloadCoreService {

    private static final Logger log = LoggerFactory.getLogger(DownloadCoreService.class);

    @Autowired
    private DownloadTaskRepository taskRepo;

    @Autowired
    private TelegramConfigRepository configRepo;

    @Autowired
    private TelegramClientService telegramClient;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final Map<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private final AtomicLong totalDownloaded = new AtomicLong(0);
    private final AtomicLong totalUploaded = new AtomicLong(0);
    private final AtomicLong downloadSpeed = new AtomicLong(0);
    private final AtomicLong uploadSpeed = new AtomicLong(0);

    // 实时速度记录（不存数据库，只在内存中维护）
    // key: taskId, value: [lastBytes, lastTime] 用于计算瞬时速度
    private final Map<Long, long[]> taskSpeedTracker = new ConcurrentHashMap<>();

    private long lastBytes = 0;
    private long lastTime = System.currentTimeMillis();

    /**
     * 获取所有任务的实时速度（不查数据库，从内存获取）
     *
     * @return Map: taskId -> speed (bytes/s)
     */
    public Map<Long, Long> getAllTaskSpeeds() {
        Map<Long, Long> result = new ConcurrentHashMap<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<Long, long[]> entry : taskSpeedTracker.entrySet()) {
            long taskId = entry.getKey();
            long[] data = entry.getValue(); // [lastBytes, lastTime]
            long elapsed = now - data[1];
            if (elapsed > 0 && elapsed < 5000) {
                // 速度 = 增量 / 时间差（简化计算，用上次记录的值作为速度基准）
                long speed = data[0];
                result.put(taskId, speed);
            }
        }

        return result;
    }

    /**
     * 提交下载任务到线程池执行
     */
    public void startDownload(DownloadTask task) {
        if (paused.get()) {
            log.info("下载已暂停，任务 {} 在队列中", task.getId());
            return;
        }

        task.setStartedAt(LocalDateTime.now());
        task.setStatus(DownloadStatus.PENDING.name());
        taskRepo.save(task);

        Future<?> future = executor.submit(() -> executeDownload(task));
        runningTasks.put(task.getId(), future);
        log.info("开始下载任务: {}, chatId={}, msgId={}", task.getId(), task.getChatId(), task.getMessageId());
    }

    /**
     * 手动停止指定任务（标记为 PAUSED，可恢复）
     */
    public void stopDownload(Long taskId) {
        Future<?> f = runningTasks.remove(taskId);
        if (f != null) f.cancel(true);

        taskRepo.findById(taskId).ifPresent(task -> {
            task.setIsStopTransmission(true);
            task.setStatus(DownloadStatus.PAUSED.name());
            task.setFinishedAt(LocalDateTime.now());
            taskRepo.save(task);
        });
        log.info("已暂停任务: {}", taskId);
    }

    /**
     * 恢复指定任务（清除手动停止标记，重新下载）
     */
    public void resumeTask(Long taskId) {
        taskRepo.findById(taskId).ifPresent(task -> {
            task.setIsStopTransmission(false);
            task.setStatus(DownloadStatus.PENDING.name());
            task.setFinishedAt(null);
            taskRepo.save(task);
            startDownload(task);
            log.info("已恢复任务: {}", taskId);
        });
    }

    /**
     * 暂停所有任务（阻止新任务启动）
     */
    public void pauseAll() {
        paused.set(true);
        log.info("已暂停所有下载");
    }

    /**
     * 恢复暂停的任务
     */
    public void resumeAll() {
        paused.set(false);
        taskRepo.findByStatus(DownloadStatus.DOWNLOADING.name())
                .forEach(task -> {
                    task.setStatus(DownloadStatus.PAUSED.name());
                    taskRepo.save(task);
                    startDownload(task);
                });
        log.info("已恢复所有下载");
    }

    /**
     * 停止全部任务
     */
    public void stopAll() {
        runningTasks.keySet().forEach(this::stopDownload);
        log.info("已停止所有任务");
    }

    /**
     * 全局下载进度
     */
    public DownloadProgressDto getGlobalProgress() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastTime;

        // 从实时速度追踪器汇总速度（更准确）
        long totalSpeed = 0;
        for (Map.Entry<Long, long[]> entry : taskSpeedTracker.entrySet()) {
            long[] data = entry.getValue(); // [speed, timestamp, downloadedSize]
            long taskElapsed = now - data[1];
            if (taskElapsed > 0 && taskElapsed < 5000 && data[0] > 0) {
                totalSpeed += data[0];
            }
        }
        downloadSpeed.set(totalSpeed);

        // 备用：如果追踪器没有数据，使用全局增量计算
        if (totalSpeed == 0 && elapsed >= 500) {
            long dl = totalDownloaded.get();
            long dlSpeed = (dl - lastBytes) * 1000 / Math.max(elapsed, 1);
            downloadSpeed.set(dlSpeed);
            lastBytes = dl;
            lastTime = now;
        }

        return new DownloadProgressDto(
                totalDownloaded.get(), totalUploaded.get(),
                downloadSpeed.get(), uploadSpeed.get(),
                runningTasks.size(), null, null, 0L, 0L, 0.0);
    }

    // ==================== 内部执行逻辑 ====================

    private void executeDownload(DownloadTask task) {
        long startTime = System.currentTimeMillis();
        try {
            task.setTotalTask(task.getTotalTask() + 1);
            task.setStatus(DownloadStatus.DOWNLOADING.name());
            task.setIsStopTransmission(false);
            taskRepo.save(task);

            // 检查用户客户端是否已连接
            if (!telegramClient.isConnected()) {
                log.warn("用户客户端未连接，跳过任务 {}", task.getId());
                task.setStatus(DownloadStatus.FAILED_DOWNLOAD.name());
                task.setFailedTask(task.getFailedTask() + 1);
                task.setFinishedAt(LocalDateTime.now());
                taskRepo.save(task);
                return;
            }

            // 优先使用已缓存的 Message（Bot 创建任务时传入），避免重复请求
            TdApi.Message msg = task.getTelegramMessage();
            // 设置 chatTitle（从 Message 获取）
            if (msg != null && (task.getChatTitle() == null || task.getChatTitle().isEmpty())) {
                try {
                    TdApi.Chat chat = telegramClient.getChatSync(msg.chatId);
                    if (chat != null) {
                        task.setChatTitle(chat.title);
                    }
                } catch (Exception e) {
                    log.warn("获取 chat 标题失败: chatId={}, error={}", msg.chatId, e.getMessage());
                }
            }

            // 下载并实时更新进度到 DownloadTask（节流：每 5% 或 2 秒更新一次数据库）
            long taskId = task.getId();
            long[] lastSave = {0L, 0L}; // [lastSavedDownloaded, lastSaveTimeMs]
            long[] speedData = {0L, 0L, 0L}; // [lastBytes, lastTime, lastSpeed] 用于计算瞬时速度
            TelegramClientService.DownloadResult result = telegramClient.downloadMessageFile(msg,
                    (downloadedSize, fileSize) -> {
                        try {
                            long now = System.currentTimeMillis();
                            long lastSaved = lastSave[0];
                            long lastTimeMs = lastSave[1];

                            // 更新实时速度追踪器
                            long delta = downloadedSize - speedData[0];
                            long elapsed = now - speedData[1];
                            long instantSpeed = speedData[2];
                            if (elapsed > 0 && delta > 0) {
                                instantSpeed = delta * 1000 / elapsed; // bytes/s
                                speedData[2] = instantSpeed;
                            }
                            speedData[0] = downloadedSize;
                            speedData[1] = now;
                            taskSpeedTracker.put(taskId, new long[]{instantSpeed, now, downloadedSize});

                            // 节流：进度变化 > 5% 或 距上次保存 > 2秒
                            boolean progressChanged = fileSize > 0 && (downloadedSize - lastSaved) * 100 / fileSize >= 5;
                            boolean timeExpired = now - lastTimeMs > 2000;
                            if (progressChanged || timeExpired || downloadedSize == fileSize) {
                                task.setDownloadedSize(downloadedSize);
                                task.setFileSize(fileSize);
                                taskRepo.save(task);
                                lastSave[0] = downloadedSize;
                                lastSave[1] = now;
                            }
                        } catch (Exception e) {
                            log.debug("更新进度失败 taskId={}: {}", taskId, e.getMessage());
                        }
                    });

            if (result == null) {
                log.info("消息无媒体文件，跳过: msgId={}", msg.id);
                task.setStatus(DownloadStatus.SKIP_DOWNLOAD.name());
                task.setSkipTask(task.getSkipTask() + 1);
                return;
            }

            // 下载成功，更新任务信息
            long elapsed = System.currentTimeMillis() - startTime;
            double speed = elapsed > 0 ? (result.fileSize() * 1000.0 / elapsed) : 0;

            String newFileName = result.fileName();
            String newLocalPath = result.localPath();

            // 获取保存路径配置
            String savePath = null;
            try {
                TelegramConfig cfg = configRepo.findByConfigName("default").orElse(null);
                if (cfg != null) {
                    savePath = cfg.getSavePath();
                    if (savePath == null || savePath.isEmpty()) {
                        savePath = cfg.getTempPath();
                    }
                    if (savePath == null || savePath.isEmpty()) {
                        savePath = cfg.getFilesDirectory();
                    }
                }
            } catch (Exception e) {
                log.warn("获取保存路径配置失败: {}", e.getMessage());
            }

            // 获取保存路径
            String targetDir = savePath != null && !savePath.isEmpty() ? savePath 
                    : result.localPath().substring(0, result.localPath().lastIndexOf(File.separator) + 1);
            File targetDirFile = new File(targetDir);
            if (!targetDirFile.exists()) {
                targetDirFile.mkdirs();
            }

            // 检查目标文件是否已存在（已存在则跳过）
            File targetFile = new File(targetDir + File.separator + newFileName);
            if (targetFile.exists()) {
                log.info("文件已存在，跳过: {}", targetFile.getAbsolutePath());
                task.setStatus(DownloadStatus.SKIP_DOWNLOAD.name());
                task.setSkipTask(task.getSkipTask() + 1);
                // 删除临时文件
                new File(result.localPath()).delete();
                return;
            }

            // 如果配置了独立的保存路径，将文件移动过去
            if (savePath != null && !savePath.isEmpty() && !savePath.equals(result.localPath().substring(0, result.localPath().lastIndexOf(File.separator) + 1))) {
                File tempFile = new File(result.localPath());
                if (tempFile.renameTo(targetFile)) {
                    newLocalPath = targetFile.getAbsolutePath();
                    log.info("文件移动到保存目录: {} → {}", result.localPath(), newLocalPath);
                } else {
                    log.warn("文件移动失败，使用临时路径: {}", result.localPath());
                    newLocalPath = result.localPath();
                }
            } else {
                // 同目录重命名
                File oldFile = new File(result.localPath());
                File newFile = new File(newLocalPath.substring(0, newLocalPath.lastIndexOf(File.separator) + 1) + newFileName);
                if (oldFile.renameTo(newFile)) {
                    newLocalPath = newFile.getAbsolutePath();
                    log.info("文件重命名: {} → {}", result.fileName(), newFileName);
                } else {
                    log.warn("文件重命名失败，使用原文件名: {}", result.localPath());
                }
            }

            task.setFileName(newFileName);
            task.setFileSize(result.fileSize());
            task.setDownloadedSize(result.fileSize());
            task.setLocalPath(newLocalPath);
            task.setDownloadSpeed(speed);
            task.setStatus(DownloadStatus.SUCCESS_DOWNLOAD.name());
            task.setSuccessTask(task.getSuccessTask() + 1);

            totalDownloaded.addAndGet(result.fileSize());
            log.info("下载完成: {} → {} ({})",
                    result.fileName(), result.localPath(), fmtSize(result.fileSize()));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 被 stopDownload 暂停的任务 isStopTransmission=true，保持 PAUSED 状态不覆盖
            if (!Boolean.TRUE.equals(task.getIsStopTransmission())) {
                task.setStatus(DownloadStatus.FAILED_DOWNLOAD.name());
                task.setFailedTask(task.getFailedTask() + 1);
            }
            log.info("任务 {} 被暂停", task.getId());
        } catch (Exception e) {
            log.error("任务 {} 下载失败: {}", task.getId(), e.getMessage(), e);
            task.setStatus(DownloadStatus.FAILED_DOWNLOAD.name());
            task.setFailedTask(task.getFailedTask() + 1);
        } finally {
            // 只有当 isStopTransmission=true（手动暂停）且 status 未达终态时，才不覆盖；其他情况 status 已由 try/catch 设置完毕，finally 不再覆盖
            // 注意：try 里的 return 不会跳过 finally，SUCCESS/SKIP 等终态已在 try 中设置好，finally 只补充 finishTime 和清理内存
            task.setFinishedAt(LocalDateTime.now());
            taskRepo.save(task);
            runningTasks.remove(task.getId());
            taskSpeedTracker.remove(task.getId()); // 清理速度追踪器
        }
    }

    private static String fmtSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
