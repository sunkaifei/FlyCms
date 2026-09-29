package com.flycms.module.comment.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.module.comment.dao.CommentDao;
import com.flycms.module.comment.model.Comment;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.other.service.FilterKeywordService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台评论服务（E4）。旧模块评论（fly_article_comment 等）随阶段 U3 退役，
 * 评论统一落 {@code fly_comment}，按 target_model + target_id 引用任意自定义模型内容。
 *
 * <p>先审后显开关沿用既有 config 键 {@code fly_comment_audit}（1=先审后显，落 status=0）。
 * 目标内容存在性/发布状态校验走 ModelDataService（与 E1 relate 校验同口径）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    /** 评论内容长度上限（与 fly_comment.content varchar(2000) 对齐） */
    private static final int MAX_CONTENT_LEN = 2000;

    @Autowired
    private CommentDao commentDao;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private FilterKeywordService filterKeywordService;
    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    /** G18：评论事件（action：comment_add / comment_audit / comment_delete） */
    private void publishCommentEvent(String action, Comment c, Long operatorId) {
        try {
            if (c != null) {
                eventPublisher.publishEvent(new com.flycms.core.event.ContentChangedEvent(
                        this, action, c.getTargetModel(), List.of(c.getTargetId()), operatorId));
            }
        } catch (Exception ignored) {
            // 事件发布失败不阻断评论主流程
        }
    }

    // /////////////////// 写入 ///////////////////

    /**
     * 前台用户发表评论。
     *
     * @param targetModel 目标模型 code
     * @param targetId    目标内容 id
     * @param userId      评论人（前台 session 用户，调用方保证非空）
     * @param content     评论内容
     * @param parentId    父评论 id（可空 = 顶层）
     */
    public DataVo addComment(String targetModel, Long targetId, Long userId, String content, Long parentId) {
        if (StringUtils.isBlank(targetModel)) {
            return DataVo.failure("目标模型不能为空");
        }
        if (targetId == null || targetId <= 0) {
            return DataVo.failure("目标内容参数错误");
        }
        if (userId == null || userId <= 0) {
            return DataVo.failure("请登录后评论");
        }
        String text = StringUtils.trimToEmpty(content);
        if (text.isEmpty()) {
            return DataVo.failure("评论内容不能为空");
        }
        if (text.length() > MAX_CONTENT_LEN) {
            return DataVo.failure("评论内容不能超过" + MAX_CONTENT_LEN + "字");
        }
        final String code;
        try {
            code = SqlSafeUtil.safeModelCode(targetModel);
        } catch (IllegalArgumentException e) {
            return DataVo.failure("目标模型不合法");
        }
        Model model = modelService.findModelByCode(code);
        if (model == null || model.getStatus() != 1) {
            return DataVo.failure("目标模型不存在或已禁用");
        }
        // E9 模型级评论开关：0 = 该模型关闭评论（前台发表直接拒绝）
        if (model.getEnableComment() != null && model.getEnableComment() == 0) {
            return DataVo.failure("该内容未开放评论");
        }
        // 目标必须存在且已发布（与 E1 relate 目标校验同口径）
        Map<String, Object> target = modelDataService.findDataById(model.getId(), targetId);
        if (target == null || !String.valueOf(target.get("status")).equals("1")) {
            return DataVo.failure("评论的内容不存在或未发布");
        }
        // 父评论必须属于同一目标
        if (parentId != null && parentId > 0) {
            Comment parent = commentDao.findCommentById(parentId);
            if (parent == null || parent.getStatus() != 1
                    || !code.equals(parent.getTargetModel()) || !targetId.equals(parent.getTargetId())) {
                return DataVo.failure("回复的评论不存在");
            }
        }
        // 违禁词过滤（与旧评论通道同源）
        text = filterKeywordService.doFilter(text);

        Comment comment = new Comment();
        comment.setId(SnowFlake.getInstance().nextId());
        comment.setTargetModel(code);
        comment.setTargetId(targetId);
        comment.setUserId(userId);
        comment.setParentId(parentId == null ? 0L : parentId);
        comment.setContent(text);
        // 评论审核开关（沿用 fly_comment_audit）：1=先审后显（落 0），0=免审直显（落 1）
        boolean auditFirst = configService.getIntKey("fly_comment_audit", 1) == 1;
        comment.setStatus(auditFirst ? 0 : 1);
        comment.setCreateTime(new Date());
        commentDao.addComment(comment);
        if (comment.getStatus() == 1) {
            adjustTargetCommentCount(code, targetId, 1);
        }
        // G18：评论发表事件
        publishCommentEvent("comment_add", comment, userId);
        return DataVo.success(auditFirst ? "评论已提交，审核通过后显示" : "评论成功");
    }

    // /////////////////// 后台管理 ///////////////////

    /**
     * 评论管理分页。rows 附带 targetTitle（所属内容标题）便于后台辨识。
     *
     * @param status null 查全部；0 待审 1 通过 2 未通过
     */
    public PageVo<Map<String, Object>> getCommentPage(String targetModel, Long targetId, Integer status,
                                                      String keyword, String createTime,
                                                      int pageNum, int rows) {
        PageVo<Map<String, Object>> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.isNotBlank(targetModel)) {
            try {
                where.add("target_model = #{params.targetModel}");
                params.put("targetModel", SqlSafeUtil.safeModelCode(targetModel));
            } catch (IllegalArgumentException ignored) {
                // 非法模型码按不过滤处理
            }
        }
        if (targetId != null && targetId > 0) {
            where.add("target_id = #{params.targetId}");
            params.put("targetId", targetId);
        }
        if (status != null && status >= 0) {
            where.add("status = #{params.status}");
            params.put("status", status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            where.add("content LIKE CONCAT('%', #{params.keyword}, '%')");
            params.put("keyword", keyword);
        }
        if (StringUtils.isNotBlank(createTime)) {
            where.add("create_time LIKE CONCAT(#{params.createTime}, '%')");
            params.put("createTime", createTime);
        }
        String whereSql = String.join(" AND ", where);
        pageVo.setCount(commentDao.countPage(whereSql, params));
        List<Comment> list = commentDao.selectPage(whereSql, "id desc",
                pageVo.getOffset(), pageVo.getRows(), params);
        List<Map<String, Object>> rowsList = new ArrayList<>();
        for (Comment c : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", String.valueOf(c.getId()));
            row.put("targetModel", c.getTargetModel());
            row.put("targetId", String.valueOf(c.getTargetId()));
            row.put("userId", String.valueOf(c.getUserId()));
            row.put("parentId", String.valueOf(c.getParentId()));
            row.put("content", c.getContent());
            row.put("status", c.getStatus());
            row.put("createTime", c.getCreateTime());
            row.put("targetTitle", resolveTargetTitle(c.getTargetModel(), c.getTargetId()));
            rowsList.add(row);
        }
        pageVo.setList(rowsList);
        return pageVo;
    }

    /** 审核单条：1=通过（计数 +1）2=未通过（已通过的计数 -1） */
    public DataVo auditComment(Long id, Integer status) {
        Comment comment = commentDao.findCommentById(id);
        if (comment == null) {
            return DataVo.failure("评论不存在");
        }
        int target = (status != null && status == 2) ? 2 : 1;
        if (comment.getStatus() == 0 && target == 1) {
            adjustTargetCommentCount(comment.getTargetModel(), comment.getTargetId(), 1);
        }
        if (comment.getStatus() == 1 && target == 2) {
            adjustTargetCommentCount(comment.getTargetModel(), comment.getTargetId(), -1);
        }
        commentDao.updateStatus(id, target);
        // G18：评论审核事件
        publishCommentEvent("comment_audit", comment, null);
        return DataVo.success("操作成功");
    }

    public DataVo deleteComment(Long id) {
        Comment comment = commentDao.findCommentById(id);
        if (comment == null) {
            return DataVo.failure("评论不存在");
        }
        if (comment.getStatus() == 1) {
            adjustTargetCommentCount(comment.getTargetModel(), comment.getTargetId(), -1);
        }
        commentDao.deleteComment(id);
        // G18：评论删除事件
        publishCommentEvent("comment_delete", comment, null);
        return DataVo.success("删除成功");
    }

    /**
     * 批量操作：status 1=批量通过 2=批量驳回 3=批量删除（沿用原评论管理接口契约）。
     */
    public DataVo batchComment(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return DataVo.failure("请选择要操作的评论");
        }
        if (status != null && status == 3) {
            for (Long id : ids) {
                deleteComment(id);
            }
            return DataVo.success("批量删除成功");
        }
        int target = (status != null && status == 2) ? 2 : 1;
        for (Long id : ids) {
            auditComment(id, target);
        }
        return DataVo.success("操作成功");
    }

    /** 待审数量（后台角标） */
    public int countPending() {
        return commentDao.countByStatus(0);
    }

    // /////////////////// 前台列表（fly_commentpage 标签用） ///////////////////

    /** 目标内容下的已通过评论（时间正序，楼层语义） */
    public List<Comment> listByTarget(String targetModel, Long targetId, int page, int rows) {
        Map<String, Object> params = new HashMap<>();
        params.put("targetModel", SqlSafeUtil.safeModelCode(targetModel));
        params.put("targetId", targetId);
        params.put("offset", (Math.max(page, 1) - 1) * rows);
        params.put("rows", rows);
        return commentDao.selectPage("target_model = #{params.targetModel} AND target_id = #{params.targetId} AND status = 1",
                "id asc", (Math.max(page, 1) - 1) * rows, rows, params);
    }

    public int countByTarget(String targetModel, Long targetId) {
        Map<String, Object> params = new HashMap<>();
        params.put("targetModel", SqlSafeUtil.safeModelCode(targetModel));
        params.put("targetId", targetId);
        return commentDao.countPage("target_model = #{params.targetModel} AND target_id = #{params.targetId} AND status = 1",
                params);
    }

    /**
     * 内容删除联动：清理目标内容下的全部评论（ModelDataService.deleteData 级联调用）。
     */
    public void deleteByTarget(String targetModel, List<Long> targetIds) {
        if (StringUtils.isBlank(targetModel) || targetIds == null || targetIds.isEmpty()) {
            return;
        }
        try {
            commentDao.deleteByTarget(SqlSafeUtil.safeModelCode(targetModel), targetIds);
        } catch (IllegalArgumentException e) {
            logger.warn("评论级联清理：非法模型码 {}", targetModel);
        }
    }

    // /////////////////// 内部 ///////////////////

    /**
     * 目标内容 count_comment 计数增减（固有列，直连 DAO 层避免 service 环依赖）。
     */
    private void adjustTargetCommentCount(String modelCode, Long targetId, int delta) {
        // 由 ModelDataService 提供的固有列计数调整（列名/表名均过白名单）
        modelDataService.adjustCommentCount(modelCode, targetId, delta);
    }

    private String resolveTargetTitle(String modelCode, Long targetId) {
        try {
            Model model = modelService.findModelByCode(SqlSafeUtil.safeModelCode(modelCode));
            if (model == null) {
                return "";
            }
            Map<String, Object> row = modelDataService.findDataById(model.getId(), targetId);
            return row == null ? "" : String.valueOf(row.get("title"));
        } catch (Exception e) {
            logger.debug("解析评论目标标题失败：{}#{}", modelCode, targetId);
            return "";
        }
    }
}
