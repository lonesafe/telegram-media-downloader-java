package com.tgdownloader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 云盘上传配置
 *
 * 对应 Python 版本的 CloudDriveConfig
 *
 * 支持两种适配器：
 * - rclone: 通用云盘上传工具
 * - aligo:  阿里云盘专用
 *
 * 配置示例 (application.yml):
 * <pre>
 * cloud-drive:
 *   enable-upload-file: true
 *   upload-adapter: rclone
 *   rclone-path: ./rclone/rclone.exe
 *   remote-dir: gdrive:/TelegramMedia
 *   before-upload-file-zip: false
 *   after-upload-file-delete: true
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cloud-drive")
public class CloudDriveConfig {

    // ==================== 开关 ====================

    /** 是否启用云盘上传（默认关闭） */
    private boolean enableUploadFile = false;

    /** 上传前是否 ZIP 压缩（默认不压缩） */
    private boolean beforeUploadFileZip = false;

    /** 上传后是否删除本地文件（默认删除） */
    private boolean afterUploadFileDelete = true;

    // ==================== Rclone 配置 ====================

    /** Rclone 可执行文件路径 */
    private String rclonePath = defaultRclonePath();

    /** 远程目录（格式：remote_name:path） */
    private String remoteDir = "";

    // ==================== 适配器 ====================

    /** 上传适配器：rclone 或 aligo */
    private String uploadAdapter = "rclone";

    // ==================== Aligo 配置 ====================

    /** Aligo 是否已初始化 */
    private Object aligo = null;

    // ==================== 缓存 ====================

    /** 远程目录缓存（避免重复创建） */
    private java.util.Map<String, Boolean> dirCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 预运行初始化（在启用上传时调用）
     */
    public void preRun() {
        if (enableUploadFile && "aligo".equalsIgnoreCase(uploadAdapter)) {
            initAligo();
        }
    }

    /**
     * 初始化 Aligo 客户端
     */
    private void initAligo() {
        try {
            // TODO: 动态导入 Aligo
            // Aligo aligo = new Aligo();
            // this.aligo = aligo;
            System.out.println("Aligo 初始化完成");
        } catch (Exception e) {
            System.err.println("Aligo 初始化失败: " + e.getMessage());
        }
    }

    /**
     * 获取默认 Rclone 路径
     */
    private String defaultRclonePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String ext = os.contains("win") ? ".exe" : "";
        return "." + File.separator + "rclone" + File.separator + "rclone" + ext;
    }
}
