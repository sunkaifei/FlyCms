package com.flycms.module.channel.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.channel.dao.ChannelDao;
import com.flycms.module.channel.model.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 统一栏目服务（规划 §8 阶段 C）
 *
 * <b>为什么必须做统一栏目</b>：
 * §6.5 指出"栏目与模型绑死"是三家的死结——改栏目就得改模型、改模板，
 * 于是"专题""推荐""城市分站"这些跨模型场景只能靠硬编码凑。
 * 这里栏目是<b>树 + URL 归属层</b>，模型只是它可选的默认数据源。
 *
 * <b>URL 冲突是硬红线</b>：dir 既要避开已有模型 code（/loupan/ 已被模型列表占用），
 * 也要避开前台固定前缀（/search、/a、/ucenter…），否则栏目会把老 URL 抢走。
 * 保存时一次性校验，不留"能建打不开"的坑。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ChannelService {

    /** 栏目目录名：小写开头，允许小写字母/数字/下划线/中划线 */
    private static final Pattern DIR = Pattern.compile("^[a-z][a-z0-9_\\-]{0,49}$");

    /**
     * 前台已占用的固定路径前缀（web/front 各 Controller 的既有路由）。
     * 与之重名会导致栏目 URL 把老页面抢走，属于必拦项。
     */
    private static final List<String> RESERVED_DIRS = Arrays.asList(
            "a", "search", "topics", "topic", "question", "share", "people", "space",
            "ucenter", "user", "admin", "api", "system", "static", "assets", "images",
            "js", "css", "uploadfiles", "upload", "help_list", "help", "index",
            "login", "logout", "reg", "register", "forget_password", "checkuser",
            "checkAdmin", "findByName", "sitemap.xml", "roboot.txt", "robots.txt",
            "404", "403", "500", "error", "common", "ueditor", "swagger", "druid");

    @Autowired
    private ChannelDao channelDao;

    // /////////////////// 查询 ///////////////////

    /** 全量栏目树（后台/标签用，含隐藏栏目） */
    public List<Channel> tree() {
        return buildTree(channelDao.findAll(), false);
    }

    /** 仅显示中的栏目树（前台标签用） */
    public List<Channel> treeVisible() {
        return buildTree(channelDao.findAll(), true);
    }

    public Channel get(Long id) {
        return id == null ? null : channelDao.findById(id);
    }

    public Channel findByDir(String dir) {
        return StringUtils.isBlank(dir) ? null : channelDao.findByDir(dir);
    }

    /** 取某栏目的全部后代 id（含自身），聚合栏目取数时用 */
    public List<Channel> offspring(Long fatherId) {
        List<Channel> result = new ArrayList<>();
        collect(fatherId, result, true);
        return result;
    }

    // /////////////////// 写 ///////////////////

    /**
     * 保存（新增/修改）。目录名参与 URL 冲突校验；修改不改变父级（移动请用拖拽接口）。
     */
    public DataVo save(Channel form, Long adminId) {
        if (form == null || StringUtils.isBlank(form.getChannelName())) {
            return DataVo.failure("请填写栏目名称");
        }
        if (form.getChannelName().length() > 50) {
            return DataVo.failure("栏目名称不得超过 50 字");
        }
        String dir = form.getChannelDir() == null ? "" : form.getChannelDir().trim().toLowerCase(Locale.ROOT);
        if (!DIR.matcher(dir).matches()) {
            return DataVo.failure("目录名只能由小写字母开头，含小写字母/数字/下划线/中划线，最长 50 位");
        }
        DataVo conflict = checkDir(dir, form.getId());
        if (conflict.getCode() != DataVo.CODE_SUCCESS) {
            return conflict;
        }
        if (form.getChannelType() < 0 || form.getChannelType() > 3) {
            return DataVo.failure("栏目类型非法");
        }
        if (form.getChannelType() == 2 && StringUtils.isBlank(form.getOutUrl())) {
            return DataVo.failure("外链栏目必须填写外链地址");
        }
        if (form.getChannelType() == 1 && StringUtils.isBlank(form.getPageContent())) {
            form.setPageContent("");
        }
        if (form.getPageSize() <= 0) {
            form.setPageSize(20);
        }
        if (form.getPageSize() > 100) {
            form.setPageSize(100);
        }
        if (form.getFatherId() == null) {
            form.setFatherId(0L);
        }
        form.setChannelDir(dir);
        if (form.getId() == null) {
            if (form.getFatherId() > 0 && channelDao.findById(form.getFatherId()) == null) {
                return DataVo.failure("父栏目不存在");
            }
            form.setId(SnowFlake.getInstance().nextId());
            form.setStatus(1);
            form.setSort(0);
            channelDao.insertChannel(form);
            return DataVo.success("栏目已创建");
        }
        Channel old = channelDao.findById(form.getId());
        if (old == null) {
            return DataVo.failure("栏目不存在");
        }
        // 修改时不允许把父栏目改成自己或自己的后代，否则树成环
        if (form.getFatherId() > 0 && (form.getFatherId().equals(form.getId()) || isDescendant(form.getFatherId(), form.getId()))) {
            return DataVo.failure("不能把栏目移动到它自己的子栏目下");
        }
        form.setFatherId(old.getFatherId());
        channelDao.updateChannel(form);
        return DataVo.success("栏目已保存");
    }

    /**
     * 删除栏目：<b>只删栏目行，绝不删除内容数据</b>（§6.5 红线）。
     *
     * @param childrenMode promote=子栏目上提到父级；hide=子栏目转为隐藏
     */
    public DataVo delete(Long id, String childrenMode) {
        Channel channel = channelDao.findById(id);
        if (channel == null) {
            return DataVo.failure("栏目不存在");
        }
        int children = channelDao.countChildren(id);
        if (children > 0) {
            if ("hide".equals(childrenMode)) {
                for (Channel child : channelDao.findByFatherId(id)) {
                    channelDao.updateStatus(child.getId(), 0);
                }
            } else {
                for (Channel child : channelDao.findByFatherId(id)) {
                    channelDao.updateFatherId(child.getId(), channel.getFatherId());
                }
            }
        }
        channelDao.deleteChannelById(id);
        return DataVo.success("栏目已删除，内容数据未受影响"
                + (children > 0 ? "（子栏目已" + ("hide".equals(childrenMode) ? "隐藏" : "上提") + "）" : ""));
    }

    public DataVo status(Long id, int status) {
        if (channelDao.findById(id) == null) {
            return DataVo.failure("栏目不存在");
        }
        channelDao.updateStatus(id, status == 1 ? 1 : 0);
        return DataVo.success(status == 1 ? "栏目已显示" : "栏目已隐藏");
    }

    /** 上移/下移：与同级相邻栏目交换 sort */
    public DataVo move(Long id, boolean up) {
        Channel cur = channelDao.findById(id);
        if (cur == null) {
            return DataVo.failure("栏目不存在");
        }
        List<Channel> siblings = channelDao.findByFatherId(cur.getFatherId());
        int index = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(id)) {
                index = i;
                break;
            }
        }
        int target = up ? index - 1 : index + 1;
        if (index < 0 || target < 0 || target >= siblings.size()) {
            return DataVo.failure(up ? "已经是第一个" : "已经是最后一个");
        }
        Channel other = siblings.get(target);
        channelDao.updateSort(id, other.getSort());
        channelDao.updateSort(other.getId(), cur.getSort());
        return DataVo.success(up ? "已上移" : "已下移");
    }

    /** 移动到某个父栏目下 */
    public DataVo moveTo(Long id, Long fatherId) {
        Channel cur = channelDao.findById(id);
        if (cur == null) {
            return DataVo.failure("栏目不存在");
        }
        if (fatherId != null && fatherId > 0) {
            Channel father = channelDao.findById(fatherId);
            if (father == null) {
                return DataVo.failure("父栏目不存在");
            }
            if (fatherId.equals(id) || isDescendant(fatherId, id)) {
                return DataVo.failure("不能移动到它自己的子栏目下");
            }
        } else {
            fatherId = 0L;
        }
        channelDao.updateFatherId(id, fatherId);
        return DataVo.success("栏目已移动");
    }

    // /////////////////// URL 冲突校验 ///////////////////

    /**
     * 目录唯一性校验：保留前缀 → 已有模型 code → 已有栏目 dir，三级全拦。
     */
    public DataVo checkDir(String dir, Long excludeId) {
        if (StringUtils.isBlank(dir)) {
            return DataVo.failure("请填写目录名");
        }
        if (RESERVED_DIRS.contains(dir)) {
            return DataVo.failure("目录名与系统内置路由冲突：" + dir);
        }
        List<String> codes = channelDao.findAllModelCodes();
        if (codes != null && codes.contains(dir)) {
            return DataVo.failure("目录名与已有模型列表路径冲突（/" + dir + "/ 已被模型占用）");
        }
        if (channelDao.existsDir(dir, excludeId)) {
            return DataVo.failure("目录名已被占用：" + dir);
        }
        return DataVo.success("ok");
    }

    // /////////////////// 内部 ///////////////////

    private boolean isDescendant(Long maybeChild, Long ancestorId) {
        Long cur = maybeChild;
        int guard = 0;
        while (cur != null && cur > 0 && guard++ < 100) {
            if (cur.equals(ancestorId)) {
                return true;
            }
            Channel c = channelDao.findById(cur);
            if (c == null) {
                return false;
            }
            cur = c.getFatherId();
        }
        return false;
    }

    /** 一维列表 → 树。children 为 [] 而不是 null，前端表格树不需要判空。 */
    private List<Channel> buildTree(List<Channel> all, boolean visibleOnly) {
        Map<Long, Channel> index = new LinkedHashMap<>();
        Map<Long, List<Channel>> childrenMap = new HashMap<>();
        for (Channel c : all) {
            if (visibleOnly && c.getStatus() != 1) {
                continue;
            }
            c.setChildren(new ArrayList<>());
            index.put(c.getId(), c);
            childrenMap.computeIfAbsent(c.getFatherId(), k -> new ArrayList<>()).add(c);
        }
        List<Channel> roots = new ArrayList<>();
        index.values().forEach(c -> {
            List<Channel> kids = childrenMap.get(c.getId());
            if (kids != null) {
                c.setChildren(kids);
            }
        });
        for (Channel c : index.values()) {
            if (c.getFatherId() == null || c.getFatherId() == 0 || !index.containsKey(c.getFatherId())) {
                roots.add(c);
            }
        }
        return roots;
    }

    /** 递归收集后代（含自身）；隐藏栏目在计算时不计入 */
    private void collect(Long fatherId, List<Channel> out, boolean includeSelf) {
        if (fatherId == null || fatherId == 0) {
            return;
        }
        Channel self = channelDao.findById(fatherId);
        if (self == null) {
            return;
        }
        if (includeSelf && self.getStatus() == 1) {
            out.add(self);
        }
        for (Channel c : channelDao.findByFatherId(fatherId)) {
            collect(c.getId(), out, true);
        }
    }
}
