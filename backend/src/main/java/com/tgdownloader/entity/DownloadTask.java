package com.tgdownloader.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.tdlight.jni.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

/**
 * 下载任务实体类
 * <p>
 * 表示一条 Telegram 媒体文件下载任务，包含任务的完整信息：
 * <ul>
 *   <li>消息信息（messageId, chatId, chatTitle）</li>
 *   <li>文件信息（fileName, fileSize, mimeType）</li>
 *   <li>下载状态（status, downloadedSize, downloadSpeed）</li>
 *   <li>上传状态（uploadStatus, remoteUrl）</li>
 *   <li>时间信息（startedAt, finishedAt, createdAt, updatedAt）</li>
 *   <li>任务统计（totalTask, successTask, failedTask, skipTask）</li>
 * </ul>
 * </p>
 * <p>
 * 使用 Kryo 序列化存储 TdApi.Message 对象（messageData 字段）。
 * </p>
 * 
 * @author Telegram Media Downloader
 * @version 1.0.0
 * @since 2024
 */
public class DownloadTask {

    /** 日志对象 */
    private static final Logger log = LoggerFactory.getLogger(DownloadTask.class);

    /** 主键 ID（自增） */
    private Long id;
    /** Telegram 消息 ID */
    private Long messageId;
    /** Telegram 聊天 ID（字符串形式，支持超长 ID） */
    private String chatId;
    /** 聊天标题（用于显示） */
    private String chatTitle;
    /** 文件名 */
    private String fileName;
    /** 文件大小（字节） */
    private Long fileSize;
    /** MIME 类型（例如 video/mp4, image/jpeg） */
    private String mimeType;
    /** 下载状态（PENDING, DOWNLOADING, SUCCESS_DOWNLOAD, FAILED_DOWNLOAD 等） */
    private String status = "PENDING";
    /** 本地文件路径（下载完成后填充） */
    private String localPath;
    /** 错误信息（下载失败时填充） */
    private String errorMessage;
    /** 远程 URL（上传到云盘后填充） */
    private String remoteUrl;
    /** 上传状态（PENDING, UPLOADING, SUCCESS_UPLOAD, FAILED_UPLOAD 等） */
    private String uploadStatus;
    /** Telegram 文件 ID（用于断点续传） */
    private String telegramFileId;
    /** Telegram 文件唯一 ID */
    private String telegramUniqueFileId;
    /** 已下载字节数 */
    private Long downloadedSize;
    /** TdApi.Message 的 Kryo 序列化数据（字节数组） */
    private byte[] messageData;
    /** 聊天配置 ID（关联 ChatConfig） */
    private Long chatConfigId;
    /** 扩展数据（JSON 格式，用于存储额外信息） */
    private String extraData;
    /** 是否停止传输（用于暂停任务） */
    private Boolean isStopTransmission = false;
    /** 任务开始时间 */
    private LocalDateTime startedAt;
    /** 任务完成时间 */
    private LocalDateTime finishedAt;
    /** 总任务数（用于批量下载） */
    private Integer totalTask = 0;
    /** 成功任务数 */
    private Integer successTask = 0;
    /** 失败任务数 */
    private Integer failedTask = 0;
    /** 跳过任务数 */
    private Integer skipTask = 0;
    /** 下载速度（字节/秒） */
    private Double downloadSpeed = 0.0;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    // ── 内存缓存（不持久化到数据库）─────────────────────────────────────────────────────
    /** 缓存的 TdApi.Message 对象（transient，不序列化到 JSON） */
    @JsonIgnore
    private transient TdApi.Message cachedMessage;

