package com.flycms.module.admin.service;

import com.flycms.core.utils.AdminSessionUtils;
import com.flycms.core.utils.BCryptUtils;
import com.flycms.core.utils.IpUtils;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.admin.dao.AdminDao;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AdminService {
    @Autowired
    private AdminDao adminDao;
    @Autowired
    private ConfigService configService;

    //添加管理员信息
    @Transactional
    public DataVo addAdmin(Admin admin){
        DataVo data = DataVo.failure("操作失败");
        if(admin.getPassword()!=null){
            if(!admin.getPassword().equals(admin.getRepassword())){
                return data=DataVo.success("两次密码不一样");
            }
        }else{
            return data=DataVo.success("新用户密码不能为空！");
        }

        if(this.checkAdminByName(admin.getAdminName())){
            return data=DataVo.success("用户名已被占用！");
        }
        SnowFlake snowFlake = new SnowFlake(2, 3);
        admin.setId(snowFlake.nextId());
        admin.setPassword(BCryptUtils.hashpw(admin.getPassword(), BCryptUtils.gensalt()));
        admin.setCreateAt(new Date());
        admin.setLastLoginTime(new Date());
        if(adminDao.addAdmin(admin)>0){
            adminDao.addAdminAndRole(admin.getId(),admin.getRoleId());
            return data=DataVo.success("新管理员添加成功！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////       删除       ////////
    // ///////////////////////////////
    //按id删除管理员信息
    @Transactional
    public DataVo deleteAdminById(Long adminId){
        DataVo data = DataVo.failure("操作失败");
        int totalCount = adminDao.deleteAdminById(adminId);
        if(totalCount > 0){
            adminDao.deleteAdminAndRole(adminId);
            data = DataVo.success("用户信息修改");
        }else{
            data=DataVo.failure("更新失败，请联系管理员！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////       修改       ////////
    // ///////////////////////////////
    //更新管理员信息
    @Transactional
    public DataVo updateAdmin(Admin admin){
        DataVo data = DataVo.failure("操作失败");
        if(admin.getPassword()!=null){
            if(!admin.getPassword().equals(admin.getRepassword())){
                return data=DataVo.success("两次密码不一样");
            }
            admin.setPassword(BCryptUtils.hashpw(admin.getPassword(), BCryptUtils.gensalt()));
        }
        if(this.checkAdminByName(admin.getAdminName(),admin.getId())){
            return DataVo.failure("管理员用户名已存在！");
        }

        if(adminDao.updateAdmin(admin)>0){
            //先删除权限关联
            adminDao.deleteAdminAndRole(admin.getId());
            //重新添加权限关联信息
            adminDao.addAdminAndRole(admin.getId(),admin.getRoleId());
            data=DataVo.success("更新成功");
        }else{
            data=DataVo.failure("更新失败");
        }
        return data;
    }

    /**
     * 用户登陆
     *
     * @param username
     *        用户名
     * @param password
     *        密码
     * @param request
     * @throws Exception
     */
    public Admin adminLogin(String username, String password, HttpServletRequest request) {

        Admin user = adminDao.findByUsername(username);
        if (user != null) {
            Admin login = new Admin();
            if (BCryptUtils.checkpw(password, user.getPassword())) {
                login.setId(user.getId());
                login.setAttempts(0);
                login.setLastLoginTime(new Date());
                login.setLastLoginIp(IpUtils.getIpAddr(request));
                adminDao.updateAdminLogin(login);

                //用户信息写入session
                AdminSessionUtils.setLoginMember(request,user);
            }else{
                login.setId(user.getId());
                login.setAttempts(user.getAttempts()+1);
                login.setAttemptsTime(new Date());
                adminDao.updateAdminLogin(login);
                user = null;
            }
        }
        return user;
    }

    /**
     * 修改管理员密码
     *
     * @param adminId
     *        用户id
     * @param oldPassword
     *        旧密码
     * @param password
     *        新密码
     */
    @Transactional
    public DataVo updateAdminPassword(Long adminId, String oldPassword, String password) {
        DataVo data = DataVo.failure("操作失败");
        Admin user = adminDao.findAdminById(adminId,0);
        if (user == null) {
            return data = DataVo.failure("用户信息错误");
        }
        if (BCryptUtils.checkpw(oldPassword, user.getPassword())) {
            String pwHash = BCryptUtils.hashpw(password, BCryptUtils.gensalt());
            adminDao.updateAdminPassword(adminId,pwHash);
        } else {
            return data = DataVo.failure("原始密码错误");
        }
        return data = DataVo.jump("密码已修改","/admin/user/admin_list");
    }
    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////

    //按用户id查询管理员信息
    public Admin findAdminById(Long adminId, int status){
        return adminDao.findAdminById(adminId, status);
    }

    /**
     * 通过username查询用户信息
     *
     * @param admin_name
     * @return Admin
     */
    public Admin findByUsername(String admin_name) {
        return adminDao.findByUsername(admin_name);
    }

    /**
     * 通过admin_name查询用户信息
     *
     * @param admin_name
     * @return
     */
    public boolean checkAdminByName(String admin_name) {
        int totalCount = adminDao.checkAdminByName(admin_name,null);
        return totalCount > 0 ? true : false;
    }

    /**
     * 通过admin_name查询用户信息
     *
     * @param admin_name
     * @return
     */
    public boolean checkAdminByName(String admin_name,Long adminId) {
        int totalCount = adminDao.checkAdminByName(admin_name, adminId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 用户翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<Admin> getAdminListPage(String adminName, String nickName, String mobile, String email, int pageNum, int rows) {
        PageVo<Admin> pageVo = new PageVo<Admin>(pageNum);
        pageVo.setRows(rows);
        List<Admin> list = new ArrayList<Admin>();
        pageVo.setList(adminDao.getAdminList(adminName, nickName, mobile, email,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(adminDao.getAdminCount(adminName, nickName, mobile, email));
        return pageVo;
    }
}
