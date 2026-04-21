package com.tgdownloader.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Data
@Table(name = "chat_config")
public class ChatConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private String chatId;

    @Column(name = "title")
    private String title;

    @Column(name = "type")
    private String type; // channel / group / supergroup / private

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "filter_type")
    private String filterType; // ALL / PHOTO / VIDEO / DOCUMENT / AUDIO / VOICE / OTHER

    @Column(name = "auto_delete_after_hours")
    private Integer autoDeleteAfterHours;

    @Column(name = "file_name_template")
    private String fileNameTemplate;

    @Column(name = "download_path")
    private String downloadPath;

    @Column(name = "remote_path")
    private String remotePath;

    @Column(name = "upload_after_download")
    private Boolean uploadAfterDownload = false;

    @Column(name = "notify_after_download")
    private Boolean notifyAfterDownload = true;

    @Column(name = "forward_to_chat_id")
    private Long forwardToChatId;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    @Column(name = "username")
    private String username;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;



    @Override public String toString() { return "ChatConfig{id=" + id + ", chatId=" + chatId + ", title='" + title + "'}"; }
    @Override public boolean equals(Object o) { return this == o; }
    @Override public int hashCode() { return getClass().hashCode(); }
}
