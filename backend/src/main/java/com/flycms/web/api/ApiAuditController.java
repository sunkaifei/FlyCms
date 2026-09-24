package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.article.model.Article;
import com.flycms.module.article.service.ArticleService;
import com.flycms.module.config.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 投稿审核 REST（规划阶段 H）
 *
 * <p>审核开关：config 键 {@code fly_article_audit}（0=直接发布，1=先审后发）。
 * 文章真实状态：0未审核 1正常 2审核未通过 3删除。</p>
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiAuditController extends ApiBaseController {

    @Autowired
    private ArticleService articleService;
    @Autowired
    private ConfigService configService;

    /**
     * 待审文章分页列表
     *
     * @param status 不传默认 0（未审核）；0未审 1正常 2未通过 3删除，传 -1 查全部
     */
    @ResponseBody
    @GetMapping("/system/audit/page")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows,
                       @RequestParam(value = "title", required = false) String title,
                       @RequestParam(value = "userId", required = false) Long userId,
                       @RequestParam(value = "createTime", required = false) String createTime,
                       @RequestParam(value = "status", defaultValue = "0") Integer status) {
        requirePermission("/api/system/audit/page");
        Integer real = (status != null && status < 0) ? null : status;
        PageVo<Article> pageVo = articleService.getArticleAuditPage(
                title, userId, createTime, real, null, null, pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    /** 待审数量（角标） */
    @ResponseBody
    @GetMapping("/system/audit/pendingCount")
    public DataVo pendingCount() {
        requirePermission("/api/system/audit/page");
        return DataVo.success("操作成功", articleService.countPendingArticle());
    }

    /**
     * 单条审核：status 1=通过 2=驳回（驳回 reason 必填）
     */
    @ResponseBody
    @PostMapping("/system/audit/audit")
    public DataVo audit(@RequestParam(value = "id", defaultValue = "0") Long id,
                        @RequestParam(value = "status", defaultValue = "1") Integer status,
                        @RequestParam(value = "reason", required = false) String reason) {
        requirePermission("/api/system/audit/audit");
        return articleService.auditArticle(id, status, reason, getLoginUserId());
    }

    /**
     * 批量审核：status 1=通过 2=驳回（驳回 reason 必填）
     * ids 支持 "1,2,3"
     */
    @ResponseBody
    @PostMapping("/system/audit/batch")
    public DataVo batch(@RequestParam(value = "ids", defaultValue = "") String ids,
                        @RequestParam(value = "status", defaultValue = "1") Integer status,
                        @RequestParam(value = "reason", required = false) String reason) {
        requirePermission("/api/system/audit/batch");
        List<Long> idList = parseIds(ids);
        return articleService.batchAuditArticle(idList, status, reason, getLoginUserId());
    }

    /** 读取投稿审核开关 */
    @ResponseBody
    @GetMapping("/system/audit/switch")
    public DataVo auditSwitch() {
        requirePermission("/api/system/audit/switch");
        return DataVo.success("操作成功", articleService.getArticleAuditSwitch());
    }

    /** 设置投稿审核开关：0=直接发布 1=先审后发 */
    @ResponseBody
    @PostMapping("/system/audit/switch")
    public DataVo setSwitch(@RequestParam(value = "value", defaultValue = "0") Integer value) {
        requirePermission("/api/system/audit/switch");
        int v = (value != null && value == 1) ? 1 : 0;
        configService.updagteConfigByKey("fly_article_audit", String.valueOf(v));
        return DataVo.success("已保存");
    }

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
