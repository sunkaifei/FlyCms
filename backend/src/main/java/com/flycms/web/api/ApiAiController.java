package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.module.ai.service.AiProviderService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * AI 内容助手 REST（G22）。红线：AI 输出一律返回给表单由人工审核后保存，
 * 服务端不直接改写已发布内容。
 *
 * <p>任务集：summary（摘要）/ keywords（关键词，逗号分隔）/ title（标题建议，换行分隔多选）/
 * translate（翻译，targetLang 指定目标语言）。复用内容发布权限（save@{modelId} 不适用——
 * 助手是跨模型的通用能力，用独立权限节点，已随迁移 SQL 登记授权）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiAiController extends ApiBaseController {

    @Autowired
    private AiProviderService aiProviderService;
    @Autowired
    private com.flycms.module.config.service.ConfigService configService;
    @Autowired
    private com.flycms.module.ai.service.EmbeddingService embeddingService;

    @ResponseBody
    @GetMapping("/system/ai/status")
    public DataVo status() {
        requirePermission("/api/system/ai/generate");
        boolean configured = aiProviderService.isConfigured();
        return DataVo.success("操作成功", Map.of(
                "configured", configured,
                "baseUrl", StringUtils.defaultString(configService.getStringByKey("fly_ai_base_url")),
                "model", StringUtils.defaultString(configService.getStringByKey("fly_ai_model")),
                "embedModel", StringUtils.defaultString(configService.getStringByKey("fly_ai_embed_model")),
                "apiKeySet", StringUtils.isNotBlank(configService.getStringByKey("fly_ai_api_key"))));
    }

    /** U1 AI 连通测试：真实调 chat，回报结果或具体错误（超时/401/网络），管理端「测试」按钮用 */
    @ResponseBody
    @PostMapping("/system/ai/test")
    public DataVo test() {
        requirePermission("/api/system/ai/generate");
        if (!aiProviderService.isConfigured()) {
            return DataVo.failure("AI 未配置完整：请在站点设置填齐 base_url / api_key / model");
        }
        return aiProviderService.chat("你是连通测试助手", "请回复：连接成功");
    }

    @ResponseBody
    @PostMapping("/system/ai/generate")
    public DataVo generate(@RequestParam(value = "task", required = false) String task,
                           @RequestParam(value = "content", required = false) String content,
                           @RequestParam(value = "title", required = false) String title,
                           @RequestParam(value = "targetLang", required = false) String targetLang) {
        requirePermission("/api/system/ai/generate");
        String text = stripTags(StringUtils.defaultString(content));
        if (StringUtils.isBlank(text) && StringUtils.isBlank(title)) {
            return DataVo.failure("请先填写正文或标题");
        }
        String system = "你是资深中文网站编辑，输出干净的纯文本结果，不要解释、不要 Markdown 修饰。";
        String user;
        switch (StringUtils.defaultString(task)) {
            case "summary" -> {
                if (text.isBlank()) {
                    return DataVo.failure("生成摘要需要先填写正文");
                }
                user = "为下面这篇文章写一段 80~120 字的内容摘要，直接给摘要正文：\n\n"
                        + abbreviate(text, 6000);
            }
            case "keywords" -> {
                if (text.isBlank()) {
                    return DataVo.failure("提取关键词需要先填写正文");
                }
                user = "从下面这篇文章提取 3~6 个关键词，用英文逗号分隔，只输出关键词：\n\n"
                        + abbreviate(text, 6000);
            }
            case "title" -> {
                user = "给下面这篇文章拟 5 个候选标题，每行一个，不要编号：\n\n"
                        + abbreviate(text.isBlank() ? title : title + "\n\n" + text, 6000);
            }
            case "translate" -> {
                if (text.isBlank()) {
                    return DataVo.failure("翻译需要先填写正文");
                }
                String lang = StringUtils.defaultIfBlank(targetLang, "English");
                user = "把下面的内容翻译成" + lang + "，只输出译文：\n\n" + abbreviate(text, 6000);
            }
            default -> {
                return DataVo.failure("不支持的任务：" + task);
            }
        }
        DataVo vo = aiProviderService.chat(system, user);
        if (vo.getCode() == DataVo.CODE_SUCCESS && "keywords".equals(task)) {
            // 关键词统一为逗号分隔（中文逗号归一）
            String kw = String.valueOf(vo.getData()).replace('，', ',');
            return DataVo.success("操作成功", kw);
        }
        return vo;
    }

    /** G21 语义检索：q 向量化 → 余弦 topK（行元数据由调用方回表） */
    @ResponseBody
    @GetMapping("/system/ai/semanticSearch")
    public DataVo semanticSearch(@RequestParam(value = "modelCode", required = false) String modelCode,
                                 @RequestParam(value = "q", required = false) String q,
                                 @RequestParam(value = "topK", defaultValue = "10") int topK) {
        requirePermission("/api/system/ai/generate");
        if (StringUtils.isBlank(q) || StringUtils.isBlank(modelCode)) {
            return DataVo.failure("请传入 modelCode 与 q");
        }
        return embeddingService.search(modelCode, q, Math.min(Math.max(topK, 1), 50));
    }

    /** G21 手动向量化单条内容（发布事件也会自动同步） */
    @ResponseBody
    @PostMapping("/system/ai/embedSync")
    public DataVo embedSync(@RequestParam(value = "modelCode", required = false) String modelCode,
                            @RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/ai/generate");
        if (StringUtils.isBlank(modelCode) || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        return embeddingService.syncOne(modelCode, id);
    }

    private String stripTags(String html) {
        return html.replaceAll("(?s)<[^>]*>", " ").replaceAll("\\s{2,}", " ").trim();
    }

    private String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
