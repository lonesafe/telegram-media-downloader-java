package com.tgdownloader.mapper;

import com.tgdownloader.entity.DownloadTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * DownloadTask Mapper - XML 版（原生 MyBatis）
 * <p>
 * 提供下载任务（DownloadTask）的数据库操作接口，包括 CRUD、按状态查询、分页查询等。
 * 所有 SQL 语句在对应的 DownloadTaskMapper.xml 文件中定义。
 * </p>
 * 
 * @author Telegram Media Downloader
 * @version 1.0.0
 * @since 2024
 */
@Mapper
public interface DownloadTaskMapper {

    /**
     * 全量查询所有下载任务（不分页）
     * <p>
     * 注意：此方法会加载表中所有记录，数据量大时可能影响性能。
     * 建议使用分页查询 {@link #findByStatusIn(String, int, int)} 或 {@link #findAllPage(int, int)}。
     * </p>
     * 
     * @return 所有下载任务的列表，按 id 降序排列
     */
    List<DownloadTask> findAll();

    /**
     * 全量查询（别名，兼容旧代码）
     * <p>
     * 此方法为 {@link #findAll()} 的别名，保持向后兼容。
     * </p>
     * 
     * @return 所有下载任务的列表
     */
    default List<DownloadTask> selectAll() { return findAll(); }

