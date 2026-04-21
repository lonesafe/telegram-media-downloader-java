package com.tgdownloader.model;

/**
 * 转发状态枚举
 */
public enum ForwardStatus {

    /** 跳过：已存在或被过滤 */
    SKIP(1, "跳过"),

    /** 成功：转发完成 */
    SUCCESS(2, "成功"),

    /** 失败：转发出错 */
    FAILED(3, "失败"),

    /** 转发中 */
    FORWARDING(4, "转发中"),

    /** 已停止：用户手动停止 */
    STOPPED(5, "已停止"),

    /** 缓存：消息来自本地缓存 */
    CACHED(6, "缓存");

    private final int code;
    private final String name;

    ForwardStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() { return code; }
    public String getName() { return name; }
}
