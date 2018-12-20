package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.module.user.model.User;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @Date: 22:20 2018/9/13
 */
@Controller
public class PeopleController extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(PeopleController.class);

    //用户首页页面
    @GetMapping(value = "/people/{shortUrl}")
    public String people(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "shortUrl", required = false) String shortUrl, ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserByShorturl(shortUrl);
        if(people==null){
            return theme.getPcTemplate("404");
        }
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("people", people);
        modelMap.addAttribute("count", userService.findUserCountById(people.getUserId()));
        return theme.getPcTemplate("/people/index");
    }

    //用户问题列表页面
    @GetMapping(value = "/people/{shortUrl}/question")
    public String peopleQuestion(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "shortUrl", required = false) String shortUrl, ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserByShorturl(shortUrl);
        if(people==null){
            return theme.getPcTemplate("404");
        }
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("people", people);
        modelMap.addAttribute("count", userService.findUserCountById(people.getUserId()));
        return theme.getPcTemplate("/people/list_question");
    }

    //用户问题列表页面
    @GetMapping(value = "/people/{shortUrl}/answers")
    public String peopleAnswers(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "shortUrl", required = false) String shortUrl, ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserByShorturl(shortUrl);
        if(people==null){
            return theme.getPcTemplate("404");
        }
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("people", people);
        modelMap.addAttribute("count", userService.findUserCountById(people.getUserId()));
        return theme.getPcTemplate("/people/list_answers");
    }

    //用户问题列表页面
    @GetMapping(value = "/people/{shortUrl}/article")
    public String peopleArticle(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "shortUrl", required = false) String shortUrl, ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserByShorturl(shortUrl);
        if(people==null){
            return theme.getPcTemplate("404");
        }
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("count", userService.findUserCountById(people.getUserId()));
        modelMap.addAttribute("people", people);
        return theme.getPcTemplate("/people/list_article");
    }

    //用户问题列表页面
    @GetMapping(value = "/people/{shortUrl}/share")
    public String peopleShare(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "shortUrl", required = false) String shortUrl, ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserByShorturl(shortUrl);
        if(people==null){
            return theme.getPcTemplate("404");
        }
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("count", userService.findUserCountById(people.getUserId()));
        modelMap.addAttribute("people", people);
        return theme.getPcTemplate("/people/list_share");
    }

    //用户关注列表页面
    @GetMapping(value = "/people/{shortUrl}/follow")
    public String peopleFollow(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "shortUrl", required = false) String shortUrl, ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserByShorturl(shortUrl);
        if(people==null){
            return theme.getPcTemplate("404");
        }
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("count", userService.findUserCountById(people.getUserId()));
        modelMap.addAttribute("people", people);
        return theme.getPcTemplate("/people/list_follow");
    }
    //用户问题列表页面
    @GetMapping(value = "/people/{shortUrl}/fans")
    public String peopleFans(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "shortUrl", required = false) String shortUrl, ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserByShorturl(shortUrl);
        if(people==null){
            return theme.getPcTemplate("404");
        }
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("count", userService.findUserCountById(people.getUserId()));
        modelMap.addAttribute("people", people);
        return theme.getPcTemplate("/people/list_fans");
    }
}
