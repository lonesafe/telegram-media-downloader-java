package com.tgdownloader.repository;

import com.tgdownloader.entity.ForwardTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 转发任务 Repository - 无 Lombok 版本
 */
@Repository
public interface ForwardTaskRepository extends JpaRepository<ForwardTask, Long> {

    // 检查是否存在（去重）
    boolean existsBySourceChatIdAndMessageIdAndTargetChatId(Long sourceChatId, Long messageId, Long targetChatId);

    // 按状态查询
    Page<ForwardTask> findByStatus(String status, Pageable pageable);

    // 查询待转发的任务
    List<ForwardTask> findByStatusIn(List<String> statuses);

    // 按是否自动转发查询
    Page<ForwardTask> findByIsAutoForward(Boolean isAutoForward, Pageable pageable);

    // 按源 chat 查询
    Page<ForwardTask> findBySourceChatId(Long sourceChatId, Pageable pageable);

    // 更新状态
    // 注意：使用 @Modifying + @Query 来执行批量更新

    // 查询用户手动提交的转发任务
    Page<ForwardTask> findByIsAutoForwardFalse(Pageable pageable);

    // 查询自动监听的转发任务
    Page<ForwardTask> findByIsAutoForwardTrue(Pageable pageable);

    // 按状态查询（不分页）
    List<ForwardTask> findByStatus(String status);

    // 统计各状态数量
    long countByStatus(String status);
}