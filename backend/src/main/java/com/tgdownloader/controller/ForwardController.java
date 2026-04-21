package com.tgdownloader.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgdownloader.dto.ApiResponse;
import com.tgdownloader.entity.ForwardTask;
import com.tgdownloader.entity.TelegramConfig;
import com.tgdownloader.repository.ForwardTaskRepository;
import com.tgdownloader.repository.TelegramConfigRepository;
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
    private ForwardTaskRepository forwardTaskRepository;

    @Autowired
    private TelegramConfigRepository configRepo;

    @Autowired
    private ForwardService forwardService;

    @GetMapping("/tasks")
    public ApiResponse<Map<String, Object>> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String chatId) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
            Page<ForwardTask> taskPage;

            if (status != null && !status.isEmpty()) {
                taskPage = forwardTaskRepository.findByStatus(status, pageRequest);
            } else if (chatId != null && !chatId.isEmpty()) {
                taskPage = forwardTaskRepository.findBySourceChatId(Long.valueOf(chatId), pageRequest);
            } else {
                taskPage = forwardTaskRepository.findAll(pageRequest);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("tasks", taskPage.getContent());
            result.put("total", taskPage.getTotalElements());
            result.put("pages", taskPage.getTotalPages());
            result.put("page", page);
            result.put("size", size);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取转发任务失败", e);
            return ApiResponse.error("获取转发任务失败: " + e.getMessage());
        }
    }

    // ===== 以下方法前端未使用，已注释 =====
    /*
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        try {
            long total = forwardTaskRepository.count();
            long success = forwardTaskRepository.countByStatus("SUCCESS");
            long failed = forwardTaskRepository.countByStatus("FAILED");
            long forwarding = forwardTaskRepository.countByStatus("FORWARDING");
            long stopped = forwardTaskRepository.countByStatus("STOPPED");

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("success", success);
            stats.put("failed", failed);
            stats.put("forwarding", forwarding);
            stats.put("stopped", stopped);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            return ApiResponse.error("获取统计失败: " + e.getMessage());
        }
    }
    */

    @GetMapping("/listener/config")
    public ApiResponse<Map<String, Object>> getListenerConfig() {
        try {
            TelegramConfig cfg = configRepo.findByConfigName("default").orElse(null);
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
            TelegramConfig cfg = configRepo.findByConfigName("default").orElse(null);
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
            configRepo.save(cfg);
            return ApiResponse.success("配置已保存");
        } catch (Exception e) {
            log.error("保存转发监听配置失败", e);
            return ApiResponse.error("保存配置失败: " + e.getMessage());
        }
    }

    // ===== 以下方法前端未使用，已注释 =====
    /*
    @GetMapping("/listener/status")
    public ApiResponse<Map<String, Object>> getListenerStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("listening", forwardService.isListening());
            status.put("sourceChatIds", new java.util.HashSet<>(forwardService.getSourceChatIds()));
            status.put("targetChatId", forwardService.getTargetChatId());
            return ApiResponse.success(status);
        } catch (Exception e) {
            return ApiResponse.error("获取监听状态失败: " + e.getMessage());
        }
    }
    */

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
            TelegramConfig cfg = configRepo.findByConfigName("default").orElse(null);
            if (cfg == null) {
                cfg = new TelegramConfig("default");
            }
            cfg.setForwardListenerEnabled(true);
            cfg.setForwardListenerSourceChatIds(json.writeValueAsString(sourceIds));
            cfg.setForwardListenerTargetChatId(Long.valueOf(targetId.toString()));
            configRepo.save(cfg);

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

    // ===== 以下方法前端未使用，已注释 =====
    /*
    @PostMapping("/task/{id}/cancel")
    public ApiResponse<String> cancelTask(@PathVariable Long id) {
        try {
            forwardService.stopTask(id);
            return ApiResponse.success("任务已取消");
        } catch (Exception e) {
            return ApiResponse.error("取消任务失败: " + e.getMessage());
        }
    }
    */

    @PostMapping("/task/{id}/retry")
    public ApiResponse<String> retryTask(@PathVariable Long id) {
        try {
            forwardService.retryTask(id);
            return ApiResponse.success("任务已重试");
        } catch (Exception e) {
            return ApiResponse.error("重试任务失败: " + e.getMessage());
        }
    }

    // ===== 以下方法前端未使用，已注释 =====
    /*
    @PostMapping("/task/batch-cancel")
    public ApiResponse<String> batchCancel(@RequestBody Map<String, Object> body) {
        try {
            Object ids = body.get("ids");
            if (ids instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) ids;
                for (Object id : list) {
                    forwardService.stopTask(((Number) id).longValue());
                }
            }
            return ApiResponse.success("批量取消完成");
        } catch (Exception e) {
            return ApiResponse.error("批量取消失败: " + e.getMessage());
        }
    }

    @PostMapping("/task/batch-retry")
    public ApiResponse<String> batchRetry(@RequestBody Map<String, Object> body) {
        try {
            Object ids = body.get("ids");
            if (ids instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) ids;
                for (Object id : list) {
                    forwardService.retryTask(((Number) id).longValue());
                }
            }
            return ApiResponse.success("批量重试完成");
        } catch (Exception e) {
            return ApiResponse.error("批量重试失败: " + e.getMessage());
        }
    }
    */

    @DeleteMapping("/task/{id}")
    public ApiResponse<String> deleteTask(@PathVariable Long id) {
        try {
            forwardTaskRepository.deleteById(id);
            return ApiResponse.success("任务已删除");
        } catch (Exception e) {
            return ApiResponse.error("删除任务失败: " + e.getMessage());
        }
    }
}
