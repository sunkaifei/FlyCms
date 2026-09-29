package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.comment.service.CommentService;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 平台评论前台接口（E4，U3 随旧文章评论通道退役而上线）。
 *
 * <p>发表评论走 session 用户（/ucenter/** 口径与收藏一致，未登录由 Service 拒绝）；
 * 目标为任意自定义模型内容（target_model + target_id）。
 * 列表展示由模板标签 {@code <@fly_commentpage/>} 渲染，不经此控制器。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
public class CommentController extends BaseController {

    @Autowired
    protected CommentService commentService;

    /** 发表评论 */
    @ResponseBody
    @PostMapping(value = "/ucenter/comment/save")
    public DataVo save(@RequestParam(value = "targetModel", required = false) String targetModel,
                       @RequestParam(value = "targetId", required = false) String targetId,
                       @RequestParam(value = "content", required = false) String content,
                       @RequestParam(value = "parentId", required = false) String parentId) {
        DataVo data;
        try {
            if (getUser() == null) {
                return DataVo.failure("请登录后评论");
            }
            if (!NumberUtils.isCreatable(targetId)) {
                return DataVo.failure("目标内容参数错误");
            }
            Long parent = NumberUtils.isCreatable(parentId) ? Long.parseLong(parentId) : 0L;
            data = commentService.addComment(targetModel, Long.parseLong(targetId),
                    getUser().getUserId(), content, parent);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }
}
