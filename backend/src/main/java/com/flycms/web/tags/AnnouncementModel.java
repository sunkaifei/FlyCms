package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.announcement.model.Announcement;
import com.flycms.module.announcement.service.AnnouncementService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 网站公告标签（对标 DedeCMS mynews / 帝国公告）。
 * 模板用法：
 * <@fly_announcement_model rows="5">
 *   <#list announcementList as a>
 *     <li><a href="${(a.linkUrl)!'#'}">${a.title}</a> ${a.createTime?string("MM-dd")}</li>
 *   </#list>
 * </@fly_announcement_model>
 * params: rows(默认5)；输出变量 announcementList（仅启用且时间窗内的公告）
 */
@Service
public class AnnouncementModel extends AbstractTagPlugin {

	@Autowired
	private AnnouncementService announcementService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		int rows = 5;
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for (String str : paramWrap.keySet()) {
			if ("rows".equals(str)) {
				rows = Integer.parseInt(paramWrap.get(str).toString());
			}
		}
		java.util.List<Announcement> announcementList = announcementService.getVisibleAnnouncements(rows);
		env.setVariable("announcementList", builder.build().wrap(announcementList));
		body.render(env.getOut());
	}
}
