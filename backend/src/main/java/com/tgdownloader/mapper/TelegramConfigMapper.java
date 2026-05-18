package com.tgdownloader.mapper;

import com.tgdownloader.entity.TelegramConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * TelegramConfig Mapper - XML 版（原生 MyBatis）
 */
public interface TelegramConfigMapper {

    Optional<TelegramConfig> findByConfigName(@Param("configName") String configName);

    List<TelegramConfig> findAll();

    void insert(TelegramConfig entity);

    void update(TelegramConfig entity);

    default TelegramConfig save(TelegramConfig entity) {
        if (entity.getId() == null) {
            insert(entity);
        } else {
            update(entity);
        }
        return entity;
    }

    default TelegramConfig findByConfigNameEntity(String configName) {
        return findByConfigName(configName).orElse(null);
    }
}
