package com.flycms.module.guide.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.channel.dao.ChannelDao;import com.flycms.module.channel.model.Channel;
import com.flycms.module.guide.dao.GuideDao;
import com.flycms.module.guide.model.Guide;
import com.flycms.module.model.dao.ModelCategoryDao;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelCategory;
import com.flycms.module.model.service.ModelService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 站点导航服务（fly_guide，树形 + 三种来源绑定）。
 *
 * <p>导航项链接解析：自定义链接直存直出；栏目按 /{channelDir}/ 拼接；
 * 模型分类按 /{modelCode}/c{分类id} 拼接。绑定对象被删后 url 回退 "#"，
 * 由管理员在导航管理里改绑（栏目解绑模型不影响导航，导航绑的是栏目本身）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class GuideService {

    @Autowired
    private GuideDao guideDao;
    @Autowired
    private ChannelDao channelDao;
    @Autowired
    private ModelCategoryDao modelCategoryDao;
    @Autowired
    private ModelService modelService;

    // /////////////////// 查询 ///////////////////

    public Guide findById(Long id) {
        return guideDao.findById(id);
    }

    /** 全部导航项（含隐藏，后台编辑用） */
    public List<Guide> findAll() {
        return guideDao.findAll();
    }

    /** 导航树：visibleOnly=true 仅显示中节点（前台标签用），每个节点带计算后的 url */
    public List<Guide> tree(boolean visibleOnly) {
        List<Guide> roots = buildTree(guideDao.findAll(), visibleOnly);
        fillUrl(roots);
        return roots;
    }

    public PageVo<Guide> findPage(int pageNum, int rows) {
        PageVo<Guide> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(guideDao.findPage(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(guideDao.countAll());
        return pageVo;
    }

    /** 单条导航项的最终链接（详情接口用；绑定对象缺失回退 "#"） */
    public String resolveUrl(Guide g) {
        return doResolveUrl(g);
    }

    // /////////////////// 写 ///////////////////

    public DataVo save(Guide form) {
        if (StringUtils.isBlank(form.getName())) {
            return DataVo.failure("导航名称不能为空");
        }
        Long fatherId = form.getFatherId() == null ? 0L : form.getFatherId();
        if (fatherId != 0) {
            Guide father = guideDao.findById(fatherId);
            if (father == null) {
                return DataVo.failure("上级导航项不存在");
            }
        }
        if (form.getType() != Guide.TYPE_LINK && form.getType() != Guide.TYPE_CHANNEL
                && form.getType() != Guide.TYPE_CATEGORY) {
            return DataVo.failure("导航类型不合法");
        }
        if (form.getType() == Guide.TYPE_LINK) {
            if (StringUtils.isBlank(form.getLink())) {
                return DataVo.failure("自定义链接不能为空");
            }
        } else {
            if (form.getRefId() == null || form.getRefId() <= 0) {
                return DataVo.failure(form.getType() == Guide.TYPE_CHANNEL
                        ? "请选择绑定的栏目" : "请选择绑定的分类");
            }
            if (form.getType() == Guide.TYPE_CHANNEL && channelDao.findById(form.getRefId()) == null) {
                return DataVo.failure("绑定的栏目不存在");
            }
            if (form.getType() == Guide.TYPE_CATEGORY) {
                ModelCategory category = modelCategoryDao.findCategoryById(form.getRefId());
                if (category == null) {
                    return DataVo.failure("绑定的分类不存在");
                }
            }
        }
        String target = StringUtils.trimToEmpty(form.getTarget());
        if (!target.isEmpty() && !"_blank".equals(target)) {
            return DataVo.failure("打开方式仅支持留空或 _blank");
        }
        form.setTarget(target);
        form.setFatherId(fatherId);
        form.setLink(StringUtils.trimToEmpty(form.getLink()));
        // ref_id 列 NOT NULL：自定义链接类型无绑定对象，显式归 0（插 NULL 在严格模式直接报错）
        form.setRefId(form.getRefId() == null || form.getRefId() <= 0 ? 0L : form.getRefId());

        if (form.getId() == null || form.getId() <= 0) {
            form.setId(SnowFlake.getInstance().nextId());
            form.setStatus(form.getStatus() == 0 ? 0 : 1);
            guideDao.insert(form);
            return DataVo.success("导航项已添加");
        }
        Guide old = guideDao.findById(form.getId());
        if (old == null) {
            return DataVo.failure("导航项不存在");
        }
        // 防环：不能把自己挂到自己（或后代）名下
        if (fatherId != 0 && (fatherId.equals(form.getId()) || isDescendant(fatherId, form.getId()))) {
            return DataVo.failure("上级导航项不能是自身或自己的下级");
        }
        guideDao.update(form);
        return DataVo.success("导航项已更新");
    }

    public DataVo delete(Long id) {
        Guide guide = guideDao.findById(id);
        if (guide == null) {
            return DataVo.failure("导航项不存在");
        }
        if (guideDao.countChildren(id) > 0) {
            return DataVo.failure("请先删除下级导航项");
        }
        guideDao.deleteById(id);
        return DataVo.success("导航项已删除");
    }

    public DataVo updateStatus(Long id, int status) {
        if (guideDao.findById(id) == null) {
            return DataVo.failure("导航项不存在");
        }
        guideDao.updateStatus(id, status == 1 ? 1 : 0);
        return DataVo.success("状态已更新");
    }

    // /////////////////// 内部 ///////////////////

    /** target 是否为 ancestorId 的后代（祖先链上出现 ancestorId 即是） */
    private boolean isDescendant(Long target, Long ancestorId) {
        Long cur = target;
        int guard = 0;
        while (cur != null && cur != 0 && guard++ < 100) {
            Guide node = guideDao.findById(cur);
            if (node == null) {
                return false;
            }
            if (ancestorId.equals(node.getFatherId())) {
                return true;
            }
            cur = node.getFatherId();
        }
        return false;
    }

    /** 一维列表 → 树（同 ChannelService.buildTree 口径：children 恒为 []） */
    private List<Guide> buildTree(List<Guide> all, boolean visibleOnly) {
        Map<Long, Guide> index = new LinkedHashMap<>();
        Map<Long, List<Guide>> childrenMap = new HashMap<>();
        for (Guide g : all) {
            if (visibleOnly && g.getStatus() != 1) {
                continue;
            }
            g.setChildren(new ArrayList<>());
            index.put(g.getId(), g);
            childrenMap.computeIfAbsent(g.getFatherId(), k -> new ArrayList<>()).add(g);
        }
        index.values().forEach(g -> {
            List<Guide> kids = childrenMap.get(g.getId());
            if (kids != null) {
                g.setChildren(kids);
            }
        });
        List<Guide> roots = new ArrayList<>();
        for (Guide g : index.values()) {
            if (g.getFatherId() == null || g.getFatherId() == 0 || !index.containsKey(g.getFatherId())) {
                roots.add(g);
            }
        }
        return roots;
    }

    /** 递归填充最终链接：绑定对象缺失时回退 "#"，页面不炸、破链可见 */
    private void fillUrl(List<Guide> nodes) {
        if (nodes == null) {
            return;
        }
        for (Guide g : nodes) {
            g.setUrl(doResolveUrl(g));
            fillUrl(g.getChildren());
        }
    }

    private String doResolveUrl(Guide g) {
        if (g.getType() == Guide.TYPE_CHANNEL) {
            Channel channel = g.getRefId() == null ? null : channelDao.findById(g.getRefId());
            return channel == null ? "#" : "/" + channel.getChannelDir() + "/";
        }
        if (g.getType() == Guide.TYPE_CATEGORY) {
            ModelCategory category = g.getRefId() == null ? null : modelCategoryDao.findCategoryById(g.getRefId());
            if (category == null) {
                return "#";
            }
            Model model = modelService.findModelById(category.getModelId());
            if (model == null) {
                return "#";
            }
            g.setRefModel(model.getCode());
            return "/" + model.getCode() + "/c" + category.getId();
        }
        return StringUtils.defaultIfBlank(g.getLink(), "#");
    }
}
