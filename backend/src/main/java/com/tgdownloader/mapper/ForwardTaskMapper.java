package com.tgdownloader.mapper;

import com.tgdownloader.entity.ForwardTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ForwardTask Mapper - XML 版（原生 MyBatis）
 */
@Mapper
public interface ForwardTaskMapper {

    /** 按状态查询 */
    List<ForwardTask> findByStatus(@Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);

    /** 按状态查询（不分页） */
    default List<ForwardTask> findByStatus(String status) {
        return findByStatus(status, 0, Integer.MAX_VALUE);
    }

    /** 按源chatId查询 */
    List<ForwardTask> findBySourceChatId(@Param("sourceChatId") Long sourceChatId, @Param("offset") int offset, @Param("limit") int limit);

    /** 按源chatId查询（不分页） */
    default List<ForwardTask> findBySourceChatId(Long sourceChatId) {
        return findBySourceChatId(sourceChatId, 0, Integer.MAX_VALUE);
    }

    /** 全量查询（不分页） */
    List<ForwardTask> findAllNoPage();

    /** 全量计数 */
    long selectCount();

    /** 全量分页 */
    List<ForwardTask> findAll(@Param("offset") int offset, @Param("limit") int limit);

    /** 按状态计数 */
    long countByStatus(@Param("status") String status);

    /** 按主键查询 */
    ForwardTask selectById(@Param("id") Long id);

    /** 检查是否已存在（用于去重） */
    boolean existsBySourceChatIdAndMessageIdAndTargetChatId(
            @Param("sourceChatId") Long sourceChatId,
            @Param("messageId") Long messageId,
            @Param("targetChatId") Long targetChatId);

    // ── CRUD ────────────────────────────────────────────────────────────────

    void insert(ForwardTask entity);

    void update(ForwardTask entity);

    void insertSelective(ForwardTask entity);

    void deleteById(@Param("id") Long id);

    default ForwardTask save(ForwardTask entity) {
        if (entity.getId() == null) {
            insertSelective(entity);
        } else {
            update(entity);
        }
        return entity;
    }

    default List<ForwardTask> selectAll() {
        return findAllNoPage();
    }
}