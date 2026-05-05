package com.tgdownloader.entity;

import java.time.LocalDateTime;

/**
 * 聊天配置实体
 */
public class ChatConfig {

    private Long id;
    private String chatId;
    private String title;
    private String type;
    private Boolean enabled = true;
    private String filterType;
    private Integer autoDeleteAfterHours;
    private String fileNameTemplate;
    private String downloadPath;
    private String remotePath;
    private Boolean uploadAfterDownload = false;
    private Boolean notifyAfterDownload = true;
    private Long forwardToChatId;
    private String extraData;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Getter / Setter ─────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getFilterType() { return filterType; }
    public void setFilterType(String filterType) { this.filterType = filterType; }
    public Integer getAutoDeleteAfterHours() { return autoDeleteAfterHours; }
    public void setAutoDeleteAfterHours(Integer autoDeleteAfterHours) { this.autoDeleteAfterHours = autoDeleteAfterHours; }
    public String getFileNameTemplate() { return fileNameTemplate; }
    public void setFileNameTemplate(String fileNameTemplate) { this.fileNameTemplate = fileNameTemplate; }
    public String getDownloadPath() { return downloadPath; }
    public void setDownloadPath(String downloadPath) { this.downloadPath = downloadPath; }
    public String getRemotePath() { return remotePath; }
    public void setRemotePath(String remotePath) { this.remotePath = remotePath; }
    public Boolean getUploadAfterDownload() { return uploadAfterDownload; }
    public void setUploadAfterDownload(Boolean uploadAfterDownload) { this.uploadAfterDownload = uploadAfterDownload; }
    public Boolean getNotifyAfterDownload() { return notifyAfterDownload; }
    public void setNotifyAfterDownload(Boolean notifyAfterDownload) { this.notifyAfterDownload = notifyAfterDownload; }
    public Long getForwardToChatId() { return forwardToChatId; }
    public void setForwardToChatId(Long forwardToChatId) { this.forwardToChatId = forwardToChatId; }
    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override public String toString() { return "ChatConfig{id=" + id + ", chatId=" + chatId + ", title='" + title + "'}"; }
}
