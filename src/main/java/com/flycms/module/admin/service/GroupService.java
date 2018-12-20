package com.flycms.module.admin.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.admin.dao.GroupDao;
import com.flycms.module.admin.model.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
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
@Service
public class GroupService {
    @Autowired
    private GroupDao groupDao;

    // ///////////////////////////////
    // /////      增加        ////////
    // ///////////////////////////////
    //添加用户组名
    public DataVo addGroup(String name){
        if(this.checkGroup(name)){
            return DataVo.failure("该用户组名已存在");
        }
        Group group =new Group();
        SnowFlake snowFlake = new SnowFlake(2, 3);
        group.setId(snowFlake.nextId());
        group.setName(name);
        group.setCreateAt(new Date());
        groupDao.addGroup(group);
        return DataVo.success("添加成功！");
    }

    public boolean addGroupPermission(Long roleId,Long permissionId){
        int totalCount = groupDao.addGroupPermission(roleId,permissionId);
        return totalCount > 0 ? true : false;
    }
    // ///////////////////////////////
    // /////        删除      ////////
    // ///////////////////////////////
    //按id删除角色信息
    public boolean deleteGroup(Long id){
        groupDao.deleteGroupPermission(id,null);
        int totalCount = groupDao.deleteGroup(id);
        return totalCount > 0 ? true : false;
    }

    //按id删除角色和权限关联信息
    public boolean deleteGroupPermission(Long roleId,Long permissionId){
        int totalCount = groupDao.deleteGroupPermission(roleId,permissionId);
        return totalCount > 0 ? true : false;
    }

    // ///////////////////////////////
    // /////       修改       ////////
    // ///////////////////////////////
    //按id修改组名
    public DataVo updateGroup(String name, Long id){
        DataVo data = DataVo.failure("操作失败");
        if(groupDao.updateGroup(name,id)>0){
            data=DataVo.success("修改成功");
        }else{
            data=DataVo.failure("修改失败！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    //按id查询权限组信息
    public Group findGroupById(Long id){
        return groupDao.findGroupById(id);
    }

    //检查该权限是否存在
    public boolean checkGroup(String name) {
        int totalCount = groupDao.checkGroup(name);
        return totalCount > 0 ? true : false;
    }

    /**
     * 用户组翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<Group> getGroupListPage(int pageNum, int rows) {
        PageVo<Group> pageVo = new PageVo<Group>(pageNum);
        pageVo.setRows(rows);
        List<Group> list = new ArrayList<Group>();
        pageVo.setList(groupDao.getGroupList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(groupDao.getGroupCount());
        return pageVo;
    }

    //所有权限小组列表
    public List<Group> getAllGroupList(){
        return groupDao.getAllGroupList();
    };

    //按管理员id查询所在会员组id
    public Integer findUserAndGroupById(Long adminId){
        return  groupDao.findUserAndGroupById(adminId);
    }

    //按管理员id查询所在会员组信息
    public Group findUserByGroup(Long adminId){
        return  groupDao.findUserByGroup(adminId);
    }
}
