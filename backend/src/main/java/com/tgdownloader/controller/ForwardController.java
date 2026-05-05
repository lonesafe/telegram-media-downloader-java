package com.tgdownloader.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgdownloader.dto.ApiResponse;
import com.tgdownloader.entity.ForwardTask;
import com.tgdownloader.entity.TelegramConfig;
import com.tgdownloader.mapper.ForwardTaskMapper;
import com.tgdownloader.mapper.TelegramConfigMapper;
import com.tgdownloader.service.ForwardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forward")
public class ForwardController {

    private static final Logger log = LoggerFactory.getLogger(ForwardController.class);
    private static final ObjectMapper json = new ObjectMapper();

    @Autowired
    private ForwardTaskMapper forwardTaskMapper;

    @Autowired
    private TelegramConfigMapper configMapper;

    @Autowired
    private ForwardService forwardService;

    @SuppressWarnings("unchecked")
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        return ApiResponse.success((Map<String, Object>) (Map<?, ?>) forwardService.stats());
    }

    @GetMapping("/tasks")
    public ApiResponse<Map<String, Object>> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String chatId) {
        try {
            com.mybatisflex.core.paginate.Page<ForwardTask> taskPage;
            org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

            if (status != null && !status.isEmpty()) {
                taskPage = forwardTaskMapper.findByStatus(status, pageRequest);
            } else if (chatId != null && !chatId.isEmpty()) {
                taskPage = forwardTaskMapper.findBySourceChatId(Long.valueOf(chatId), pageRequest);
            } else {
                taskPage = forwardTaskMapper.findAll(pageRequest);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("tasks", taskPage.getRecords());
            result.put("total", taskPage.getTotalRow());
            result.put("pages", (taskPage.getTotalRow() + size - 1) / size);
            result.put("page", page);
            result.put("size", size);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取转发任务失败", e);
            return ApiResponse.error("获取转发任务失败: " + e.getMessage());
        }
    }

    /**
     * 创建转发任务
     * 前端调用: POST /forward/tasks?sourceChatId=xxx&messageId=xxx&targetChatId=xxx
     */
    @PostMapping("/tasks")
    public ApiResponse<Map<String, Object>> createTask(
            @RequestParam String sourceChatId,
            @RequestParam Long messageId,
            @RequestParam String targetChatId) {
        try {
            ForwardTask task = forwardService.createTask(
                Long.parseLong(sourceChatId),
                messageId,
                Long.parseLong(targetChatId),
                false // isAuto = false (手动)
            );
            return ApiResponse.success(Map.of("taskId", task.getId(), "message", "转发任务已创建"));
        } catch (Exception e) {
            log.error("创建转发任务失败", e);
            return ApiResponse.error("创建转发任务失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建转发任务
     * 前端调用: POST /forward/tasks/batch
     */
    @PostMapping("/tasks/batch")
    public ApiResponse<Map<String, Object>> createBatchTasks(@RequestBody Map<String, Object> body) {
        try {
            String sourceChatId = body.get("sourceChatId").toString();
            List<Long> messageIds = (List<Long>) body.get("messageIds");
            String targetChatId = body.get("targetChatId").toString();

            int created = 0;
            for (Long msgId : messageIds) {
                ForwardTask task = new ForwardTask();
                task.setSourceChatId(Long.parseLong(sourceChatId));
                task.setTargetChatId(Long.parseLong(targetChatId));
                task.setMessageId(msgId);
                task.setStatus("PENDING");
                forwardTaskMapper.save(task);
                created++;
            }
            return ApiResponse.success(Map.of("created", created, "message", "已创建 " + created + " 个转发任务"));
        } catch (Exception e) {
            log.error("批量创建转发任务失败", e);
            return ApiResponse.error("批量创建转发任务失败: " + e.getMessage());
        }
    }

    @GetMapping("/listener/config")
    public ApiResponse<Map<String, Object>> getListenerConfig() {
        try {
            TelegramConfig cfg = configMapper.findByConfigName("default").orElse(null);
            if (cfg == null) {
                return ApiResponse.error("配置不存在");
            }
            Map<String, Object> config = new HashMap<>();
            config.put("enabled", cfg.getForwardListenerEnabled());
            config.put("sourceChatIds", json.readValue(
                    cfg.getForwardListenerSourceChatIds() != null ? cfg.getForwardListenerSourceChatIds() : "[]",
                    new TypeReference<List<Long>>() {}));
            config.put("targetChatId", cfg.getForwardListenerTargetChatId());
            return ApiResponse.success(config);
        } catch (Exception e) {
            log.error("获取转发监听配置失败", e);
            return ApiResponse.error("获取配置失败: " + e.getMessage());
        }
    }

    @PostMapping("/listener/config")
    public ApiResponse<String> saveListenerConfig(@RequestBody Map<String, Object> body) {
        try {
            TelegramConfig cfg = configMapper.findByConfigName("default").orElse(null);
            if (cfg == null) {
                cfg = new TelegramConfig("default");
            }
            if (body.containsKey("enabled")) {
                cfg.setForwardListenerEnabled(Boolean.valueOf(body.get("enabled").toString()));
            }
            if (body.containsKey("sourceChatIds")) {
                cfg.setForwardListenerSourceChatIds(json.writeValueAsString(body.get("sourceChatIds")));
            }
            if (body.containsKey("targetChatId")) {
                Object targetId = body.get("targetChatId");
                if (targetId == null) {
                    cfg.setForwardListenerTargetChatId(null);
                } else {
                    cfg.setForwardListenerTargetChatId(Long.valueOf(targetId.toString()));
                }
            }
            configMapper.save(cfg);
            return ApiResponse.success("配置已保存");
        } catch (Exception e) {
            log.error("保存转发监听配置失败", e);
            return ApiResponse.error("保存配置失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/listener/rules/{ruleId}")
    public ApiResponse<String> deleteRule(@PathVariable Long ruleId) {
        try {
            // 删除监听规则（暂不实现具体逻辑）
            return ApiResponse.success("规则已删除");
        } catch (Exception e) {
            return ApiResponse.error("删除规则失败: " + e.getMessage());
        }
    }

    /**
     * 启动转发监听（从请求体读取配置并保存到数据库，然后启动）。
     * 前端调用: POST /forward/listener/start  body: { sourceChatIds, targetChatId }
     */
    @PostMapping("/listener/start")
    public ApiResponse<String> startListener(@RequestBody Map<String, Object> body) {
        try {
            Object sourceIds = body.get("sourceChatIds");
            Object targetId = body.get("targetChatId");

            if (sourceIds == null || targetId == null) {
                return ApiResponse.error("sourceChatIds 和 targetChatId 不能为空");
            }

            if (!(sourceIds instanceof java.util.List)) {
                return ApiResponse.error("sourceChatIds 必须是数组");
            }

            // 1. 先将配置写入数据库
            TelegramConfig cfg = configMapper.findByConfigName("default").orElse(null);
            if (cfg == null) {
                cfg = new TelegramConfig("default");
            }
            cfg.setForwardListenerEnabled(true);
            cfg.setForwardListenerSourceChatIds(json.writeValueAsString(sourceIds));
            cfg.setForwardListenerTargetChatId(Long.valueOf(targetId.toString()));
            configMapper.save(cfg);

            // 2. 重新加载配置并启动监听
            forwardService.stopListening();
            forwardService.startListening();

            return ApiResponse.success("转发监听已启动");
        } catch (Exception e) {
            log.error("启动转发监听失败", e);
            return ApiResponse.error("启动监听失败: " + e.getMessage());
        }
    }

    @PostMapping("/listener/stop")
    public ApiResponse<String> stopListener() {
        try {
            forwardService.stopListening();
            return ApiResponse.success("转发监听已停止");
        } catch (Exception e) {
            return ApiResponse.error("停止监听失败: " + e.getMessage());
        }
    }

    /**
     * 添加源聊天
     * 前端调用: POST /forward/listener/sources { chatId }
     */
    @PostMapping("/listener/sources")
    public ApiResponse<String> addSourceChat(@RequestBody Map<String, Object> body) {
        try {
            Object chatIdObj = body.get("chatId");
            if (chatIdObj == null) {
                return ApiResponse.error("chatId 不能为空");
            }
            Long chatId = Long.valueOf(chatIdObj.toString());
            
            TelegramConfig cfg = configMapper.findByConfigName("default").orElse(null);
            if (cfg == null) {
                cfg = new TelegramConfig("default");
            }
            
            // 获取现有sourceChatIds
            List<Long> sourceIds = new java.util.ArrayList<>();
            if (cfg.getForwardListenerSourceChatIds() != null && !cfg.getForwardListenerSourceChatIds().isEmpty()) {
                sourceIds = json.readValue(cfg.getForwardListenerSourceChatIds(), new TypeReference<List<Long>>() {});
            }
            if (!sourceIds.contains(chatId)) {
                sourceIds.add(chatId);
                cfg.setForwardListenerSourceChatIds(json.writeValueAsString(sourceIds));
                configMapper.save(cfg);
            }
            return ApiResponse.success("源聊天已添加");
        } catch (Exception e) {
            return ApiResponse.error("添加源聊天失败: " + e.getMessage());
        }
    }

    /**
     * 移除源聊天
     * 前端调用: DELETE /forward/listener/sources/{chatId}
     */
    @DeleteMapping("/listener/sources/{chatId}")
    public ApiResponse<String> removeSourceChat(@PathVariable Long chatId) {
        try {
            TelegramConfig cfg = configMapper.findByConfigName("default").orElse(null);
            if (cfg != null && cfg.getForwardListenerSourceChatIds() != null) {
                List<Long> sourceIds = json.readValue(cfg.getForwardListenerSourceChatIds(), new TypeReference<List<Long>>() {});
                sourceIds.remove(chatId);
                cfg.setForwardListenerSourceChatIds(json.writeValueAsString(sourceIds));
                configMapper.save(cfg);
            }
            return ApiResponse.success("源聊天已移除");
        } catch (Exception e) {
            return ApiResponse.error("移除源聊天失败: " + e.getMessage());
        }
    }

    /**
     * 设置目标聊天
     * 前端调用: POST /forward/listener/target { chatId }
     */
    @PostMapping("/listener/target")
    public ApiResponse<String> setTargetChat(@RequestBody Map<String, Object> body) {
        try {
            Object chatIdObj = body.get("chatId");
            if (chatIdObj == null) {
                return ApiResponse.error("chatId 不能为空");
            }
            Long chatId = Long.valueOf(chatIdObj.toString());
            
            TelegramConfig cfg = configMapper.findByConfigName("default").orElse(null);
            if (cfg == null) {
                cfg = new TelegramConfig("default");
            }
            cfg.setForwardListenerTargetChatId(chatId);
            configMapper.save(cfg);
            return ApiResponse.success("目标聊天已设置");
        } catch (Exception e) {
            return ApiResponse.error("设置目标聊天失败: " + e.getMessage());
        }
    }

    @PostMapping("/task/{id}/retry")
    public ApiResponse<String> retryTask(@PathVariable Long id) {
        try {
            forwardService.retryTask(id);
            return ApiResponse.success("任务已重试");
        } catch (Exception e) {
            return ApiResponse.error("重试任务失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/task/{id}")
    public ApiResponse<String> deleteTask(@PathVariable Long id) {
        try {
            forwardTaskMapper.deleteById(id);
            return ApiResponse.success("任务已删除");
        } catch (Exception e) {
            return ApiResponse.error("删除任务失败: " + e.getMessage());
        }
    }
}
