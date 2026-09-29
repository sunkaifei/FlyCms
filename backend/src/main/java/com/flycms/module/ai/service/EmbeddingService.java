package com.flycms.module.ai.service;

import com.alibaba.fastjson2.JSON;
import com.flycms.core.entity.DataVo;
import com.flycms.core.event.ContentChangedEvent;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.ai.dao.EmbeddingDao;
import com.flycms.module.ai.model.Embedding;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * G21 语义搜索（P 阶段可自研部分）：向量存 MySQL（小站规模免向量库），Java 侧余弦检索。
 *
 * <p>链路：内容发布事件（G18 status/insert，且 status=1）→ 异步向量化（标题+正文按段落
 * 分块，每块 ≤1200 字，最多 8 块）→ {@code fly_embedding}；内容删除/下架 → 删向量。
 * 检索：查询串向量化 → 与目标模型全部向量算余弦 → topK（行元数据由调用方回表）。
 * 未配置 AI 时整条链路静默停用，关键词搜索不受影响。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private static final int CHUNK_LEN = 1200;
    private static final int MAX_CHUNKS = 8;

    @Autowired
    private AiProviderService aiProviderService;
    @Autowired
    private EmbeddingDao embeddingDao;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private com.flycms.module.model.dao.ModelDataDao modelDataDao;
    @Autowired
    private ConfigService configService;

    // /////////////////// 同步（事件驱动） ///////////////////

    /** G18 事件消费：发布/更新/下架/删除 → 维护向量。异步执行不阻塞主流程。 */
    @Async
    @org.springframework.context.event.EventListener
    public void onContentChanged(ContentChangedEvent event) {
        try {
            if (!aiProviderService.isConfigured()
                    || StringUtils.isBlank(configService.getStringByKey("fly_ai_embed_model"))) {
                return;
            }
            if (StringUtils.isBlank(event.getModelCode()) || event.getIds().isEmpty()) {
                return;
            }
            if ("delete".equals(event.getAction())
                    || ("status".equals(event.getAction()) && !isPublished(event))) {
                embeddingDao.deleteByTarget(event.getModelCode(), event.getIds());
                return;
            }
            if ("insert".equals(event.getAction())
                    || "update".equals(event.getAction())
                    || ("status".equals(event.getAction()) && isPublished(event))) {
                syncOne(event.getModelCode(), event.getIds().get(0));
            }
        } catch (Exception e) {
            log.warn("[G21] 向量同步失败（不影响主流程）：{}#{} {}", event.getModelCode(), event.getIds(), e.getMessage());
        }
    }

    private boolean isPublished(ContentChangedEvent event) {
        try {
            Model model = modelService.findModelByCode(event.getModelCode());
            if (model == null) {
                return false;
            }
            Map<String, Object> row = modelDataService.findDataById(model.getId(), event.getIds().get(0));
            return row != null && "1".equals(String.valueOf(row.get("status")));
        } catch (Exception e) {
            return false;
        }
    }

    /** 向量化单条内容（标题+正文按段落分块） */
    public DataVo syncOne(String modelCode, Long id) {
        Model model = modelService.findModelByCode(modelCode);
        if (model == null) {
            return DataVo.failure("模型不存在");
        }
        Map<String, Object> row = modelDataService.findDataById(model.getId(), id);
        if (row == null || !"1".equals(String.valueOf(row.get("status")))) {
            return DataVo.failure("内容不存在或未发布");
        }
        String title = String.valueOf(row.getOrDefault("title", ""));
        String content = stripTags(String.valueOf(row.getOrDefault("content", "")));
        List<String> chunks = chunk(title + "\n" + content);
        if (chunks.isEmpty()) {
            return DataVo.failure("无可向量化文本");
        }
        DataVo vo = aiProviderService.embed(chunks);
        if (vo.getCode() != DataVo.CODE_SUCCESS) {
            return vo;
        }
        double[][] vectors = (double[][]) vo.getData();
        embeddingDao.deleteByTarget(modelCode, List.of(id));
        String embedModel = configService.getStringByKey("fly_ai_embed_model");
        for (int i = 0; i < chunks.size(); i++) {
            Embedding e = new Embedding();
            e.setId(SnowFlake.getInstance().nextId());
            e.setTargetModel(modelCode);
            e.setTargetId(id);
            e.setChunk(i);
            e.setVectorJson(JSON.toJSONString(vectors[i]));
            e.setEmbedModel(embedModel);
            e.setUpdateTime(new Date());
            embeddingDao.add(e);
        }
        return DataVo.success("已向量化 " + chunks.size() + " 块");
    }

    // /////////////////// 检索 ///////////////////

    /** 语义检索是否可用（AI 已配置且向量模型就位）——SearchPage 标签据此决定语义/关键词通道 */
    public boolean semanticReady() {
        return aiProviderService.isConfigured()
                && StringUtils.isNotBlank(configService.getStringByKey("fly_ai_embed_model"));
    }

    /**
     * 跨模型语义检索（前台搜索页用）：查询向量化后在全部启用模型的向量中算余弦，
     * 合并排序取 topK，并回表拼好模板可直接消费的行（title/shortUrl/__modelCode/__modelName/
     * description/countView/thumbnailUrl）。
     */
    public List<Map<String, Object>> searchAcrossModels(String query, int topK) {
        List<Map<String, Object>> out = new ArrayList<>();
        DataVo vo = aiProviderService.embed(List.of(query));
        if (vo.getCode() != DataVo.CODE_SUCCESS) {
            return out;
        }
        double[] qv = ((double[][]) vo.getData())[0];
        List<Map<String, Object>> hits = new ArrayList<>();
        for (Model model : modelService.getEnabledModels()) {
            if (!modelDataDao.tableExists(model.getCode())) {
                continue;
            }
            for (Embedding e : embeddingDao.findByModel(model.getCode())) {
                try {
                    double[] v = toVector(e.getVectorJson());
                    if (v == null || v.length != qv.length) {
                        continue;
                    }
                    double score = cosine(qv, v);
                    hits.add(Map.of("model", model, "targetId", e.getTargetId(), "score", score));
                } catch (Exception ignored) {
                    // 坏向量跳过
                }
            }
        }
        hits.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        int n = Math.min(topK, hits.size());
        for (int i = 0; i < n; i++) {
            Model model = (Model) hits.get(i).get("model");
            Long id = (Long) hits.get(i).get("targetId");
            Map<String, Object> row = modelDataService.findDataById(model.getId(), id);
            if (row == null || !"1".equals(String.valueOf(row.get("status")))) {
                continue;
            }
            Map<String, Object> display = new LinkedHashMap<>(row);
            display.put("__modelCode", model.getCode());
            display.put("__modelName", model.getName());
            out.add(display);
        }
        // 展开附件缩略图（mixRows 消费 thumbnailUrl）
        if (!out.isEmpty()) {
            Map<Long, List<Map<String, Object>>> byModel = new LinkedHashMap<>();
            for (Map<String, Object> row : out) {
                byModel.computeIfAbsent(
                        modelService.findModelByCode(String.valueOf(row.get("__modelCode"))).getId(),
                        k -> new ArrayList<>()).add(row);
            }
            for (List<Map<String, Object>> rows : byModel.values()) {
                modelDataService.expandAttachments(
                        modelService.findModelByCode(
                                String.valueOf(rows.get(0).get("__modelCode"))).getId(), rows);
            }
        }
        return out;
    }


    /**
     * 语义检索：返回 [{targetId, score}]（余弦相似度降序，score∈[-1,1]，越大越相关）。
     */
    public DataVo search(String modelCode, String query, int topK) {
        if (!aiProviderService.isConfigured()) {
            return DataVo.failure(aiProviderService.notConfiguredMessage());
        }
        DataVo vo = aiProviderService.embed(List.of(query));
        if (vo.getCode() != DataVo.CODE_SUCCESS) {
            return vo;
        }
        double[] qv = ((double[][]) vo.getData())[0];
        Model model = modelService.findModelByCode(modelCode);
        if (model == null) {
            return DataVo.failure("模型不存在");
        }
        List<Embedding> all = embeddingDao.findByModel(model.getCode());
        List<Map<String, Object>> hits = new ArrayList<>();
        for (Embedding e : all) {
            try {
                double[] v = toVector(e.getVectorJson());
                if (v == null || v.length != qv.length) {
                    continue;
                }
                double score = cosine(qv, v);
                Map<String, Object> hit = new LinkedHashMap<>();
                hit.put("targetId", String.valueOf(e.getTargetId()));
                hit.put("chunk", e.getChunk());
                hit.put("score", Math.round(score * 10000) / 10000.0);
                hits.add(hit);
            } catch (Exception ignored) {
                // 坏向量跳过
            }
        }
        hits.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        if (hits.size() > topK) {
            hits = new ArrayList<>(hits.subList(0, topK));
        }
        return DataVo.success("操作成功", hits);
    }

    // /////////////////// 内部 ///////////////////

    /** 按段落/句子边界分块（每块 ≤CHUNK_LEN，最多 MAX_CHUNKS 块——长文尾部截断，控制向量成本） */
    private List<String> chunk(String text) {
        List<String> out = new ArrayList<>();
        String t = stripTags(text);
        if (t.isBlank()) {
            return out;
        }
        String[] paras = t.split("\\n+");
        StringBuilder cur = new StringBuilder();
        for (String p : paras) {
            if (cur.length() + p.length() + 1 > CHUNK_LEN && cur.length() > 0) {
                out.add(cur.toString().trim());
                cur = new StringBuilder();
                if (out.size() >= MAX_CHUNKS) {
                    return out;
                }
            }
            cur.append(p).append('\n');
        }
        if (cur.length() > 0 && out.size() < MAX_CHUNKS) {
            out.add(cur.toString().trim());
        }
        return out;
    }

    private String stripTags(String html) {
        return html == null ? "" : html.replaceAll("(?s)<[^>]*>", " ").replaceAll("\\s{2,}", " ").trim();
    }

    private double[] toVector(String json) {
        List<Double> list = JSON.parseArray(json, Double.class);
        if (list == null || list.isEmpty()) {
            return null;
        }
        double[] v = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            v[i] = list.get(i);
        }
        return v;
    }

    private double cosine(double[] a, double[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