    // ── Getter / Setter 方法 ─────────────────────────────────────────────────────
    /** @return 主键 ID */
    public Long getId() { return id; }
    /** @param id 主键 ID */
    public void setId(Long id) { this.id = id; }
    /** @return Telegram 消息 ID */
    public Long getMessageId() { return messageId; }
    /** @param messageId Telegram 消息 ID */
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    /** @return 聊天 ID（字符串形式） */
    public String getChatId() { return chatId; }
    /** @param chatId 聊天 ID */
    public void setChatId(String chatId) { this.chatId = chatId; }
    /** @return 聊天标题 */
    public String getChatTitle() { return chatTitle; }
    /** @param chatTitle 聊天标题 */
    public void setChatTitle(String chatTitle) { this.chatTitle = chatTitle; }
    /** @return 文件名 */
    public String getFileName() { return fileName; }
    /** @param fileName 文件名 */
    public void setFileName(String fileName) { this.fileName = fileName; }
    /** @return 文件大小（字节） */
    public Long getFileSize() { return fileSize; }
    /** @param fileSize 文件大小（字节） */
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    /** @return MIME 类型 */
    public String getMimeType() { return mimeType; }
    /** @param mimeType MIME 类型 */
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    /** @return 下载状态 */
    public String getStatus() { return status; }
    /** @param status 下载状态 */
    public void setStatus(String status) { this.status = status; }
    /** @return 本地文件路径 */
    public String getLocalPath() { return localPath; }
    /** @param localPath 本地文件路径 */
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    /** @return 错误信息 */
    public String getErrorMessage() { return errorMessage; }
    /** @param errorMessage 错误信息 */
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    /** @return 远程 URL（云盘链接） */
    public String getRemoteUrl() { return remoteUrl; }
    /** @param remoteUrl 远程 URL */
    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }
    /** @return 上传状态 */
    public String getUploadStatus() { return uploadStatus; }
    /** @param uploadStatus 上传状态 */
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }
    /** @return Telegram 文件 ID */
    public String getTelegramFileId() { return telegramFileId; }
    /** @param telegramFileId Telegram 文件 ID */
    public void setTelegramFileId(String telegramFileId) { this.telegramFileId = telegramFileId; }
    /** @return Telegram 文件唯一 ID */
    public String getTelegramUniqueFileId() { return telegramUniqueFileId; }
    /** @param telegramUniqueFileId Telegram 文件唯一 ID */
    public void setTelegramUniqueFileId(String telegramUniqueFileId) { this.telegramUniqueFileId = telegramUniqueFileId; }
    /** @return 已下载字节数 */
    public Long getDownloadedSize() { return downloadedSize; }
    /** @param downloadedSize 已下载字节数 */
    public void setDownloadedSize(Long downloadedSize) { this.downloadedSize = downloadedSize; }
    /** @return TdApi.Message 的 Kryo 序列化数据 */
    public byte[] getMessageData() { return messageData; }
    /** @param messageData Kryo 序列化数据 */
    public void setMessageData(byte[] messageData) { this.messageData = messageData; }
    /** @return 聊天配置 ID */
    public Long getChatConfigId() { return chatConfigId; }
    /** @param chatConfigId 聊天配置 ID */
    public void setChatConfigId(Long chatConfigId) { this.chatConfigId = chatConfigId; }
    /** @return 扩展数据（JSON 格式） */
    public String getExtraData() { return extraData; }
    /** @param extraData 扩展数据 */
    public void setExtraData(String extraData) { this.extraData = extraData; }
    /** @return 是否停止传输 */
    public Boolean getIsStopTransmission() { return isStopTransmission; }
    /** @param isStopTransmission 是否停止传输 */
    public void setIsStopTransmission(Boolean isStopTransmission) { this.isStopTransmission = isStopTransmission; }
    /** @return 任务开始时间 */
    public LocalDateTime getStartedAt() { return startedAt; }
    /** @param startedAt 任务开始时间 */
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    /** @return 任务完成时间 */
    public LocalDateTime getFinishedAt() { return finishedAt; }
    /** @param finishedAt 任务完成时间 */
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    /** @return 总任务数 */
    public Integer getTotalTask() { return totalTask; }
    /** @param totalTask 总任务数 */
    public void setTotalTask(Integer totalTask) { this.totalTask = totalTask; }
    /** @return 成功任务数 */
    public Integer getSuccessTask() { return successTask; }
    /** @param successTask 成功任务数 */
    public void setSuccessTask(Integer successTask) { this.successTask = successTask; }
    /** @return 失败任务数 */
    public Integer getFailedTask() { return failedTask; }
    /** @param failedTask 失败任务数 */
    public void setFailedTask(Integer failedTask) { this.failedTask = failedTask; }
    /** @return 跳过任务数 */
    public Integer getSkipTask() { return skipTask; }
    /** @param skipTask 跳过任务数 */
    public void setSkipTask(Integer skipTask) { this.skipTask = skipTask; }
    /** @return 下载速度（字节/秒） */
    public Double getDownloadSpeed() { return downloadSpeed; }
    /** @param downloadSpeed 下载速度 */
    public void setDownloadSpeed(Double downloadSpeed) { this.downloadSpeed = downloadSpeed; }
    /** @return 创建时间 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @param createdAt 创建时间 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    /** @return 更新时间 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** @param updatedAt 更新时间 */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    /** @return 缓存的 TdApi.Message 对象 */
    public TdApi.Message getCachedMessage() { return cachedMessage; }
    /** @param cachedMessage 缓存的 TdApi.Message 对象 */
    public void setCachedMessage(TdApi.Message cachedMessage) { this.cachedMessage = cachedMessage; }

    // ── TdApi.Message Kryo 序列化 ─────────────────────────────────────────────
    private static final ThreadLocal<com.esotericsoftware.kryo.Kryo> kryoLocal = ThreadLocal.withInitial(() -> {
        com.esotericsoftware.kryo.Kryo kryo = new com.esotericsoftware.kryo.Kryo();
        kryo.setRegistrationRequired(false);
        return kryo;
    });

    public void setTelegramMessage(TdApi.Message msg) {
        this.cachedMessage = msg;
        if (msg != null) {
            this.messageData = serialize(msg);
        } else {
            this.messageData = null;
        }
    }

    public TdApi.Message getTelegramMessage() {
        if (cachedMessage != null) return cachedMessage;
        if (messageData == null) return null;
        try {
            cachedMessage = deserialize(messageData, TdApi.Message.class);
        } catch (Exception e) {
            log.error("deserialize TdApi.Message error", e);
        }
        return cachedMessage;
    }

    private static byte[] serialize(Object obj) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             com.esotericsoftware.kryo.io.Output output = new com.esotericsoftware.kryo.io.Output(baos)) {
            kryoLocal.get().writeObject(output, obj);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Kryo序列化失败", e);
        }
    }

    private static <T> T deserialize(byte[] data, Class<T> clazz) {
        if (data == null) return null;
        try (com.esotericsoftware.kryo.io.Input input = new com.esotericsoftware.kryo.io.Input(data)) {
            return kryoLocal.get().readObject(input, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Kryo反序列化失败", e);
        }
    }

    @Override public String toString() { return "DownloadTask{id=" + id + ", chatId=" + chatId + ", status=" + status + "}"; }
}
