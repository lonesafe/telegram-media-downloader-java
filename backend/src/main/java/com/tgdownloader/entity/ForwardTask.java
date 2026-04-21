package com.tgdownloader.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 转发任务实体 - 无 Lombok 版本
 */
@Entity
@Table(name = "forward_task")
public class ForwardTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_chat_id", nullable = false)
    private Long sourceChatId;

    @Column(name = "source_chat_title")
    private String sourceChatTitle;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "target_chat_id", nullable = false)
    private Long targetChatId;

    @Column(name = "target_chat_title")
    private String targetChatTitle;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, FORWARDING, SUCCESS, FAILED, SKIP

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "is_auto_forward")
    private Boolean isAutoForward = false; // true = 监听自动触发, false = 用户手动提交

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // --- getters ---
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

    // --- setters ---
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

    @Override
    public String toString() { return "ForwardTask{id=" + id + ", sourceChatId=" + sourceChatId + ", messageId=" + messageId + ", status=" + status + "}"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForwardTask that = (ForwardTask) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ForwardTask t = new ForwardTask();
        public Builder id(Long v) { t.setId(v); return this; }
        public Builder sourceChatId(Long v) { t.setSourceChatId(v); return this; }
        public Builder sourceChatTitle(String v) { t.setSourceChatTitle(v); return this; }
        public Builder messageId(Long v) { t.setMessageId(v); return this; }
        public Builder targetChatId(Long v) { t.setTargetChatId(v); return this; }
        public Builder targetChatTitle(String v) { t.setTargetChatTitle(v); return this; }
        public Builder status(String v) { t.setStatus(v); return this; }
        public Builder errorMessage(String v) { t.setErrorMessage(v); return this; }
        public Builder isAutoForward(Boolean v) { t.setIsAutoForward(v); return this; }
        public ForwardTask build() { return t; }
    }
}