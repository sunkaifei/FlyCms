package com.flycms.module.share.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 15:28 2018/9/18
 */
@Setter
@Getter
public class ShareCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long fatherId;

    //分类所有归属id
    private String categoryId;

    @NotEmpty(message="分类名称不能为空")
    private String name;

    private String keywords;

    private String description;

    private Integer recomment;

    private Integer status;

    //系统分类前台不展示，仅作为不可见的文章分类，主要是给管理员归纳文章使用的，1为显示，0不显示
    private Integer issys;

    private Integer sort;

}
