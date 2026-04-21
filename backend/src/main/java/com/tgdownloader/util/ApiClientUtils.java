package com.tgdownloader.util;

/**
 * API 客户端工具类
 *
 * 原 org.drinkless.tdlib.Client 方式需要静态持有 Client 实例，
 * 切换到 TDLight 后，TelegramClientService / BotClientService 各自持有
 * SimpleTelegramClient 实例，不再需要这个工具类做中转。
 *
 * 保留此类供未来扩展用（如静态工具方法）。
 */
public class ApiClientUtils {

    private ApiClientUtils() {}

    // 注意：TDLight 架构下，客户端由各自 Service 独立管理，无需静态持有
}
