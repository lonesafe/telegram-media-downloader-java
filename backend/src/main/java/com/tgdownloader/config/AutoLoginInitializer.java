package com.tgdownloader.config;

import com.tgdownloader.entity.DownloadTask;
import com.tgdownloader.entity.TelegramConfig;
import com.tgdownloader.model.DownloadStatus;
import com.tgdownloader.repository.DownloadTaskRepository;
import com.tgdownloader.repository.TelegramConfigRepository;
import com.tgdownloader.service.BotClientService;
import com.tgdownloader.service.DownloadCoreService;
import com.tgdownloader.service.ForwardService;
import com.tgdownloader.controller.SavedMessagesController;
import com.tgdownloader.service.TelegramClientService;
import com.tgdownloader.util.TelegramUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Startup initializer
 * 1. Auto-login user Telegram + Bot
 * 2. Resume unfinished tasks, start forward listener and saved messages monitoring
 */
@Component
public class AutoLoginInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AutoLoginInitializer.class);

    @Autowired
    private TelegramUtils telegramUtils;

    @Autowired
    private TelegramConfigRepository configRepository;

    @Autowired
    private DownloadTaskRepository taskRepository;

    @Autowired
    private TelegramClientService telegramClientService;

    @Autowired
    private BotClientService botClientService;

    @Autowired
    private DownloadCoreService downloadCoreService;

    @Autowired
    private ForwardService forwardService;

    @Autowired
    private SavedMessagesController savedMessagesController;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== Startup init check ==========");

        TelegramConfig config = configRepository.findByConfigName("default").orElse(null);

        if (config != null) {
            tryAutoLoginUser(config);
            tryAutoLoginBot(config);
        } else {
            log.info("No config found, skip auto-login");
        }

        startResumeAndSavedMessagesCheck();
    }

    private void startResumeAndSavedMessagesCheck() {
        new Thread(() -> {
            int waited = 0;
            while (!telegramClientService.isConnected() && waited < 30000) {
                try { Thread.sleep(1000); waited += 1000; } catch (InterruptedException ignored) {}
            }

            if (!telegramClientService.isConnected()) {
                log.warn("Telegram connection timeout, skip init");
                return;
            }

            TelegramConfig cfg = configRepository.findByConfigName("default").orElse(null);

            // Resume unfinished download tasks
            resumeUnfinishedTasks();

            // Resume unfinished forward tasks
            forwardService.processPending();

            // Start forward listener
            if (cfg != null && Boolean.TRUE.equals(cfg.getForwardListenerEnabled())) {
                log.info("Starting forward listener...");
                forwardService.startListening();
            }

            // Start saved messages monitoring
            if (cfg != null && Boolean.TRUE.equals(cfg.getSavedMessagesEnabled())) {
                log.info("Starting saved messages monitoring...");
                savedMessagesController.start();
            }
        }, "ResumeTasks").start();
    }

    private void resumeUnfinishedTasks() {
        List<String> skipStatuses = List.of(
                DownloadStatus.SUCCESS_DOWNLOAD.name(),
                DownloadStatus.SKIP_DOWNLOAD.name(),
                DownloadStatus.PAUSED.name(),
                DownloadStatus.FAILED_DOWNLOAD.name()
        );
        List<DownloadTask> unfinished = taskRepository.findByStatusNotIn(skipStatuses, Pageable.unpaged())
                .getContent();

        if (unfinished.isEmpty()) {
            log.info("No unfinished download tasks");
            return;
        }

        AtomicInteger resumed = new AtomicInteger(0);
        for (DownloadTask task : unfinished) {
            if (DownloadStatus.DOWNLOADING.name().equals(task.getStatus())) {
                log.info("Task {} was running before crash, resetting status and resuming", task.getId());
                task.setStatus(DownloadStatus.PAUSED.name());
                telegramUtils.saveTask(task);
                downloadCoreService.resumeTask(task.getId());
            } else if (DownloadStatus.PAUSED.name().equals(task.getStatus()) || DownloadStatus.PENDING.name().equals(task.getStatus())) {
                downloadCoreService.resumeTask(task.getId());
            }
        }

        log.info("Resumed {} download tasks", resumed.get());
    }

    private void tryAutoLoginUser(TelegramConfig config) {
        boolean hasApiCredentials = config.getApiId() != null && !config.getApiId().isEmpty()
                && config.getApiHash() != null && !config.getApiHash().isEmpty();

        if (!hasApiCredentials) {
            log.info("No API ID/Hash configured, skip Telegram auto-login");
            return;
        }

        String baseDir = System.getProperty("user.dir");
        String dbDir = config.getDatabaseDirectory();
        if (dbDir == null || dbDir.isEmpty()) dbDir = baseDir + File.separator + "tdlib_db";

        File dbDirFile = new File(dbDir);
        boolean hasSession = dbDirFile.exists() && dbDirFile.list().length > 0;

        if (hasSession) {
            log.info("Detected Telegram session dir {}, trying auto-login...", dbDir);
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    telegramClientService.connect();
                    log.info("Telegram auto-login done, connected: {}",
                            telegramClientService.isConnected());
                } catch (Exception e) {
                    log.error("Telegram auto-login failed: {}", e.getMessage());
                }
            }, "TelegramAutoLogin").start();
        } else {
            log.info("No Telegram session dir {}, skip auto-login", dbDir);
        }
    }

    private void tryAutoLoginBot(TelegramConfig config) {
        String botToken = config.getBotToken();
        if (botToken == null || botToken.isEmpty()) {
            log.info("No Bot Token configured, skip Bot auto-connect");
            return;
        }
        log.info("Detected Bot Token, trying auto-connect...");
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                botClientService.connect(botToken);
                log.info("Bot auto-connect done, connected: {}",
                        botClientService.isConnected());
            } catch (Exception e) {
                log.error("Bot auto-connect failed: {}", e.getMessage());
            }
        }, "BotAutoLogin").start();
    }
}
