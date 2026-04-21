package com.tgdownloader.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Telegram 配置实体
 * Lombok 版本: 移除，改用手写 getter/setter/builder
 * 解决 Spring Boot parent pom 强制 Lombok 版本导致的注解处理器问题
 */
@Entity
@Table(name = "telegram_config")
public class TelegramConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_name", unique = true, nullable = false)
    private String configName = "default";

    @Column(name = "api_id")
    private String apiId;
    @Column(name = "api_hash")
    private String apiHash;
    @Column(name = "bot_token")
    private String botToken;

    @Column(name = "database_directory")
    private String databaseDirectory;
    @Column(name = "temp_path")
    private String tempPath;
    @Column(name = "save_path")
    private String savePath;
    @Column(name = "files_directory")
    private String filesDirectory;

    @Column(name = "use_test_dc")
    private Boolean useTestDc = false;
    @Column(name = "language_code")
    private String languageCode;
    @Column(name = "max_concurrent_tasks")
    private Integer maxConcurrentTasks = 3;

    // 下载类型配置（JSON数组：video,audio,photo,document）
    @Column(name = "download_types", columnDefinition = "TEXT")
    private String downloadTypes = "[\"video\"]";

    @Column(name = "proxy_enabled")
    private Boolean proxyEnabled = false;
    @Column(name = "proxy_scheme")
    private String proxyScheme;
    @Column(name = "proxy_hostname")
    private String proxyHostname;
    @Column(name = "proxy_port")
    private Integer proxyPort;
    @Column(name = "proxy_username")
    private String proxyUsername;
    @Column(name = "proxy_password")
    private String proxyPassword;

    @Column(name = "enable_upload_file")
    private Boolean enableUploadFile = false;
    @Column(name = "upload_adapter")
    private String uploadAdapter;
    @Column(name = "remote_dir")
    private String remoteDir;
    @Column(name = "rclone_path")
    private String rclonePath;
    @Column(name = "before_upload_file_zip")
    private Boolean beforeUploadFileZip = false;
    @Column(name = "after_upload_file_delete")
    private Boolean afterUploadFileDelete = false;

    @Column(name = "download_chat_ids", columnDefinition = "TEXT")
    private String downloadChatIds = "[]";
    @Column(name = "last_read_message_ids", columnDefinition = "TEXT")
    private String lastReadMessageIds = "{}";

    // Saved Messages 扫描配置
    @Column(name = "saved_messages_enabled")
    private Boolean savedMessagesEnabled = true;
    @Column(name = "saved_messages_last_scan_message_id")
    private Long savedMessagesLastScanMessageId = 0L;

    // 转发监听配置
    @Column(name = "forward_listener_enabled")
    private Boolean forwardListenerEnabled = true;
    @Column(name = "forward_listener_source_chat_ids", columnDefinition = "TEXT")
    private String forwardListenerSourceChatIds = "[]";
    @Column(name = "forward_listener_target_chat_id")
    private Long forwardListenerTargetChatId;
    @Column(name = "forward_listener_last_message_id")
    private Long forwardListenerLastMessageId = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TelegramConfig() {}

    public TelegramConfig(String configName) { this.configName = configName; }

    // --- getters ---
    public Long getId() { return id; }
    public String getConfigName() { return configName; }
    public String getApiId() { return apiId; }
    public String getApiHash() { return apiHash; }
    public String getBotToken() { return botToken; }
    public String getDatabaseDirectory() { return databaseDirectory; }
    public String getTempPath() { return tempPath; }
    public String getSavePath() { return savePath; }
    public String getFilesDirectory() { return filesDirectory; }
    public Boolean getUseTestDc() { return useTestDc; }
    public String getLanguageCode() { return languageCode; }
    public Integer getMaxConcurrentTasks() { return maxConcurrentTasks; }
    public String getDownloadTypes() { return downloadTypes; }
    public Boolean getProxyEnabled() { return proxyEnabled; }
    public String getProxyScheme() { return proxyScheme; }
    public String getProxyHostname() { return proxyHostname; }
    public Integer getProxyPort() { return proxyPort; }
    public String getProxyUsername() { return proxyUsername; }
    public String getProxyPassword() { return proxyPassword; }
    public Boolean getEnableUploadFile() { return enableUploadFile; }
    public String getUploadAdapter() { return uploadAdapter; }
    public String getRemoteDir() { return remoteDir; }
    public String getRclonePath() { return rclonePath; }
    public Boolean getBeforeUploadFileZip() { return beforeUploadFileZip; }
    public Boolean getAfterUploadFileDelete() { return afterUploadFileDelete; }
    public String getDownloadChatIds() { return downloadChatIds; }
    public String getLastReadMessageIds() { return lastReadMessageIds; }
    public Boolean getSavedMessagesEnabled() { return savedMessagesEnabled; }
    public Long getSavedMessagesLastScanMessageId() { return savedMessagesLastScanMessageId; }
    public Boolean getForwardListenerEnabled() { return forwardListenerEnabled; }
    public String getForwardListenerSourceChatIds() { return forwardListenerSourceChatIds; }
    public Long getForwardListenerTargetChatId() { return forwardListenerTargetChatId; }
    public Long getForwardListenerLastMessageId() { return forwardListenerLastMessageId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // --- setters ---
    public void setId(Long id) { this.id = id; }
    public void setConfigName(String configName) { this.configName = configName; }
    public void setApiId(String apiId) { this.apiId = apiId; }
    public void setApiHash(String apiHash) { this.apiHash = apiHash; }
    public void setBotToken(String botToken) { this.botToken = botToken; }
    public void setDatabaseDirectory(String databaseDirectory) { this.databaseDirectory = databaseDirectory; }
    public void setTempPath(String tempPath) { this.tempPath = tempPath; }
    public void setSavePath(String savePath) { this.savePath = savePath; }
    public void setFilesDirectory(String filesDirectory) { this.filesDirectory = filesDirectory; }
    public void setUseTestDc(Boolean useTestDc) { this.useTestDc = useTestDc; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public void setMaxConcurrentTasks(Integer maxConcurrentTasks) { this.maxConcurrentTasks = maxConcurrentTasks; }
    public void setDownloadTypes(String downloadTypes) { this.downloadTypes = downloadTypes; }
    public void setProxyEnabled(Boolean proxyEnabled) { this.proxyEnabled = proxyEnabled; }
    public void setProxyScheme(String proxyScheme) { this.proxyScheme = proxyScheme; }
    public void setProxyHostname(String proxyHostname) { this.proxyHostname = proxyHostname; }
    public void setProxyPort(Integer proxyPort) { this.proxyPort = proxyPort; }
    public void setProxyUsername(String proxyUsername) { this.proxyUsername = proxyUsername; }
    public void setProxyPassword(String proxyPassword) { this.proxyPassword = proxyPassword; }
    public void setEnableUploadFile(Boolean enableUploadFile) { this.enableUploadFile = enableUploadFile; }
    public void setUploadAdapter(String uploadAdapter) { this.uploadAdapter = uploadAdapter; }
    public void setRemoteDir(String remoteDir) { this.remoteDir = remoteDir; }
    public void setRclonePath(String rclonePath) { this.rclonePath = rclonePath; }
    public void setBeforeUploadFileZip(Boolean beforeUploadFileZip) { this.beforeUploadFileZip = beforeUploadFileZip; }
    public void setAfterUploadFileDelete(Boolean afterUploadFileDelete) { this.afterUploadFileDelete = afterUploadFileDelete; }
    public void setDownloadChatIds(String downloadChatIds) { this.downloadChatIds = downloadChatIds; }
    public void setLastReadMessageIds(String lastReadMessageIds) { this.lastReadMessageIds = lastReadMessageIds; }
    public void setSavedMessagesEnabled(Boolean savedMessagesEnabled) { this.savedMessagesEnabled = savedMessagesEnabled; }
    public void setSavedMessagesLastScanMessageId(Long v) { this.savedMessagesLastScanMessageId = v; }
    public void setForwardListenerEnabled(Boolean forwardListenerEnabled) { this.forwardListenerEnabled = forwardListenerEnabled; }
    public void setForwardListenerSourceChatIds(String v) { this.forwardListenerSourceChatIds = v; }
    public void setForwardListenerTargetChatId(Long v) { this.forwardListenerTargetChatId = v; }
    public void setForwardListenerLastMessageId(Long v) { this.forwardListenerLastMessageId = v; }

    @Override public String toString() { return "TelegramConfig{id=" + id + ", configName='" + configName + "'}"; }
    @Override public boolean equals(Object o) { return this == o || (o != null && getClass() == o && id != null && Objects.equals(id, ((TelegramConfig) o).id)); }
    @Override public int hashCode() { return getClass().hashCode(); }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final TelegramConfig c = new TelegramConfig();
        public Builder id(Long v) { c.setId(v); return this; }
        public Builder configName(String v) { c.setConfigName(v); return this; }
        public Builder apiId(String v) { c.setApiId(v); return this; }
        public Builder apiHash(String v) { c.setApiHash(v); return this; }
        public Builder botToken(String v) { c.setBotToken(v); return this; }
        public Builder databaseDirectory(String v) { c.setDatabaseDirectory(v); return this; }
        public Builder tempPath(String v) { c.setTempPath(v); return this; }
        public Builder savePath(String v) { c.setSavePath(v); return this; }
        public Builder filesDirectory(String v) { c.setFilesDirectory(v); return this; }
        public Builder useTestDc(Boolean v) { c.setUseTestDc(v); return this; }
        public Builder languageCode(String v) { c.setLanguageCode(v); return this; }
        public Builder maxConcurrentTasks(Integer v) { c.setMaxConcurrentTasks(v); return this; }
        public Builder downloadTypes(String v) { c.setDownloadTypes(v); return this; }
        public Builder proxyEnabled(Boolean v) { c.setProxyEnabled(v); return this; }
        public Builder proxyScheme(String v) { c.setProxyScheme(v); return this; }
        public Builder proxyHostname(String v) { c.setProxyHostname(v); return this; }
        public Builder proxyPort(Integer v) { c.setProxyPort(v); return this; }
        public Builder proxyUsername(String v) { c.setProxyUsername(v); return this; }
        public Builder proxyPassword(String v) { c.setProxyPassword(v); return this; }
        public Builder enableUploadFile(Boolean v) { c.setEnableUploadFile(v); return this; }
        public Builder uploadAdapter(String v) { c.setUploadAdapter(v); return this; }
        public Builder remoteDir(String v) { c.setRemoteDir(v); return this; }
        public Builder rclonePath(String v) { c.setRclonePath(v); return this; }
        public Builder beforeUploadFileZip(Boolean v) { c.setBeforeUploadFileZip(v); return this; }
        public Builder afterUploadFileDelete(Boolean v) { c.setAfterUploadFileDelete(v); return this; }
        public Builder downloadChatIds(String v) { c.setDownloadChatIds(v); return this; }
        public Builder lastReadMessageIds(String v) { c.setLastReadMessageIds(v); return this; }
        public Builder savedMessagesEnabled(Boolean v) { c.setSavedMessagesEnabled(v); return this; }
        public Builder savedMessagesLastScanMessageId(Long v) { c.setSavedMessagesLastScanMessageId(v); return this; }
        public Builder forwardListenerEnabled(Boolean v) { c.setForwardListenerEnabled(v); return this; }
        public Builder forwardListenerSourceChatIds(String v) { c.setForwardListenerSourceChatIds(v); return this; }
        public Builder forwardListenerTargetChatId(Long v) { c.setForwardListenerTargetChatId(v); return this; }
        public Builder forwardListenerLastMessageId(Long v) { c.setForwardListenerLastMessageId(v); return this; }
        public TelegramConfig build() { return c; }
    }
}
