package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.comment.model.Comment;
import com.flycms.module.comment.service.CommentService;
import com.flycms.module.user.model.User;
import com.flycms.module.user.service.UserService;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台评论列表标签（E4，U3 替代 {@code fly_articlecommentpage}）。
 *
 * <p>模板用法（模型详情页内）：
 * <pre>
 * &lt;@fly_commentpage targetModel="articles" targetId="${info.id}" p="${p!1}" rows="10"&gt;
 *   &lt;#list comment_page.list as c&gt;
 *     ${c.nickname}：${c.content}
 *   &lt;/#list&gt;
 *   ${commentPageHtml}
 * &lt;/@fly_commentpage&gt;
 * </pre>
 *
 * <p>输出变量：{@code comment_page}（分页对象，list 行含 nickname 头像昵称）、
 * {@code commentPageHtml}（分页条）。仅渲染已通过审核（status=1）的评论。
 */
@Service
public class Commentpage extends AbstractTagPlugin {

    @Autowired
    private CommentService commentService;
    @Autowired
    private UserService userService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, String> p = new LinkedHashMap<>();
        if (params != null) {
            for (Object k : params.keySet()) {
                Object v = params.get(k);
                if (k != null && v != null) {
                    p.put(String.valueOf(k), String.valueOf(v));
                }
            }
        }
        String targetModel = p.get("targetModel");
        long targetId = longVal(p.get("targetId"), 0);
        int page = intVal(p.get("p"), 1);
        int rows = intVal(p.get("rows"), 10);

        Map<String, Object> vars = new LinkedHashMap<>();
        PageVo<Map<String, Object>> pageVo = new PageVo<>(page);
        pageVo.setRows(rows);
        pageVo.setList(new ArrayList<>());
        vars.put("comment_page", pageVo);
        vars.put("commentPageHtml", "");
        if (StringUtils.isBlank(targetModel) || targetId <= 0 || body == null) {
            render(env, body, vars);
            return;
        }
        try {
            List<Comment> list = commentService.listByTarget(targetModel, targetId, page, rows);
            int count = commentService.countByTarget(targetModel, targetId);
            pageVo.setCount(count);
            List<Map<String, Object>> rowsList = new ArrayList<>();
            java.text.SimpleDateFormat iso =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            // 批量取评论人：此前在循环内逐条 findUserById → 一页 N 条评论即 N 次查询
            List<Long> userIds = new ArrayList<>();
            for (Comment c : list) {
                if (c.getUserId() != null) {
                    userIds.add(c.getUserId());
                }
            }
            Map<Long, User> userMap = new java.util.HashMap<>();
            for (User u : userService.getUsersByIds(userIds)) {
                if (u != null && u.getUserId() != null) {
                    userMap.put(u.getUserId(), u);
                }
            }
            for (Comment c : list) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", String.valueOf(c.getId()));
                row.put("parentId", String.valueOf(c.getParentId()));
                row.put("userId", String.valueOf(c.getUserId()));
                row.put("content", c.getContent());
                // 模型行惯例：DATETIME 列以 ISO 字符串输出（common/macros.html 的 dt16 只认字符串）
                row.put("createTime", c.getCreateTime() == null ? "" : iso.format(c.getCreateTime()));
                // findUsersByIds 一次取回，状态口径由 DAO 决定（不再逐条查）
                User user = userMap.get(c.getUserId());
                String nick = user == null ? null : user.getNickName();
                if (nick == null || nick.isBlank()) {
                    nick = user == null || user.getUserName() == null ? "游客" : user.getUserName();
                }
                // 昵称是纯文本用户字段，此处做 HTML 转义：模板 ${c.nickname} 不自动转义，
                // 用户把昵称改成 <script> 即构成存储型 XSS（内容侧已在写入时过 jsoup 白名单）
                row.put("nickname", org.springframework.web.util.HtmlUtils.htmlEscape(nick));
                row.put("avatar", user == null || user.getAvatar() == null ? "" : user.getAvatar());
                rowsList.add(row);
            }
            pageVo.setList(rowsList);
            vars.put("comment_page", pageVo);
            vars.put("commentPageHtml", buildCommentPageBar(page, rows, count));
        } catch (Exception e) {
            logTagFailure("fly_commentpage", e);
        }
        render(env, body, vars);
    }

    private void render(Environment env, TemplateDirectiveBody body, Map<String, Object> vars)
            throws IOException, TemplateException, TemplateModelException {
        DefaultObjectWrapperBuilder builder =
                new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            env.setVariable(e.getKey(), builder.build().wrap(e.getValue()));
        }
        // 自闭合调用（<@fly_commentpage .../>）body 为 null：仅设置变量不渲染
        if (body != null) {
            body.render(env.getOut());
        }
    }

    private String buildCommentPageBar(int page, int rows, int count) {
        if (count <= 0 || rows <= 0) {
            return "";
        }
        int totalPage = (int) Math.ceil((double) count / rows);
        if (totalPage <= 1) {
            return "";
        }
        if (page < 1) {
            page = 1;
        }
        if (page > totalPage) {
            page = totalPage;
        }
        StringBuilder sb = new StringBuilder("<div class=\"fly-page\">");
        sb.append("<span>共").append(count).append("条/").append(totalPage).append("页</span>");
        sb.append(page > 1 ? "<a href='?p=" + (page - 1) + "'>上一页</a>" : "<span>上一页</span>");
        int begin = Math.max(1, page - 5);
        int end = Math.min(totalPage, page + 5);
        for (int i = begin; i <= end; i++) {
            if (i == page) {
                sb.append("<span class='current'>").append(i).append("</span>");
            } else {
                sb.append("<a href='?p=").append(i).append("'>").append(i).append("</a>");
            }
        }
        sb.append(page < totalPage ? "<a href='?p=" + (page + 1) + "'>下一页</a>" : "<span>下一页</span>");
        sb.append("</div>");
        return sb.toString();
    }

    private int intVal(String v, int def) {
        try {
            return (v == null || v.isEmpty()) ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private long longVal(String v, long def) {
        try {
            return (v == null || v.isEmpty()) ? def : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
