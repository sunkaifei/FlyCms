package com.flycms.module.question.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 11:24 2018/9/11
 */
@Setter
@Getter
public class QuestionFollow implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long questionId;
    private Long userId;
}
