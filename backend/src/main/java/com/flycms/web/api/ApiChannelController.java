package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.module.channel.model.Channel;
import com.flycms.module.channel.service.ChannelService;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 栏目管理 REST（规划 §8 阶段 C）
 *
 * 接口路径与既有后台保持一致风格（/api/system/channel/...）。
 * 后台页面由此驱动栏目树、模型下拉（含"不绑定模型"选项）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiChannelController extends ApiBaseController {

    @Autowired
    private ChannelService channelService;
    @Autowired
    private ModelService modelService;

    /** 栏目树（后台用，含隐藏） */
    @ResponseBody
    @GetMapping("/system/channel/tree")
    public DataVo tree() {
        requirePermission("/api/system/channel/tree");
        return DataVo.success("操作成功", channelService.tree());
    }

    /** 栏目详情 */
    @ResponseBody
    @GetMapping("/system/channel/get")
    public DataVo get(@RequestParam(value = "id", required = false) Long id) {
        requirePermission("/api/system/channel/tree");
        if (id == null) {
            return DataVo.failure("请选择栏目");
        }
        Channel channel = channelService.get(id);
        if (channel == null) {
            return DataVo.failure("栏目不存在");
        }
        return DataVo.success("操作成功", channel);
    }

    /**
     * 可选模型列表：显式加入 id=0 的"不绑定模型"，
     * 保证单页/外链/聚合栏目能被正常创建（§6.5 的解绑诉求）。
     */
    @ResponseBody
    @GetMapping("/system/channel/models")
    public DataVo models() {
        requirePermission("/api/system/channel/tree");
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> none = new LinkedHashMap<>();
        none.put("id", 0);
        none.put("modelName", "（不绑定模型，单页/外链/聚合栏目用）");
        none.put("code", "");
        data.add(none);
        List<Model> models = modelService.getEnabledModels();
        if (models != null) {
            for (Model m : models) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", m.getId());
                row.put("modelName", m.getName());
                row.put("code", m.getCode());
                data.add(row);
            }
        }
        return DataVo.success("操作成功", data);
    }

    /** 保存（新增/修改） */
    @ResponseBody
    @PostMapping("/system/channel/save")
    public DataVo save(Channel channel) {
        requirePermission("/api/system/channel/save");
        return channelService.save(channel, null);
    }

    /** 删除：childrenMode=promote(上提子栏目)/hide(隐藏子栏目) */
    @ResponseBody
    @PostMapping("/system/channel/delete")
    public DataVo delete(@RequestParam("id") Long id,
                         @RequestParam(value = "childrenMode", required = false) String childrenMode) {
        requirePermission("/api/system/channel/delete");
        return channelService.delete(id, StringUtils.defaultIfBlank(childrenMode, "promote"));
    }

    /** 显示/隐藏 */
    @ResponseBody
    @PostMapping("/system/channel/status")
    public DataVo status(@RequestParam("id") Long id, @RequestParam("status") int status) {
        requirePermission("/api/system/channel/save");
        return channelService.status(id, status);
    }

    /** 上移/下移 */
    @ResponseBody
    @PostMapping("/system/channel/move")
    public DataVo move(@RequestParam("id") Long id, @RequestParam("up") int up) {
        requirePermission("/api/system/channel/save");
        return channelService.move(id, up == 1);
    }

    /** 拖拽移动到某父栏目下 */
    @ResponseBody
    @PostMapping("/system/channel/moveTo")
    public DataVo moveTo(@RequestParam("id") Long id,
                         @RequestParam(value = "fatherId", required = false) Long fatherId) {
        requirePermission("/api/system/channel/save");
        return channelService.moveTo(id, fatherId);
    }

    /** 目录名冲突预检（新增/改名时即时反馈，不必等保存报错） */
    @ResponseBody
    @GetMapping("/system/channel/checkdir")
    public DataVo checkDir(@RequestParam("dir") String dir,
                           @RequestParam(value = "id", required = false) Long id) {
        requirePermission("/api/system/channel/tree");
        return channelService.checkDir(StringUtils.trimToEmpty(dir).toLowerCase(), id);
    }
}
