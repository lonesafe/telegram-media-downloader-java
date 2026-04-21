package com.tgdownloader.service;

import com.tgdownloader.config.CloudDriveConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 云盘上传服务
 *
 * 支持两种上传适配器：
 * 1. Rclone - 支持多种云盘（Google Drive, OneDrive, Dropbox 等）
 * 2. Aligo  - 阿里云盘专用适配器
 *
 * 功能：
 * - 上传前 ZIP 压缩（可选）
 * - 上传后删除本地文件（可选）
 * - 上传进度追踪
 * - 自动创建远程目录
 *
 * @see com.tgdownloader.config.CloudDriveConfig 云盘配置
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudDriveService {

    // ==================== 配置 ====================

    private final CloudDriveConfig config;

    // ==================== 上传状态 ====================

    /** 正在上传的文件映射（文件路径 → 上传状态） */
    private final Map<String, UploadTask> activeUploads = new ConcurrentHashMap<>();

    /** 累计上传成功文件数 */
    @Getter
    private final AtomicLong uploadSuccessCount = new AtomicLong(0);

    /** 累计上传失败文件数 */
    @Getter
    private final AtomicLong uploadFailedCount = new AtomicLong(0);

    /** 累计上传字节数 */
    @Getter
    private final AtomicLong totalUploadedBytes = new AtomicLong(0);

    // ==================== 生命周期 ====================

    @PostConstruct
    public void init() {
        log.info("CloudDriveService 初始化，适配器: {}, 启用: {}", 
            config.getUploadAdapter(), config.isEnableUploadFile());
        
        // 初始化适配器
        if (config.isEnableUploadFile()) {
            initAdapter();
        }
    }

    /**
     * 初始化上传适配器
     */
    private void initAdapter() {
        if ("aligo".equalsIgnoreCase(config.getUploadAdapter())) {
            initAligo();
        } else {
            initRclone();
        }
    }

    /**
     * 初始化 Aligo（阿里云盘）
     */
    private void initAligo() {
        try {
            // Aligo 需要动态导入
            Class<?> aligoClass = Class.forName("aligo.Aligo");
            log.info("Aligo 适配器已加载");
            // TODO: 初始化 Aligo 客户端
        } catch (ClassNotFoundException e) {
            log.warn("Aligo 未安装，请执行: pip install aligo");
        }
    }

    /**
     * 初始化 Rclone
     */
    private void initRclone() {
        String rclonePath = config.getRclonePath();
        if (rclonePath == null || rclonePath.isEmpty()) {
            log.warn("Rclone 路径未配置");
            return;
        }

        Path path = Paths.get(rclonePath);
        if (!Files.exists(path)) {
            log.warn("Rclone 不存在: {}", rclonePath);
            return;
        }

        log.info("Rclone 已配置: {}", rclonePath);
    }

    // ==================== 公共 API ====================

    /**
     * 上传文件到云盘
     *
     * @param localPath 本地文件路径
     * @param remoteDir 远程目录（相对于配置的 remote_dir）
     * @return 上传结果（成功返回远程路径，失败返回 null）
     */
    public String uploadFile(String localPath, String remoteDir) {
        if (!config.isEnableUploadFile()) {
            log.debug("云盘上传未启用");
            return null;
        }

        Path file = Paths.get(localPath);
        if (!Files.exists(file)) {
            log.error("文件不存在: {}", localPath);
            return null;
        }

        UploadTask task = new UploadTask(localPath, remoteDir);
        activeUploads.put(localPath, task);

        try {
            // 1. 可选：压缩文件
            String uploadPath = localPath;
            if (config.isBeforeUploadFileZip()) {
                uploadPath = zipFile(localPath);
                task.setZipped(true);
            }

            // 2. 执行上传
            String remotePath = doUpload(uploadPath, remoteDir);
            task.setCompleted(true);

            // 3. 统计
            uploadSuccessCount.incrementAndGet();
            totalUploadedBytes.addAndGet(file.toFile().length());

            // 4. 可选：删除本地文件
            if (config.isAfterUploadFileDelete()) {
                Files.deleteIfExists(file);
                if (task.isZipped()) {
                    Files.deleteIfExists(Paths.get(uploadPath));
                }
                log.debug("已删除本地文件: {}", localPath);
            }

            return remotePath;

        } catch (Exception e) {
            log.error("上传失败: {} - {}", localPath, e.getMessage());
            uploadFailedCount.incrementAndGet();
            task.setError(e.getMessage());
            return null;

        } finally {
            activeUploads.remove(localPath);
        }
    }

    /**
    /**
     * 创建远程目录
     *
     * @param remoteDir 远程目录路径
     */
    public void mkdirRemote(String remoteDir) {
        if (!config.isEnableUploadFile()) return;

        try {
            if ("aligo".equalsIgnoreCase(config.getUploadAdapter())) {
                mkdirAligo(remoteDir);
            } else {
                mkdirRclone(remoteDir);
            }
        } catch (Exception e) {
            log.warn("创建远程目录失败: {} - {}", remoteDir, e.getMessage());
        }
    }

    /**
     * 获取上传统计信息
     *
     * @return 统计信息映射
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "enabled", config.isEnableUploadFile(),
            "adapter", config.getUploadAdapter(),
            "uploadSuccess", uploadSuccessCount.get(),
            "uploadFailed", uploadFailedCount.get(),
            "totalBytes", totalUploadedBytes.get(),
            "activeUploads", activeUploads.size()
        );
    }

    // ==================== 内部实现 ====================

    /**
     * 执行上传（根据适配器选择）
     */
    private String doUpload(String localPath, String remoteDir) throws Exception {
        String fullRemotePath = config.getRemoteDir() + "/" + remoteDir;
        
        // 确保目录存在
        mkdirRemote(remoteDir);

        if ("aligo".equalsIgnoreCase(config.getUploadAdapter())) {
            return uploadAligo(localPath, fullRemotePath);
        } else {
            return uploadRclone(localPath, fullRemotePath);
        }
    }

    /**
     * 使用 Rclone 上传
     */
    private String uploadRclone(String localPath, String remotePath) throws Exception {
        String rclonePath = config.getRclonePath();
        String command = String.format("\"%s\" copy \"%s\" \"%s\" --progress",
            rclonePath, localPath, remotePath);

        ProcessBuilder pb = new ProcessBuilder(
            rclonePath, "copy", localPath, remotePath, "--progress");
        pb.redirectErrorStream(true);

        Process process = pb.start();
        
        // 读取输出（进度追踪）
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseRcloneProgress(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Rclone 退出码: " + exitCode);
        }

        Path file = Paths.get(localPath);
        return remotePath + "/" + file.getFileName();
    }

    /**
     * 解析 Rclone 进度输出
     */
    private void parseRcloneProgress(String line) {
        // Rclone 输出格式: Transferred:   	    1.234 MiB / 10 MiB, 12%, 1.23 MiB/s, ETA 8s
        if (line.contains("Transferred:")) {
            log.debug("Rclone: {}", line.trim());
        }
    }

    /**
     * 使用 Aligo 上传（阿里云盘）
     */
    private String uploadAligo(String localPath, String remotePath) throws Exception {
        // TODO: 实现 Aligo 上传
        // Aligo aligo = new Aligo();
        // aligo.upload_file(localPath, parent_file_id=...)
        throw new UnsupportedOperationException("Aligo 上传待实现");
    }

    /**
     * Rclone 创建远程目录
     */
    private void mkdirRclone(String remoteDir) throws Exception {
        String rclonePath = config.getRclonePath();
        String fullPath = config.getRemoteDir() + "/" + remoteDir;

        ProcessBuilder pb = new ProcessBuilder(rclonePath, "mkdir", fullPath + "/");
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        process.waitFor(30, TimeUnit.SECONDS);
    }

    /**
     * Aligo 创建远程目录
     */
    private void mkdirAligo(String remoteDir) throws Exception {
        // TODO: 实现 Aligo mkdir
        throw new UnsupportedOperationException("Aligo mkdir 待实现");
    }

    /**
     * ZIP 压缩文件
     *
     * @param localPath 原始文件路径
     * @return ZIP 文件路径
     */
    private String zipFile(String localPath) throws Exception {
        Path source = Paths.get(localPath);
        String zipPath = localPath.replaceAll("\\.[^.]+$", ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(
            new FileOutputStream(zipPath))) {
            
            ZipEntry entry = new ZipEntry(source.getFileName().toString());
            zos.putNextEntry(entry);

            Files.copy(source, zos);
            zos.closeEntry();
        }

        log.debug("已压缩: {} -> {}", localPath, zipPath);
        return zipPath;
    }

    // ==================== 内部类 ====================

    /**
     * 上传任务状态
     */
    private static class UploadTask {
        private final String localPath;
        private final String remoteDir;
        private boolean completed = false;
        private boolean zipped = false;
        private String error;

        public UploadTask(String localPath, String remoteDir) {
            this.localPath = localPath;
            this.remoteDir = remoteDir;
        }

        public void setCompleted(boolean completed) { this.completed = completed; }
        public void setZipped(boolean zipped) { this.zipped = zipped; }
        public void setError(String error) { this.error = error; }
        
        public boolean isZipped() { return zipped; }
    }
}
