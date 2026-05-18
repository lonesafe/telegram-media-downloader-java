package com.tgdownloader.mapper;

import com.tgdownloader.entity.ChatConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * ChatConfig Mapper - XML 版（原生 MyBatis）
 */
public interface ChatConfigMapper {

    Optional<ChatConfig> findByChatId(@Param("chatId") String chatId);

    Optional<ChatConfig> findById(@Param("id") Long id);

    long countAll();

    List<ChatConfig> findAll();

    void insert(ChatConfig entity);

    void update(ChatConfig entity);

    void deleteById(@Param("id") Long id);

    default ChatConfig save(ChatConfig entity) {
        if (entity.getId() == null) {
            insert(entity);
        } else {
            update(entity);
        }
        return entity;
    }

    default ChatConfig findByChatIdEntity(String chatId) {
        return findByChatId(chatId).orElse(null);
    }

    /** 插入选择性字段（兼容旧代码） */
    default void insertSelective(ChatConfig entity) {
        insert(entity);
    }
}
