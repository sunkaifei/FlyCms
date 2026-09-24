package com.flycms.module.block.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.block.dao.BlockDao;
import com.flycms.module.block.model.Block;
import com.flycms.module.block.model.BlockItem;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 碎片/推荐位服务（规划阶段 E）
 *
 * 渲染缓存（阶段 E 收尾，落实 §6.4"精准失效"）：
 * 碎片是首页/频道页最高频读取的对象，而渲染结果取决于碎片本体 + 条目集合。
 * 这里用 Caffeine 按 block_key 缓存已装配好的 Block（含 items），
 * TTL 直接取 fly_block.cache_seconds（0=不缓存），
 * 碎片/条目任何一次写操作立即 invalidate —— 运营改完当场生效，而不是等 TTL 自然过期。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class BlockService {

    @Autowired
    private BlockDao blockDao;

    /** 已装配渲染用缓存：key=blockKey，value=CachedBlock（带自己的 TTL） */
    private final com.github.benmanes.caffeine.cache.Cache<String, CachedBlock> renderCache =
            Caffeine.newBuilder()
                    .maximumSize(2000)
                    .expireAfter(new Expiry<String, CachedBlock>() {
                        @Override
                        public long expireAfterCreate(String key, CachedBlock value, long currentTime) {
                            return value.ttlNanos();
                        }

                        @Override
                        public long expireAfterUpdate(String key, CachedBlock value,
                                                      long currentTime, long currentDuration) {
                            return value.ttlNanos();
                        }

                        @Override
                        public long expireAfterRead(String key, CachedBlock value,
                                                    long currentTime, long currentDuration) {
                            return currentDuration;
                        }
                    })
                    .build();

    /**
     * 前台渲染取碎片（含时间窗内条目）：带 cache_seconds 粒度的渲染缓存。
     * 标签 <@fly_block key="x"/> 走这里；后台管理与 GET 单次查询不走缓存。
     *
     * @return 碎片不存在/已隐藏返回 null
     */
    public Block findRenderBlock(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        CachedBlock hit = renderCache.getIfPresent(key);
        if (hit != null) {
            return hit.block;
        }
        Block block = blockDao.findBlockByKey(key);
        if (block == null || block.getStatus() != 1) {
            return null;
        }
        List<BlockItem> items = blockDao.findItemsByBlockId(block.getId(), true);
        if (items != null && items.size() > block.getItemCount()) {
            items = items.subList(0, block.getItemCount());
        }
        block.setItems(items);
        if (block.getCacheSeconds() > 0) {
            renderCache.put(key, new CachedBlock(block, block.getCacheSeconds()));
        }
        return block;
    }

    /** 写操作统一失效：碎片本体变更 */
    private void evictRenderCache(Long blockId) {
        Block block = blockDao.findBlockById(blockId);
        if (block != null && StringUtils.isNotBlank(block.getBlockKey())) {
            renderCache.invalidate(block.getBlockKey());
        }
    }

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
        block.setId(SnowFlake.getInstance().nextId());
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
        evictRenderCache(form.getId());
        return DataVo.success("碎片位已更新，前台 10 秒内生效");
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
        evictRenderCache(id);
        blockDao.deleteBlockById(id);
        return DataVo.success("碎片位已删除");
    }

    public DataVo addItem(BlockItem item) {
        if (item.getBlockId() == null || blockDao.findBlockById(item.getBlockId()) == null) {
            return DataVo.failure("碎片位不存在");
        }
        item.setId(SnowFlake.getInstance().nextId());
        item.setStatus(1);
        item.setCreateTime(new java.util.Date());
        blockDao.insertItem(item);
        evictRenderCache(item.getBlockId());
        return DataVo.success("条目已添加");
    }

    public DataVo deleteItem(Long id) {
        BlockItem item = blockDao.findItemById(id);
        blockDao.deleteItemById(id);
        if (item != null) {
            evictRenderCache(item.getBlockId());
        }
        return DataVo.success("条目已删除");
    }

    /** 渲染缓存条目：block 快照 + 自己的 TTL（秒） */
    private static final class CachedBlock {
        private final Block block;
        private final int seconds;

        CachedBlock(Block block, int seconds) {
            this.block = block;
            this.seconds = seconds;
        }

        long ttlNanos() {
            return TimeUnit.SECONDS.toNanos(seconds);
        }
    }
}
