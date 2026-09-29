package com.flycms.module.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.flycms.core.entity.DataVo;
import com.flycms.module.config.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 服务提供者（G22/G21，P 阶段）：OpenAI 兼容协议的 chat / embeddings 客户端。
 *
 * <p>配置三键（系统参数 fly_config_web，后台参数配置或 SQL 维护）：
 * <ul>
 *   <li>{@code fly_ai_base_url} — 如 https://api.deepseek.com/v1、https://dashscope.aliyuncs.com/compatible-mode/v1</li>
 *   <li>{@code fly_ai_api_key} — Bearer Key（DeepSeek / 通义 / OpenAI 等 OpenAI 兼容服务均可）</li>
 *   <li>{@code fly_ai_model} / {@code fly_ai_embed_model} — 对话模型 / 向量模型</li>
 * </ul>
 * 三键齐备即 {@link #isConfigured()}；未配置时所有 AI 端点返回可读提示，全站功能不受影响。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class AiProviderService {

    private static final Logger log = LoggerFactory.getLogger(AiProviderService.class);

    @Autowired
    private ConfigService configService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public boolean isConfigured() {
        return StringUtils.isNotBlank(configService.getStringByKey("fly_ai_base_url"))
                && StringUtils.isNotBlank(configService.getStringByKey("fly_ai_api_key"))
                && StringUtils.isNotBlank(configService.getStringByKey("fly_ai_model"));
    }

    public String notConfiguredMessage() {
        return "AI 服务未配置：请在系统参数中设置 fly_ai_base_url / fly_ai_api_key / fly_ai_model（OpenAI 兼容服务均可）";
    }

    /** 对话补全：system + user → 文本 */
    public DataVo chat(String system, String user) {
        if (!isConfigured()) {
            return DataVo.failure(notConfiguredMessage());
        }
        String base = trimSlash(configService.getStringByKey("fly_ai_base_url"));
        String model = configService.getStringByKey("fly_ai_model");
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", system == null ? "" : system),
                Map.of("role", "user", "content", user == null ? "" : user)));
        body.put("temperature", 0.7);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + configService.getStringByKey("fly_ai_api_key"))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                    .build();
            HttpResponse<String> resp =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("AI chat 响应 {}：{}", resp.statusCode(), abbreviate(resp.body()));
                return DataVo.failure("AI 服务响应 " + resp.statusCode() + "，请检查配置与额度");
            }
            JSONObject json = JSON.parseObject(resp.body());
            String content = json.getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content");
            return DataVo.success("操作成功", StringUtils.trimToEmpty(content));
        } catch (Exception e) {
            log.warn("AI chat 调用失败：{}", e.getMessage());
            return DataVo.failure("AI 服务调用失败：" + e.getMessage());
        }
    }

    /** 向量化（G21 语义搜索）：texts → 向量数组（顺序与入参一致） */
    @SuppressWarnings("unchecked")
    public DataVo embed(List<String> texts) {
        if (!isConfigured()) {
            return DataVo.failure(notConfiguredMessage());
        }
        String embedModel = configService.getStringByKey("fly_ai_embed_model");
        if (StringUtils.isBlank(embedModel)) {
            return DataVo.failure("未配置向量模型 fly_ai_embed_model");
        }
        String base = trimSlash(configService.getStringByKey("fly_ai_base_url"));
        JSONObject body = new JSONObject();
        body.put("model", embedModel);
        body.put("input", texts);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/embeddings"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + configService.getStringByKey("fly_ai_api_key"))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                    .build();
            HttpResponse<String> resp =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("AI embed 响应 {}：{}", resp.statusCode(), abbreviate(resp.body()));
                return DataVo.failure("AI 服务响应 " + resp.statusCode());
            }
            JSONObject json = JSON.parseObject(resp.body());
            JSONArray arr = json.getJSONArray("data");
            double[][] vectors = new double[arr.size()][];
            for (int i = 0; i < arr.size(); i++) {
                JSONArray v = arr.getJSONObject(i).getJSONArray("embedding");
                double[] vec = new double[v.size()];
                for (int j = 0; j < v.size(); j++) {
                    vec[j] = v.getDoubleValue(j);
                }
                vectors[i] = vec;
            }
            return DataVo.success("操作成功", vectors);
        } catch (Exception e) {
            log.warn("AI embed 调用失败：{}", e.getMessage());
            return DataVo.failure("AI 服务调用失败：" + e.getMessage());
        }
    }

    private String trimSlash(String v) {
        return StringUtils.removeEnd(StringUtils.trimToEmpty(v), "/");
    }

    private String abbreviate(String s) {
        return s == null ? "" : s.substring(0, Math.min(s.length(), 300));
    }
}
