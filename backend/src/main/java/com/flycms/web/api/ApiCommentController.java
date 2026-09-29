package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.comment.service.CommentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 评论管理 REST（E4 平台评论；U3 随旧文章评论通道退役重写）。
 *
 * <p>端点路径与响应信封保持不变（vben 评论管理页零改动迁移），
 * 数据源由 fly_article_comment 换为多态表 fly_comment（target_model + target_id）。
 * status 语义沿用：0待审 1通过 2未通过（3=删除，走 batch 契约）。
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Controller
@RequestMapping("/api")
public class ApiCommentController extends ApiBaseController {

    @Autowired
    private CommentService commentService;

    /**
     * 评论分页列表（行内附带 targetTitle 所属内容标题）
     *
     * @param status 不传=全部；0待审 1通过 2未通过
     */
    @ResponseBody
    @GetMapping("/system/comment/page")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows,
                       @RequestParam(value = "targetModel", required = false) String targetModel,
                       @RequestParam(value = "articleId", required = false) Long articleId,
                       @RequestParam(value = "userId", required = false) Long userId,
                       @RequestParam(value = "createTime", required = false) String createTime,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "status", required = false) Integer status) {
        requirePermission("/api/system/comment/page");
        // 兼容旧参数名 articleId（原文章评论过滤），映射为 targetId
        Long targetId = articleId;
        if (targetId == null) {
            String raw = request.getParameter("targetId");
            if (StringUtils.isNotBlank(raw) && StringUtils.isNumeric(raw)) {
                targetId = Long.parseLong(raw);
            }
        }
        PageVo<Map<String, Object>> pageVo = commentService.getCommentPage(
                targetModel, targetId, status, keyword, createTime, pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    /** 待审数量（角标） */
    @ResponseBody
    @GetMapping("/system/comment/pendingCount")
    public DataVo pendingCount() {
        requirePermission("/api/system/comment/page");
        return DataVo.success("操作成功", commentService.countPending());
    }

    /**
     * 单条审核：status 1=通过 2=驳回
     */
    @ResponseBody
    @PostMapping("/system/comment/audit")
    public DataVo audit(@RequestParam(value = "id", defaultValue = "0") Long id,
                        @RequestParam(value = "status", defaultValue = "1") Integer status) {
        requirePermission("/api/system/comment/audit");
        return commentService.auditComment(id, status);
    }

    /**
     * 删除单条（物理删除，计数联动 -1）
     */
    @ResponseBody
    @PostMapping("/system/comment/delete")
    public DataVo delete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/comment/delete");
        return commentService.deleteComment(id);
    }

    /**
     * 批量操作：status 1=通过 2=驳回 3=删除
     * ids 支持 "1,2,3" 或重复 id 参数
     */
    @ResponseBody
    @PostMapping("/system/comment/batch")
    public DataVo batch(@RequestParam(value = "ids", defaultValue = "") String ids,
                        @RequestParam(value = "status", defaultValue = "1") Integer status) {
        requirePermission("/api/system/comment/batch");
        List<Long> idList = parseIds(ids);
        return commentService.batchComment(idList, status);
    }

    /**
     * 解析 "1,2,3" 形式的 id 列表，非法段直接跳过
     */
    private List<Long> parseIds(String ids) {
        List<Long> list = new ArrayList<Long>();
        if (StringUtils.isBlank(ids)) {
            return list;
        }
        for (String part : ids.split("[,;\\s]+")) {
            String v = part.trim();
            if (v.isEmpty()) {
                continue;
            }
            try {
                list.add(Long.parseLong(v));
            } catch (NumberFormatException e) {
                // 忽略非法 id
            }
        }
        return list;
    }
}
