package com.tgdownloader.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 字节格式化工具
 *
 * 对应原 Python: utils/format.py
 *
 * 提供字节数与人类可读字符串之间的相互转换
 *
 * 示例：
 * <pre>
 * ByteFormatUtil.format(1024 * 1024 * 10)  // "10.00 MB"
 * ByteFormatUtil.parse("10MB")             // 10485760
 * ByteFormatUtil.createProgressBar(50.0, 10) // "█████░░░░░"
 * </pre>
 */
public class ByteFormatUtil {

    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

    // ==================== 格式化 ====================

    /**
     * 格式化字节数为人类可读格式
     *
     * @param size 字节数（必须非负）
     * @param dot  保留小数位数
     * @return 格式化字符串，如 "1.5 MB"
     * @throws IllegalArgumentException size 为负数时抛出
     */
    public static String format(long size, int dot) {
        if (size < 0) {
            throw new IllegalArgumentException("Size must be non-negative: " + size);
        }
        if (size == 0) {
            return "0B";
        }

        int unitIndex = 0;
        double value = size;
        while (value >= 1024 && unitIndex < UNITS.length - 1) {
            value /= 1024;
            unitIndex++;
        }

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(dot, RoundingMode.HALF_UP);

        return bd.doubleValue() == bd.longValue()
                ? bd.longValue() + UNITS[unitIndex]
                : bd.doubleValue() + UNITS[unitIndex];
    }

    /**
     * 格式化字节数（默认保留2位小数）
     *
     * @param size 字节数
     * @return 格式化字符串
     */
    public static String format(long size) {
        return format(size, 2);
    }

    // ==================== 解析 ====================

    /**
     * 从字符串解析字节数
     *
     * 对应原 Python: get_byte_from_str
     *
     * @param byteStr 字节字符串，如 "100MB"、"1.5GB"、"10KB"
     * @return 字节数
     * @throws IllegalArgumentException 格式无效时抛出
     */
    public static long parse(String byteStr) {
        if (byteStr == null || byteStr.isEmpty()) {
            throw new IllegalArgumentException("Byte string cannot be empty");
        }

        byteStr = byteStr.trim().toUpperCase();

        // 查找数字和单位的分界点
        int unitStart = 0;
        for (int i = 0; i < byteStr.length(); i++) {
            char c = byteStr.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                unitStart = i;
                break;
            }
        }

        double value = Double.parseDouble(byteStr.substring(0, unitStart));
        String unit = byteStr.substring(unitStart);

        long multiplier = switch (unit) {
            case "B"  -> 1L;
            case "KB" -> 1024L;
            case "MB" -> 1024L * 1024L;
            case "GB" -> 1024L * 1024L * 1024L;
            case "TB" -> 1024L * 1024L * 1024L * 1024L;
            case "PB" -> 1024L * 1024L * 1024L * 1024L * 1024L;
            default -> throw new IllegalArgumentException("Unknown unit: " + unit);
        };

        return (long) (value * multiplier);
    }

    // ==================== 进度条 ====================

    /**
     * 创建进度条
     *
     * 对应原 Python: create_progress_bar
     *
     * @param progress   进度百分比 (0-100)
     * @param totalBars  进度条总块数
     * @return 进度条字符串，如 "█████░░░░░"
     */
    public static String createProgressBar(double progress, int totalBars) {
        int completedBars = (int) (progress * totalBars / 100);
        int remainingBars = totalBars - completedBars;

        return "█".repeat(Math.max(0, completedBars))
             + "░".repeat(Math.max(0, remainingBars));
    }

    /**
     * 创建带文本的进度条（10块，1位小数）
     *
     * @param progress 进度百分比 (0-100)
     * @return 如 "[███████░░░] 70.0%"
     */
    public static String createProgressBarWithText(double progress) {
        return String.format("[%s] %.1f%%", createProgressBar(progress, 10), progress);
    }
}
