package com.tgdownloader.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Set;

@Log4j2
public class TelegramUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static FileRef getFileRef(TdApi.Message msg){
        FileRef fileRef = null;
        if (msg.content != null) {
            // 视频
            if (msg.content instanceof TdApi.MessageVideo v) {
                fileRef = new FileRef(v.video.video.id, msg.id +"_"+v.video.fileName, v.video.video.size, "video");
            }
            // 文档
            if (msg.content instanceof TdApi.MessageDocument d) {
                fileRef = new FileRef(d.document.document.id, msg.id +"_"+d.document.fileName, d.document.document.size, "document");
            }
            // 音频
            if (msg.content instanceof TdApi.MessageAudio a) {
                fileRef = new FileRef(a.audio.audio.id, msg.id +"_"+a.audio.fileName, a.audio.audio.size, "audio");
            }
            // 语音
            if (msg.content instanceof TdApi.MessageVoiceNote vn) {
                fileRef = new FileRef(vn.voiceNote.voice.id, msg.id +"_voice_note.ogg", vn.voiceNote.voice.size, "voice");
            }
            // 动画(GIF)
            if (msg.content instanceof TdApi.MessageAnimation anim) {
                fileRef = new FileRef(anim.animation.animation.id, msg.id +"_"+anim.animation.fileName, anim.animation.animation.size, "animation");
            }
            // 图片：取最大尺寸
            if (msg.content instanceof TdApi.MessagePhoto p && p.photo.sizes != null && p.photo.sizes.length > 0) {
                TdApi.PhotoSize largest = p.photo.sizes[p.photo.sizes.length - 1];
                fileRef = new FileRef(largest.photo.id, msg.id +"_photo.jpg", largest.photo.size, "photo");
            }
            // 视频笔记(圆圈视频)
            if (msg.content instanceof TdApi.MessageVideoNote vn) {
                fileRef = new FileRef(vn.videoNote.video.id, msg.id +"_video_note.mp4", vn.videoNote.video.size, "video");
            }
        }
        if (fileRef == null) {
            log.warn("消息 {} 无可下载媒体", msg.id);
            return null;
        }
        return fileRef;
    }

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
     * @param msg 消息
     * @param allowedTypesJson 允许的类型JSON数组，如 ["video","photo","audio"]
     * @return true if allowed, false if not
     */
    public static boolean isAllowedDownloadType(TdApi.Message msg, String allowedTypesJson) {
        String mediaType = getMediaType(msg);
        if (mediaType == null) return false;
        
        if (allowedTypesJson == null || allowedTypesJson.isEmpty()) {
            // 默认只允许 video
            return "video".equals(mediaType);
        }
        
        try {
            List<String> allowedTypes = mapper.readValue(allowedTypesJson, new TypeReference<List<String>>() {});
            return allowedTypes.contains(mediaType);
        } catch (Exception e) {
            log.warn("解析下载类型配置失败: {}", allowedTypesJson);
            return "video".equals(mediaType);
        }
    }

    /**
     * 检查消息类型是否在允许的转发类型中（用于监听转发）
     */
    public static boolean isAllowedForwardType(TdApi.Message msg, Set<String> allowedTypes) {
        String mediaType = getMediaType(msg);
        if (mediaType == null) return false;
        
        // 如果没有设置类型限制，默认允许所有
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return true;
        }
        
        // animation 归类为 video
        if ("animation".equals(mediaType) && allowedTypes.contains("video")) {
            return true;
        }
        // voice 归类为 audio
        if ("voice".equals(mediaType) && allowedTypes.contains("audio")) {
            return true;
        }
        
        return allowedTypes.contains(mediaType);
    }

    /**
     * 文件引用（ID + 名称 + 大小 + 类型）
     */
    public record FileRef(int fileId, String fileName, long fileSize, String mediaType) {
    }
}
