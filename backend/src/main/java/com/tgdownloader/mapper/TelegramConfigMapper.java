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
@Mapper
public interface TelegramConfigMapper {

    @Select("SELECT * FROM telegram_config WHERE config_name = #{configName} LIMIT 1")
    Optional<TelegramConfig> findByConfigName(@Param("configName") String configName);

    @Select("SELECT COUNT(*) FROM telegram_config")
    long countAll();

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
