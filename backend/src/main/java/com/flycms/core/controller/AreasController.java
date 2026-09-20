package com.flycms.core.controller;

import com.flycms.module.config.model.Areas;
import com.flycms.module.config.service.AreasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:56 2018/7/7
 */
@Controller
public class AreasController {
    @Autowired
    protected AreasService srv;

    //按父级id查询id下所有地区列表
    @ResponseBody
    @RequestMapping(value = "/areas/area_child")
    public List<Areas> selectAreasByPid(@RequestParam(value = "parentId", defaultValue = "0") int parentId){
        List<Areas> areas=srv.selectAreasByPid(parentId);
        return areas;
    }
}
