package com.flycms.module.tag.service;

import com.flycms.core.entity.PageVo;
import com.flycms.module.model.dao.ModelDataDao;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标签聚合服务（规划 §5.1 标签页 / §9.4 {@code <@fly_tag_list/>}）。
 *
 * <p><b>背景</b>：原 {@code module/search} 的 Solr 空壳已随阶段 U3 退役删除；
 * 本服务直接利用"文件系统为事实源"之外的第二类事实源：<b>模型数据表</b> {@code fly_cmodel_*}，
 * 对全部启用的内容模型做关键词聚合。
 *
 * <p><b>命中语义</b>：{@code title} / {@code keywords} 模糊匹配；表内存在 {@code tags} 列时
 * （目前只有 {@code fly_cmodel_articles}）一并匹配。这与织梦"标签页 = 关键词聚合页"的语义一致。
 *
 * <p><b>分页</b>：每个模型先取"前 page*rows 条"再在内存合并排序、切页，
 * 总数为各模型精确计数之和（不依赖 LIMIT 近似，避免"共 N 篇"说谎）。
 * 模型数量少（个位数）、内容量级在千级以内时开销可接受；更大规模应改为 UNION ALL 或搜索引擎。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class TagService {

    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    /** 单页最大条数，防止 ?rows=999999 被滥用 */
    private static final int MAX_ROWS = 100;

    /** 关键词最大长度（超出按前缀截断，避免超长 LIKE 打满慢查询） */
    private static final int MAX_KEYWORD_LEN = 50;

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private ModelDataDao modelDataDao;

    /** 各模型表是否存在 tags 列（探测一次后缓存；DDL 变更极低频） */
    private final Map<String, Boolean> tagsColumnCache = new ConcurrentHashMap<>();

    /**
     * 跨模型关键词聚合。
     *
     * @param keyword 关键词（标签名 / 搜索词）
     * @param page    页码，从 1 起
     * @param rows    每页条数
     * @return 分页结果，每行含 {@code __modelCode} / {@code __modelName}，可直接拼详情链
     */
    public TagResult search(String keyword, int page, int rows) {
        TagResult result = new TagResult();
        result.setKeyword(StringUtils.trimToEmpty(keyword));
        if (page < 1) {
            page = 1;
        }
        result.setPage(page);
        if (rows < 1) {
            rows = 10;
        }
        if (rows > MAX_ROWS) {
            rows = MAX_ROWS;
        }
        result.setRows(rows);

        String kw = result.getKeyword();
        if (StringUtils.isBlank(kw)) {
            result.setRowsList(new ArrayList<>());
            return result;
        }
        if (kw.length() > MAX_KEYWORD_LEN) {
            kw = kw.substring(0, MAX_KEYWORD_LEN);
            result.setKeyword(kw);
        }

        List<Map<String, Object>> merged = new ArrayList<>();
        List<Map<String, Object>> modelHits = new ArrayList<>();
        int total = 0;
        int perModelLimit = page * rows;

        for (Model m : safeEnabledModels()) {
            String code = m.getCode();
            if (StringUtils.isBlank(code)) {
                continue;
            }
            try {
                if (!modelDataDao.tableExists(code)) {
                    continue;
                }
                boolean hasTags = hasTagsColumn(code);
                int cnt = modelDataDao.countByKeyword(code, kw, hasTags);
                if (cnt <= 0) {
                    continue;
                }
                total += cnt;
                Map<String, Object> hit = new LinkedHashMap<>();
                hit.put("code", code);
                hit.put("name", m.getName());
                hit.put("count", cnt);
                modelHits.add(hit);
                List<Map<String, Object>> part =
                        modelDataDao.searchByKeyword(code, kw, hasTags, perModelLimit);
                if (part == null || part.isEmpty()) {
                    continue;
                }
                // 展开附件（thumbnail → thumbnailUrl）并把 snake_case 键规整为 camelCase
                modelDataService.expandAttachments(m.getId(), part);
                for (Map<String, Object> row : part) {
                    row.put("__modelCode", code);
                    row.put("__modelName", m.getName());
                    merged.add(row);
                }
            } catch (Exception e) {
                // 单个模型异常不能拖垮整页（表被删、列缺失等）
                logger.warn("标签聚合跳过模型 [{}]：{}", code, e.getMessage());
            }
        }

        // 合并排序：雪花 ID 单调递增，按 id 倒序即"最新优先"，与 ChannelRenderService.aggregateRows 一致
        merged.sort(Comparator.comparingLong((Map<String, Object> r) -> toLong(r.get("id")))
                .reversed());

        result.setTotal(total);
        result.setPageCount(rows > 0 ? (int) Math.ceil((double) total / rows) : 0);
        int from = (page - 1) * rows;
        if (from >= merged.size()) {
            result.setRowsList(new ArrayList<>());
        } else {
            int to = Math.min(from + rows, merged.size());
            result.setRowsList(new ArrayList<>(merged.subList(from, to)));
        }
        result.setRelatedTags(collectTags(merged));
        modelHits.sort(Comparator.comparingInt(
                (Map<String, Object> h) -> ((Number) h.get("count")).intValue()).reversed());
        result.setModelHits(modelHits);
        return result;
    }

    /**
     * 从命中行里统计"相关标签"：拆 {@code keywords}（逗号/顿号/空格分隔），
     * 排除关键词自身，按出现次数倒序取前 20。
     */
    private List<Map<String, Object>> collectTags(List<Map<String, Object>> rows) {
        Map<String, Integer> counter = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object kws = row.get("keywords");
            if (kws == null) {
                continue;
            }
            String s = String.valueOf(kws);
            for (String t : s.split("[,，、;；|\\s]+")) {
                String v = t.trim();
                if (v.isEmpty() || v.length() > MAX_KEYWORD_LEN) {
                    continue;
                }
                counter.merge(v, 1, Integer::sum);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        counter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .forEach(e -> {
                    Map<String, Object> tag = new LinkedHashMap<>();
                    tag.put("name", e.getKey());
                    tag.put("count", e.getValue());
                    out.add(tag);
                });
        return out;
    }

    private List<Model> safeEnabledModels() {
        try {
            List<Model> list = modelService.getEnabledModels();
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            logger.warn("读取启用模型清单失败：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean hasTagsColumn(String code) {
        return tagsColumnCache.computeIfAbsent(code, c -> {
            try {
                return modelDataDao.columnExists(c, "tags");
            } catch (Exception e) {
                logger.debug("探测 fly_cmodel_{} 的 tags 列失败：{}", c, e.getMessage());
                return false;
            }
        });
    }

    private long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 标签聚合结果。行内带 {@code __modelCode}/{@code __modelName}，模板可直接拼详情链 */
    public static class TagResult {
        private String keyword;
        private int page = 1;
        private int rows = 10;
        private int total;
        private int pageCount;
        private List<Map<String, Object>> rowsList = new ArrayList<>();
        private List<Map<String, Object>> relatedTags = new ArrayList<>();
        private List<Map<String, Object>> modelHits = new ArrayList<>();

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getRows() {
            return rows;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getPageCount() {
            return pageCount;
        }

        public void setPageCount(int pageCount) {
            this.pageCount = pageCount;
        }

        public List<Map<String, Object>> getRowsList() {
            return rowsList;
        }

        public void setRowsList(List<Map<String, Object>> rowsList) {
            this.rowsList = rowsList;
        }

        public List<Map<String, Object>> getRelatedTags() {
            return relatedTags;
        }

        public void setRelatedTags(List<Map<String, Object>> relatedTags) {
            this.relatedTags = relatedTags;
        }

        public List<Map<String, Object>> getModelHits() {
            return modelHits;
        }

        public void setModelHits(List<Map<String, Object>> modelHits) {
            this.modelHits = modelHits;
        }

        /** 供 {@code PageVo} 风格的模板用法（可选） */
        public PageVo<Map<String, Object>> toPageVo() {
            PageVo<Map<String, Object>> pv = new PageVo<>(page);
            pv.setRows(rows);
            pv.setCount(total);
            pv.setList(rowsList);
            return pv;
        }
    }
}
