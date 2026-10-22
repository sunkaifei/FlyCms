package com.flycms.module.search;

import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import com.huaban.analysis.jieba.JiebaSegmenter.SegMode;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 站内搜索 Lucene 全文索引（jieba 中文分词（INDEX/SEARCH 双粒度））。
 *
 * <p><b>职责</b>：所有启用模型已发布行（status=1）的跨模型全文索引。
 * 写入走 G18 内容变更事件（{@link com.flycms.core.event.ContentChangedEvent} →
 * {@link SearchIndexListener}）：保存/更新增量索引，删除/下架即从索引移除；
 * 启动时索引为空则后台全量重建一次。查询侧 {@link #search} 返回按相关度排序的
 * 命中键（modelCode + contentId），由 TagService 回表装配展示行。
 *
 * <p><b>文档结构</b>：uid（唯一删除键 modelCode:id）、model_code、content_id、
 * title（存）/keywords/description（存）/content（不存正文，展示行回表取）。
 *
 * <p><b>失败语义</b>：索引目录不可写/打开失败只记日志并标记 {@link #openFailed}，
 * 搜索侧 {@link #isReady()} 返回 false 时 TagService 回退数据库 LIKE——搜索永不空转。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class LuceneSearchService {

    private static final Logger logger = LoggerFactory.getLogger(LuceneSearchService.class);

    /** 文档唯一键（删除/更新的 Term），modelCode:contentId */
    public static final String F_UID = "uid";
    public static final String F_MODEL_CODE = "model_code";
    public static final String F_CONTENT_ID = "content_id";
    public static final String F_TITLE = "title";
    public static final String F_KEYWORDS = "keywords";
    public static final String F_DESCRIPTION = "description";
    public static final String F_CONTENT = "content";

    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private ModelService modelService;

    @Value("${flycms.search.index-dir:./data/search-index}")
    private String indexDir;

    /** jieba 双粒度：INDEX 细粒度建索引（提召回）/ SEARCH 粗粒度查询（提精度），与 IK 双模式同设计 */
    private final JiebaAnalyzer indexAnalyzer = new JiebaAnalyzer(SegMode.INDEX);
    private final JiebaAnalyzer queryAnalyzer = new JiebaAnalyzer(SegMode.SEARCH);
    private final Object writerLock = new Object();
    private final AtomicBoolean rebuilding = new AtomicBoolean(false);

    private volatile IndexWriter writer;
    private volatile SearcherManager searcherManager;
    private volatile boolean openFailed;

    @PreDestroy
    public void close() {
        try {
            if (searcherManager != null) {
                searcherManager.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception ignored) {
        }
        indexAnalyzer.close();
        queryAnalyzer.close();
    }

    /**
     * 启动后检查：索引为空（首次启动或目录被清）则后台全量重建，不阻塞启动。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void rebuildIfEmpty() {
        if (isReady()) {
            logger.info("[lucene] 搜索索引就绪：{} 条文档", numDocs());
            return;
        }
        Thread t = new Thread(() -> {
            try {
                int n = rebuildAll();
                logger.info("[lucene] 全量索引重建完成：{} 条", n);
            } catch (Exception e) {
                logger.warn("[lucene] 全量索引重建失败（搜索回退数据库 LIKE）：{}", e.getMessage());
            }
        }, "lucene-rebuild");
        t.setDaemon(true);
        t.start();
    }

    /** 索引是否可用于查询（打开成功且有文档）。 */
    public boolean isReady() {
        return !openFailed && searcherManager != null && numDocs() > 0;
    }

    /** 当前已索引文档数（管理端状态展示用；索引未打开时为 0）。 */
    public int indexedCount() {
        return numDocs();
    }

    /** 单条增量：按当前行状态 upsert（未发布/缺失行自动从索引移除）。 */
    public void upsert(String modelCode, Long id) {
        if (modelCode == null || modelCode.isBlank() || id == null || id <= 0) {
            return;
        }
        try {
            IndexWriter w = openWriter();
            Model model = modelService.findModelByCode(modelCode);
            Document doc = null;
            if (model != null) {
                Map<String, Object> row = modelDataService.findDataById(model.getId(), id);
                if (row != null && toLong(row.get("status")) == 1) {
                    doc = buildDoc(modelCode, id, row);
                }
            }
            synchronized (writerLock) {
                if (doc != null) {
                    w.updateDocument(new Term(F_UID, uid(modelCode, id)), doc);
                } else {
                    w.deleteDocuments(new Term(F_UID, uid(modelCode, id)));
                }
                w.commit();
            }
            refreshReader();
        } catch (Exception e) {
            logger.warn("[lucene] 增量索引失败 {}#{}：{}", modelCode, id, e.getMessage());
        }
    }

    /** 从索引移除（删除/下架）。 */
    public void remove(String modelCode, Long id) {
        if (modelCode == null || id == null) {
            return;
        }
        try {
            IndexWriter w = openWriter();
            synchronized (writerLock) {
                w.deleteDocuments(new Term(F_UID, uid(modelCode, id)));
                w.commit();
            }
            refreshReader();
        } catch (Exception e) {
            logger.warn("[lucene] 移除索引失败 {}#{}：{}", modelCode, id, e.getMessage());
        }
    }

    /**
     * 全量重建：清空索引后遍历所有启用模型的已发布行。
     * 返回索引文档数；同一时刻只允许一个重建任务。
     */
    public int rebuildAll() throws Exception {
        if (!rebuilding.compareAndSet(false, true)) {
            return numDocs();
        }
        try {
            IndexWriter w = openWriter();
            synchronized (writerLock) {
                w.deleteAll();
                w.commit();
            }
            int count = 0;
            for (Model m : enabledModels()) {
                int page = 1;
                try {
                    while (true) {
                        List<Map<String, Object>> rows = modelDataService.selectPage(
                                m.getId(), null, null, 1, null, "id", "asc",
                                page, 200, null, true, null, null, null, null, true).getList();
                        if (rows == null || rows.isEmpty()) {
                            break;
                        }
                        List<Document> batch = new ArrayList<>(rows.size());
                        for (Map<String, Object> row : rows) {
                            long id = toLong(row.get("id"));
                            if (id > 0) {
                                batch.add(buildDoc(m.getCode(), id, row));
                            }
                        }
                        synchronized (writerLock) {
                            w.addDocuments(batch);
                            w.commit();
                        }
                        count += batch.size();
                        if (rows.size() < 200) {
                            break;
                        }
                        page++;
                    }
                } catch (Exception e) {
                    logger.warn("[lucene] 模型 [{}] 全量索引跳过：{}", m.getCode(), e.getMessage());
                }
            }
            refreshReader();
            return count;
        } finally {
            rebuilding.set(false);
        }
    }

    /** 是否正在全量重建。 */
    public boolean isRebuilding() {
        return rebuilding.get();
    }

    /**
     * 关键词检索：返回按相关度排序的命中键。
     *
     * @return 命中列表（每个元素 [modelCode, contentId 字符串]），total 为总命中数
     */
    public Hits search(String keyword, int page, int rows) throws Exception {
        Hits out = new Hits();
        SearcherManager sm = searcherManager;
        if (sm == null || keyword == null || keyword.isBlank()) {
            return out;
        }
        IndexSearcher searcher = sm.acquire();
        try {
            MultiFieldQueryParser parser = new MultiFieldQueryParser(
                    new String[]{F_TITLE, F_KEYWORDS, F_DESCRIPTION, F_CONTENT}, queryAnalyzer);
            org.apache.lucene.search.Query q;
            try {
                q = parser.parse(keyword.trim());
            } catch (org.apache.lucene.queryparser.classic.ParseException pe) {
                q = parser.parse(org.apache.lucene.queryparser.classic.QueryParser
                        .escape(keyword.trim()));
            }
            int limit = Math.max(page, 1) * Math.max(rows, 1);
            TopDocs td = searcher.search(q, limit);
            out.total = td.totalHits.value();
            int from = (Math.max(page, 1) - 1) * Math.max(rows, 1);
            ScoreDoc[] docs = td.scoreDocs;
            for (int i = from; i < docs.length; i++) {
                Document d = searcher.storedFields().document(docs[i].doc);
                out.keys.add(new String[]{
                        d.get(F_MODEL_CODE), d.get(F_CONTENT_ID)});
            }
            return out;
        } finally {
            sm.release(searcher);
        }
    }

    /** 检索命中：keys[i] = [modelCode, contentId]。 */
    public static class Hits {
        public final List<String[]> keys = new ArrayList<>();
        public long total;
    }

    // /////////////////// 内部 ///////////////////

    private Document buildDoc(String modelCode, long id, Map<String, Object> row) {
        Document doc = new Document();
        doc.add(new StringField(F_UID, uid(modelCode, id), Field.Store.YES));
        doc.add(new StringField(F_MODEL_CODE, modelCode, Field.Store.YES));
        doc.add(new LongPoint(F_CONTENT_ID, id));
        doc.add(new StoredField(F_CONTENT_ID, id));
        doc.add(new TextField(F_TITLE, str(row.get("title")), Field.Store.YES));
        doc.add(new TextField(F_KEYWORDS, str(row.get("keywords")), Field.Store.NO));
        doc.add(new TextField(F_DESCRIPTION, str(row.get("description")), Field.Store.YES));
        // 正文只参与检索不存储（TextField 存储会整段拉进内存）；展示行由 TagService 回表装配
        doc.add(new TextField(F_CONTENT, str(row.get("content")), Field.Store.NO));
        return doc;
    }

    private static String uid(String modelCode, Long id) {
        return modelCode + ":" + id;
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static long toLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v != null) {
            try {
                return Long.parseLong(v.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private List<Model> enabledModels() {
        try {
            List<Model> list = modelService.getEnabledModels();
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private IndexWriter openWriter() throws Exception {
        if (openFailed) {
            throw new IllegalStateException("Lucene 索引初始化失败过，不再重试");
        }
        IndexWriter w = writer;
        if (w != null) {
            return w;
        }
        synchronized (writerLock) {
            if (writer != null) {
                return writer;
            }
            Path dir = Paths.get(indexDir);
            Files.createDirectories(dir);
            // 索引侧用 INDEX 细粒度模式；换分词器后旧索引 token 不兼容，需全量重建
            IndexWriterConfig cfg = new IndexWriterConfig(indexAnalyzer);
            cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            writer = new IndexWriter(FSDirectory.open(dir), cfg);
            searcherManager = new SearcherManager(writer, true, true, new SearcherFactory());
            return writer;
        }
    }

    private void refreshReader() {
        try {
            if (searcherManager != null) {
                searcherManager.maybeRefreshBlocking();
            }
        } catch (Exception e) {
            logger.warn("[lucene] 索引读侧刷新失败：{}", e.getMessage());
        }
    }

    private int numDocs() {
        try {
            SearcherManager sm = searcherManager;
            if (sm == null) {
                return 0;
            }
            IndexSearcher s = sm.acquire();
            try {
                return s.getIndexReader().numDocs();
            } finally {
                sm.release(s);
            }
        } catch (Exception e) {
            return 0;
        }
    }
}
