package com.tgdownloader.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgdownloader.entity.ChatConfig;
import com.tgdownloader.entity.DownloadTask;
import com.tgdownloader.model.DownloadStatus;
import com.tgdownloader.repository.ChatConfigRepository;
import com.tgdownloader.repository.DownloadTaskRepository;
import com.tgdownloader.repository.TelegramConfigRepository;
import com.tgdownloader.service.TelegramClientService;
import it.tdlight.jni.TdApi;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Telegram 工具类
 * <p>
 * 统一管理 DownloadTask 的创建与修改，所有业务代码不得直接操作 DownloadTask 实体，
 * 必须通过本类提供的静态方法进行。
 */
@Log4j2
@Service
@Data
public class TelegramUtils {

    @Autowired
    private TelegramClientService telegramClientService;

    @Autowired
    private TelegramConfigRepository configRepository;

    @Autowired
    private ChatConfigRepository chatConfigRepository;

    @Autowired
    private DownloadTaskRepository downloadTaskRepository;


    /**
     * 保存任务（统一持久化入口）
     */
    public void saveTask(DownloadTask task) {
        downloadTaskRepository.save(task);
    }

    // ==================== 实例实现 ====================

    public DownloadTask buildTask(ChatConfig cfg, TdApi.Message msg) {
        // 0. 去重检查
        if (downloadTaskRepository.existsByChatIdAndMessageId(String.valueOf(msg.chatId), msg.id)) {
            log.debug("任务已存在，跳过: chatId={}, msgId={}", msg.chatId, msg.id);
            return null;
        }

        // 1. 检查媒体类型是否在允许范围内
        String downloadTypes = null;
        try {
            var tgc = configRepository.findByConfigName("default").orElse(null);
            if (tgc != null) downloadTypes = tgc.getDownloadTypes();
        } catch (Exception e) {
            log.warn("获取下载类型配置失败: {}", e.getMessage());
        }

        if (!TelegramUtils.isAllowedDownloadType(msg, downloadTypes)) {
            log.info("消息类型不在允许的下载类型中，跳过: msgId={}", msg.id);
            return null;
        }

        // 2. 自动查找或创建 ChatConfig
        if (cfg == null) {
            String chatIdStr = String.valueOf(msg.chatId);
            cfg = chatConfigRepository.findByChatId(chatIdStr).orElseGet(() -> {
                ChatConfig c = new ChatConfig();
                c.setChatId(chatIdStr);
                c.setEnabled(true);
                return chatConfigRepository.save(c);
            });
        }

        // 3. 构建任务
        DownloadTask task = new DownloadTask();
        task.setChatId(String.valueOf(msg.chatId));
        task.setMessageId(msg.id);
        task.setChatConfig(cfg);
        task.setStatus(DownloadStatus.PENDING.name());
        task.setStartedAt(LocalDateTime.now());
        task.setTelegramMessage(msg);

        // 文件名
        FileRef fr = getFileRef(msg);
        if (fr == null) return null;
        task.setFileName(fr.fileName());


        // Chat 标题
        try {
            TdApi.Chat chat = telegramClientService.getChatSync(msg.chatId);
            if (chat != null) {
                task.setChatTitle(chat.title);
            }
        } catch (Exception e) {
            log.debug("获取 chat 标题失败: chatId={}", msg.chatId);
        }

        // 序列化消息
        task.setTelegramMessage(msg);

        // 4. 持久化
        return downloadTaskRepository.save(task);
    }

    // ==================== 文件引用 ====================

    public static FileRef getFileRef(TdApi.Message msg) {
        FileRef fileRef = null;
        if (msg.content != null) {
            if (msg.content instanceof TdApi.MessageVideo v) {
                if (StringUtils.isEmpty(v.video.fileName)) v.video.fileName = ".mp4";
                fileRef = new FileRef(v.video.video.id, msg.id + "_" + v.video.fileName, v.video.video.size, "video");
            }
            if (msg.content instanceof TdApi.MessageDocument d) {
                fileRef = new FileRef(d.document.document.id, msg.id + "_" + d.document.fileName, d.document.document.size, "document");
            }
            if (msg.content instanceof TdApi.MessageAudio a) {
                if (StringUtils.isEmpty(a.audio.fileName)) a.audio.fileName = ".mp3";
                fileRef = new FileRef(a.audio.audio.id, msg.id + "_" + a.audio.fileName, a.audio.audio.size, "audio");
            }
            if (msg.content instanceof TdApi.MessageVoiceNote vn) {
                fileRef = new FileRef(vn.voiceNote.voice.id, msg.id + "_voice_note.ogg", vn.voiceNote.voice.size, "voice");
            }
            if (msg.content instanceof TdApi.MessageAnimation anim) {
                fileRef = new FileRef(anim.animation.animation.id, msg.id + "_" + anim.animation.fileName, anim.animation.animation.size, "animation");
            }
            if (msg.content instanceof TdApi.MessagePhoto p && p.photo.sizes != null && p.photo.sizes.length > 0) {
                TdApi.PhotoSize largest = p.photo.sizes[p.photo.sizes.length - 1];
                fileRef = new FileRef(largest.photo.id, msg.id + "_photo.jpg", largest.photo.size, "photo");
            }
            if (msg.content instanceof TdApi.MessageVideoNote vn) {
                fileRef = new FileRef(vn.videoNote.video.id, msg.id + "_video_note.mp4", vn.videoNote.video.size, "video");
            }
        }
        if (fileRef == null) {
            log.warn("消息 {} 无可下载媒体", msg.id);
            return null;
        }
        return fileRef;
    }

    // ==================== 静态工具方法 ====================

    /**
     * 获取消息的媒体类型
     */
    public static String getMediaType(TdApi.Message msg) {
        if (msg.content == null) return null;
        if (msg.content instanceof TdApi.MessageVideo || msg.content instanceof TdApi.MessageVideoNote) return "video";
        if (msg.content instanceof TdApi.MessageAudio) return "audio";
        if (msg.content instanceof TdApi.MessagePhoto) return "photo";
        if (msg.content instanceof TdApi.MessageDocument) return "document";
        if (msg.content instanceof TdApi.MessageVoiceNote) return "voice";
        if (msg.content instanceof TdApi.MessageAnimation) return "animation";
        return null;
    }

    /**
     * 检查消息类型是否在允许的下载类型中
     */
    public static boolean isAllowedDownloadType(TdApi.Message msg, String allowedTypesJson) {
        String mediaType = getMediaType(msg);
        if (mediaType == null) return false;
        if (allowedTypesJson == null || allowedTypesJson.isEmpty()) return "video".equals(mediaType);
        try {
            List<String> allowedTypes = new ObjectMapper().readValue(allowedTypesJson, new TypeReference<List<String>>() {
            });
            return allowedTypes.contains(mediaType);
        } catch (Exception e) {
            log.warn("解析下载类型配置失败: {}", allowedTypesJson);
            return "video".equals(mediaType);
        }
    }

    /**
     * 文件引用（ID + 名称 + 大小 + 类型）
     */
    public record FileRef(int fileId, String fileName, long fileSize, String mediaType) {
    }
}
