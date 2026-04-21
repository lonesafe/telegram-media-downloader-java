package com.tgdownloader.dto;

import lombok.Data;

/**
 * 聊天配置 DTO
 *
 * 用于前后端传输聊天配置信息
 */
@Data
public class ChatConfigDto {

    /** 数据库主键 ID */
    private Long id;

    /** Telegram 聊天 ID（字符串形式，兼容私有频道负数 ID） */
    private String chatId;

    /** 聊天标题（如频道名称） */
    private String chatTitle;

    /** 聊天类型：private / group / supergroup / channel */
    private String chatType;

    /** 最后已读消息 ID（用于断点续传） */
    private Long lastReadMessageId;

    /**
     * 下载过滤表达式
     */
    private String downloadFilter;

    /** 转发目标聊天 ID（可选） */
    private String uploadTelegramChatId;
}
