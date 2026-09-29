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
 * <b>参数表来源</b>：全部取自 {@code web/tags/} 各 Tag 类 {@code execute()} 中实际读取的
 * 参数名与 {@code env.setVariable(...)} / {@code vars.put(...)} 写入的输出变量名，
 * 非手写猜测。<b>登记名规则</b>：{@code fly_} + 类名首字母小写后「仅在大写字母前插下划线」
 * （见 {@code StringHelperUtils.toUnderline}）——注意它<b>不拆驼峰单词</b>，
 * 所以 {@code PageModel} → {@code fly_page_model}（不是 {@code fly_pagemodel}）、
 * {@code Userinfo} → {@code fly_userinfo}、而 {@code ListModel} → {@code fly_list_model}。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class TagManualService {

    /** 分组顺序：决定后台手册页的分组展示顺序 */
    private static final List<String> GROUPS = Arrays.asList(
            "模型", "栏目", "碎片", "部件", "文章", "问答", "分享", "专题", "用户", "检索", "通用");

    // /////////////////// 作用域（§9.2 借鉴织梦的"标签作用域"分类） ///////////////////

    /** 作用域代码 → 中文名。key 与 {@link #SCOPES} 的取值一致 */
    private static final Map<String, String> SCOPE_LABELS = new LinkedHashMap<>();

    static {
        SCOPE_LABELS.put("global", "全局");
        SCOPE_LABELS.put("list", "列表");
        SCOPE_LABELS.put("detail", "内容");
        SCOPE_LABELS.put("module", "模块");
    }

    /**
     * 每个标签的<b>作用域声明</b>：明确"这个标签在哪类模板里能用"，
     * 避免新手在列表页用了内容页标签（§9.2 要解决的正是这个）。
     *
     * <p>取值：{@code global} 任意模板 / {@code list} 列表页（{@code list-*}）/
     * {@code detail} 内容页（{@code detail-*}）/ {@code module} 特定模块页。
     *
     * <p><b>为什么用一张表而不是给每个 {@link #add} 调用加参数</b>：
     * 声明集中一处便于一眼看全"哪些标签属于哪个作用域"，也便于新增标签时只补一行；
     * 未登记的标签默认 {@code global}（不误伤）。
     */
    private static final Map<String, String> SCOPES = new LinkedHashMap<>();

    static {
        // 模型
        SCOPES.put("fly_page_model", "list");
        SCOPES.put("fly_list_model", "list");
        SCOPES.put("fly_info_model", "detail");
        SCOPES.put("fly_fields_model", "detail");
        SCOPES.put("fly_category_model", "list");
        SCOPES.put("fly_rel_model", "detail");
        SCOPES.put("fly_hot_model", "global");
        // 栏目
        SCOPES.put("fly_channel_tree", "global");
        SCOPES.put("fly_channel_info", "global");
        SCOPES.put("fly_list_channel", "list");
        // 碎片 / 部件 / 区域
        SCOPES.put("fly_block", "global");
        // G19 设计令牌（theme.json settings → CSS 变量）
        SCOPES.put("fly_theme_vars", "global");
        // V4 聚合统计（按分类/作者分组计数）
        SCOPES.put("fly_stats_model", "list");
        // E4 平台评论（U3 替代 fly_articlecommentpage）
        SCOPES.put("fly_commentpage", "detail");
        SCOPES.put("fly_part", "global");
        SCOPES.put("fly_area", "global");
        // 文章

        // 问答
        // 分享
        // 专题

        // 用户
        SCOPES.put("fly_userinfo", "global");
        SCOPES.put("fly_usercount", "global");
        SCOPES.put("fly_avatar", "global");
        SCOPES.put("fly_userhotpage", "list");
        SCOPES.put("fly_fanspage", "list");
        SCOPES.put("fly_userfanspage", "list");
        SCOPES.put("fly_favoritepage", "list");
        SCOPES.put("fly_feedpage", "list");
        SCOPES.put("fly_invitepage", "list");
        SCOPES.put("fly_userpower", "global");
        SCOPES.put("fly_login", "global");
        SCOPES.put("fly_useractivation", "global");
        SCOPES.put("fly_checkfollow", "global");
        // 检索
        SCOPES.put("fly_tag_list", "list");
        SCOPES.put("fly_search_page", "list");
        // 通用
        SCOPES.put("fly_form", "global");
        SCOPES.put("fly_scoredetailpage", "list");
        SCOPES.put("fly_scorerulepage", "list");
        SCOPES.put("fly_stringcut", "global");
        SCOPES.put("fly_dateformat", "global");
        SCOPES.put("fly_areaslist", "global");
    }

    /** 作用域清单（代码→中文名），供后台下拉渲染 */
    public Map<String, String> scopeOptions() {
        return new LinkedHashMap<>(SCOPE_LABELS);
    }

    /**
     * 返回一个 {group: [{name,label,group,scope,scopeLabel,usage,output,params,snippet}...]} 结构。
     * 通用写法统一：标签把结果放进环境变量，标签体内用 <#list>/${} 取值。
     */
    public Map<String, List<Map<String, Object>>> manual() {
        return manual(null);
    }

    /**
     * 按作用域过滤的手册数据（§9.2）：编辑器里可只显示"当前模板类型可用的标签"。
     *
     * @param scope {@code global}/{@code list}/{@code detail}/{@code module}；
     *              空或 {@code all} 表示不过滤。过滤时 {@code global} 标签<b>始终保留</b>
     *              ——"全局"的字面含义就是任意模板都能用。
     */
    public Map<String, List<Map<String, Object>>> manual(String scope) {
        Map<String, List<Map<String, Object>>> all = buildAll();
        if (scope == null || scope.isEmpty() || "all".equalsIgnoreCase(scope)
                || !SCOPE_LABELS.containsKey(scope)) {
            return all;
        }
        Map<String, List<Map<String, Object>>> filtered = new LinkedHashMap<>();
        for (String g : GROUPS) {
            List<Map<String, Object>> kept = new ArrayList<>();
            for (Map<String, Object> tag : all.getOrDefault(g, new ArrayList<>())) {
                String s = String.valueOf(tag.get("scope"));
                if ("global".equals(s) || scope.equals(s)) {
                    kept.add(tag);
                }
            }
            if (!kept.isEmpty()) {
                filtered.put(g, kept);
            }
        }
        return filtered;
    }

    private Map<String, List<Map<String, Object>>> buildAll() {
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        for (String g : GROUPS) {
            data.put(g, new ArrayList<>());
        }

        // /////////////////// 模型（fly_page_model / fly_list_model / …） ///////////////////
        // 说明：模型类标签共享同一套参数（AbstractModelTag.PARAM_WHITELIST），
        //       model 必填，其余按需。

        add(data, "模型", "fly_page_model", "模型分页列表",
                "输出某模型前台列表，自带帝国式分页条；列表页首选。",
                "dataList（数据行列表）、model_page（分页信息）、pageHtml（分页条 HTML）",
                params(p("model", true, "模型标识 code，如 loupan"),
                        p("category", false, "模型分类 id，不填=全部分类"),
                        p("title", false, "标题模糊匹配"),
                        p("notid", false, "排除的内容 id"),
                        p("orderby", false, "排序列，默认 id，白名单校验"),
                        p("order", false, "asc/desc，默认 desc"),
                        p("p", false, "页码，默认 1"),
                        p("rows", false, "每页条数，默认 10")),
                "<@fly_page_model model=\"loupan\" rows=\"10\" p=\"${(p)!1}\">\n"
                        + "  <#list dataList as row>\n"
                        + "    <a href=\"/loupan/${row.shortUrl}.html\">${row.title}</a>\n"
                        + "  </#list>\n"
                        + "  ${pageHtml!''}\n"
                        + "</@fly_page_model>");

        add(data, "模型", "fly_list_model", "模型列表（无分页条）",
                "同分页列表但不输出分页条，适合侧栏/首页区块。",
                "dataList",
                params(p("model", true, "模型标识 code"),
                        p("category", false, "分类 id"),
                        p("title", false, "标题模糊匹配"),
                        p("notid", false, "排除的内容 id"),
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
                        p("id", false, "内容 id，与 shortUrl 二选一"),
                        p("shortUrl", false, "短链接，与 id 二选一")),
                "<@fly_info_model model=\"loupan\" shortUrl=\"${shortUrl!''}\">\n"
                        + "  <h1>${info.title}</h1>\n"
                        + "  <div>${info.content!''}</div>\n"
                        + "</@fly_info_model>");

        add(data, "模型", "fly_fields_model", "模型字段定义",
                "取某模型的启用字段列表，用于自定义表头或动态表单渲染。",
                "fieldsList（字段定义列表）",
                params(p("model", true, "模型标识 code")),
                "<@fly_fields_model model=\"loupan\">\n"
                        + "  <#list fieldsList as f>${f.fieldLabel} </#list>\n"
                        + "</@fly_fields_model>");

        add(data, "模型", "fly_category_model", "模型分类树",
                "取某模型的分类树，用于导航或筛选条。",
                "categoryList",
                params(p("model", true, "模型标识 code")),
                "<@fly_category_model model=\"loupan\">\n"
                        + "  <#list categoryList as c><a href=\"?c=${c.id}\">${c.name}</a></#list>\n"
                        + "</@fly_category_model>");

        add(data, "模型", "fly_hot_model", "热门/推荐内容",
                "按浏览量取热门内容，首页推荐位常用。",
                "dataList",
                params(p("model", true, "模型标识 code"),
                        p("category", false, "分类 id"),
                        p("rows", false, "条数，默认 10")),
                "<@fly_hot_model model=\"loupan\" rows=\"8\">\n"
                        + "  <#list dataList as row><li>${row.title}</li></#list>\n"
                        + "</@fly_hot_model>");

        add(data, "模型", "fly_rel_model", "相关内容",
                "取同分类的相邻内容，详情页「相关阅读」场景。",
                "dataList",
                params(p("model", true, "模型标识 code"),
                        p("category", false, "分类 id"),
                        p("notid", false, "排除的当前内容 id"),
                        p("orderby", false, "排序列"),
                        p("order", false, "asc/desc"),
                        p("rows", false, "条数")),
                "<@fly_rel_model model=\"loupan\" notid=\"${info.id!0}\" rows=\"6\">\n"
                        + "  <#list dataList as row><li>${row.title}</li></#list>\n"
                        + "</@fly_rel_model>");

        // /////////////////// 栏目 ///////////////////

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
                "<@fly_channel_info dir=\"${(dir)!''}\">\n"
                        + "  <title>${channel.seoTitle!channel.channelName}</title>\n"
                        + "</@fly_channel_info>");

        add(data, "栏目", "fly_list_channel", "聚合栏目内容",
                "跨栏目/跨模型混排（channel_type=3 聚合栏目用），承接原「专题」场景。",
                "dataList、model_page",
                params(p("channelIds", false, "栏目 id 列表，逗号分隔，空=全部列表栏目"),
                        p("rows", false, "每页条数，默认 10"),
                        p("p", false, "页码，默认 1")),
                "<@fly_list_channel channelIds=\"1,2\" rows=\"10\">\n"
                        + "  <#list dataList as row><li>${row.title}</li></#list>\n"
                        + "</@fly_list_channel>");

        // /////////////////// 碎片 / 部件 / 区域 ///////////////////

        add(data, "碎片", "fly_block", "碎片/推荐位/广告位",
                "后台「碎片管理」建的区块，首页/频道页运营位统一写法；条目支持定时上下线与限量。",
                "block（含 content 富文本、items 推荐位条目列表）",
                params(p("key", true, "碎片调用键，如 home_focus")),
                "<@fly_block key=\"home_focus\">\n"
                        + "  <#if block?? && block.blockType == 2>\n"
                        + "    <#list block.items as it><a href=\"${it.url}\">${it.title}</a></#list>\n"
                        + "  <#elseif block??>${block.content!''}</#if>\n"
                        + "</@fly_block>");

        add(data, "部件", "fly_part", "模板部件引入",
                "引入部件（页头/页脚/侧栏）。路径不写死，按「子主题 → 父主题」自动查找 "
                        + "parts/{name}.html，换父主题或子主题覆盖后自动生效。",
                "（无输出变量；部件内容就地渲染，部件内可继续用其它标签）",
                params(p("name", true, "部件名，对应 parts/{name}.html")),
                "<@fly_part name=\"header\"/>\n"
                        + "<#-- 部件缺失时渲染标签体作兜底，不会整页 500 -->\n"
                        + "<@fly_part name=\"sidebar\">默认侧栏内容</@fly_part>");

        add(data, "部件", "fly_area", "区域区块编排",
                "区域占位：渲染后台「布局管理」为该区域编排的区块（碎片/内容列表/自定义 HTML）。"
                        + "布局从代码变成数据——模板只说「这里可以放东西」，放什么由后台决定。",
                "area（区域对象：name/blocks）、area_html（拼好的 HTML，可直接输出）",
                params(p("name", true, "区域名，需在该主题 theme.json 的 supports.regions 中声明")),
                "<@fly_area name=\"content_top\"/>\n"
                        + "<#-- 或自渲染：<#if (area.blocks)?? && area.blocks?size gt 0>…</#if> -->");

        // /////////////////// 文章 ///////////////////






        // /////////////////// 聚合统计（V4） ///////////////////

        add(data, "模型", "fly_stats_model", "聚合统计",
                "按固有维度（分类/作者）对已发布内容分组计数，适合侧栏「栏目文章数」「活跃作者」等。",
                "statsList（行含 key/name/count，按 count 降序）",
                params(p("model", true, "模型标识 code"),
                        p("by", false, "分组维度：category（默认，分类树节点）/ author（发布者）"),
                        p("status", false, "状态，默认 1（已发布）"),
                        p("rows", false, "条数，默认 20")),
                "<@fly_stats_model model=\"articles\" by=\"category\" rows=\"10\">\n"
                        + "  <#if (statsList)?? && statsList?size gt 0>\n"
                        + "    <#list statsList as s><a href=\"/articles/c${s.key}.html\">${s.name}（${s.count}）</a></#list>\n"
                        + "  </#if>\n"
                        + "</@fly_stats_model>");

        // /////////////////// 设计令牌（G19） ///////////////////

        add(data, "通用", "fly_theme_vars", "设计令牌",
                "输出当前主题 theme.json settings 的 CSS 变量（:root{--fly-color-*, --fly-font-*, --fly-layout-*}）。"
                        + "已由 common/header.html 全局引入；模板/自定义 CSS 一律消费 var(--fly-color-*) 等令牌，改色只改 theme.json。",
                "直接输出 <style> 块，无输出变量",
                params(),
                "<@fly_theme_vars/>");

        // /////////////////// 评论（E4 平台评论） ///////////////////

        add(data, "评论", "fly_commentpage", "评论列表",
                "目标内容的已通过评论分页（U3 替代 fly_articlecommentpage，适用于任意模型详情页）。",
                "comment_page（分页对象，数据行取 comment_page.list，行含 nickname/avatar/content/createTime）",
                params(p("targetModel", true, "目标模型 code"),
                        p("targetId", true, "目标内容 id"),
                        p("status", false, "状态，默认已通过"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_commentpage targetModel=\"articles\" targetId=\"${(id)!0}\" rows=\"10\">\n"
                        + "  <#if (comment_page.list)??>\n"
                        + "    <#list comment_page.list as c><li>${c.nickname}：${c.content}</li></#list>\n"
                        + "  </#if>\n"
                        + "</@fly_commentpage>");

        // /////////////////// 问答 ///////////////////






        // /////////////////// 分享 ///////////////////



        // /////////////////// 专题 / 话题 ///////////////////




        // /////////////////// 用户 ///////////////////

        add(data, "用户", "fly_userinfo", "用户信息",
                "按用户 id 取用户资料。",
                "userinfo",
                params(p("userId", true, "用户 id"),
                        p("status", false, "状态过滤")),
                "<@fly_userinfo userId=\"${(userId)!0}\">\n"
                        + "  ${userinfo.nickName!''}\n"
                        + "</@fly_userinfo>");

        add(data, "用户", "fly_usercount", "用户计数",
                "取用户维度统计数（内容/粉丝等）。",
                "count",
                params(p("userId", true, "用户 id")),
                "<@fly_usercount userId=\"${(userId)!0}\">${count!0}</@fly_usercount>");

        add(data, "用户", "fly_avatar", "头像地址",
                "把用户头像字段转成可访问地址。",
                "avatar",
                params(p("avatar", false, "头像字段值"),
                        p("avatarurl", false, "已有完整地址")),
                "<@fly_avatar avatarurl=\"${(userinfo.avatarurl)!''}\" avatar=\"${(userinfo.avatar)!''}\"><img src=\"${avatar!''}\"/></@fly_avatar>");

        add(data, "用户", "fly_userhotpage", "活跃用户榜",
                "按昵称/地区等条件筛选用户，做「达人墙」。",
                "hot_page（分页对象，数据行取 hot_page.list）",
                params(p("userName", false, "账号关键字"),
                        p("nickName", false, "昵称关键字"),
                        p("mobile", false, "手机号"),
                        p("email", false, "邮箱"),
                        p("province", false, "省份"),
                        p("city", false, "城市"),
                        p("area", false, "区县"),
                        p("status", false, "状态过滤"),
                        p("orderby", false, "排序列"),
                        p("order", false, "asc/desc"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_userhotpage rows=\"12\">\n"
                        + "  <#if (hot_page.list)??>\n"
                        + "    <#list hot_page.list as u><li>${u.nickName!''}</li></#list>\n"
                        + "  </#if>\n"
                        + "</@fly_userhotpage>");

        add(data, "用户", "fly_fanspage", "关注/粉丝列表",
                "按 (userFollow, userFans) 取关注关系分页列表。",
                "fans_page（分页对象，数据行取 fans_page.list）",
                params(p("userFollow", false, "关注发起人 id"),
                        p("userFans", false, "被关注人 id"),
                        p("createTime", false, "按关注时间筛选"),
                        p("orderby", false, "排序列"),
                        p("order", false, "asc/desc"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_fanspage userFollow=\"${(userId)!0}\" rows=\"20\">\n"
                        + "  <#if (fans_page.list)??>\n"
                        + "    <#list fans_page.list as f><li>${f.userFans}</li></#list>\n"
                        + "  </#if>\n"
                        + "</@fly_fanspage>");

        add(data, "用户", "fly_userfanspage", "我的粉丝分页",
                "前台「我的粉丝」页专用分页列表（带 time 时间筛选）。",
                "fans_page（分页对象，数据行取 fans_page.list）",
                params(p("userFollow", false, "关注发起人 id"),
                        p("userFans", false, "被关注人 id"),
                        p("time", false, "时间区间筛选"),
                        p("orderby", false, "排序列"),
                        p("order", false, "asc/desc"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_userfanspage userFans=\"${(userId)!0}\" rows=\"20\">\n"
                        + "  <#if (fans_page.list)??>\n"
                        + "    <#list fans_page.list as f><li>${f.userFollow}</li></#list>\n"
                        + "  </#if>\n"
                        + "</@fly_userfanspage>");

        add(data, "用户", "fly_favoritepage", "我的收藏分页",
                "前台「我的收藏」页专用分页列表。",
                "favorite_page（分页对象，数据行取 favorite_page.list）",
                params(p("userId", true, "用户 id"),
                        p("infoType", false, "收藏内容类型"),
                        p("createTime", false, "按收藏时间筛选"),
                        p("orderby", false, "排序列"),
                        p("order", false, "asc/desc"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_favoritepage userId=\"${(userId)!0}\" rows=\"20\">\n"
                        + "  <#if (favorite_page.list)??>\n"
                        + "    <#list favorite_page.list as r><li>${r.title!''}</li></#list>\n"
                        + "  </#if>\n"
                        + "</@fly_favoritepage>");

        add(data, "用户", "fly_feedpage", "用户动态分页",
                "前台「用户动态」页专用分页列表。",
                "feed_page（分页对象，数据行取 feed_page.list）",
                params(p("userId", true, "用户 id"),
                        p("status", false, "状态过滤"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_feedpage userId=\"${(userId)!0}\" rows=\"20\">\n"
                        + "  <#if (feed_page.list)??>\n"
                        + "    <#list feed_page.list as f><li>${f.content!''}</li></#list>\n"
                        + "  </#if>\n"
                        + "</@fly_feedpage>");

        add(data, "用户", "fly_invitepage", "用户邀请分页",
                "前台「我的邀请」页专用分页列表。",
                "invite_page（分页对象，数据行取 invite_page.list）",
                params(p("userId", true, "用户 id"),
                        p("status", false, "状态过滤"),
                        p("orderby", false, "排序列"),
                        p("order", false, "asc/desc"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_invitepage userId=\"${(userId)!0}\" rows=\"20\">\n"
                        + "  <#if (invite_page.list)??>\n"
                        + "    <#list invite_page.list as i><li>${i.userName!''}</li></#list>\n"
                        + "  </#if>\n"
                        + "</@fly_invitepage>");

        add(data, "用户", "fly_userpower", "用户名下权限",
                "按用户组名读取权限位（模块开关判断）。",
                "（无输出变量；权限判断结果由模板按需读取）",
                params(p("groupName", true, "用户组名")),
                "<@fly_userpower groupName=\"vip\">VIP 可见内容</@fly_userpower>");

        add(data, "用户", "fly_login", "登录状态",
                "输出当前请求的登录状态（前台模板做菜单/按钮显隐）。",
                "status（0=未登录，1=已登录）",
                params(),
                "<@fly_login><#if (status!0) == 1>已登录<#else>请登录</#if></@fly_login>");

        add(data, "用户", "fly_useractivation", "账号激活状态",
                "判断某账号是否已激活。",
                "status",
                params(p("userId", true, "用户 id")),
                "<@fly_useractivation userId=\"${(userId)!0}\">\n"
                        // status 是 boolean（userService.checkUserByActivation 返回值），直接判真假
                        + "  <#if status!false>已激活</#if>\n"
                        + "</@fly_useractivation>");

        add(data, "用户", "fly_checkfollow", "是否关注",
                "判断 (关注人, 被关注人) 的关注关系。",
                "result（1=已关注）",
                params(p("userFollow", true, "关注发起人 id"),
                        p("userFans", true, "被关注人 id")),
                "<@fly_checkfollow userFollow=\"${(userId)!0}\" userFans=\"${(targetId)!0}\">\n"
                        + "  <#if (result!0) == 1>已关注<#else>关注</#if>\n"
                        + "</@fly_checkfollow>");


        // /////////////////// 检索（标签页 / 搜索页，§5.1 / §9.4） ///////////////////

        add(data, "检索", "fly_tag_list", "标签聚合",
                "某个标签（关键词）下的跨模型内容聚合。与前台 /tag/{tag}/ 页面同源，"
                        + "可在任意模板（侧栏、单页、首页区块）就地嵌入。",
                "dataList（每行含 __modelCode/__modelName）、tag_total、tag_page、relatedTags",
                params(p("tag", true, "标签名（也可用 keyword）"),
                        p("rows", false, "条数，默认 8，上限 100"),
                        p("p", false, "页码，默认 1")),
                "<@fly_tag_list tag=\"Spring\" rows=\"8\">\n"
                        + "  <#if dataList?? && dataList?size gt 0>\n"
                        + "    <#list dataList as row>\n"
                        + "      <a href=\"/${row['__modelCode']}/${row.shortUrl}.html\">${row.title}</a>\n"
                        + "    </#list>\n"
                        + "  </#if>\n"
                        + "</@fly_tag_list>");

        add(data, "检索", "fly_search_page", "搜索结果",
                "站内搜索结果（跨模型关键词聚合，与标签页同源）。配合 search.html 使用；"
                        + "分页请自行拼 /search?q=..&p=N（必须保留关键词）。",
                "dataList（每行含 __modelCode/__modelName）、search_total、search_page",
                params(p("q", true, "搜索词（兼容 title/keyword 旧写法）"),
                        p("rows", false, "条数，默认 10"),
                        p("p", false, "页码，默认 1")),
                "<@fly_search_page q=\"${q!}\" p=\"${(p)!1}\" rows=\"10\">\n"
                        + "  <#list dataList as row>\n"
                        + "    <a href=\"/${row['__modelCode']}/${row.shortUrl}.html\">${row.title}</a>\n"
                        + "  </#list>\n"
                        + "</@fly_search_page>");

        // /////////////////// 通用 ///////////////////





        add(data, "通用", "fly_form", "自定义表单",
                "渲染后台「表单生成器」建的表单（含 fields 字段列表）。",
                "form（含 fields 字段定义）",
                params(p("code", true, "表单编码")),
                "<@fly_form code=\"contact\">\n"
                        + "  <form method=\"post\" action=\"/form/submit\">\n"
                        + "    <#list form.fields as f>\n"
                        + "      <label>${f.fieldLabel}</label><input name=\"${f.fieldName}\"/>\n"
                        + "    </#list>\n"
                        + "  </form>\n"
                        + "</@fly_form>");


        add(data, "通用", "fly_scoredetailpage", "积分明细分页",
                "积分流水分页列表。",
                "detail_page（分页对象，数据行取 detail_page.list）",
                params(p("userId", false, "用户 id"),
                        p("status", false, "状态过滤"),
                        p("orderby", false, "排序列"),
                        p("order", false, "asc/desc"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_scoredetailpage userId=\"${(userId)!0}\" rows=\"20\">\n"
                        + "  <#if (detail_page.list)??>\n"
                        + "    <#list detail_page.list as d><li>${d.score!0}</li></#list>\n"
                        + "  </#if>\n"
                        + "</@fly_scoredetailpage>");

        add(data, "通用", "fly_scorerulepage", "积分规则分页",
                "积分规则分页列表。",
                "rule_page（分页对象，数据行取 rule_page.list）",
                params(p("name", false, "规则名关键字"),
                        p("status", false, "状态过滤"),
                        p("orderby", false, "排序列"),
                        p("order", false, "asc/desc"),
                        p("p", false, "页码"),
                        p("rows", false, "每页条数")),
                "<@fly_scorerulepage rows=\"20\">\n"
                        + "  <#if (rule_page.list)??>\n"
                        + "    <#list rule_page.list as r><li>${r.name!''}</li></#list>\n"
                        + "  </#if>\n"
                        + "</@fly_scorerulepage>");

        add(data, "通用", "fly_stringcut", "富文本摘要截断",
                "去标签 + 按字数截断（超出加省略号）。<b>必须带标签体</b>（内部会 render 标签体），"
                        + "结果写入 info_content 而不是直接输出。",
                "info_content（截断后的纯文本）",
                params(p("content", true, "原始富文本/HTML"),
                        p("num", true, "保留字数")),
                "<@fly_stringcut content=\"${(row.content)!''}\" num=\"60\">\n"
                        + "  <p>${info_content}</p>\n"
                        + "</@fly_stringcut>");

        add(data, "通用", "fly_dateformat", "日期格式化",
                "把时间字符串格式化为 yyyy-MM-dd HH:mm:ss，<b>直接写到页面输出</b>（无输出变量），"
                        + "因此标签体可省。",
                "（无输出变量；结果直接输出到当前位置）",
                params(p("time", true, "时间字符串")),
                "发布于 <@fly_dateformat time=\"${(row.createTime)!''}\"/>");

        add(data, "通用", "fly_areaslist", "地区列表",
                "级联地区数据（省/市/区）。",
                "areaslist",
                params(p("parentId", false, "父地区 id，0=省，默认 0")),
                "<@fly_areaslist parentId=\"0\">\n"
                        + "  <#list areaslist as a><option value=\"${a.areaId!''}\">${a.areaName!''}</option></#list>\n"
                        + "</@fly_areaslist>");

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
        String scope = SCOPES.getOrDefault(name, "global");
        tag.put("scope", scope);
        tag.put("scopeLabel", SCOPE_LABELS.getOrDefault(scope, "全局"));
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

    @SafeVarargs
    private final List<Map<String, Object>> params(Map<String, Object>... list) {
        return new ArrayList<>(Arrays.asList(list));
    }
}
