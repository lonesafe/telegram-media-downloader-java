package com.tgdownloader;

import it.tdlight.Init;
import it.tdlight.Log;
import it.tdlight.Slf4JLogMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Telegram Media Downloader - Java Version
 * 无 Lombok 版本
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class TelegramMediaDownloaderApplication {

    private static final Logger log = LoggerFactory.getLogger(TelegramMediaDownloaderApplication.class);

    static {
        try {
            Init.init();
            log.info("[TDLight] TDLight 原生库加载成功");
        } catch (Exception e) {
            log.error("[TDLight] TDLight 初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("TDLight 初始化失败: " + e.getMessage(), e);
        }

        Log.setLogMessageHandler(0, new Slf4JLogMessageHandler());
        log.info("[TDLight] 日志 handler 已配置");
    }

    public static void main(String[] args) {
        SpringApplication.run(TelegramMediaDownloaderApplication.class, args);
    }
}
