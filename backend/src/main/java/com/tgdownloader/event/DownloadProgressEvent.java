package com.tgdownloader.event;

import org.springframework.context.ApplicationEvent;

/**
 * 下载进度事件
 * <p>
 * 用于在特定下载状态变化时发布事件，通知相关监听器（如 Bot 消息更新）。
 * </p>
 * 
 * @author Telegram Media Downloader
 * @version 1.0.0
 * @since 2024
 */
public class DownloadProgressEvent extends ApplicationEvent {

    /** 任务 ID */
    private final Long taskId;
    
    /** 事件类型（SUBMITTED, PROGRESS, COMPLETED, FAILED） */
    private final String eventType;
    
    /** 进度百分比（0-100，仅 PROGRESS 事件有效） */
    private final int progress;
    
    /** 已下载字节数 */
    private final long downloadedSize;
    
    /** 文件总字节数 */
    private final long fileSize;
    
    /** 下载速度（字节/秒） */
    private final double speed;
    
    /** 状态信息 */
    private final String status;
    
    /** Bot 聊天 ID（用于发送消息） */
    private Long botChatId;
    
    /** Bot 消息 ID（用于编辑消息） */
    private Long botMessageId;

    /**
     * 构造函数
     * 
     * @param source 事件源
     * @param taskId 任务 ID
     * @param eventType 事件类型
     * @param progress 进度百分比
     * @param downloadedSize 已下载字节数
     * @param fileSize 文件总字节数
     * @param speed 下载速度
     * @param status 状态信息
     */
    public DownloadProgressEvent(Object source, Long taskId, String eventType, 
                               int progress, long downloadedSize, long fileSize, 
                               double speed, String status) {
        super(source);
        this.taskId = taskId;
        this.eventType = eventType;
        this.progress = progress;
        this.downloadedSize = downloadedSize;
        this.fileSize = fileSize;
        this.speed = speed;
        this.status = status;
    }

    // ==================== Getter 方法 ====================

    public Long getTaskId() {
        return taskId;
    }

    public String getEventType() {
        return eventType;
    }

    public int getProgress() {
        return progress;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public double getSpeed() {
        return speed;
    }

    public String getStatus() {
        return status;
    }

    public Long getBotChatId() {
        return botChatId;
    }

    public void setBotChatId(Long botChatId) {
        this.botChatId = botChatId;
    }

    public Long getBotMessageId() {
        return botMessageId;
    }

    public void setBotMessageId(Long botMessageId) {
        this.botMessageId = botMessageId;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建任务提交事件
     */
    public static DownloadProgressEvent submitted(Long taskId) {
        return new DownloadProgressEvent("DownloadCoreService", taskId, "SUBMITTED", 0, 0, 0, 0.0, "PENDING");
    }

    /**
     * 创建进度更新事件
     */
    public static DownloadProgressEvent progress(Long taskId, int progress, long downloadedSize, long fileSize, double speed) {
        return new DownloadProgressEvent("DownloadCoreService", taskId, "PROGRESS", progress, downloadedSize, fileSize, speed, "DOWNLOADING");
    }

    /**
     * 创建下载完成事件
     */
    public static DownloadProgressEvent completed(Long taskId, long fileSize) {
        return new DownloadProgressEvent("DownloadCoreService", taskId, "COMPLETED", 100, fileSize, fileSize, 0.0, "SUCCESS_DOWNLOAD");
    }

    /**
     * 创建下载失败事件
     */
    public static DownloadProgressEvent failed(Long taskId, String error) {
        return new DownloadProgressEvent("DownloadCoreService", taskId, "FAILED", 0, 0, 0, 0.0, "FAILED_DOWNLOAD: " + error);
    }

    /**
     * 创建下载跳过事件
     */
    public static DownloadProgressEvent skipped(Long taskId) {
        return new DownloadProgressEvent("DownloadCoreService", taskId, "SKIPPED", 0, 0, 0, 0.0, "SKIP_DOWNLOAD");
    }

    // ==================== 工具方法 ====================

    /**
     * 格式化文件大小
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024));
        return String.format("%.1fGB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 生成进度条
     * 
     * @param progress 进度百分比（0-100）
     * @return 进度条字符串
     */
    public static String generateProgressBar(int progress) {
        int barLength = 10;
        int filled = (int) (progress / 100.0 * barLength);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "▓" : "░");
        }
        return bar.toString();
    }

    /**
     * 生成任务提交消息
     */
    public String buildSubmitMessage(int totalTask, int successTask, int failedTask, int skipTask) {
        return "任务提交：\n" +
               "🆔 task id: " + taskId + "\n" +
               "📥 下载: " + formatSize(0) + "\n" +
               "├─ 📁 总数: " + totalTask + "\n" +
               "├─ ✅ 成功: " + successTask + "\n" +
               "├─ ❌ 失败: " + failedTask + "\n" +
               "└─ ⏩ 跳过: " + skipTask;
    }

    /**
     * 生成下载进度消息
     * 
     * @param messageId Telegram 消息 ID
     * @param fileName 文件名
     */
    public String buildProgressMessage(long messageId, String fileName) {
        return "下载中：\n" +
               "🆔 task id: " + taskId + "\n" +
               "📥 下载: " + formatSize(downloadedSize) + "\n" +
               "├─ 📁 总数: 1\n" +
               "├─ ✅ 成功: 0\n" +
               "├─ ❌ 失败: 0\n" +
               "└─ ⏩ 跳过: 0\n\n" +
               "📥 下载进度:\n" +
               " ├─ 🆔 消息ID: " + messageId + "\n" +
               " │ ├─ 📁 : " + messageId + " " + fileName + "\n" +
               " │ ├─ 📏 : " + formatSize(fileSize) + "\n" +
               " │ ├─ ⚡ : " + formatSize((long) speed) + "/s\n" +
               " │ └─ 📊 : [" + generateProgressBar(progress) + "] (" + progress + "%)";
    }

    /**
     * 生成下载完成消息
     */
    public String buildCompleteMessage(int totalTask, int successTask, int failedTask, int skipTask) {
        return "下载完成：\n" +
               "🆔 task id: " + taskId + "\n" +
               "📥 下载: " + formatSize(fileSize) + "\n" +
               "├─ 📁 总数: " + totalTask + "\n" +
               "├─ ✅ 成功: " + successTask + "\n" +
               "├─ ❌ 失败: " + failedTask + "\n" +
               "└─ ⏩ 跳过: " + skipTask;
    }
}
