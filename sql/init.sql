-- ===============================================================
-- Telegram Media Downloader - MySQL 数据库初始化脚本
-- ===============================================================
-- 适用于 MySQL 8.0+
-- 使用方式: mysql -u root -p < init.sql

CREATE DATABASE IF NOT EXISTS tg_downloader DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tg_downloader;

-- ---------------------------------------------------------------
-- 1. Telegram 全局配置表
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS telegram_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name     VARCHAR(100) NOT NULL DEFAULT 'default' UNIQUE,
    api_id          VARCHAR(32),
    api_hash        VARCHAR(128),
    bot_token       VARCHAR(256),
    database_directory VARCHAR(512),
    temp_path       VARCHAR(512),
    save_path       VARCHAR(512),
    files_directory VARCHAR(512),
    use_test_dc     TINYINT(1) DEFAULT 0,
    language_code   VARCHAR(16),
    max_concurrent_tasks INT DEFAULT 3,
    download_types  TEXT COMMENT 'JSON数组: ["video","photo","document"]',
    proxy_enabled   TINYINT(1) DEFAULT 0,
    proxy_scheme    VARCHAR(16),
    proxy_hostname  VARCHAR(256),
    proxy_port      INT,
    proxy_username  VARCHAR(128),
    proxy_password  VARCHAR(256),
    enable_upload_file TINYINT(1) DEFAULT 0,
    upload_adapter  VARCHAR(64),
    remote_dir      VARCHAR(512),
    rclone_path     VARCHAR(512),
    before_upload_file_zip TINYINT(1) DEFAULT 0,
    after_upload_file_delete TINYINT(1) DEFAULT 0,
    download_chat_ids TEXT COMMENT 'JSON数组',
    last_read_message_ids TEXT COMMENT 'JSON对象 {chatId: lastMsgId}',
    saved_messages_enabled TINYINT(1) DEFAULT 1,
    saved_messages_last_scan_message_id BIGINT DEFAULT 0,
    forward_listener_enabled TINYINT(1) DEFAULT 1,
    forward_listener_source_chat_ids TEXT COMMENT 'JSON数组',
    forward_listener_target_chat_id BIGINT,
    forward_listener_last_message_id BIGINT DEFAULT 0,
    created_at      DATETIME,
    updated_at      DATETIME,
    INDEX idx_config_name (config_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------
-- 2. 聊天配置表
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_config (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_id             VARCHAR(64) NOT NULL UNIQUE COMMENT 'Telegram chat ID',
    title               VARCHAR(256),
    type                VARCHAR(32) COMMENT 'supergroup / channel / private / chat',
    enabled             TINYINT(1) DEFAULT 1,
    filter_type         VARCHAR(32) DEFAULT 'all' COMMENT 'all / text / media / document / video / photo ...',
    auto_delete_after_hours INT,
    file_name_template  VARCHAR(256),
    download_path       VARCHAR(512),
    remote_path         VARCHAR(512),
    upload_after_download TINYINT(1) DEFAULT 0,
    notify_after_download TINYINT(1) DEFAULT 1,
    forward_to_chat_id  BIGINT COMMENT '转发目标的 chat_id',
    extra_data          TEXT COMMENT 'JSON 扩展配置',
    username            VARCHAR(128) COMMENT 'Telegram username',
    created_at          DATETIME,
    updated_at          DATETIME,
    INDEX idx_chat_id (chat_id),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------
-- 3. 下载任务表
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS download_task (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id              BIGINT COMMENT 'Telegram message ID',
    chat_id                 VARCHAR(64) COMMENT 'Telegram chat ID',
    chat_title              VARCHAR(256),
    file_name               VARCHAR(512),
    file_size               BIGINT DEFAULT 0,
    mime_type               VARCHAR(128),
    status                  VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING / DOWNLOADING / SUCCESS_DOWNLOAD / FAILED_DOWNLOAD / PAUSED / SKIP_DOWNLOAD',
    local_path              VARCHAR(512) COMMENT '本地存储路径',
    error_message           TEXT,
    remote_url              VARCHAR(1024),
    upload_status           VARCHAR(32),
    telegram_file_id        VARCHAR(256),
    telegram_unique_file_id VARCHAR(256),
    downloaded_size         BIGINT DEFAULT 0,
    message_data            MEDIUMBLOB COMMENT 'Kryo 序列化的 TdApi.Message',
    chat_config_id          BIGINT COMMENT '关联 chat_config 主键',
    extra_data              TEXT,
    is_stop_transmission    TINYINT(1) DEFAULT 0,
    started_at              DATETIME,
    finished_at             DATETIME,
    total_task              INT DEFAULT 0 COMMENT '批量任务总数',
    success_task            INT DEFAULT 0,
    failed_task             INT DEFAULT 0,
    skip_task               INT DEFAULT 0,
    download_speed          DOUBLE DEFAULT 0.0 COMMENT 'MB/s',
    created_at              DATETIME,
    updated_at              DATETIME,
    INDEX idx_chat_id (chat_id),
    INDEX idx_message_id (message_id),
    INDEX idx_status (status),
    INDEX idx_chat_config_id (chat_config_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------
-- 4. 转发任务表
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS forward_task (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_chat_id      BIGINT COMMENT '源聊天 ID',
    source_chat_title   VARCHAR(256),
    message_id          BIGINT,
    target_chat_id      BIGINT COMMENT '目标聊天 ID',
    target_chat_title   VARCHAR(256),
    status              VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING / SUCCESS / FAILED',
    error_message       TEXT,
    is_auto_forward     TINYINT(1) DEFAULT 0 COMMENT '是否为自动监听触发',
    created_at          DATETIME,
    updated_at          DATETIME,
    INDEX idx_source_chat (source_chat_id),
    INDEX idx_target_chat (target_chat_id),
    INDEX idx_status (status),
    INDEX idx_is_auto_forward (is_auto_forward)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------
-- 5. 初始化默认配置
-- ---------------------------------------------------------------
INSERT INTO telegram_config (config_name, language_code, max_concurrent_tasks, download_types,
    saved_messages_enabled, forward_listener_enabled, created_at, updated_at)
VALUES ('default', 'zh-CN', 3, '["video","photo","document"]', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
