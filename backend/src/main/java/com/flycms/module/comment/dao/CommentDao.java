package com.flycms.module.comment.dao;

import com.flycms.module.comment.model.Comment;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 平台评论 DAO（E4）。whereSql 由 Service 组装，列名一律过 SqlSafeUtil 白名单。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface CommentDao {

    int addComment(Comment comment);

    Comment findCommentById(@Param("id") Long id);

    /** 通用分页（后台管理列表）：whereSql/orderBySql 由 Service 白名单组装 */
    List<Comment> selectPage(@Param("whereSql") String whereSql,
                             @Param("orderBySql") String orderBySql,
                             @Param("offset") int offset,
                             @Param("rows") int rows,
                             @Param("params") Map<String, Object> params);

    int countPage(@Param("whereSql") String whereSql,
                  @Param("params") Map<String, Object> params);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    int deleteComment(@Param("id") Long id);

    int batchDelete(@Param("ids") List<Long> ids);

    /** 内容删除时联动清理其全部评论（ModelDataService.deleteData 级联调用） */
    int deleteByTarget(@Param("targetModel") String targetModel, @Param("targetIds") List<Long> targetIds);

    int countByStatus(@Param("status") Integer status);
}
