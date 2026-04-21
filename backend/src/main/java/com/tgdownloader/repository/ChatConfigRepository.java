package com.tgdownloader.repository;

import com.tgdownloader.entity.ChatConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 聊天配置仓库
 *
 * 提供 ChatConfig 的数据库 CRUD 操作
 *
 * 每个 Telegram 频道/群组对应一条 ChatConfig 记录
 */
@Repository
public interface ChatConfigRepository extends JpaRepository<ChatConfig, Long> {

    /**
     * 通过 Telegram 聊天 ID 查找配置
     *
     * @param chatId Telegram 聊天 ID
     * @return 存在时返回配置，否则返回空
     */
    Optional<ChatConfig> findByChatId(String chatId);

}
