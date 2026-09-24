package com.flycms.module.template.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线标签手册数据源（规划 §8 阶段 D7）
 *
 * <b>为什么要有手册</b>：§6.3 指出"标签方言黑盒、学不会"是三家老 CMS 的通病，
 * 用户只能背文档、抄论坛代码。这里把每个标签的真实参数与最小可用示例做成接口数据，
 * 后台"组件面板/标签手册"页直接渲染——文档与代码同源，改了标签不再出现"文档过时"。
 *
 * <b>参数表来源</b>：全部取自 web/tags/ 各 Tag 类 execute() 中实际读取的参数名，
 * 非手写猜测。新增标签时只需在 MANUAL 里追加一条。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class TagManualService {

    /** 分组顺序：决定后台手册页的分组展示顺序 */
    private static final List<String> GROUPS = Arrays.asList(
            "模型", "栏目", "碎片", "文章", "问答", "分享", "专题", "用户", "通用");

    /**
     * 返回一个 {group: [{name,label,usage,output,params,snippet}...]} 结构。
     * 通用写法统一：标签把结果放进环境变量，标签体内用 <#list>/${} 取值。
     */
    public Map<String, List<Map<String, Object>>> manual() {
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        for (String g : GROUPS) {
            data.put(g, new ArrayList<>());
        }
        add(data, "模型", "fly_page_model", "模型分页列表",
                "输出某模型前台列表，自带帝国式分页条；列表页首选。",
                "dataList（数据行列表）、pageHtml（分页条 HTML）、model_page（分页信息）",
                params(p("model", true, "模型标识 code，如 loupan"),
                        p("category", false, "模型分类 id，不填=全部分类"),
                        p("title", false, "标题模糊匹配"),
                        p("orderby", false, "排序列，默认 id，白名单校验"),
                        p("order", false, "asc/desc，默认 desc"),
                        p("p", false, "页码，默认 1"),
                        p("rows", false, "每页条数，默认 10")),
                "<@fly_page_model model=\"loupan\" rows=\"10\" p=\"${p!1}\">\n"
                        + "  <#list dataList as row>\n"
                        + "    <a href=\"/loupan/${row.shortUrl}.html\">${row.title}</a>\n"
                        + "  </#list>\n"
                        + "  ${pageHtml!''}\n"
                        + "</@fly_page_model>");

        add(data, "模型", "fly_list_model", "模型列表",
                "同分页列表但不输出分页条，适合侧栏/首页区块。",
                "dataList、model_page",
                params(p("model", true, "模型标识 code"),
                        p("category", false, "分类 id"),
                        p("title", false, "标题模糊匹配"),
                        p("orderby", false, "排序列"),
                        p("order", false, "asc/desc"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_list_model model=\"loupan\" rows=\"6\">\n"
                        + "  <#list dataList as row><li>${row.title}</li></#list>\n"
                        + "</@fly_list_model>");

        add(data, "模型", "fly_info_model", "模型内容详情",
                "按 id 或 shortUrl 取单条内容；详情页首选。",
                "info（当前内容，含自定义字段与附件 xxxUrl）",
                params(p("model", true, "模型标识 code"),
                        p("id", false, "内容 id，二选一"),
                        p("shortUrl", false, "短链接，二选一")),
                "<@fly_info_model model=\"loupan\" shortUrl=\"${shortUrl!''}\">\n"
                        + "  <h1>${info.title}</h1>\n"
                        + "  <div>${info.content!''}</div>\n"
                        + "</@fly_info_model>");

        add(data, "模型", "fly_fields_model", "模型字段定义",
                "取某模型的启用字段列表，用于自定义表头或动态表单渲染。",
                "fieldList（字段定义列表）",
                params(p("model", true, "模型标识 code")),
                "<@fly_fields_model model=\"loupan\">\n"
                        + "  <#list fieldList as f>${f.fieldLabel} </#list>\n"
                        + "</@fly_fields_model>");

        add(data, "模型", "fly_hot_model", "热门/推荐内容",
                "按浏览量或推荐权重取热门内容，首页推荐位常用。",
                "dataList",
                params(p("model", true, "模型标识 code"),
                        p("category", false, "分类 id"),
                        p("rows", false, "条数，默认 10")),
                "<@fly_hot_model model=\"loupan\" rows=\"8\">\n"
                        + "  <#list dataList as row><li>${row.title}</li></#list>\n"
                        + "</@fly_hot_model>");

        add(data, "模型", "fly_rel_model", "相关内容",
                "取与当前内容同分类的相邻内容，详情页「相关阅读」场景。",
                "dataList",
                params(p("model", true, "模型标识 code"),
                        p("category", false, "分类 id"),
                        p("notid", false, "排除的当前内容 id"),
                        p("rows", false, "条数")),
                "<@fly_rel_model model=\"loupan\" rows=\"6\">\n"
                        + "  <#list dataList as row><li>${row.title}</li></#list>\n"
                        + "</@fly_rel_model>");

        add(data, "模型", "fly_category_model", "模型分类树",
                "取某模型的分类树，用于导航或筛选条。",
                "categoryList",
                params(p("model", true, "模型标识 code")),
                "<@fly_category_model model=\"loupan\">\n"
                        + "  <#list categoryList as c><a href=\"?c=${c.id}\">${c.name}</a></#list>\n"
                        + "</@fly_category_model>");

        add(data, "栏目", "fly_channel_tree", "栏目树",
                "全站统一栏目树（阶段 C）；隐藏栏目自动过滤。",
                "channelTree（带 children 的树）",
                params(p("fatherId", false, "父栏目 id，0=根，默认 0")),
                "<@fly_channel_tree fatherId=\"0\">\n"
                        + "  <#list channelTree as c>\n"
                        + "    <a href=\"/${c.channelDir}/\">${c.channelName}</a>\n"
                        + "  </#list>\n"
                        + "</@fly_channel_tree>");

        add(data, "栏目", "fly_channel_info", "当前栏目信息",
                "按 dir 或 id 取单个栏目；配合列表页输出栏目级 TDK。",
                "channel",
                params(p("dir", false, "栏目目录名"),
                        p("id", false, "栏目 id")),
                "<@fly_channel_info dir=\"${dir!''}\">\n"
                        + "  <title>${channel.seoTitle!channel.channelName}</title>\n"
                        + "</@fly_channel_info>");

        add(data, "栏目", "fly_list_channel", "聚合栏目内容",
                "跨栏目/跨模型混排（channel_type=3 聚合栏目用），承接原「专题」场景。",
                "dataList、model_page",
                params(p("channelIds", false, "栏目 id 列表，逗号分隔，空=全部子元素"),
                        p("rows", false, "每页条数"),
                        p("p", false, "页码")),
                "<@fly_list_channel rows=\"10\">\n"
                        + "  <#list dataList as row><li>${row.title}</li></#list>\n"
                        + "</@fly_list_channel>");

        add(data, "碎片", "fly_block", "碎片/推荐位/广告位",
                "后台「碎片管理」建的区块，首页/频道页运营位统一步行方式；条目支持定时上下线。",
                "block.content（富文本/模板内容）、block.items（推荐位条目列表）",
                params(p("key", true, "碎片调用键，如 home_focus")),
                "<@fly_block key=\"home_focus\">\n"
                        + "  <#if block?? && block.blockType == 2>\n"
                        + "    <#list block.items as it><a href=\"${it.url}\">${it.title}</a></#list>\n"
                        + "  <#elseif block??>${block.content!''}</#if>\n"
                        + "</@fly_block>");

        add(data, "文章", "fly_article_page", "文章列表",
                "文章模块列表查询。",
                "articleList",
                params(p("title", false, "标题模糊匹配"),
                        p("userId", false, "作者 id"),
                        p("createTime", false, "按发布时间筛选"),
                        p("status", false, "状态，默认 1"),
                        p("orderby", false, "排序列"),
                        p("order", false, "asc/desc"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_article_page rows=\"10\">\n"
                        + "  <#list articleList as a><li>${a.title}</li></#list>\n"
                        + "</@fly_article_page>");

        add(data, "文章", "fly_article_info", "文章详情",
                "按 id 取单篇文章。",
                "article",
                params(p("id", true, "文章 id"),
                        p("status", false, "状态过滤")),
                "<@fly_article_info id=\"${id!0}\">\n"
                        + "  <h1>${article.title}</h1>${article.content!''}\n"
                        + "</@fly_article_info>");

        add(data, "文章", "fly_article_type_list", "文章栏目分类",
                "文章模块的分类树（按 fatherId 递归），用于侧栏文章频道导航。",
                "typeList",
                params(p("fatherId", false, "父分类 id，默认根")),
                "<@fly_article_type_list fatherId=\"0\">\n"
                        + "  <#list typeList as t><li>${t.name}</li></#list>\n"
                        + "</@fly_article_type_list>");

        add(data, "文章", "fly_article_comment_page", "文章评论列表",
                "某文章的评论列表（配合阶段 B 评论审核使用）。",
                "commentList",
                params(p("articleId", true, "文章 id"),
                        p("userId", false, "评论人 id"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_article_comment_page articleId=\"${id!0}\" rows=\"10\">\n"
                        + "  <#list commentList as c><li>${c.content}</li></#list>\n"
                        + "</@fly_article_comment_page>");

        add(data, "问答", "fly_question_page", "问题列表",
                "问答模块问题列表。",
                "questionList",
                params(p("title", false, "标题模糊匹配"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_question_page rows=\"10\">\n"
                        + "  <#list questionList as q><li>${q.title}</li></#list>\n"
                        + "</@fly_question_page>");

        add(data, "问答", "fly_answer_page", "回答列表",
                "某问题的回答列表。",
                "answerList",
                params(p("questionId", true, "问题 id"),
                        p("orderby", false, "排序列"),
                        p("rows", false, "条数")),
                "<@fly_answer_page questionId=\"${id!0}\">\n"
                        + "  <#list answerList as a><li>${a.content}</li></#list>\n"
                        + "</@fly_answer_page>");

        add(data, "分享", "fly_share_page", "分享列表",
                "分享模块列表。",
                "shareList",
                params(p("title", false, "标题模糊匹配"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_share_page rows=\"10\">\n"
                        + "  <#list shareList as s><li>${s.title}</li></#list>\n"
                        + "</@fly_share_page>");

        add(data, "专题", "fly_topic_page", "话题/专题列表",
                "话题列表，支持推荐（isgood）筛选。",
                "topicList",
                params(p("topic", false, "话题名关键字"),
                        p("isgood", false, "是否推荐 1/0"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_topic_page rows=\"10\">\n"
                        + "  <#list topicList as t><li>${t.topicName}</li></#list>\n"
                        + "</@fly_topic_page>");

        add(data, "用户", "fly_userinfo", "用户信息",
                "按用户 id 取用户资料。",
                "user",
                params(p("userId", true, "用户 id"),
                        p("status", false, "状态过滤")),
                "<@fly_userinfo userId=\"${info.userId!0}\">\n"
                        + "  ${user.nickName!''}\n"
                        + "</@fly_userinfo>");

        add(data, "用户", "fly_user_hot_page", "活跃用户榜",
                "按昵称/地区等条件筛选用户，做「达人墙」。",
                "userList",
                params(p("userName", false, "账号关键字"),
                        p("nickName", false, "昵称关键字"),
                        p("province", false, "省份"),
                        p("rows", false, "条数")),
                "<@fly_user_hot_page rows=\"12\">\n"
                        + "  <#list userList as u><li>${u.nickName!''}</li></#list>\n"
                        + "</@fly_user_hot_page>");

        add(data, "通用", "fly_guide_page", "导航列表",
                "后台「导航管理」维护的顶部导航。",
                "guideList",
                params(p("name", false, "导航分组名"),
                        p("orderby", false, "排序列"),
                        p("rows", false, "条数")),
                "<@fly_guide_page rows=\"8\">\n"
                        + "  <#list guideList as g><a href=\"${g.link}\">${g.name}</a></#list>\n"
                        + "</@fly_guide_page>");

        add(data, "通用", "fly_links_page", "友情链接",
                "按类型输出友链（图文/文字）。",
                "linksList",
                params(p("type", false, "类型过滤"),
                        p("show", false, "展示位过滤"),
                        p("rows", false, "条数")),
                "<@fly_links_page rows=\"20\">\n"
                        + "  <#list linksList as l><a href=\"${l.url}\">${l.name}</a></#list>\n"
                        + "</@fly_links_page>");

        add(data, "通用", "fly_string_cut", "字符串截断",
                "超长标题安全截断（超出加省略号）。",
                "s（截断后的字符串）",
                params(p("s", true, "原始字符串"),
                        p("len", false, "保留长度，默认 50")),
                "<@fly_string_cut s=\"${row.title}\" len=\"20\"/>\n${s}");

        add(data, "通用", "fly_date_format", "日期格式化",
                "把时间格式化为指定样式，详情页发布时间常用。",
                "d（格式化结果）",
                params(p("d", true, "时间字符串/日期对象"),
                        p("pattern", false, "格式，默认 yyyy-MM-dd")),
                "<@fly_date_format d=\"${row.createTime}\" pattern=\"yyyy-MM-dd\"/>\n${d}");

        add(data, "通用", "fly_areas_list", "地区列表",
                "级联地区数据（省/市/区）。",
                "areasList",
                params(p("parentId", false, "父地区 id，0=省")),
                "<@fly_areas_list parentId=\"0\">\n"
                        + "  <#list areasList as a><option value=\"${a.id}\">${a.name}</option></#list>\n"
                        + "</@fly_areas_list>");

        add(data, "通用", "fly_announcement_model", "站内公告",
                "后台「公告管理」发布的站内公告列表。",
                "noticeList",
                params(p("rows", false, "条数，默认 5")),
                "<@fly_announcement_model rows=\"5\">\n"
                        + "  <#list noticeList as n><li>${n.title}</li></#list>\n"
                        + "</@fly_announcement_model>");

        return data;
    }

    // /////////////////// 内部构造器 ///////////////////

    private void add(Map<String, List<Map<String, Object>>> data, String group, String name,
                     String label, String usage, String output,
                     List<Map<String, Object>> params, String snippet) {
        Map<String, Object> tag = new LinkedHashMap<>();
        tag.put("name", name);
        tag.put("label", label);
        tag.put("group", group);
        tag.put("usage", usage);
        tag.put("output", output);
        tag.put("params", params);
        tag.put("snippet", snippet);
        data.computeIfAbsent(group, k -> new ArrayList<>()).add(tag);
    }

    private Map<String, Object> p(String name, boolean required, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("required", required);
        m.put("desc", desc);
        return m;
    }

    private List<Map<String, Object>> params(Map<String, Object>... list) {
        return new ArrayList<>(Arrays.asList(list));
    }
}
