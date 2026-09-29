package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.favorite.service.FavoriteService;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
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
 * @Date: 14:25 2018/9/6
 */
@Controller
public class FavoriteController extends BaseController {

    @Autowired
    protected FavoriteService favoriteService;


    // E7 收藏平台化：任意模型内容收藏（modelCode 必填，如 articles/brands）
    @ResponseBody
    @PostMapping(value = "/ucenter/favorite/model/add")
    public DataVo addModelFavorite(@RequestParam(value = "id", required = false) String id,
                                   @RequestParam(value = "modelCode", required = false) String modelCode) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!org.apache.commons.lang3.math.NumberUtils.isCreatable(id)) {
                return data = DataVo.failure("目标id参数错误");
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(modelCode)) {
                return data = DataVo.failure("modelCode 不能为空");
            }
            if (getUser() == null) {
                return data = DataVo.failure("请登陆后收藏");
            }
            data = favoriteService.addFavorite(getUser().getUserId(), modelCode.trim(), Long.parseLong(id));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    // E7 取消收藏（平台化）
    @ResponseBody
    @PostMapping(value = "/ucenter/favorite/model/remove")
    public DataVo removeModelFavorite(@RequestParam(value = "id", required = false) String id,
                                      @RequestParam(value = "modelCode", required = false) String modelCode) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!org.apache.commons.lang3.math.NumberUtils.isCreatable(id)) {
                return data = DataVo.failure("目标id参数错误");
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(modelCode)) {
                return data = DataVo.failure("modelCode 不能为空");
            }
            if (getUser() == null) {
                return data = DataVo.failure("请登陆后操作");
            }
            data = favoriteService.removeFavorite(getUser().getUserId(), modelCode.trim(), Long.parseLong(id));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //处理关注信息
    @ResponseBody
    @PostMapping(value = "/ucenter/favorite/add")
    public DataVo addFavorite(@RequestParam(value = "id", required = false) String id,@RequestParam(value = "type", required = false) String type) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isCreatable(id)) {
                return data=DataVo.failure("话题id参数错误");
            }
            if (!NumberUtils.isCreatable(type)) {
                return data=DataVo.failure("话题id参数错误");
            }
            if(getUser()==null){
                return data=DataVo.failure("请登陆后关注");
            }
            data=favoriteService.addFavorite(getUser().getUserId(),Integer.valueOf(type),Long.parseLong(id));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

}
