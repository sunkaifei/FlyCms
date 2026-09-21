package com.flycms.module.block.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.block.dao.BlockDao;
import com.flycms.module.block.model.Block;
import com.flycms.module.block.model.BlockItem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 碎片/推荐位服务（规划阶段 E）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class BlockService {

    @Autowired
    private BlockDao blockDao;

    public PageVo<Block> getBlockPage(int pageNum, int rows) {
        PageVo<Block> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(blockDao.getBlockList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(blockDao.getBlockCount());
        return pageVo;
    }

    public Block findBlockById(Long id) {
        return blockDao.findBlockById(id);
    }

    public Block findBlockByKey(String key) {
        return blockDao.findBlockByKey(key);
    }

    public List<BlockItem> findItems(Long blockId, boolean visibleOnly) {
        return blockDao.findItemsByBlockId(blockId, visibleOnly);
    }

    public DataVo addBlock(Block block) {
        if (StringUtils.isBlank(block.getBlockKey()) || StringUtils.isBlank(block.getBlockName())) {
            return DataVo.failure("调用键与名称不能为空");
        }
        if (!block.getBlockKey().matches("^[a-z][a-z0-9_]{1,49}$")) {
            return DataVo.failure("调用键须为小写字母开头的字母/数字/下划线");
        }
        if (blockDao.checkBlockKey(block.getBlockKey())) {
            return DataVo.failure("调用键已存在");
        }
        block.setId(new SnowFlake(2, 3).nextId());
        block.setStatus(1);
        block.setCreateTime(new java.util.Date());
        blockDao.insertBlock(block);
        return DataVo.success("碎片位已创建");
    }

    public DataVo updateBlock(Block form) {
        Block old = blockDao.findBlockById(form.getId());
        if (old == null) {
            return DataVo.failure("碎片位不存在");
        }
        form.setBlockKey(old.getBlockKey());
        blockDao.updateBlock(form);
        return DataVo.success("碎片位已更新");
    }

    /** 删除碎片位级联删除条目 */
    public DataVo deleteBlock(Long id) {
        Block block = blockDao.findBlockById(id);
        if (block == null) {
            return DataVo.failure("碎片位不存在");
        }
        for (BlockItem item : blockDao.findItemsByBlockId(id, false)) {
            blockDao.deleteItemById(item.getId());
        }
        blockDao.deleteBlockById(id);
        return DataVo.success("碎片位已删除");
    }

    public DataVo addItem(BlockItem item) {
        if (item.getBlockId() == null || blockDao.findBlockById(item.getBlockId()) == null) {
            return DataVo.failure("碎片位不存在");
        }
        item.setId(new SnowFlake(2, 3).nextId());
        item.setStatus(1);
        item.setCreateTime(new java.util.Date());
        blockDao.insertItem(item);
        return DataVo.success("条目已添加");
    }

    public DataVo deleteItem(Long id) {
        blockDao.deleteItemById(id);
        return DataVo.success("条目已删除");
    }
}
