package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.search.service.SolrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 15:19 2018/10/19
 */
@Controller
@RequestMapping("/system/indexes")
public class SolrAdminController extends BaseController {
    @Autowired
    private SolrService solrService;

    //索引列表
    @GetMapping(value = "/content_index")
    public String indexList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/content_index");
    }

    //删除全部索引
    @ResponseBody
    @RequestMapping(value = "/delete_all")
    public DataVo indexAllShare() {
        DataVo data = DataVo.failure("操作失败");
        try {
            solrService.deleteAllInfoindex();
            data=DataVo.success("已成功删除全部索引！");
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

}
