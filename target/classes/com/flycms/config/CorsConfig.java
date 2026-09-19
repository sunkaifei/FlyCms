package com.flycms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    //配置允许跨域访问的ip--从这些url发过来的请求，都是允许支持的跨源请求
    private static String[] orginVal=new String[]{
            "www.28844.com",
            "28844.com",
            "localhost",
            "127.0.0.1"         //ip与域名会被当成两个不同的url
    };

    private void addAllowedOrigins(CorsConfiguration corsConfiguration){
        for(String origin:orginVal){
            //不同协议也是不同的url
            corsConfiguration.addAllowedOrigin("http://"+origin);
            corsConfiguration.addAllowedOrigin("https://"+origin);
        }
    }

    @Bean  //项目加载时，把过滤器生成，来统一管理跨源请求（不用再在每个controller上单独配置）
    public CorsFilter corsFilter(){
        //配置跨域访问的过滤器
        //基于url的数据源
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration=new CorsConfiguration();
        //把允许的跨域源添加到corsConfiguration中
        this.addAllowedOrigins(corsConfiguration);
        corsConfiguration.addAllowedMethod("*");          //不对method做限制,允许所有method请求(get,post....)
        corsConfiguration.addAllowedHeader("*");          //不对head做限制
        corsConfiguration.setAllowCredentials(true);      //允许跨域访问(在响应报文里带上跨域请求的凭证，和浏览器请求里面xhrFields相匹配，前后端才能正常通信)
        source.registerCorsConfiguration("/**",corsConfiguration);   //指定对当前这个服务下的所有请求都启用corsConfiguration的配置
        return new CorsFilter(source);
    }

}
