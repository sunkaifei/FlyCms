package com.flycms.constant;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 13:35 2018/11/13
 */
@Configuration
@ConfigurationProperties(prefix = "solr")
@Setter
@Getter
public class SolrConst {
    /** solr服务器连接设置 **/
    private String serverUrl;

}
