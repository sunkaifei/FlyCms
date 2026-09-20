package com.flycms.module.admin.dao;

import com.flycms.module.admin.model.Group;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 2018/7/9
 */
@Repository
public interface GroupDao {

    // ///////////////////////////////
    // /////      增加        ////////
    // ///////////////////////////////
    //添加权限小组名
    public int addGroup(Group group);

    // ///////////////////////////////
    // /////       修改       ////////
    // ///////////////////////////////
    //按id修改组名
    public int updateGroup(@Param("name") String name, @Param("id") Long id);

    //权限组id和权限id关联
    public int addGroupPermission(@Param("groupId") Long groupId, @Param("permissionId") Long permissionId);
    // ///////////////////////////////
    // /////        删除      ////////
    // ///////////////////////////////
    //按id删除角色信息
    public int deleteGroup(@Param("id") Long id);

    //按id删除角色和权限关联信息
    public int deleteGroupPermission(@Param("groupId") Long groupId, @Param("permissionId") Long permissionId);

    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    //按ID查询权限组信息
    public Group findGroupById(@Param("id") Long id);

    //查询用户组名是否存在
    public int checkGroup(@Param("name") String name);

    //查询所有权限组
    public int getGroupCount();

    //权限组列表
    public List<Group> getGroupList(@Param("offset") int offset, @Param("rows") int rows);

    //所有权限小组列表
    public List<Group> getAllGroupList();

    //按管理员id查询所在会员组id
    public Integer findUserAndGroupById(@Param("adminId") Long adminId);

    //按管理员id查询所在会员组信息
    public Group findUserByGroup(@Param("adminId") Long adminId);
}
