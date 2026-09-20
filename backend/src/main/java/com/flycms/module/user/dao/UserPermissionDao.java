package com.flycms.module.user.dao;

import com.flycms.module.user.model.UserPermission;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionDao {

    // ///////////////////////////////
    // /////      增加        ////////
    // ///////////////////////////////
    /**
     * @author: kaifei-sun
     * @Description:
     * @param: pre
     * @Date: 11:47 2018/6/28
     *
     */
    public int addPermission(UserPermission pre);

    // ///////////////////////////////
    // /////        删除      ////////
    // ///////////////////////////////
    //按id删除权限
    public int deletePermission(@Param("id") Long id);

    //按id删除权限和权限组关联信息
    public int deleteGroupPermission(@Param("permissionId") Long permissionId);

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    public int updatePermissions(UserPermission pre);

    // ///////////////////////////////
    // /////       查詢       ////////
    // ///////////////////////////////
    //按id查询权限节点详细信息
    public UserPermission findPermissionById(@Param("id") Long id);

    //查询权限url是否存在
    public UserPermission findPermissionByActionKey(@Param("actionKey") String actionKey);

    //查询权限url是否存在
    public int checkPermission(@Param("actionKey") String actionKey, @Param("controller") String controller);

    /**
     * @author: kaifei-sun
     * @param: id
     *          用户id
     * @Description: 查询当前用户所有权限
     * @Date: 11:58 2018/6/28
     *
     */
    public List<UserPermission> findPermissionByUserId(@Param("userId") Long userId);

    //查询所有权限数量
    public int getPermissionCount();

    //权限列表
    public List<UserPermission> getPermissionList(@Param("offset") int offset, @Param("rows") int rows);

    //查询所有权限list
    public List<UserPermission> getAllPermissions();

    //检查是否有权限
    public int markAssignedPermissions(@Param("groupId") Long groupId, @Param("permissionId") Long permissionId);
}
