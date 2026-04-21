package com.tgdownloader.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import jakarta.persistence.*;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.codec.binary.Base64;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

/**
 * 下载任务实体（手写 getter/setter，避免 Lombok 注解处理器问题）
 */
@Entity
@Data
@Log4j2
@Table(name = "download_task")
public class DownloadTask {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "chat_id")
    private String chatId;

    @Column(name = "chat_title")
    private String chatTitle;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "local_path")
    private String localPath;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "remote_url")
    private String remoteUrl;

    @Column(name = "upload_status")
    private String uploadStatus;

    @Column(name = "telegram_file_id")
    private String telegramFileId;

    @Column(name = "telegram_unique_file_id")
    private String telegramUniqueFileId;

//    @Column(name = "message_link")
//    private String messageLink;

    @Column(name = "downloaded_size")
    private Long downloadedSize;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    @Column(name = "is_stop_transmission")
    private Boolean isStopTransmission = false;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "total_task")
    private Integer totalTask = 0;

    @Column(name = "success_task")
    private Integer successTask = 0;

    @Column(name = "failed_task")
    private Integer failedTask = 0;

    @Column(name = "skip_task")
    private Integer skipTask = 0;

    @Column(name = "download_speed")
    private Double downloadSpeed = 0.0;

    /**
     * 序列化的 TdApi.Message JSON
     */
    @Column(name = "message_data", columnDefinition = "TEXT")
    private String messageData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_config_id")
    private ChatConfig chatConfig;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========== TdApi.Message 序列化 ==========

    /**
     * 内存缓存，不持久化
     */
    @JsonIgnore
    private transient TdApi.Message cachedMessage;

    public void setTelegramMessage(TdApi.Message msg) {
        this.cachedMessage = msg;
        if (msg != null) {
            this.messageData = Base64.encodeBase64String(serialize(msg));
        } else {
            this.messageData = null;
        }
    }

    public TdApi.Message getTelegramMessage() {
        if (cachedMessage != null) return cachedMessage;
        if (messageData == null) return null;
        try {
            cachedMessage = deserialize(Base64.decodeBase64(messageData), TdApi.Message.class);
        } catch (Exception e) {
            log.error("parse messageData error: {}", messageData, e);
        }
        return cachedMessage;
    }

    // ========== getters ==========


    // ========== Object ==========

    @Override
    public String toString() {
        return "DownloadTask{id=" + id + ", chatId=" + chatId + ", status=" + status + "}";
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final ThreadLocal<Kryo> kryoLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false); // 不要求预先注册
        return kryo;
    });

    public static byte[] serialize(Object obj) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Output output = new Output(baos)) {
            kryoLocal.get().writeObject(output, obj);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Kryo序列化失败", e);
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        if (data == null) return null;
        try (Input input = new Input(data)) {
            return kryoLocal.get().readObject(input, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Kryo反序列化失败", e);
        }
    }
}
