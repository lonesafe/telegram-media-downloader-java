package com.tgdownloader.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.tgdownloader.entity.ForwardTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ForwardTask Mapper - XML 版
 */
public interface ForwardTaskMapper extends BaseMapper<ForwardTask> {

    Page<ForwardTask> findByStatus(@Param("status") String status, int pageNum, int pageSize);

    Page<ForwardTask> findBySourceChatId(@Param("sourceChatId") Long sourceChatId, int pageNum, int pageSize);

    Page<ForwardTask> findAll(int pageNum, int pageSize);

    long countByStatus(@Param("status") String status);

    long selectCount();

    ForwardTask selectById(Long id);

    boolean existsBySourceChatIdAndMessageIdAndTargetChatId(
            @Param("sourceChatId") Long sourceChatId,
            @Param("messageId") Long messageId,
            @Param("targetChatId") Long targetChatId);

    // ── Spring Data PageRequest 适配 ────────────────────────────────────

    default Page<ForwardTask> findByStatus(String status, org.springframework.data.domain.PageRequest pageRequest) {
        return findByStatus(status, pageRequest.getPageNumber(), pageRequest.getPageSize());
    }

    default Page<ForwardTask> findBySourceChatId(Long sourceChatId, org.springframework.data.domain.PageRequest pageRequest) {
        return findBySourceChatId(sourceChatId, pageRequest.getPageNumber(), pageRequest.getPageSize());
    }

    default Page<ForwardTask> findAll(org.springframework.data.domain.PageRequest pageRequest) {
        return findAll(pageRequest.getPageNumber(), pageRequest.getPageSize());
    }

    default ForwardTask save(ForwardTask entity) {
        insertOrUpdate(entity);
        return entity;
    }
}