    /**
     * 分页查询：按状态列表查询下载任务
     * <p>
     * 示例：statuses = "'PENDING', 'DOWNLOADING'"
     * </p>
     * 
     * @param statuses 状态列表字符串，格式为 SQL IN 子句，例如 "'PENDING', 'DOWNLOADING'"
     * @param offset   偏移量（从0开始）
     * @param limit    每页记录数
     * @return 符合条件的下载任务列表，按 id 降序排列
     */
    List<DownloadTask> findByStatusIn(@Param("statuses") String statuses, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 按状态查询下载任务（不分页）
     * 
     * @param status 任务状态，例如 "PENDING", "DOWNLOADING", "SUCCESS_DOWNLOAD" 等
     * @return 符合条件的下载任务列表
     */
    List<DownloadTask> findByStatus(@Param("status") String status);

    /**
     * 查询未完成的任务（排除终态）
     * <p>
     * 终态通常指 SUCCESS_DOWNLOAD、FAILED_DOWNLOAD、SKIP_DOWNLOAD 等。
     * 此方法用于查询仍在处理中的任务。
     * </p>
     * 
     * @param statuses 要排除的状态列表（终态），格式为 SQL IN 子句
     * @return 未完成的下载任务列表
     */
    List<DownloadTask> findUnfinishedTasks(@Param("statuses") String statuses);

    /**
     * 检查指定聊天和消息ID的任务是否已存在（去重）
     * <p>
     * 用于避免重复创建下载任务。
     * </p>
     * 
     * @param chatId    聊天ID（Telegram chat_id）
     * @param messageId 消息ID（Telegram message_id）
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByChatIdAndMessageId(@Param("chatId") String chatId, @Param("messageId") Long messageId);

    /**
     * 按状态列表统计任务数量
     * 
     * @param statuses 状态列表字符串，格式为 SQL IN 子句
     * @return 符合条件的任务数量
     */
    long countByStatusInStr(@Param("statuses") String statuses);

    /**
     * 按主键 ID 查询下载任务
     * 
     * @param id 任务主键 ID
     * @return 对应的下载任务，如果不存在则返回 null
     */
    DownloadTask findById(@Param("id") Long id);

    // ── CRUD 操作 ────────────────────────────────────────────────────────────────

    /**
     * 插入新下载任务（全字段）
     * <p>
     * 插入所有字段，包括空值。如果只需要插入非空字段，使用 {@link #insertSelective(DownloadTask)}。
     * </p>
     * 
     * @param entity 下载任务实体，必须包含 messageId, chatId, status 字段
     */
    void insert(DownloadTask entity);

    /**
     * 更新下载任务（全字段）
     * <p>
     * 更新所有字段，包括空值。根据主键 id 进行更新。
     * </p>
     * 
     * @param entity 下载任务实体，必须包含 id 字段
     */
    void update(DownloadTask entity);

    /**
     * 插入新下载任务（非空字段）
     * <p>
     * 只插入实体中不为 null 的字段，适用于部分字段有值的场景。
     * 插入后，自增主键 id 会自动回填到实体中。
     * </p>
     * 
     * @param entity 下载任务实体
     */
    void insertSelective(DownloadTask entity);

    /**
     * 按主键 ID 删除下载任务
     * 
     * @param id 任务主键 ID
     */
    void deleteById(@Param("id") Long id);

    /**
     * 统计所有下载任务的数量
     * 
     * @return 任务总数量
     */
    long countAll();

    /**
     * 查询收藏夹消息（chat_id 以 '-' 开头）带分页
     * <p>
     * 收藏夹消息的 chat_id 为负数（以 '-' 开头），例如 -1001234567890。
     * 此方法用于查询当前用户在 Telegram 收藏夹中的消息对应的下载任务。
     * </p>
     * 
     * @param offset 偏移量（从0开始）
     * @param limit  每页记录数
     * @return 收藏夹消息对应的下载任务列表，按 id 降序排列
     */
    List<DownloadTask> findSavedMessages(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询收藏夹任务（分页，按下载状态过滤）
     * <p>
     * 根据 {@code includeDownloaded} 参数过滤任务：
     * <ul>
     *   <li>{@code true} - 只返回已下载完成的任务（status = 'SUCCESS_DOWNLOAD'）</li>
     *   <li>{@code false} - 只返回待下载的任务（status != 'SUCCESS_DOWNLOAD'）</li>
     * </ul>
     * </p>
     * 
     * @param offset           偏移量（从0开始）
     * @param size             每页记录数
     * @param includeDownloaded 是否只包含已下载任务
     * @return 符合条件的收藏夹任务列表
     */
    List<DownloadTask> findSavedMessagesTasks(@Param("offset") int offset, @Param("size") int size, @Param("includeDownloaded") boolean includeDownloaded);

    /**
     * 统计收藏夹消息数量
     * 
     * @return 收藏夹消息对应的下载任务数量
     */
    long countSavedMessages();

    /**
     * 统计收藏夹任务数量（按下载状态过滤）
     * <p>
     * 根据 {@code includeDownloaded} 参数过滤任务。
     * </p>
     * 
     * @param includeDownloaded 是否只包含已下载任务
     * @return 符合条件的收藏夹任务数量
     */
    long countSavedMessagesTasks(@Param("includeDownloaded") boolean includeDownloaded);

    /**
     * 按状态统计收藏夹消息数量
     * <p>
     * 用于统计收藏夹中特定状态的任务数量，例如已下载、待下载等。
     * </p>
     * 
     * @param statuses 状态列表字符串，格式为 SQL IN 子句
     * @return 符合条件的收藏夹消息任务数量
     */
    long countSavedMessagesByStatuses(@Param("statuses") String statuses);

    // ── 便捷包装方法（default 实现，避免方法重载） ──────────────────────────

    /**
     * 按状态列表分页查询（便捷方法）
     * <p>
     * 此方法是 {@link #findByStatusIn(String, int, int)} 的包装，接受 List&lt;String&gt; 参数，
     * 自动转换为 SQL IN 子句格式。
     * </p>
     * 
     * @param statuses 状态列表，例如 ["PENDING", "DOWNLOADING"]
     * @param page     页码（从1开始）
     * @param size     每页记录数
     * @return 符合条件的下载任务列表
     */
    default List<DownloadTask> findByStatuses(List<String> statuses, int page, int size) {
        String statusesStr = String.join(",", statuses.stream().map(s -> "'" + s + "'").toList());
        int offset = Math.max(0, (page - 1) * size);
        return findByStatusIn(statusesStr, offset, size);
    }

    /**
     * 按状态列表查询（不分页，便捷方法）
     * <p>
     * 查询所有符合条件的状态的任务，不分页。数据量大时不建议使用。
     * </p>
     * 
     * @param statuses 状态列表
     * @return 符合条件的下载任务列表
     */
    default List<DownloadTask> findByStatuses(List<String> statuses) {
        String statusesStr = String.join(",", statuses.stream().map(s -> "'" + s + "'").toList());
        return findByStatusIn(statusesStr, 0, Integer.MAX_VALUE);
    }

    /**
     * 按状态列表统计任务数量（便捷方法）
     * 
     * @param statuses 状态列表
     * @return 符合条件的任务数量
     */
    default long countByStatuses(List<String> statuses) {
        String statusesStr = String.join(",", statuses.stream().map(s -> "'" + s + "'").toList());
        return countByStatusInStr(statusesStr);
    }

    /**
     * 全量分页查询（兼容旧代码）
     * <p>
     * 此方法使用虚拟状态 "DUMMY" 来复用 {@link #findByStatusIn(String, int, int)} 实现全量分页。
     * 建议直接使用 SQL 中的 LIMIT/OFFSET 实现全量分页。
     * </p>
     * 
     * @param page 页码（从1开始）
     * @param size 每页记录数
     * @return 下载任务列表
     */
    default List<DownloadTask> findAllPage(int page, int size) {
        return findByStatusIn("'DUMMY'", Math.max(0, (page - 1) * size), size);
    }

    /**
     * 保存下载任务（插入）
     * <p>
     * 此方法调用 {@link #insertSelective(DownloadTask)} 插入任务，并返回实体本身。
     * 插入后，自增主键 id 会自动回填到实体中。
     * </p>
     * 
     * @param entity 下载任务实体
     * @return 保存后的实体（包含主键 id）
     */
    default DownloadTask saveTask(DownloadTask entity) {
        insertSelective(entity);
        return entity;
    }

    /**
     * 保存下载任务（别名，兼容旧代码）
     * 
     * @param entity 下载任务实体
     * @return 保存后的实体
     */
    default DownloadTask save(DownloadTask entity) {
        return saveTask(entity);
    }

    /**
     * 插入或更新下载任务（兼容旧代码）
     * <p>
     * 如果实体 id 为 null，则插入新记录；否则更新已有记录。
     * </p>
     * 
     * @param entity 下载任务实体
     * @return 保存后的实体
     */
    default DownloadTask insertOrUpdate(DownloadTask entity) {
        if (entity.getId() == null) {
            insertSelective(entity);
        } else {
            update(entity);
        }
        return entity;
    }
}
