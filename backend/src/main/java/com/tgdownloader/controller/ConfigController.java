package com.tgdownloader.controller;

import com.tgdownloader.dto.ApiResponse;
import com.tgdownloader.dto.ChatConfigDto;
import com.tgdownloader.entity.ChatConfig;
import com.tgdownloader.mapper.ChatConfigMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天配置 Controller
 * 
 * ⚠️ 统一配置入口（telegram_config 表）已移至 TelegramController：
 *   - GET  /api/telegram/config  → 获取全部配置
 *   - POST /api/telegram/config  → 保存全部配置
 * 
 * 本 Controller 只负责 ChatConfig 聊天配置 CRUD：
 *   - GET  /api/config/chats
 *   - POST /api/config/chats
 *   - PUT  /api/config/chats/{id}
 *   - DELETE /api/config/chats/{id}
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ChatConfigMapper chatConfigMapper;

    public ConfigController(ChatConfigMapper chatConfigMapper) {
        this.chatConfigMapper = chatConfigMapper;
    }

    // ── 聊天配置 CRUD ────────────────────────────────────────────────────────

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
        ChatConfig existing = chatConfigMapper.findById(id).orElse(null);
        if (existing == null) throw new RuntimeException("Chat not found: " + id);
        BeanUtils.copyProperties(dto, existing, "id", "createdAt");
        return ApiResponse.success(toDto(chatConfigMapper.save(existing)));
    }

    @DeleteMapping("/chats/{id}")
    public ApiResponse<Void> deleteChat(@PathVariable Long id) {
        chatConfigMapper.deleteById(id);
        return ApiResponse.success();
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────────────

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
