package com.flycms.module.user.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.user.dao.UserGroupDao;
import com.flycms.module.user.model.UserGroup;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 12:14 2018/7/9
 */
@Service
public class UserGroupService {
    @Autowired
    private UserGroupDao userGroupDao;
    // ///////////////////////////////
    // /////      增加        ////////
    // ///////////////////////////////
    //添加用户组
    public DataVo addUserGroup(UserGroup group){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkUuserGroupByName(group.getGroupName(),null)){
            return data=DataVo.failure("该用户组名已存在！");
        }
        SnowFlake snowFlake = new SnowFlake(2, 3);
        group.setId(snowFlake.nextId());
        int total = userGroupDao.addUserGroup(group);
        if(total>0){
            data=DataVo.success("操作成功");
        }else{
            data=DataVo.failure("添加失败");
        }
        return data;
    }

    public boolean addUserGroupPermission(Long groupId,Long permissionId){
        int totalCount = userGroupDao.addUserGroupPermission(groupId,permissionId);
        return totalCount > 0 ? true : false;
    }
    // ///////////////////////////////
    // /////        删除      ////////
    // ///////////////////////////////
    //按id删除角色信息
    public boolean deleteUserGroup(Long id){
        userGroupDao.deleteUserGroupPermission(id,null);
        int totalCount = userGroupDao.deleteUserGroup(id);
        return totalCount > 0 ? true : false;
    }

    //按id删除角色和权限关联信息
    public boolean deleteUserGroupPermission(Long roleId,Long permissionId){
        int totalCount = userGroupDao.deleteUserGroupPermission(roleId,permissionId);
        return totalCount > 0 ? true : false;
    }
    // ///////////////////////////////
    // /////       修改       ////////
    // ///////////////////////////////
    //添加用户组
    public DataVo updateUserGroup(UserGroup group){
        DataVo data = DataVo.failure("操作失败");
        if(group.getId()==null){
            return DataVo.failure("传递参数错误！");
        }
        if(this.checkUuserGroupByName(group.getGroupName(),group.getId())){
            return data=DataVo.failure("该用户组名已存在！");
        }
        int total = userGroupDao.updateUserGroup(group);
        if(total>0){
            data=DataVo.jump("用户更新成功！","/system/user/group_list");
        }else{
            data=DataVo.failure("添加失败");
        }
        return data;
    }


    //更新用户组排序
    public DataVo updateGroupSort(Long id,Integer sort){
        DataVo data = DataVo.failure("操作失败");
        if(userGroupDao.updateGroupSort(id,sort)>0){
            data=DataVo.success("修改成功");
        }else{
            data=DataVo.failure("修改失败！");
        }
        return data;
    }
    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    //按id查询用户组信息
    public UserGroup findUserGroupByid(Long id){
        return userGroupDao.findUserGroupByid(id);
    }

    //按用户userId查询所在的用户组信息
    public UserGroup findUuserGroupByUserId(Long userId){
        return userGroupDao.findUuserGroupByUserId(userId);
    }

    //查询用户组名和用户session里的用户id查询用户当前权限
    public boolean checkUuserPower(String groupName,Long userId) {
        int totalCount = userGroupDao.checkUuserPower(groupName,userId);
        return totalCount > 0 ? true : false;
    }

    //查询用户组名是否存在
    public boolean checkUuserGroupByName(String groupName,Long id) {
        int totalCount = userGroupDao.checkUuserGroupByName(groupName,id);
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
    public PageVo<UserGroup> getUserGroupListPage(int pageNum, int rows) {
        PageVo<UserGroup> pageVo = new PageVo<UserGroup>(pageNum);
        pageVo.setRows(rows);
        List<UserGroup> list = new ArrayList<UserGroup>();
        pageVo.setList(userGroupDao.getUserGroupList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(userGroupDao.getUserGroupCount());
        return pageVo;
    }

    //所有用户组列表
    public List<UserGroup> getAllUserGroupList(){
        return  userGroupDao.getAllUserGroupList();
    };
}
