package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.block.service.BlockService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 碎片/推荐位标签（规划阶段 E，对标帝国碎片）。类名 Block → 注册名 fly_block，与规划文档用法一致。
 * 模板用法：
 * <@fly_block key="home_focus">
 *   <#if block?? && block.blockType == 2>
 *     <#list block.items as it><a href="${it.url}">${it.title}</a></#list>
 *   <#elseif block??>${block.content!''}</#if>
 * </@fly_block>
 * params: key(调用键)；输出变量 block（含 items：时间窗内已启用条目，item_count 限量）
 */
@Service
public class Block extends AbstractTagPlugin {

	@Autowired
	private BlockService blockService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		String key = null;
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for (String str : paramWrap.keySet()) {
			if ("key".equals(str)) {
				key = paramWrap.get(str).toString();
			}
		}
		// 渲染走 BlockService.findRenderBlock：命中 cache_seconds（0=不缓存），
		// 条目任何变更会立即失效缓存（§6.4 发布即生效红线）。
		com.flycms.module.block.model.Block block = blockService.findRenderBlock(key);
		if (block != null && block.getItems() == null) {
			block = null;
		}
		env.setVariable("block", builder.build().wrap(block));
		body.render(env.getOut());
	}
}
