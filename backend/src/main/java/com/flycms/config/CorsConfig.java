package com.flycms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    /**
     * 允许跨域访问的域名白名单（不要带协议和端口）
     * ip与域名会被当成两个不同的url，因此两者都要列出
     */
    private static String[] orginVal=new String[]{
            "www.28844.com",
            "28844.com",
            "localhost",
            "127.0.0.1"
    };

    /**
     * 历史 Bug：仅用 addAllowedOrigin("http://localhost")，
     * Origin 必须精确匹配 协议+域名+端口，而前端开发服务跑在 3000/8080 等端口，
     * "http://localhost:3000" 与 "http://localhost" 不匹配，导致跨域被拦。
     * 改用前缀模式匹配，放行白名单域名下的任意端口。
     */
    private void addAllowedOrigins(CorsConfiguration corsConfiguration){
        for(String origin:orginVal){
            //不同协议也是不同的url；:* 放行任意端口
            corsConfiguration.addAllowedOriginPattern("http://"+origin+":*");
            corsConfiguration.addAllowedOriginPattern("https://"+origin+":*");
            //同时保留默认的 80/443 端口形式（无端口写法）
            corsConfiguration.addAllowedOriginPattern("http://"+origin);
            corsConfiguration.addAllowedOriginPattern("https://"+origin);
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
