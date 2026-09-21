package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.block.model.Block;
import com.flycms.module.block.model.BlockItem;
import com.flycms.module.block.service.BlockService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.Map;

/**
 * 碎片/推荐位管理 REST（规划阶段 E）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiBlockController extends ApiBaseController {

    @Autowired
    private BlockService blockService;

    @ResponseBody
    @GetMapping("/system/block/list")
    public DataVo list(@RequestParam(value = "p", defaultValue = "1") int pageNum) {
        requirePermission("/api/system/block/list");
        PageVo<Block> pageVo = blockService.getBlockPage(pageNum, 20);
        return DataVo.success("操作成功", pageVo);
    }

    @ResponseBody
    @PostMapping("/system/block/save")
    public DataVo save(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/block/save");
        String id = params.get("id");
        Block block = new Block();
        block.setBlockKey(params.get("blockKey"));
        block.setBlockName(params.get("blockName"));
        block.setBlockType(parseInt(params.get("blockType"), 0));
        block.setContent(params.get("content"));
        block.setItemCount(parseInt(params.get("itemCount"), 10));
        block.setCacheSeconds(parseInt(params.get("cacheSeconds"), 0));
        block.setSort(parseInt(params.get("sort"), 0));
        block.setStatus(parseInt(params.get("status"), 1));
        if (StringUtils.isBlank(id)) {
            return blockService.addBlock(block);
        }
        block.setId(parseLong(id));
        return blockService.updateBlock(block);
    }

    @ResponseBody
    @PostMapping("/system/block/delete")
    public DataVo delete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/block/delete");
        return blockService.deleteBlock(id);
    }

    @ResponseBody
    @GetMapping("/system/blockItem/list")
    public DataVo itemList(@RequestParam(value = "blockId", defaultValue = "0") Long blockId) {
        requirePermission("/api/system/block/list");
        return DataVo.success("操作成功", blockService.findItems(blockId, false));
    }

    @ResponseBody
    @PostMapping("/system/blockItem/save")
    public DataVo itemSave(@RequestParam Map<String, String> params) throws ParseException {
        requirePermission("/api/system/blockItem/save");
        BlockItem item = new BlockItem();
        item.setBlockId(parseLong(params.get("blockId")));
        item.setTitle(params.get("title"));
        item.setImage(params.get("image"));
        item.setUrl(params.get("url"));
        item.setSummary(params.get("summary"));
        item.setStartTime(parseDate(params.get("startTime")));
        item.setEndTime(parseDate(params.get("endTime")));
        item.setSort(parseInt(params.get("sort"), 0));
        String id = params.get("id");
        if (StringUtils.isNotBlank(id)) {
            item.setId(parseLong(id));
            // 简化：删除重插（保留创建时间语义弱化）
            blockService.deleteItem(item.getId());
        }
        return blockService.addItem(item);
    }

    @ResponseBody
    @PostMapping("/system/blockItem/delete")
    public DataVo itemDelete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/blockItem/delete");
        return blockService.deleteItem(id);
    }

    private Long parseLong(String v) {
        try {
            return v == null ? null : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseInt(String v, int def) {
        try {
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private java.util.Date parseDate(String v) throws ParseException {
        if (StringUtils.isBlank(v)) {
            return null;
        }
        v = v.trim().replace('T', ' ');
        return v.length() > 10
                ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(v)
                : new java.text.SimpleDateFormat("yyyy-MM-dd").parse(v);
    }
}
