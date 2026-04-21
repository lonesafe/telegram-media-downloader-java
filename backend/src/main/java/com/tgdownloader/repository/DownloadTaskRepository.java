package com.tgdownloader.repository;

import com.tgdownloader.entity.DownloadTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 下载任务仓库
 *
 * 提供 DownloadTask 的数据库 CRUD 操作
 *
 * 设计说明：
 * - 每个 DownloadTask 属于一个 ChatConfig（一对多）
 * - 任务状态统一由 status 字段管理（PENDING/DOWNLOADING/PAUSED/SUCCESS/FAILED/SKIP）
 */
@Repository
public interface DownloadTaskRepository extends JpaRepository<DownloadTask, Long> {

    /**
     * 通过状态查找任务列表
     */
    List<DownloadTask> findByStatus(String status);

    /**
     * 通过状态列表查找任务（排除某些状态）
     */
    Page<DownloadTask> findByStatusNotIn(List<String> statuses, Pageable pageable);

    /**
     * 通过状态列表查找任务（不分页）
     */
    List<DownloadTask> findByStatusIn(List<String> statuses);

    /**
     * 通过状态列表查找任务（分页）
     */
    Page<DownloadTask> findByStatusIn(List<String> statuses, Pageable pageable);

    /**
     * 统计指定状态的任务数量
     */
    long countByStatusIn(List<String> statuses);

    /**
     * 统计非指定状态的任务数量
     */
    long countByStatusNotIn(List<String> statuses);

    /**
     * 检查指定聊天和消息的任务是否已存在（用于去重）
     */
    boolean existsByChatIdAndMessageId(String chatId, Long messageId);

}
