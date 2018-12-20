package com.flycms.module.topic.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 用户话题编辑实体类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 11:57 2018/9/28
 */
@Setter
@Getter
public class TopicEdit implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    //用户id
    private Long userId;
    //话题id
    @NotNull(message="话题id不能为空！")
    private Long topicId;
    //话题编辑内容
    @NotEmpty(message="话题说明内容不能为空！")
    private String content;
    //编辑时间
    private Date createTime;
    //审核状态
    private Integer status;
}
