package com.flycms.module.admin.dao;

import com.flycms.module.admin.model.Permission;
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
public interface PermissionDao {

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
    public int addPermission(Permission pre);

    // ///////////////////////////////
    // /////        删除      ////////
    // ///////////////////////////////
    //按id删除权限
    public int deletePermission(@Param("id") Long id);

    //按id删除权限和权限组关联信息
    public int deleteRolePermission(@Param("permissionId") Long permissionId);

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    public int updatePermissions(Permission pre);

    // ///////////////////////////////
    // /////       查詢       ////////
    // ///////////////////////////////
    //按id查询权限节点详细信息
    public Permission findPermissionById(@Param("id") Long id);

    //查询权限url是否存在
    public List<Permission> findPermissionByActionKey(@Param("actionKey") String actionKey);

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
    public List<Permission> findPermissionByUserId(@Param("adminId") Long adminId);

    //查询所有权限数量
    public int getPermissionCount();

    //权限列表
    public List<Permission> getPermissionList(@Param("offset") int offset, @Param("rows") int rows);

    //查询所有权限list
    public List<Permission> getAllPermissions();

    //检查是否有权限
    public int markAssignedPermissions(@Param("groupId") Long groupId, @Param("permissionId") Long permissionId);
}
