package com.flycms.module.search.service;


import com.flycms.core.entity.PageVo;
import com.flycms.module.search.model.Info;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * 站内搜索服务（技术中立接缝）
 * <p>
 * 原 Solr 已整体移除（2026-09-21），规划改用 Elasticsearch。本类保留与原版本一致的
 * 方法签名（全部空实现/空结果），调用方（文章/问答/分享的索引同步、前台搜索标签与页面）
 * 无需改动；接入 Elasticsearch 时提供实现类替换本空壳即可。
 */
@Service
public class SearchService {
	protected final Logger logger = LoggerFactory.getLogger(SearchService.class);

    public boolean indexQuestionId(long id) throws ParseException {
        return true;
    }

    public boolean indexAllQuestion() throws ParseException {
        return true;
    }

    public boolean indexAllArticle() throws ParseException {
        return true;
    }

    public boolean indexArticleId(long id) throws ParseException {
        return true;
    }

    public boolean indexAllShare() throws ParseException {
        return true;
    }

    public boolean indexShareId(Long id) throws ParseException {
        return true;
    }

    public void indexDeleteInfo(Integer infoType,long infoId) {
    }

    public void deleteAllInfoindex() {
    }

    public PageVo<Info> searchInfo(String title, Long userId, Integer infoType, Long categoryId, String notId,String orderby,int page,int rows) throws IOException, ParseException {
        PageVo<Info> pageVo = new PageVo<Info>(page);
        pageVo.setRows(rows);
        pageVo.setList(new ArrayList<Info>());
        pageVo.setCount(0);
        return pageVo;
    }

    /**
     * 查询语句拼接处理，字符串是否以AND起始，如果有则去除，没有的直接返回
     * @param str
     * @return
     */
    public static String StrStartFind(String str){
        try {
            boolean b = str.startsWith(" AND ");//判断字符串是否以‘AND’开始
            if(b) {
                return str.substring(str.indexOf(" AND ")+5,str.length());
            }return str ;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


	//搜索结果翻页处理
	public String labelPage(
			String fullName,
			String city,
			String industry,
			String scale,
			String capital,
			int page,
			int rows,
			int maxdoc
		) throws IOException{

		StringBuffer link=new StringBuffer();
		if(maxdoc!=0){
            int pagesize = 10;//每页显示记录数
            if(rows>0) {
            	pagesize=rows;
            }
            int liststep = 10;//最多显示分页页数
            int pages = 1;//默认显示第一页
            if(page>1) {
            	//分页页码变量
            	pages = page;
            }
            int count = 0;
            //假设取出记录总数
            if(maxdoc>0) {
            	count = maxdoc;
            }
            int pagescount = (int) Math.ceil((double) count / pagesize);//求总页数，ceil（num）取整不小于num
            if (pagescount < pages) {
                pages = pagescount;//如果分页变量大总页数，则将分页变量设计为总页数
            }
            if (pages < 1) {
                pages = 1;//如果分页变量小于１,则将分页变量设为１
            }
            int listbegin = (pages - (int) Math.ceil((double) liststep / 2));//从第几页开始显示分页信息
            if (listbegin < 1) {
                listbegin = 1;
            }
            if(pages>=26 && pagescount>30){
            	listbegin=21;
            }
            if(pages>=26 && pagescount<30){
            	listbegin=pagescount-9;
            }
            if(pages<=pagescount && pagescount<10){
            	listbegin=1;
            }
            int listend = pages + liststep/2;//分页信息显示到第几页
            if (listend > pagescount) {
                listend = pagescount + 1;
            }

            if(listend<=10 && pagescount>=10){
            	listend = 11;
            }
            if(listend<10 && pagescount<10){
            	listend = pagescount + 1;
            }
            //获取搜索参数处理
            StringBuffer buffer = new StringBuffer();
	        if (!StringUtils.isEmpty(fullName)) {
	        	buffer.append("name="+java.net.URLEncoder.encode(fullName,"UTF-8"));
	        }
	        if (!StringUtils.isEmpty(city)) {
	        	buffer.append("&city="+city);
	        }
	        if (!StringUtils.isEmpty(industry)) {
	        	buffer.append("&it="+industry);
	        }
	        if (!StringUtils.isEmpty(scale)) {
	        	buffer.append("&sc="+scale);
	        }
	        if (!StringUtils.isEmpty(capital)) {
	        	buffer.append("&ct="+capital);
	        }

            //<显示分页信息
            //<显示上一页
            if (pages > 1) {
            	link.append("<li class=\"prev\"><a href=?"+buffer+"&p=" + (pages + -1) + " rel=\"prev\">上一页</a></li>");
            }//>显示上一页
            //<显示分页码
            for (int i = listbegin; i < listend; i++) {
	            if (i != pages) {
	            	//如果i不等于当前页
	    	        if(i<=30){
	    	        	link.append("<li><a href=?"+buffer+"&p=" + i + ">" + i + "</a></li>");
	    	        }
	            } else {
	            	if(i<=30){
	            		if((listend-1)>1) {
		            		link.append("<li class=\"active\"><a href=\"javascript:void(0);\">" + i + "</a></li>");
	            		}
	            	}
	            }
            }//显示分页码>
            //<显示下一页
            if (pages != pagescount) {
	        	if(pages<30){
	        		link.append("<li class=\"next\"><a href=?"+buffer+"&p=" + (pages + 1) +" rel=\"next\">下一页</a></li>");
	        	}
            }//>显示分页信息
		}
		return link.append("").toString();
	}
}
