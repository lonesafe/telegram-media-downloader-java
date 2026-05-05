package com.tgdownloader.controller;

import com.tgdownloader.dto.ApiResponse;
import com.tgdownloader.dto.ChatConfigDto;
import com.tgdownloader.entity.ChatConfig;
import com.tgdownloader.entity.TelegramConfig;
import com.tgdownloader.mapper.ChatConfigMapper;
import com.tgdownloader.mapper.TelegramConfigMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 閰嶇疆绠＄悊 Controller - 鏃?Lombok 鐗堟湰
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final TelegramConfigMapper telegramConfigMapper;
    private final ChatConfigMapper chatConfigMapper;

    public ConfigController(TelegramConfigMapper telegramConfigMapper,
                            ChatConfigMapper chatConfigMapper) {
        this.telegramConfigMapper = telegramConfigMapper;
        this.chatConfigMapper = chatConfigMapper;
    }

    private TelegramConfig createDefaultConfig() {
        TelegramConfig cfg = new TelegramConfig();
        cfg.setConfigName("default");
        return cfg;
    }

    @GetMapping
    public ApiResponse<TelegramConfig> getConfig() {
        TelegramConfig config = telegramConfigMapper.findByConfigName("default")
                .orElseGet(() -> {
                    TelegramConfig cfg = new TelegramConfig();
                    cfg.setConfigName("default");
                    cfg.setApiId("");
                    cfg.setApiHash("");
                    return cfg;
                });
        return ApiResponse.success(config);
    }

    @PutMapping
    public ApiResponse<TelegramConfig> updateConfig(@RequestBody TelegramConfig config) {
        TelegramConfig existing = telegramConfigMapper.findByConfigName("default")
                .orElseGet(this::createDefaultConfig);

        if (config.getApiId() != null) existing.setApiId(config.getApiId());
        if (config.getApiHash() != null) existing.setApiHash(config.getApiHash());
        if (config.getBotToken() != null) existing.setBotToken(config.getBotToken());
        if (config.getDatabaseDirectory() != null) existing.setDatabaseDirectory(config.getDatabaseDirectory());
        if (config.getTempPath() != null) existing.setTempPath(config.getTempPath());
        if (config.getSavePath() != null) existing.setSavePath(config.getSavePath());
        if (config.getFilesDirectory() != null) existing.setFilesDirectory(config.getFilesDirectory());
        if (config.getUseTestDc() != null) existing.setUseTestDc(config.getUseTestDc());
        if (config.getLanguageCode() != null) existing.setLanguageCode(config.getLanguageCode());
        if (config.getMaxConcurrentTasks() != null) existing.setMaxConcurrentTasks(config.getMaxConcurrentTasks());
        if (config.getDownloadTypes() != null) existing.setDownloadTypes(config.getDownloadTypes());

        return ApiResponse.success(telegramConfigMapper.save(existing));
    }

    @GetMapping("/chats")
    public ApiResponse<List<ChatConfigDto>> getChats() {
        List<ChatConfigDto> dtos = chatConfigMapper.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @PostMapping("/chats")
    public ApiResponse<ChatConfigDto> addChat(@RequestBody ChatConfigDto dto) {
        ChatConfig entity = toEntity(dto);
        entity.setId(null);
        return ApiResponse.success(toDto(chatConfigMapper.save(entity)));
    }

    @PutMapping("/chats/{id}")
    public ApiResponse<ChatConfigDto> updateChat(@PathVariable Long id, @RequestBody ChatConfigDto dto) {
        ChatConfig existing = chatConfigMapper.findById(id);
        if (existing == null) throw new RuntimeException("Chat not found: " + id);
        BeanUtils.copyProperties(dto, existing, "id", "createdAt");
        return ApiResponse.success(toDto(chatConfigMapper.save(existing)));
    }

    @DeleteMapping("/chats/{id}")
    public ApiResponse<Void> deleteChat(@PathVariable Long id) {
        chatConfigMapper.deleteById(id);
        return ApiResponse.success();
    }

    @GetMapping("/proxy")
    public ApiResponse<Map<String, Object>> getProxy() {
        TelegramConfig c = telegramConfigMapper.findByConfigName("default")
                .orElseGet(this::createDefaultConfig);
        return ApiResponse.success(Map.of(
            "enabled",   c.getProxyEnabled() != null && c.getProxyEnabled(),
            "scheme",    c.getProxyScheme() != null ? c.getProxyScheme() : "socks5",
            "hostname",  c.getProxyHostname() != null ? c.getProxyHostname() : "",
            "port",      c.getProxyPort() != null ? c.getProxyPort() : 1080,
            "username",  c.getProxyUsername() != null ? c.getProxyUsername() : "",
            "password",  c.getProxyPassword() != null ? c.getProxyPassword() : ""
        ));
    }

    @PutMapping("/proxy")
    public ApiResponse<Map<String, Object>> updateProxy(@RequestBody Map<String, Object> p) {
        TelegramConfig c = telegramConfigMapper.findByConfigName("default")
                .orElseGet(this::createDefaultConfig);

        if (p.containsKey("enabled"))   c.setProxyEnabled((Boolean) p.get("enabled"));
        if (p.containsKey("scheme"))    c.setProxyScheme((String) p.get("scheme"));
        if (p.containsKey("hostname"))  c.setProxyHostname((String) p.get("hostname"));
        if (p.containsKey("port"))       c.setProxyPort((Integer) p.get("port"));
        if (p.containsKey("username"))  c.setProxyUsername((String) p.get("username"));
        if (p.containsKey("password"))  c.setProxyPassword((String) p.get("password"));

        telegramConfigMapper.save(c);
        return ApiResponse.success(p);
    }

    @GetMapping("/cloud")
    public ApiResponse<Map<String, Object>> getCloudConfig() {
        TelegramConfig c = telegramConfigMapper.findByConfigName("default")
                .orElseGet(this::createDefaultConfig);
        return ApiResponse.success(Map.of(
            "enableUploadFile",     c.getEnableUploadFile() != null && c.getEnableUploadFile(),
            "uploadAdapter",        c.getUploadAdapter() != null ? c.getUploadAdapter() : "rclone",
            "remoteDir",            c.getRemoteDir() != null ? c.getRemoteDir() : "",
            "rclonePath",           c.getRclonePath() != null ? c.getRclonePath() : "rclone",
            "beforeUploadFileZip",  c.getBeforeUploadFileZip() != null && c.getBeforeUploadFileZip(),
            "afterUploadFileDelete",c.getAfterUploadFileDelete() != null && c.getAfterUploadFileDelete()
        ));
    }

    @PutMapping("/cloud")
    public ApiResponse<Map<String, Object>> updateCloudConfig(@RequestBody Map<String, Object> c) {
        TelegramConfig cfg = telegramConfigMapper.findByConfigName("default")
                .orElseGet(this::createDefaultConfig);

        if (c.containsKey("enableUploadFile"))     cfg.setEnableUploadFile((Boolean) c.get("enableUploadFile"));
        if (c.containsKey("uploadAdapter"))        cfg.setUploadAdapter((String) c.get("uploadAdapter"));
        if (c.containsKey("remoteDir"))            cfg.setRemoteDir((String) c.get("remoteDir"));
        if (c.containsKey("rclonePath"))           cfg.setRclonePath((String) c.get("rclonePath"));
        if (c.containsKey("beforeUploadFileZip"))  cfg.setBeforeUploadFileZip((Boolean) c.get("beforeUploadFileZip"));
        if (c.containsKey("afterUploadFileDelete"))cfg.setAfterUploadFileDelete((Boolean) c.get("afterUploadFileDelete"));

        telegramConfigMapper.save(cfg);
        return ApiResponse.success(c);
    }

    private ChatConfigDto toDto(ChatConfig e) {
        ChatConfigDto d = new ChatConfigDto();
        BeanUtils.copyProperties(e, d);
        return d;
    }

    private ChatConfig toEntity(ChatConfigDto d) {
        ChatConfig e = new ChatConfig();
        BeanUtils.copyProperties(d, e);
        return e;
    }
}
