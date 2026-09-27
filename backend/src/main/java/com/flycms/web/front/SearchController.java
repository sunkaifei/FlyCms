package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.core.utils.StringHelperUtils;
import com.flycms.module.search.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 11:35 2018/10/7
 */
@Controller
public class SearchController extends BaseController {
    protected final static Logger logger = LoggerFactory.getLogger(SearchController.class);
    @Autowired
    private SearchService searchService;
    @Autowired
    private com.flycms.module.template.service.TemplateResolver templateResolver;

    /**
     * 搜索列表
     *
     * @param q
     *         搜索标题
     * @param modelMap
     * @return
     */
    @GetMapping(value = {"/search"})
    public String companyList(@RequestParam(value = "q", required = false) String q,
                              @RequestParam(value = "type", required = false) String type,
                              @RequestParam(value = "ct", required = false) String ct,
                              @RequestParam(value = "p", defaultValue = "1") int p, ModelMap modelMap) {
        try {
            if(!StringUtils.isBlank(type)) {
                if(!StringHelperUtils.checkInteger(type)) {
                    type=null;
                }
            }
            if(!StringUtils.isBlank(ct)) {
                if(!StringHelperUtils.checkInteger(ct)) {
                    ct=null;
                }
            }
            if (getUser() != null) {
                modelMap.addAttribute("user", getUser());
            }
            modelMap.addAttribute("title", q);
            modelMap.addAttribute("type", type);
            modelMap.addAttribute("ct", ct);
            modelMap.addAttribute("p", p);
            return templateResolver.resolveAndExpose(
                    com.flycms.module.template.model.TemplateContext.search(), modelMap);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return theme.get404();
        }
    }
}
