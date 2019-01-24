package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.order.service.OrderService;
import com.flycms.module.share.model.Share;
import com.flycms.module.share.service.ShareService;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:07 2018/9/1
 */
@Controller
public class ShareController extends BaseController {
    @Autowired
    protected ShareService shareService;

    @Autowired
    protected OrderService orderService;

    /**
     * 首页
     *
     * @return
     */
    @GetMapping(value = {"/sc/" , "/sc/index"})
    public String indexShare(@RequestParam(value = "p", defaultValue = "1") int p,ModelMap modelMap){
        //String welcome = messageSourceUtil.getMessage("welcome");
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("share/index");
    }

    //分享内容详细页面
    @GetMapping(value = "/s/{shortUrl}")
    public String shareShow(@PathVariable(value = "shortUrl", required = false) String shortUrl, ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        Share share=shareService.findShareByShorturl(shortUrl);
        if(share==null){
            return theme.getPcTemplate("404");
        }
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("share", share);
        return theme.getPcTemplate("share/detail");
    }

    //问答详细页面
    @ResponseBody
    @RequestMapping(value = "/findShareById/{id}")
    public DataVo findShareById(@PathVariable(value = "id", required = false) String id,ModelMap modelMap){
        DataVo data = DataVo.failure("操作失败");
        if (!NumberUtils.isNumber(id)) {
            return data = DataVo.failure("id参数错误");
        }
        Share share=shareService.findShareById(Long.parseLong(id),2);
        if(share==null){
            return data = DataVo.failure("该内容不存在或者未审核！");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("content",share.getContent());
        return DataVo.success(map);
    }

    //发布分享资源
    @GetMapping(value = "/ucenter/share/add")
    public String addShare(ModelMap modelMap){
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        return theme.getPcTemplate("share/add");
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/share/add")
    public DataVo addShare(@Valid Share share, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            share.setUserId(getUser().getUserId());
            data = shareService.saveShare(share);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //编辑分享资源
    @GetMapping(value = "/ucenter/share/edit-{id}")
    public String editShare(@PathVariable(value = "id", required = false) String id,ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        Share share=shareService.findShareById(Long.parseLong(id),0);
        if(share==null){
            return theme.getPcTemplate("404");
        }
        if (getUser().getUserId() != share.getUserId()) {
            modelMap.addAttribute("message", "只能修改属于自己的文章！");
            return theme.getPcTemplate("message_tip");
        }
        modelMap.addAttribute("share", share);
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("share/edit_share");
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/share/share_update")
    public DataVo editShare(@Valid Share share, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        if (share.getId() == null) {
            return DataVo.failure("内容id错误！");
        }
        Share shareinfo=shareService.findShareById(share.getId(),0);
        if (getUser().getUserId() != shareinfo.getUserId()) {
            return DataVo.failure("只能修改属于自己的分享内容！");
        }
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            share.setUserId(getUser().getUserId());
            data = shareService.updateShareById(share);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //处理关注信息
    @ResponseBody
    @PostMapping(value = "/share/buy")
    public DataVo shareFollow(@RequestParam(value = "id", required = false) String id) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data=DataVo.failure("话题参数错误");
            }
            if(getUser()==null){
                return data=DataVo.failure("请登陆后关注");
            }
            data=orderService.addSharOrdere(Long.parseLong(id),getUser().getUserId());
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //分享浏览量更新
    @ResponseBody
    @GetMapping(value = "/share/viewcount")
    public DataVo updateShareViewCount(@RequestParam(value = "id", required = false) String id) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data=DataVo.failure("分享参数错误");
            }
            shareService.updateShareViewCount(Long.parseLong(id));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }
}
