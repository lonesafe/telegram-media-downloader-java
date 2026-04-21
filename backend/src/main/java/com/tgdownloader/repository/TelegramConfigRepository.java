package com.tgdownloader.repository;

import com.tgdownloader.entity.TelegramConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Telegram 配置仓库
 *
 * 提供 TelegramConfig 的数据库 CRUD 操作
 *
 * 注意：当前设计中配置为单例（configName="default"），
 * 所有配置共用一条记录，通过 findByConfigName("default") 获取
 */
@Repository
public interface TelegramConfigRepository extends JpaRepository<TelegramConfig, Long> {

    /**
     * 通过配置名称查找配置
     *
     * @param configName 配置名称（目前固定为 "default"）
     * @return 存在时返回配置，否则返回空
     */
    Optional<TelegramConfig> findByConfigName(String configName);
}
