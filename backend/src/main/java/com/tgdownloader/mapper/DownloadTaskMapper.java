package com.tgdownloader.mapper;

import com.tgdownloader.entity.DownloadTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * DownloadTask Mapper - XML 版（原生 MyBatis）
 */
@Mapper
public interface DownloadTaskMapper {

    /** 全量查询（不分页） */
    List<DownloadTask> findAll();

    /** 全量查询（别名，兼容旧代码） */
    default List<DownloadTask> selectAll() { return findAll(); }

    /** 分页查询：按状态列表（XML 映射） */
    List<DownloadTask> findByStatusIn(@Param("statuses") String statuses, @Param("offset") int offset, @Param("limit") int limit);

    /** 按状态查询（不分页） */
    List<DownloadTask> findByStatus(@Param("status") String status);

    /** 查询未完成任务（排除终态） */
    List<DownloadTask> findUnfinishedTasks(@Param("statuses") String statuses);

    /** 检查是否存在（去重） */
    boolean existsByChatIdAndMessageId(@Param("chatId") String chatId, @Param("messageId") Long messageId);

    /** 按状态计数（XML 映射） */
    long countByStatusInStr(@Param("statuses") String statuses);

    /** 按 ID 查询 */
    DownloadTask findById(@Param("id") Long id);

    // ── CRUD ────────────────────────────────────────────────────────────────

    void insert(DownloadTask entity);

    void update(DownloadTask entity);

    void insertSelective(DownloadTask entity);

    void deleteById(@Param("id") Long id);

    /** 全量计数 */
    long countAll();

    // ── 便捷包装方法（default 实现，避免方法重载） ──────────────────────────

    /**
     * 按状态列表分页查询（避免与 XML 映射方法重载）
     */
    default List<DownloadTask> findByStatuses(List<String> statuses, int page, int size) {
        String statusesStr = String.join(",", statuses.stream().map(s -> "'" + s + "'").toList());
        int offset = Math.max(0, (page - 1) * size);
        return findByStatusIn(statusesStr, offset, size);
    }

    /**
     * 按状态列表查询（不分页）
     */
    default List<DownloadTask> findByStatuses(List<String> statuses) {
        String statusesStr = String.join(",", statuses.stream().map(s -> "'" + s + "'").toList());
        return findByStatusIn(statusesStr, 0, Integer.MAX_VALUE);
    }

    /**
     * 按状态列表计数
     */
    default long countByStatuses(List<String> statuses) {
        String statusesStr = String.join(",", statuses.stream().map(s -> "'" + s + "'").toList());
        return countByStatusInStr(statusesStr);
    }

    /**
     * 全量分页查询
     */
    default List<DownloadTask> findAllPage(int page, int size) {
        return findByStatusIn("'DUMMY'", Math.max(0, (page - 1) * size), size);
    }

    default DownloadTask saveTask(DownloadTask entity) {
        insertSelective(entity);
        return entity;
    }

    default DownloadTask save(DownloadTask entity) {
        return saveTask(entity);
    }

    /** 插入或更新（兼容旧代码） */
    default DownloadTask insertOrUpdate(DownloadTask entity) {
        if (entity.getId() == null) {
            insertSelective(entity);
        } else {
            update(entity);
        }
        return entity;
    }
}
