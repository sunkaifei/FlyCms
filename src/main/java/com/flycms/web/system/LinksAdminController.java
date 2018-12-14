/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 *
 */

package com.flycms.web.system;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.links.model.Links;
import com.flycms.module.links.service.LinksService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


/**
 * 友情链接管理
 * 
 * @author sunkaifei
 * 
 */
@Slf4j
@Controller
@RequestMapping("/system/links")
public class LinksAdminController extends BaseController {
	@Autowired
	protected LinksService linksService;

	@GetMapping(value = "/add")
	public String index(HttpServletRequest request,ModelMap modelMap) throws Exception {
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("links/add_links");
	}
	
	@ResponseBody
	@PostMapping(value = "/add_link")
	public DataVo addFriendLink(@Valid Links links, BindingResult result) {
		DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            links.setLinkName(links.getLinkName().trim());
            links.setLinkUrl(links.getLinkUrl().trim());
            data = linksService.addLinks(links);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
            log.warn(e.getMessage());
        }
		return data;
	}

	
	@GetMapping(value = "/list")
	public String FriendLinkList(HttpServletRequest request,
			@RequestParam(value = "linkType", defaultValue = "0") int linkType,
			@RequestParam(value = "show", defaultValue = "0") int show,
			@RequestParam(value = "p", defaultValue = "1") int pageNum,
			ModelMap modelMap) throws Exception {		
		PageVo<Links> pageVo = linksService.getLinksListPage(linkType,show ,pageNum,20);
		modelMap.put("pageVo", pageVo);
		modelMap.put("p", pageNum);
        modelMap.addAttribute("admin", getAdminUser());
		return theme.getAdminTemplate("links/list_links");
	}

	@ResponseBody
	@PostMapping(value = "/delete_link")
	public DataVo deleteLinks(@RequestParam(value = "id") Integer id) throws  Exception {
		DataVo data = DataVo.failure("操作失败");
		try {
            Links friendLink = linksService.findLinksById(id);
			if (friendLink != null) {
                linksService.deleteLinksById(id);
				return DataVo.success("操作成功", DataVo.NOOP);
			} else {
				DataVo.failure("您提交的友情链接信息不存在！");
			}
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			DataVo.failure("未知错误！");
		}
		return data;
	}
	
	@GetMapping(value = "/update/{id}")
	public String UpdateFriendLink(@PathVariable(value = "id", required = false) Integer id,ModelMap modelMap) throws Exception {
        Links link = linksService.findLinksById(id);
		if(link==null){
            return theme.getPcTemplate("404");
		}
		modelMap.put("link",link);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("links/update_links");
	}
	
	@ResponseBody
	@PostMapping(value = "/update_links")
	public DataVo updateFriendLink(@Valid Links links, BindingResult result) {
		DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            if (!NumberUtils.isNumber(links.getId().toString())) {
                return DataVo.failure("友情链接ID参数错误！");
            }
            links.setLinkName(links.getLinkName().trim());
            links.setLinkUrl(links.getLinkUrl().trim());
            data = linksService.updateLinksById(links);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
            log.warn(e.getMessage());
        }
		return data;
	}
}
