package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelpController extends BaseController {
    //帮助列表
    @GetMapping(value = "/help_list")
    public String helpList(){
        return theme.getPcTemplate("help/list");
    }
}
