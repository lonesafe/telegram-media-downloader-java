package com.tgdownloader.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Telegram 全局配置实体
 */
@Table("telegram_config")
public class TelegramConfig {

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("config_name")
    private String configName = "default";

    public TelegramConfig() {}

    public TelegramConfig(String configName) {
        this.configName = configName;
    }

    @Column("api_id")
    private String apiId;

    @Column("api_hash")
    private String apiHash;

    @Column("bot_token")
    private String botToken;

    @Column("database_directory")
    private String databaseDirectory;

    @Column("temp_path")
    private String tempPath;

    @Column("save_path")
    private String savePath;

    @Column("files_directory")
    private String filesDirectory;

    @Column("use_test_dc")
    private Boolean useTestDc = false;

    @Column("language_code")
    private String languageCode;

    @Column("max_concurrent_tasks")
    private Integer maxConcurrentTasks = 3;

    @Column("download_types")
    private String downloadTypes = "[\"video\"]";

    @Column("proxy_enabled")
    private Boolean proxyEnabled = false;

    @Column("proxy_scheme")
    private String proxyScheme;

    @Column("proxy_hostname")
    private String proxyHostname;

    @Column("proxy_port")
    private Integer proxyPort;

    @Column("proxy_username")
    private String proxyUsername;

    @Column("proxy_password")
    private String proxyPassword;

    @Column("enable_upload_file")
    private Boolean enableUploadFile = false;

    @Column("upload_adapter")
    private String uploadAdapter;

    @Column("remote_dir")
    private String remoteDir;

    @Column("rclone_path")
    private String rclonePath;

    @Column("before_upload_file_zip")
    private Boolean beforeUploadFileZip = false;

    @Column("after_upload_file_delete")
    private Boolean afterUploadFileDelete = false;

    @Column("download_chat_ids")
    private String downloadChatIds = "[]";

    @Column("last_read_message_ids")
    private String lastReadMessageIds = "{}";

    @Column("saved_messages_enabled")
    private Boolean savedMessagesEnabled = true;

    @Column("saved_messages_last_scan_message_id")
    private Long savedMessagesLastScanMessageId = 0L;

    @Column("forward_listener_enabled")
    private Boolean forwardListenerEnabled = true;

    @Column("forward_listener_source_chat_ids")
    private String forwardListenerSourceChatIds = "[]";

    @Column("forward_listener_target_chat_id")
    private Long forwardListenerTargetChatId;

    @Column("forward_listener_last_message_id")
    private Long forwardListenerLastMessageId = 0L;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    // ── getters / setters ────────────────────────────────────────────────────
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

    public void setId(Long id) { this.id = id; }
    public void setConfigName(String v) { this.configName = v; }
    public void setApiId(String v) { this.apiId = v; }
    public void setApiHash(String v) { this.apiHash = v; }
    public void setBotToken(String v) { this.botToken = v; }
    public void setDatabaseDirectory(String v) { this.databaseDirectory = v; }
    public void setTempPath(String v) { this.tempPath = v; }
    public void setSavePath(String v) { this.savePath = v; }
    public void setFilesDirectory(String v) { this.filesDirectory = v; }
    public void setUseTestDc(Boolean v) { this.useTestDc = v; }
    public void setLanguageCode(String v) { this.languageCode = v; }
    public void setMaxConcurrentTasks(Integer v) { this.maxConcurrentTasks = v; }
    public void setDownloadTypes(String v) { this.downloadTypes = v; }
    public void setProxyEnabled(Boolean v) { this.proxyEnabled = v; }
    public void setProxyScheme(String v) { this.proxyScheme = v; }
    public void setProxyHostname(String v) { this.proxyHostname = v; }
    public void setProxyPort(Integer v) { this.proxyPort = v; }
    public void setProxyUsername(String v) { this.proxyUsername = v; }
    public void setProxyPassword(String v) { this.proxyPassword = v; }
    public void setEnableUploadFile(Boolean v) { this.enableUploadFile = v; }
    public void setUploadAdapter(String v) { this.uploadAdapter = v; }
    public void setRemoteDir(String v) { this.remoteDir = v; }
    public void setRclonePath(String v) { this.rclonePath = v; }
    public void setBeforeUploadFileZip(Boolean v) { this.beforeUploadFileZip = v; }
    public void setAfterUploadFileDelete(Boolean v) { this.afterUploadFileDelete = v; }
    public void setDownloadChatIds(String v) { this.downloadChatIds = v; }
    public void setLastReadMessageIds(String v) { this.lastReadMessageIds = v; }
    public void setSavedMessagesEnabled(Boolean v) { this.savedMessagesEnabled = v; }
    public void setSavedMessagesLastScanMessageId(Long v) { this.savedMessagesLastScanMessageId = v; }
    public void setForwardListenerEnabled(Boolean v) { this.forwardListenerEnabled = v; }
    public void setForwardListenerSourceChatIds(String v) { this.forwardListenerSourceChatIds = v; }
    public void setForwardListenerTargetChatId(Long v) { this.forwardListenerTargetChatId = v; }
    public void setForwardListenerLastMessageId(Long v) { this.forwardListenerLastMessageId = v; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    @Override public String toString() { return "TelegramConfig{id=" + id + ", configName='" + configName + "'}"; }
    @Override public boolean equals(Object o) { return this == o || (o != null && getClass() == o && id != null && Objects.equals(id, ((TelegramConfig) o).id)); }
    @Override public int hashCode() { return getClass().hashCode(); }
}
