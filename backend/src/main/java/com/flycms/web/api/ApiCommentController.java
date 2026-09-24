package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.article.model.ArticleCommentVo;
import com.flycms.module.article.service.ArticleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 评论审核管理 REST（规划阶段 B1）
 *
 * status 语义沿用 fly_article_comment：0未审 1正常 2未通过 3删除
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiCommentController extends ApiBaseController {

    @Autowired
    private ArticleService articleService;

    /**
     * 评论分页列表（join 文章标题）
     *
     * @param status 不传=全部(排除已删除)；0未审 1正常 2未通过 3删除
     */
    @ResponseBody
    @GetMapping("/system/comment/page")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows,
                       @RequestParam(value = "articleId", required = false) Long articleId,
                       @RequestParam(value = "userId", required = false) Long userId,
                       @RequestParam(value = "createTime", required = false) String createTime,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "status", required = false) Integer status) {
        requirePermission("/api/system/comment/page");
        PageVo<ArticleCommentVo> pageVo = articleService.getCommentAuditPage(
                articleId, userId, createTime, keyword, status, pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    /**
     * 单条审核：status 1=通过 2=驳回
     */
    @ResponseBody
    @PostMapping("/system/comment/audit")
    public DataVo audit(@RequestParam(value = "id", defaultValue = "0") Long id,
                        @RequestParam(value = "status", defaultValue = "1") Integer status) {
        requirePermission("/api/system/comment/audit");
        return articleService.auditComment(id, status);
    }

    /**
     * 删除单条（逻辑删除）
     */
    @ResponseBody
    @PostMapping("/system/comment/delete")
    public DataVo delete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/comment/delete");
        return articleService.deleteComment(id);
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
        if (status != null && status == 3) {
            return articleService.batchDeleteComment(idList);
        }
        return articleService.batchAuditComment(idList, status);
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
