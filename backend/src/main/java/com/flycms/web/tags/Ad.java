package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.ad.model.AdPosition;
import com.flycms.module.ad.service.AdService;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateException;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 广告标签（fly_ad）：按广告位标识输出在投广告。
 *
 * <p>用法一（缺省直出 HTML，快速上线）：
 * {@code <@fly_ad key="sidebar"/>} —— 图片广告输出
 * {@code <a href="/ad/click/{id}" target="_blank" title="..."><img src="..." alt="..."></a>}，
 * 文字广告输出 {@code <a>}，代码广告原样输出。
 *
 * <p>用法二（自定义循环）：
 * <pre>{@code
 * <@fly_ad key="sidebar">
 *   <#list adList as ad>
 *     <a href="/ad/click/${ad.id}" target="_blank">${ad.name}</a>
 *   </#list>
 * </@fly_ad>
 * }</pre>
 * body 内可用变量：adList（id/name/adType/imageUrl/url/textContent/htmlCode/countView/countClick）、
 * adPosition（id/name/adKey/width/height）。
 *
 * <p>参数：key（广告位标识，必填）、rows（最多输出条数，默认 10）。
 * 仅输出启用中、在投放时间窗内的广告，按权重降序；渲染即计展示次数。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class Ad extends AbstractTagPlugin {

	@Autowired
	private AdService adService;

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		String key = null;
		int rows = 10;
		Map<String, TemplateModel> paramWrap = new HashMap<>(params);
		for (String str : paramWrap.keySet()) {
			if ("key".equals(str)) {
				key = paramWrap.get(str).toString();
			}
			if ("rows".equals(str)) {
				rows = Integer.parseInt(paramWrap.get(str).toString());
			}
		}

		List<Map<String, Object>> adList = new ArrayList<>();
		Map<String, Object> positionMap = new HashMap<>();
		List<com.flycms.module.ad.model.Ad> ads = new ArrayList<>();
		if (StringUtils.isNotBlank(key)) {
			try {
				AdPosition position = adService.findPositionByKey(key.trim());
				if (position != null && position.getStatus() == 1) {
					positionMap.put("id", position.getId());
					positionMap.put("name", position.getName());
					positionMap.put("adKey", position.getAdKey());
					positionMap.put("width", position.getWidth());
					positionMap.put("height", position.getHeight());
					ads = adService.listActiveAds(position.getId(), rows);
					List<Long> shownIds = new ArrayList<>();
					for (com.flycms.module.ad.model.Ad ad : ads) {
						Map<String, Object> map = new HashMap<>();
						map.put("id", ad.getId());
						map.put("name", ad.getName());
						map.put("adType", ad.getAdType());
						map.put("imageUrl", ad.getImageUrl());
						map.put("url", ad.getUrl());
						map.put("textContent", ad.getTextContent());
						map.put("htmlCode", ad.getHtmlCode());
						map.put("countView", ad.getCountView());
						map.put("countClick", ad.getCountClick());
						adList.add(map);
						shownIds.add(ad.getId());
					}
					// 渲染即计展示次数（累计总数 + 按日明细）
					adService.incrementViews(position.getId(), shownIds);
				}
			} catch (Exception e) {
				logTagFailure("fly_ad", e);
			}
		}

		env.setVariable("adList", builder.build().wrap(adList));
		try {
			env.setVariable("adPosition", builder.build().wrap(positionMap));
		} catch (TemplateModelException e) {
			logTagFailure("fly_ad#adPosition", e);
		}

		if (body == null) {
			// 缺省直出（与 /ad/js/{adKey} JS 分发共用 AdService.renderDefaultHtml）
			try {
				env.getOut().write(adService.renderDefaultHtml(ads));
			} catch (IOException e) {
				logTagFailure("fly_ad#out", e);
			}
			return;
		}
		body.render(env.getOut());
	}
}
