package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.module.user.model.User;
import org.apache.commons.lang.math.NumberUtils;
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
    @GetMapping(value = "/people/{id}")
    public String people(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "id", required = false) String id, ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserById(Integer.valueOf(id),0);
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
    @GetMapping(value = "/people/{id}/question")
    public String peopleQuestion(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "id", required = false) String id, ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserById(Integer.valueOf(id),0);
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
    @GetMapping(value = "/people/{id}/answers")
    public String peopleAnswers(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "id", required = false) String id, ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserById(Integer.valueOf(id),0);
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
    @GetMapping(value = "/people/{id}/article")
    public String peopleArticle(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "id", required = false) String id, ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserById(Integer.valueOf(id),0);
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
    @GetMapping(value = "/people/{id}/share")
    public String peopleShare(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "id", required = false) String id, ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserById(Integer.valueOf(id),0);
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
    @GetMapping(value = "/people/{id}/follow")
    public String peopleFollow(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "id", required = false) String id, ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserById(Integer.valueOf(id),0);
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
    @GetMapping(value = "/people/{id}/fans")
    public String peopleFans(@RequestParam(value = "p", defaultValue = "1") int p, @PathVariable(value = "id", required = false) String id, ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        User people=userService.findUserById(Integer.valueOf(id),0);
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
