package com.flycms.module.admin.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.admin.dao.GroupDao;
import com.flycms.module.admin.dao.PermissionDao;
import com.flycms.module.admin.model.Permission;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.transaction.Transactional;
import java.util.*;

@Service
public class PermissionService {
    @Autowired
    private PermissionDao permissionDao;

    @Autowired
    private GroupDao groupDao;

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
                Permission per=new Permission();
                String urlstr=info.getPatternsCondition().toString();
                //替换花括号和里面所有内容为*
                urlstr=urlstr.replaceAll("\\{([^\\}]+)\\}", "*");
                //替换前后中括号
                urlstr=urlstr.replaceAll("[\\[\\]]", "");
                // 只处理后台管理 action，其它跳过
                if (urlstr.startsWith("/system")) {
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
                                int permissionId=permissionDao.addPermission(per);
                                if(!this.markAssignedPermissions(272835742965968896L,per.getId())){
                                    //默认超级管理员组添加新的权限关联，272835742965968896为超级管理员组ID
                                    groupDao.addGroupPermission(272835742965968896L,per.getId());
                                }
                            }
                            //添加当前Controller里所有的权限路径到list里
                            newlist.add(StringUtils.deleteWhitespace(s));
                        }
                    }
                }
            }
        }

        //处理数据库多余的权限路径数据
        List<Permission> allList=this.getAllPermissions();
        for(Permission date : allList){
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
        int totalCount = permissionDao.deletePermission(id);
        permissionDao.deleteRolePermission(id);
        return totalCount > 0 ? true : false;
    }

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //添加产品分类信息
    public DataVo updatePermissions(Permission permission){
        DataVo data = DataVo.failure("操作失败");
        if(permissionDao.updatePermissions(permission)>0){
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
    public Permission findPermissionById(Long id){
        return permissionDao.findPermissionById(id);
    }

    //检查该权限是否存在
    public boolean checkPermission(String actionKey, String controller) {
        int totalCount = permissionDao.checkPermission(actionKey,controller);
        return totalCount > 0 ? true : false;
    }

    //检查多余和作废的权限设置并删除
    public void checkAndDeletePermission(List<String> list1, List<String> list2){
        for(String str1 : list1){
            for(String str2 : list2){
                if(!list1.contains(str2)){
                    // 打印出list1没有f,g
                    List<Permission> permission=permissionDao.findPermissionByActionKey(str2);
                    //删除权限信息和关联信息
                    permission.stream().filter(m -> this.deletePermission(m.getId())).findFirst().orElse(null);
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
    public List<Permission> findPermissionByUserId(Long userId) {
        return permissionDao.findPermissionByUserId(userId);
    }

    /**
     * 权限翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<Permission> getPermissionListPage(int pageNum, int rows) {
        PageVo<Permission> pageVo = new PageVo<Permission>(pageNum);
        pageVo.setRows(rows);
        List<Permission> list = new ArrayList<Permission>();
        pageVo.setList(permissionDao.getPermissionList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(permissionDao.getPermissionCount());
        return pageVo;
    }

    //查询所有权限list
    public List<Permission> getAllPermissions() {
        return permissionDao.getAllPermissions();
    }

    /**
     * 标记出 role 拥有的权限，用于在界面输出 checkbox 的 checked 属性
     * 未来用 permission left join role_permission 来优化
     */
    public boolean markAssignedPermissions(Long groupId,Long permissionId) {
        int totalCount = permissionDao.markAssignedPermissions(groupId,permissionId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 根据 controller 将 permission 进行分组
     */
    public LinkedHashMap<String, List<Permission>> groupByController(List<Permission> permissionList) {
        LinkedHashMap<String, List<Permission>> ret = new LinkedHashMap<String, List<Permission>>();
        for (Permission permission : permissionList) {
            String controller = permission.getController();
            List<Permission> list = ret.get(controller);
            if (list == null) {
                list = new ArrayList<Permission>();
                ret.put(controller, list);
            }
            list.add(permission);
        }
        return ret;
    }
}
