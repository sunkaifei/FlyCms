package com.flycms.module.channel.dao;

import com.flycms.module.channel.model.Channel;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 统一栏目 DAO（规划阶段 C）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface ChannelDao {

    void insertChannel(Channel channel);

    int updateChannel(Channel channel);

    void deleteChannelById(@Param("id") Long id);

    Channel findById(@Param("id") Long id);

    Channel findByDir(@Param("dir") String dir);

    /** 目录名是否被占用（不含自身） */
    boolean existsDir(@Param("dir") String dir, @Param("excludeId") Long excludeId);

    /** 全部栏目（father_id asc, sort asc, id asc），用于构建栏目树 */
    List<Channel> findAll();

    List<Channel> findByFatherId(@Param("fatherId") Long fatherId);

    int countChildren(@Param("fatherId") Long fatherId);

    /** 全部模型 code，用于保存时做 URL 冲突校验 */
    List<String> findAllModelCodes();

    void updateStatus(@Param("id") Long id, @Param("status") int status);

    void updateSort(@Param("id") Long id, @Param("sort") int sort);

    void updateFatherId(@Param("id") Long id, @Param("fatherId") Long fatherId);
}
