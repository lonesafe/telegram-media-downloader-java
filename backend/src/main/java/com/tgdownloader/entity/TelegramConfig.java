package com.tgdownloader.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Telegram 全局配置实体
 * 注意：MyBatis XML resultMap 已定义所有列映射，这里的字段注解仅供 IDE 使用，
 * MyBatis 本身不读取这些注解。
 */
public class TelegramConfig {

    private Long id;
    private String configName = "default";
    private String apiId;
    private String apiHash;
    private String botToken;
    private String databaseDirectory;
    private String tempPath;
    private String savePath;
    private String filesDirectory;
    private Boolean useTestDc = false;
    private String languageCode;
    private Integer maxConcurrentTasks = 3;
    private String downloadTypes = "[\"video\"]";
    private Boolean proxyEnabled = false;
    private String proxyScheme;
    private String proxyHostname;
    private Integer proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    private Boolean enableUploadFile = false;
    private String uploadAdapter;
    private String remoteDir;
    private String rclonePath;
    private Boolean beforeUploadFileZip = false;
    private Boolean afterUploadFileDelete = false;
    private String downloadChatIds = "[]";
    private String lastReadMessageIds = "{}";
    private Boolean savedMessagesEnabled = true;
    private Long savedMessagesLastScanMessageId = 0L;
    private Boolean forwardListenerEnabled = true;
    private String forwardListenerSourceChatIds = "[]";
    private Long forwardListenerTargetChatId;
    private Long forwardListenerLastMessageId = 0L;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TelegramConfig() {}

    public TelegramConfig(String configName) {
        this.configName = configName;
    }

    // ── getters / setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigName() { return configName; }
    public void setConfigName(String v) { this.configName = v; }
    public String getApiId() { return apiId; }
    public void setApiId(String v) { this.apiId = v; }
    public String getApiHash() { return apiHash; }
    public void setApiHash(String v) { this.apiHash = v; }
    public String getBotToken() { return botToken; }
    public void setBotToken(String v) { this.botToken = v; }
    public String getDatabaseDirectory() { return databaseDirectory; }
    public void setDatabaseDirectory(String v) { this.databaseDirectory = v; }
    public String getTempPath() { return tempPath; }
    public void setTempPath(String v) { this.tempPath = v; }
    public String getSavePath() { return savePath; }
    public void setSavePath(String v) { this.savePath = v; }
    public String getFilesDirectory() { return filesDirectory; }
    public void setFilesDirectory(String v) { this.filesDirectory = v; }
    public Boolean getUseTestDc() { return useTestDc; }
    public void setUseTestDc(Boolean v) { this.useTestDc = v; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String v) { this.languageCode = v; }
    public Integer getMaxConcurrentTasks() { return maxConcurrentTasks; }
    public void setMaxConcurrentTasks(Integer v) { this.maxConcurrentTasks = v; }
    public String getDownloadTypes() { return downloadTypes; }
    public void setDownloadTypes(String v) { this.downloadTypes = v; }
    public Boolean getProxyEnabled() { return proxyEnabled; }
    public void setProxyEnabled(Boolean v) { this.proxyEnabled = v; }
    public String getProxyScheme() { return proxyScheme; }
    public void setProxyScheme(String v) { this.proxyScheme = v; }
    public String getProxyHostname() { return proxyHostname; }
    public void setProxyHostname(String v) { this.proxyHostname = v; }
    public Integer getProxyPort() { return proxyPort; }
    public void setProxyPort(Integer v) { this.proxyPort = v; }
    public String getProxyUsername() { return proxyUsername; }
    public void setProxyUsername(String v) { this.proxyUsername = v; }
    public String getProxyPassword() { return proxyPassword; }
    public void setProxyPassword(String v) { this.proxyPassword = v; }
    public Boolean getEnableUploadFile() { return enableUploadFile; }
    public void setEnableUploadFile(Boolean v) { this.enableUploadFile = v; }
    public String getUploadAdapter() { return uploadAdapter; }
    public void setUploadAdapter(String v) { this.uploadAdapter = v; }
    public String getRemoteDir() { return remoteDir; }
    public void setRemoteDir(String v) { this.remoteDir = v; }
    public String getRclonePath() { return rclonePath; }
    public void setRclonePath(String v) { this.rclonePath = v; }
    public Boolean getBeforeUploadFileZip() { return beforeUploadFileZip; }
    public void setBeforeUploadFileZip(Boolean v) { this.beforeUploadFileZip = v; }
    public Boolean getAfterUploadFileDelete() { return afterUploadFileDelete; }
    public void setAfterUploadFileDelete(Boolean v) { this.afterUploadFileDelete = v; }
    public String getDownloadChatIds() { return downloadChatIds; }
    public void setDownloadChatIds(String v) { this.downloadChatIds = v; }
    public String getLastReadMessageIds() { return lastReadMessageIds; }
    public void setLastReadMessageIds(String v) { this.lastReadMessageIds = v; }
    public Boolean getSavedMessagesEnabled() { return savedMessagesEnabled; }
    public void setSavedMessagesEnabled(Boolean v) { this.savedMessagesEnabled = v; }
    public Long getSavedMessagesLastScanMessageId() { return savedMessagesLastScanMessageId; }
    public void setSavedMessagesLastScanMessageId(Long v) { this.savedMessagesLastScanMessageId = v; }
    public Boolean getForwardListenerEnabled() { return forwardListenerEnabled; }
    public void setForwardListenerEnabled(Boolean v) { this.forwardListenerEnabled = v; }
    public String getForwardListenerSourceChatIds() { return forwardListenerSourceChatIds; }
    public void setForwardListenerSourceChatIds(String v) { this.forwardListenerSourceChatIds = v; }
    public Long getForwardListenerTargetChatId() { return forwardListenerTargetChatId; }
    public void setForwardListenerTargetChatId(Long v) { this.forwardListenerTargetChatId = v; }
    public Long getForwardListenerLastMessageId() { return forwardListenerLastMessageId; }
    public void setForwardListenerLastMessageId(Long v) { this.forwardListenerLastMessageId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    @Override public String toString() { return "TelegramConfig{id=" + id + ", configName='" + configName + "'}"; }
    @Override public boolean equals(Object o) { return this == o || (o != null && getClass() == o && id != null && Objects.equals(id, ((TelegramConfig) o).id)); }
    @Override public int hashCode() { return getClass().hashCode(); }
}
