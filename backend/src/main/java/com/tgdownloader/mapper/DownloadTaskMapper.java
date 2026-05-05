package com.tgdownloader.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.tgdownloader.entity.DownloadTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * DownloadTask Mapper - XML 版
 */
public interface DownloadTaskMapper extends BaseMapper<DownloadTask> {

    // ── XML SQL 方法 ──────────────────────────────────────────────────────────

    /**
     * 全量查询（不分页）
     */
    List<DownloadTask> findAll();

    /**
     * 分页查询：按状态列表
     */
    Page<DownloadTask> findByStatusIn(@Param("statuses") String statuses, int pageNum, int pageSize);

    /**
     * 分页查询：全部
     */
    Page<DownloadTask> findAll(int pageNum, int pageSize);

    /**
     * 按状态查询（不分页）
     */
    List<DownloadTask> findByStatus(@Param("status") String status);

    /**
     * 查询未完成任务（排除终态）
     */
    List<DownloadTask> findUnfinishedTasks(@Param("statuses") String statuses);

    /**
     * 检查是否存在（去重）
     */
    boolean existsByChatIdAndMessageId(@Param("chatId") String chatId, @Param("messageId") Long messageId);

    /**
     * 按状态计数（String 版）
     */
    long countByStatusInStr(@Param("statuses") String statuses);

    /**
     * 按 ID 查询
     */
    Optional<DownloadTask> findById(@Param("id") Long id);

    // ── 便捷包装方法（default 实现） ─────────────────────────────────────────

    /**
     * 适配 Spring Data PageRequest 的分页重载
     */
    default Page<DownloadTask> findAll(org.springframework.data.domain.PageRequest pageRequest) {
        return findAll(pageRequest.getPageNumber(), pageRequest.getPageSize());
    }

    /**
     * 适配 Spring Data PageRequest 的按状态分页
     */
    default Page<DownloadTask> findByStatusIn(List<String> statuses, org.springframework.data.domain.PageRequest pageRequest) {
        String statusesStr = String.join(",", statuses.stream().map(s -> "'" + s + "'").toList());
        return findByStatusIn(statusesStr, pageRequest.getPageNumber(), pageRequest.getPageSize());
    }

    /**
     * 按状态列表查询（不分页），使用 QueryWrapper
     */
    default List<DownloadTask> findByStatusIn(List<String> statuses) {
        String statusesStr = String.join(",", statuses.stream().map(s -> "'" + s + "'").toList());
        QueryWrapper q = QueryWrapper.create().in("status", (Object[]) statuses.toArray());
        return selectListByQuery(q);
    }

    /**
     * 按状态列表计数
     */
    default long countByStatusIn(List<String> statuses) {
        String statusesStr = String.join(",", statuses.stream().map(s -> "'" + s + "'").toList());
        return countByStatusInStr(statusesStr);
    }

    // ── save 包装 ─────────────────────────────────────────────────────────
    default DownloadTask saveTask(DownloadTask entity) {
        insertSelective(entity);
        return entity;
    }

    default DownloadTask save(DownloadTask entity) {
        return saveTask(entity);
    }
}
