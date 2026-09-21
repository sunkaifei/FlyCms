package com.flycms.module.block.dao;

import com.flycms.module.block.model.Block;
import com.flycms.module.block.model.BlockItem;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 碎片位 DAO
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface BlockDao {

    public void insertBlock(Block block);

    public void updateBlock(Block block);

    public void deleteBlockById(@Param("id") Long id);

    public Block findBlockById(@Param("id") Long id);

    public Block findBlockByKey(@Param("key") String key);

    public boolean checkBlockKey(@Param("key") String key);

    public List<Block> getBlockList(@Param("offset") int offset, @Param("rows") int rows);

    public int getBlockCount();

    public void insertItem(BlockItem item);

    public void deleteItemById(@Param("id") Long id);

    public BlockItem findItemById(@Param("id") Long id);

    public List<BlockItem> findItemsByBlockId(@Param("blockId") Long blockId, @Param("visibleOnly") boolean visibleOnly);
}
