package com.tgdownloader.mapper;

import com.mybatisflex.core.BaseMapper;
import com.tgdownloader.entity.ChatConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * ChatConfig Mapper - XML 版
 */
public interface ChatConfigMapper extends BaseMapper<ChatConfig> {

    Optional<ChatConfig> findByChatId(@Param("chatId") String chatId);

    ChatConfig findById(Long id);

    long countAll();

    List<ChatConfig> findAll();

    default ChatConfig save(ChatConfig entity) {
        insertOrUpdate(entity);
        return entity;
    }

    default ChatConfig findByChatIdEntity(String chatId) {
        return findByChatId(chatId).orElse(null);
    }
}