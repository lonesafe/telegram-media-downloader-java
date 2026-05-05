package com.tgdownloader.mapper;

import com.mybatisflex.core.BaseMapper;
import com.tgdownloader.entity.TelegramConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * TelegramConfig Mapper - XML 版
 */
public interface TelegramConfigMapper extends BaseMapper<TelegramConfig> {

    Optional<TelegramConfig> findByConfigName(@Param("configName") String configName);

    long countAll();

    List<TelegramConfig> findAll();

    default TelegramConfig save(TelegramConfig entity) {
        insertOrUpdate(entity);
        return entity;
    }

    default TelegramConfig findByConfigNameEntity(String configName) {
        return findByConfigName(configName).orElse(null);
    }
}
