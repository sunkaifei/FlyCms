package com.flycms.module.admin.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Date;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 2018-7-1
 */
@Setter
@Getter
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    @NotEmpty(message="权限组名不能为空")
    private String name;
    private Date createAt;
}
