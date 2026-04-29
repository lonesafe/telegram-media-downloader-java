package com.tgdownloader.model;

/**
 * 下载状态枚举
 *
 * 描述一个文件或任务的生命周期状态
 */
public enum DownloadStatus {

    /** 跳过：文件已存在或被过滤规则排除 */
    SKIP_DOWNLOAD(1, "跳过"),

    /** 成功：下载完成 */
    SUCCESS_DOWNLOAD(2, "成功"),

    /** 失败：下载出错 */
    FAILED_DOWNLOAD(3, "失败"),

    /** 下载中：正在下载 */
    DOWNLOADING(4, "下载中"),

    /** 已暂停：用户手动停止 */
    PAUSED(5, "已暂停"),

    /** 排队中：已加入下载队列，等待执行 */
    QUEUED(6, "排队中"),

    /** 等待中：任务已创建但尚未加入队列 */
    PENDING(7, "等待中");

    private final int code;
    private final String name;

    DownloadStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /** 获取状态码 */
    public int getCode() { return code; }

    /** 获取状态名称 */
    public String getName() { return name; }
}
