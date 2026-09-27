package com.flycms.web.front;

import com.flycms.core.utils.LocaleMessageSourceUtils;
import com.flycms.core.base.BaseController;
import com.flycms.module.user.model.User;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 2018-7-2
 */
@Controller
public class IndexController extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(IndexController.class);
    @Autowired
    private com.flycms.module.template.service.TemplateResolver templateResolver;
    @Autowired
    private LocaleMessageSourceUtils messageSourceUtil;

    /**
     * 403
     *
     * <p><b>为什么必须显式标 {@code @ResponseStatus}</b>：全站错误页都靠
     * {@code return "forward:/404"} 转发进来（ChannelController / ModelController / TagController …）。
     * forward 只是把请求内部转给本方法，<b>不会自动携带原始语义状态码</b>——
     * 不加注解时整站"页面不存在"都会以 <b>HTTP 200</b> 返回，搜索引擎会把 404 页当正常页收录，
     * 监控也无法区分健康与错误（2026-09-28 修复）。
     */
    @GetMapping(value = "/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String pageForbidden(ModelMap modelMap) {
        return templateResolver.resolveAndExpose(
                com.flycms.module.template.model.TemplateContext.error(403), modelMap);
    }

    /**
     * 404（状态码语义同上）
     *
     * @return
     */
    @GetMapping(value = "/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String pageNotFound(ModelMap modelMap) {
        return templateResolver.resolveAndExpose(
                com.flycms.module.template.model.TemplateContext.error(404), modelMap);
    }

    /**
     * 500（状态码语义同上）
     *
     * @return
     */
    @GetMapping(value = "/500")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String error(ModelMap modelMap) {
        return templateResolver.resolveAndExpose(
                com.flycms.module.template.model.TemplateContext.error(500), modelMap);
    }
    /**
     * 中、英区域转换控制器
     * @param response
     * @param language
     * @return
     */
    @GetMapping("/checklanguage/{language}")
    public String  language( HttpServletResponse response,@PathVariable String language,ModelMap modelMap){
        //打印日志
        Locale locale= request.getLocale();
        //logger.error("----------------"+locale.toString());
       // LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        language=language.toLowerCase();
        logger.info("language:"+language);
        if(language==null||language.equals("")){
            return "redirect:/";
        }else{
            if(language.equals("cn")){
                request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, new Locale("zh", "CN"));
            }else if(language.equals("us")){
                request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, new Locale("en", "US"));
            }else{
                request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, new Locale("zh", "CN"));
            }
        }
        return "redirect:/";
    }

    /**
     * 首页
     *
     * @return
     */
    @GetMapping(value = {"/" , "/index", "/index-{sort}"})
    public String index(@PathVariable(value = "sort", required = false) String sort,@RequestParam(value = "p", defaultValue = "1") int p,ModelMap modelMap){
        //String welcome = messageSourceUtil.getMessage("welcome");
        if("hot".equals(sort)){
            sort="hot";
        }else if("recommend".equals(sort)){
            sort="recommend";
        }else{
            sort=null;
        }
        modelMap.addAttribute("sort", sort);
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("user", getUser());
        return templateResolver.resolveAndExpose(
                com.flycms.module.template.model.TemplateContext.index(), modelMap);
    }


    /**
     * 首页
     *
     * @return
     */
    @GetMapping(value = {"/explore/"})
    public String explore(@RequestParam(value = "p", defaultValue = "1") int p,ModelMap modelMap){
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("explore");
    }

    /**
     * 首页
     *
     * @return
     */
    @ResponseBody
    @GetMapping(value = {"/.well-known/pki-validation/fileauth.txt"})
    public void fileauth() throws Exception{
        PrintWriter out = response.getWriter();
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-type", "text/html;charset=utf-8");
        response.setContentType("text/javascript;charset=utf-8");

        out.println("201811230718454idcyp4cua5505z6ve2wv4sev8t27ooy181p1gh7b7xkg1nuet");
        out.close();
    }
}
