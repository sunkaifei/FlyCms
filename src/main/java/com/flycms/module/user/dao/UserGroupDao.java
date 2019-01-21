package com.flycms.module.user.dao;

import com.flycms.module.user.model.UserGroup;
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
 * @Date: 10:07 2018/7/9
 */
@Repository
public interface UserGroupDao {

    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加用户组
    public int addUserGroup(UserGroup group);

    //权限组id和权限id关联
    public int addUserGroupPermission(@Param("groupId") Long groupId, @Param("permissionId") Long permissionId);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除角色信息
    public int deleteUserGroup(@Param("id") Long id);

    //按id删除角色和权限关联信息
    public int deleteUserGroupPermission(@Param("groupId") Long groupId, @Param("permissionId") Long permissionId);

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //添加用户组
    public int updateUserGroup(UserGroup group);

    //更新用户组排序
    public int updateGroupSort(@Param("id") Long id,@Param("sort") Integer sort);


    // ///////////////////////////////
    // ///// 查詢 ////////
    // ///////////////////////////////
    //按id查询用户组信息
    public UserGroup findUserGroupByid(@Param("id") Long id);

    //按用户userId查询所在的用户组信息
    public UserGroup findUuserGroupByUserId(@Param("userId") Long userId);

    //查询用户组名和用户session里的用户id查询用户当前权限
    public int checkUuserPower(@Param("groupName") String groupName, @Param("userId") Long userId);

    //查询用户组名是否存在,如果id不为空或者null，排除当前id意外检查是否已存在！
    public int checkUuserGroupByName(@Param("groupName") String groupName,@Param("id") Long id);

    //查询用户组总数
    public int getUserGroupCount();

    //用户组列表
    public List<UserGroup> getUserGroupList(@Param("offset") int offset, @Param("rows") int rows);

    //所有用户组列表
    public List<UserGroup> getAllUserGroupList();
}
