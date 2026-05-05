package com.tgdownloader.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 转发任务实体
 */
public class ForwardTask {

    private Long id;
    private Long sourceChatId;
    private String sourceChatTitle;
    private Long messageId;
    private Long targetChatId;
    private String targetChatTitle;
    private String status = "PENDING";
    private String errorMessage;
    private Boolean isAutoForward = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── getters / setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceChatId() { return sourceChatId; }
    public void setSourceChatId(Long sourceChatId) { this.sourceChatId = sourceChatId; }
    public String getSourceChatTitle() { return sourceChatTitle; }
    public void setSourceChatTitle(String sourceChatTitle) { this.sourceChatTitle = sourceChatTitle; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Long getTargetChatId() { return targetChatId; }
    public void setTargetChatId(Long targetChatId) { this.targetChatId = targetChatId; }
    public String getTargetChatTitle() { return targetChatTitle; }
    public void setTargetChatTitle(String targetChatTitle) { this.targetChatTitle = targetChatTitle; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Boolean getIsAutoForward() { return isAutoForward; }
    public void setIsAutoForward(Boolean isAutoForward) { this.isAutoForward = isAutoForward; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
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
