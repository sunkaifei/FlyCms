package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.guide.service.GuideService;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站点导航标签。类名 Guide → 注册名 fly_guide，与在线标签手册一致。
 *
 * <p>注意：本类与 {@code com.flycms.module.guide.model.Guide} 同名（标签注册规则
 * 类名→fly_+下划线 决定类名不能加后缀），故模型类型一律用全限定名引用，不能 import。
 *
 * 模板用法：
 * &lt;@fly_guide fatherId="0"&gt;
 *   &lt;#list guideList as g&gt;
 *     &lt;a href="${g.url}"&lt;#if (g.target)?? &amp;&amp; g.target != ''&gt; target="${g.target}"&lt;/#if&gt;&gt;${g.name}&lt;/a&gt;
 *     &lt;#list g.children as sub&gt;&lt;a href="${sub.url}"&gt;${sub.name}&lt;/a&gt;&lt;/#list&gt;
 *   &lt;/#list&gt;
 * &lt;/@fly_guide&gt;
 *
 * params: fatherId（父项 id，0=顶级，默认 0）；status（"1"=仅显示中，默认；"0"=含隐藏）
 * 输出变量 guideList（含 children 的树，节点字段：name/url/target/type/children…）
 */
@Service
public class Guide extends AbstractTagPlugin {

    @Autowired
    private GuideService guideService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        long fatherId = 0;
        boolean visibleOnly = true;
        Map<String, TemplateModel> paramWrap = new HashMap<>(params);
        for (String str : paramWrap.keySet()) {
            if ("fatherId".equals(str)) {
                try {
                    fatherId = Long.parseLong(paramWrap.get(str).toString());
                } catch (NumberFormatException ignored) {
                    fatherId = 0;
                }
            } else if ("status".equals(str) && "0".equals(paramWrap.get(str).toString())) {
                visibleOnly = false;
            }
        }

        List<com.flycms.module.guide.model.Guide> guideList = guideService.tree(visibleOnly);
        if (fatherId > 0) {
            guideList = subTree(guideList, fatherId);
        }
        env.setVariable("guideList", builder.build().wrap(guideList));
        if (body != null) {
            body.render(env.getOut());
        }
    }

    /** 从树中截取某个父项下的一层子树；fatherId<=0 返回全部根 */
    private List<com.flycms.module.guide.model.Guide> subTree(
            List<com.flycms.module.guide.model.Guide> tree, long fatherId) {
        if (fatherId <= 0) {
            return tree == null ? new ArrayList<>() : tree;
        }
        List<com.flycms.module.guide.model.Guide> found = findChildren(tree, fatherId);
        return found == null ? new ArrayList<>() : found;
    }

    private List<com.flycms.module.guide.model.Guide> findChildren(
            List<com.flycms.module.guide.model.Guide> nodes, long fatherId) {
        if (nodes == null) {
            return null;
        }
        for (com.flycms.module.guide.model.Guide g : nodes) {
            if (g.getId() != null && g.getId() == fatherId) {
                return g.getChildren();
            }
            List<com.flycms.module.guide.model.Guide> deeper = findChildren(g.getChildren(), fatherId);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }
}
