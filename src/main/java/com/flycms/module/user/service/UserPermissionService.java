package com.flycms.module.user.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;

import com.flycms.core.utils.SnowFlake;
import com.flycms.module.user.dao.UserPermissionDao;
import com.flycms.module.user.model.UserPermission;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.transaction.Transactional;
import java.util.*;
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
public class UserPermissionService {
    @Autowired
    private UserPermissionDao userPermissionDao;

    @Autowired
    WebApplicationContext applicationContext;

    //同步并更新所有权限
    @Transactional
    public boolean getSyncAllPermission(){
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        //获取url与类和方法的对应信息
        Map<RequestMappingInfo,HandlerMethod> map = mapping.getHandlerMethods();
        List<String> datalist = new ArrayList<String>();
        List<String> newlist = new ArrayList<String>();
        for (RequestMappingInfo info : map.keySet()){
            //获取url的Set集合，一个方法可能对应多个url
            Set<String> patterns = info.getPatternsCondition().getPatterns();
            for (String url : patterns){
                UserPermission per=new UserPermission();
                String urlstr=info.getPatternsCondition().toString();
                //替换花括号和里面所有内容为*
                urlstr=urlstr.replaceAll("\\{([^\\}]+)\\}", "*");
                //替换前后中括号
                urlstr=urlstr.replaceAll("[\\[\\]]", "");
                // 只处理后台管理 action，其它跳过
                if (!urlstr.startsWith("/system") || urlstr.startsWith("/ucenter")) {
                    per.setActionKey(urlstr);
                    per.setController(map.get(info).getBean().toString());
                    String str=per.getActionKey();
                    String[] array=str.split("\\|\\|");
                    for(String s:array){
                        if(s!=null){
                            if (!checkPermission(StringUtils.deleteWhitespace(s), per.getController())) {
                                per.setActionKey(StringUtils.deleteWhitespace(s));
                                SnowFlake snowFlake = new SnowFlake(2, 3);
                                per.setId(snowFlake.nextId());
                                int permissionId=userPermissionDao.addPermission(per);
                            }
                            //添加当前Controller里所有的权限路径到list里
                            newlist.add(StringUtils.deleteWhitespace(s));
                        }
                    }
                }
            }
        }

        //处理数据库多余的权限路径数据
        List<UserPermission> allList=this.getAllPermissions();
        for(UserPermission date : allList){
            datalist.add(date.getActionKey());
        }
        //检查并删除
        this.checkAndDeletePermission(newlist,datalist);
        return true;
    }
    // ///////////////////////////////
    // /////        删除      ////////
    // ///////////////////////////////
    //按id删除权限权限
    @Transactional
    public boolean deletePermission(Long id){
        int totalCount = userPermissionDao.deletePermission(id);
        userPermissionDao.deleteGroupPermission(id);
        return totalCount > 0 ? true : false;
    }

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //修改用户组权限信息
    public DataVo updatePermissions(UserPermission permission){
        DataVo data = DataVo.failure("操作失败");
        if(userPermissionDao.updatePermissions(permission)>0){
            data=DataVo.jump("修改成功","/system/user/group_update/"+permission.getId());
        }else{
            data=DataVo.failure("修改失败！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    //按id查询权限组信息
    public UserPermission findPermissionById(Long id){
        return userPermissionDao.findPermissionById(id);
    }

    //检查该权限是否存在
    public boolean checkPermission(String actionKey, String controller) {
        int totalCount = userPermissionDao.checkPermission(actionKey,controller);
        return totalCount > 0 ? true : false;
    }

    //检查多余和作废的权限设置并删除
    public void checkAndDeletePermission(List<String> list1, List<String> list2){
        for(String str1 : list1){
            for(String str2 : list2){
                if(!list1.contains(str2)){
                    // 打印出list1没有f,g
                    UserPermission permission=userPermissionDao.findPermissionByActionKey(str2);
                    //删除权限信息和关联信息
                    if(permission!=null){
                        this.deletePermission(permission.getId());
                    }
                }
            }
        }
    }

    /**
     * 查询该用户所有权限url
     *
     * @param userId
     * @return
     */
    public List<UserPermission> findPermissionByUserId(Long userId) {
        return userPermissionDao.findPermissionByUserId(userId);
    }

    /**
     * 权限翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<UserPermission> getPermissionListPage(int pageNum, int rows) {
        PageVo<UserPermission> pageVo = new PageVo<UserPermission>(pageNum);
        pageVo.setRows(rows);
        List<UserPermission> list = new ArrayList<UserPermission>();
        pageVo.setList(userPermissionDao.getPermissionList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(userPermissionDao.getPermissionCount());
        return pageVo;
    }

    //查询所有权限list
    public List<UserPermission> getAllPermissions() {
        return userPermissionDao.getAllPermissions();
    }

    /**
     * 标记出 role 拥有的权限，用于在界面输出 checkbox 的 checked 属性
     * 未来用 permission left join role_permission 来优化
     */
    public boolean markAssignedPermissions(Long groupId,Long permissionId) {
        int totalCount = userPermissionDao.markAssignedPermissions(groupId,permissionId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 根据 controller 将 permission 进行分组
     */
    public LinkedHashMap<String, List<UserPermission>> groupByController(List<UserPermission> permissionList) {
        LinkedHashMap<String, List<UserPermission>> ret = new LinkedHashMap<String, List<UserPermission>>();

        for (UserPermission permission : permissionList) {
            String controller = permission.getController();
            List<UserPermission> list = ret.get(controller);
            if (list == null) {
                list = new ArrayList<UserPermission>();
                ret.put(controller, list);
            }

            list.add(permission);
        }

        return ret;
    }
}
