package com.flycms.module.admin.dao;

import com.flycms.module.admin.model.Admin;
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
 * @Date: 9:24 2018/7/13
 */
@Repository
public interface AdminDao {

    // ///////////////////////////////
    // ///// 增加 ////////
    // ///////////////////////////////
    public int addAdmin(Admin admin);

    //添加用户权限组
    public void addAdminAndRole(@Param("adminId") Long adminId, @Param("roleId") Long roleId);
    // ///////////////////////////////
    // /////        删除      ////////
    // ///////////////////////////////
    //按id删除管理员信息
    public int deleteAdminById(@Param("adminId") Long adminId);

    //按管理员id删除权限管理员关联权限
    public int deleteAdminAndRole(@Param("adminId") Long adminId);
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    public int updateAdminLogin(Admin admin);

    public int updateAdmin(Admin admin);

    /**
     * 修改管理员密码
     *
     * @param adminId
     * @param password
     * @return Integer
     */
    public int updateAdminPassword(@Param("adminId") Long adminId, @Param("password") String password);

    // ///////////////////////////////
    // ///// 查询 ////////
    // ///////////////////////////////
    /**
     * 按用户id查询管理员信息
     *
     * @param adminId
     *         用户id
     * @param status
     *         审核状态
     * @return User
     */
    public Admin findAdminById(@Param("adminId") Long adminId, @Param("status") int status);

    /**
     * 通过username查询用户信息
     *
     * @param admin_name
     * @return User
     */
    public Admin findByUsername(@Param("admin_name") String admin_name);

    public int checkAdminByName(@Param("admin_name") String admin_name, @Param("adminId") Long adminId);

    //查询管理员总数
    public int getAdminCount(@Param("adminName") String adminName,
                             @Param("nickName") String nickName,
                             @Param("mobile") String mobile,
                             @Param("email") String email);

    //管理员列表
    public List<Admin> getAdminList(@Param("adminName") String adminName,
                                    @Param("nickName") String nickName,
                                    @Param("mobile") String mobile,
                                    @Param("email") String email,
                                    @Param("offset") int offset,
                                    @Param("rows") int rows);

}
