package com.tgdownloader.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.tdlight.jni.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

/**
 * 下载任务实体
 */
public class DownloadTask {

    private static final Logger log = LoggerFactory.getLogger(DownloadTask.class);

    private Long id;
    private Long messageId;
    private String chatId;
    private String chatTitle;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String status = "PENDING";
    private String localPath;
    private String errorMessage;
    private String remoteUrl;
    private String uploadStatus;
    private String telegramFileId;
    private String telegramUniqueFileId;
    private Long downloadedSize;
    private byte[] messageData;
    private Long chatConfigId;
    private String extraData;
    private Boolean isStopTransmission = false;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer totalTask = 0;
    private Integer successTask = 0;
    private Integer failedTask = 0;
    private Integer skipTask = 0;
    private Double downloadSpeed = 0.0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── 内存缓存，不持久化 ─────────────────────────────────────────────────────
    @JsonIgnore
    private transient TdApi.Message cachedMessage;

    // ── Getter / Setter ─────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getChatTitle() { return chatTitle; }
    public void setChatTitle(String chatTitle) { this.chatTitle = chatTitle; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getRemoteUrl() { return remoteUrl; }
    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }
    public String getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }
    public String getTelegramFileId() { return telegramFileId; }
    public void setTelegramFileId(String telegramFileId) { this.telegramFileId = telegramFileId; }
    public String getTelegramUniqueFileId() { return telegramUniqueFileId; }
    public void setTelegramUniqueFileId(String telegramUniqueFileId) { this.telegramUniqueFileId = telegramUniqueFileId; }
    public Long getDownloadedSize() { return downloadedSize; }
    public void setDownloadedSize(Long downloadedSize) { this.downloadedSize = downloadedSize; }
    public byte[] getMessageData() { return messageData; }
    public void setMessageData(byte[] messageData) { this.messageData = messageData; }
    public Long getChatConfigId() { return chatConfigId; }
    public void setChatConfigId(Long chatConfigId) { this.chatConfigId = chatConfigId; }
    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
    public Boolean getIsStopTransmission() { return isStopTransmission; }
    public void setIsStopTransmission(Boolean isStopTransmission) { this.isStopTransmission = isStopTransmission; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Integer getTotalTask() { return totalTask; }
    public void setTotalTask(Integer totalTask) { this.totalTask = totalTask; }
    public Integer getSuccessTask() { return successTask; }
    public void setSuccessTask(Integer successTask) { this.successTask = successTask; }
    public Integer getFailedTask() { return failedTask; }
    public void setFailedTask(Integer failedTask) { this.failedTask = failedTask; }
    public Integer getSkipTask() { return skipTask; }
    public void setSkipTask(Integer skipTask) { this.skipTask = skipTask; }
    public Double getDownloadSpeed() { return downloadSpeed; }
    public void setDownloadSpeed(Double downloadSpeed) { this.downloadSpeed = downloadSpeed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public TdApi.Message getCachedMessage() { return cachedMessage; }
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
