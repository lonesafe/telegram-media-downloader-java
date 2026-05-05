package com.tgdownloader.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 转发任务实体
 */
@Table("forward_task")
public class ForwardTask {

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("source_chat_id")
    private Long sourceChatId;

    @Column("source_chat_title")
    private String sourceChatTitle;

    @Column("message_id")
    private Long messageId;

    @Column("target_chat_id")
    private Long targetChatId;

    @Column("target_chat_title")
    private String targetChatTitle;

    @Column("status")
    private String status = "PENDING";

    @Column("error_message")
    private String errorMessage;

    @Column("is_auto_forward")
    private Boolean isAutoForward = false;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    // ── getters / setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public Long getSourceChatId() { return sourceChatId; }
    public String getSourceChatTitle() { return sourceChatTitle; }
    public Long getMessageId() { return messageId; }
    public Long getTargetChatId() { return targetChatId; }
    public String getTargetChatTitle() { return targetChatTitle; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Boolean getIsAutoForward() { return isAutoForward; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setSourceChatId(Long sourceChatId) { this.sourceChatId = sourceChatId; }
    public void setSourceChatTitle(String sourceChatTitle) { this.sourceChatTitle = sourceChatTitle; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public void setTargetChatId(Long targetChatId) { this.targetChatId = targetChatId; }
    public void setTargetChatTitle(String targetChatTitle) { this.targetChatTitle = targetChatTitle; }
    public void setStatus(String status) { this.status = status; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setIsAutoForward(Boolean isAutoForward) { this.isAutoForward = isAutoForward; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override public String toString() { return "ForwardTask{id=" + id + ", status=" + status + "}"; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForwardTask that = (ForwardTask) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override public int hashCode() { return getClass().hashCode(); }
}
