package com.tgdownloader.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgdownloader.entity.ChatConfig;
import com.tgdownloader.entity.DownloadTask;
import com.tgdownloader.model.DownloadStatus;
import com.tgdownloader.mapper.ChatConfigMapper;
import com.tgdownloader.mapper.DownloadTaskMapper;
import com.tgdownloader.mapper.TelegramConfigMapper;
import com.tgdownloader.service.TelegramClientService;
import it.tdlight.jni.TdApi;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Telegram 工具类
 * <p>
 * 统一管理 DownloadTask 的创建与修改，所有业务代码不得直接操作 DownloadTask 实体，
 * 必须通过本类提供的静态方法进行。
 */
@Log4j2
@Service
public class TelegramUtils {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TelegramUtils.class);

    // ── Autowired fields (set by Spring) ─────────────────────────────────────
    private TelegramClientService telegramClientService;
    private TelegramConfigMapper configMapper;
    private ChatConfigMapper chatConfigMapper;
    private DownloadTaskMapper downloadTaskMapper;

    @Autowired
    public void setTelegramClientService(TelegramClientService s) { this.telegramClientService = s; }
    @Autowired
    public void setConfigMapper(TelegramConfigMapper m) { this.configMapper = m; }
    @Autowired
    public void setChatConfigMapper(ChatConfigMapper m) { this.chatConfigMapper = m; }
    @Autowired
    public void setDownloadTaskMapper(DownloadTaskMapper m) { this.downloadTaskMapper = m; }

    public TelegramClientService getTelegramClientService() { return telegramClientService; }
    public TelegramConfigMapper getConfigMapper() { return configMapper; }
    public ChatConfigMapper getChatConfigMapper() { return chatConfigMapper; }
    public DownloadTaskMapper getDownloadTaskMapper() { return downloadTaskMapper; }

    public void saveTask(DownloadTask task) {
        getDownloadTaskMapper().insertOrUpdate(task);
    }

    // ==================== 实例实现 ====================

    public DownloadTask buildTask(ChatConfig cfg, TdApi.Message msg) {
        // 0. 去重检查
        if (getDownloadTaskMapper().existsByChatIdAndMessageId(String.valueOf(msg.chatId), msg.id)) {
            log.debug("任务已存在，跳过: chatId={}, msgId={}", msg.chatId, msg.id);
            return null;
        }

        // 1. 自动查找或创建 ChatConfig
        String chatIdStr = String.valueOf(msg.chatId);
        if (cfg == null) {
            cfg = getChatConfigMapper().findByChatId(chatIdStr).orElse(null);
            if (cfg == null) {
                cfg = new ChatConfig();
                cfg.setChatId(chatIdStr);
                cfg.setEnabled(true);
                getChatConfigMapper().insertSelective(cfg);
            }
        }

        // 2. 构建任务
        DownloadTask task = new DownloadTask();
        task.setChatId(String.valueOf(msg.chatId));
        task.setMessageId(msg.id);
        task.setChatConfigId(cfg.getId());
        task.setStatus(DownloadStatus.PENDING.name());
        task.setStartedAt(LocalDateTime.now());

        // 文件名
        FileRef fr = getFileRef(msg);
        if (fr == null) return null;
        task.setFileName(fr.fileName());

        // Chat 标题
        try {
            TdApi.Chat chat = getTelegramClientService().getChatSync(msg.chatId);
            if (chat != null) {
                task.setChatTitle(chat.title);
            }
        } catch (Exception e) {
            log.debug("获取 chat 标题失败: chatId={}", msg.chatId);
        }

        // 序列化消息
        task.setTelegramMessage(msg);

        // 3. 持久化
        getDownloadTaskMapper().insertSelective(task);
        return task;
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
            List<String> allowedTypes = new ObjectMapper().readValue(allowedTypesJson, new TypeReference<List<String>>() {});
            return allowedTypes.contains(mediaType);
        } catch (Exception e) {
            log.warn("解析下载类型配置失败: {}", allowedTypesJson);
            return "video".equals(mediaType);
        }
    }

    /**
     * 文件引用（ID + 名称 + 大小 + 类型）
     */
    public record FileRef(int fileId, String fileName, long fileSize, String mediaType) {}
}