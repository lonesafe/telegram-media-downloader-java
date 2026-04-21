package com.tgdownloader.dto;

/**
 * 下载进度数据传输对象 - 无 Lombok 版本
 */
public class DownloadProgressDto {

    private Long taskId;
    private String fileName;
    private Long totalSize;
    private Long downloadedSize;
    private Double progress;
    private Double speed;
    private String status;
    private Long successCount;
    private Long failedCount;
    private Long skipCount;

    private Long totalDownloaded;
    private Long totalUploaded;
    private Long downloadSpeed;
    private Long uploadSpeed;
    private Integer activeTasks;

    public DownloadProgressDto() {}

    public DownloadProgressDto(Long totalDownloaded, Long totalUploaded, Long downloadSpeed,
                               Long uploadSpeed, Integer activeTasks, Long taskId, Long totalSize,
                               Long downloadedSize, Long speed, Double progress) {
        this.totalDownloaded = totalDownloaded;
        this.totalUploaded = totalUploaded;
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.activeTasks = activeTasks;
        this.taskId = taskId;
        this.totalSize = totalSize;
        this.downloadedSize = downloadedSize;
        this.speed = speed != null ? speed.doubleValue() : null;
        this.progress = progress;
    }

    // --- getters ---
    public Long getTaskId() { return taskId; }
    public String getFileName() { return fileName; }
    public Long getTotalSize() { return totalSize; }
    public Long getDownloadedSize() { return downloadedSize; }
    public Double getProgress() { return progress; }
    public Double getSpeed() { return speed; }
    public String getStatus() { return status; }
    public Long getSuccessCount() { return successCount; }
    public Long getFailedCount() { return failedCount; }
    public Long getSkipCount() { return skipCount; }
    public Long getTotalDownloaded() { return totalDownloaded; }
    public Long getTotalUploaded() { return totalUploaded; }
    public Long getDownloadSpeed() { return downloadSpeed; }
    public Long getUploadSpeed() { return uploadSpeed; }
    public Integer getActiveTasks() { return activeTasks; }

    // --- setters ---
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
    public void setDownloadedSize(Long downloadedSize) { this.downloadedSize = downloadedSize; }
    public void setProgress(Double progress) { this.progress = progress; }
    public void setSpeed(Double speed) { this.speed = speed; }
    public void setStatus(String status) { this.status = status; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    public void setFailedCount(Long failedCount) { this.failedCount = failedCount; }
    public void setSkipCount(Long skipCount) { this.skipCount = skipCount; }
    public void setTotalDownloaded(Long totalDownloaded) { this.totalDownloaded = totalDownloaded; }
    public void setTotalUploaded(Long totalUploaded) { this.totalUploaded = totalUploaded; }
    public void setDownloadSpeed(Long downloadSpeed) { this.downloadSpeed = downloadSpeed; }
    public void setUploadSpeed(Long uploadSpeed) { this.uploadSpeed = uploadSpeed; }
    public void setActiveTasks(Integer activeTasks) { this.activeTasks = activeTasks; }

    public String getDownloadSpeedFormatted() {
        if (downloadSpeed == null) return "0 B/s";
        return formatSpeed(downloadSpeed);
    }

    public String getUploadSpeedFormatted() {
        if (uploadSpeed == null) return "0 B/s";
        return formatSpeed(uploadSpeed);
    }

    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        } else if (bytesPerSecond < 1024 * 1024 * 1024) {
            return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB/s", bytesPerSecond / (1024.0 * 1024 * 1024));
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
