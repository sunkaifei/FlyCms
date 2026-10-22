package com.flycms.module.search;

import com.flycms.core.event.ContentChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * G18 内容变更 → Lucene 增量索引。
 *
 * <p>监听 {@link ContentChangedEvent}（insert/update/delete/status）：
 * 删除动作直接移除索引；其余动作按当前行状态 upsert（未发布/缺失自动移除）。
 * 评论类事件（comment_*）不改变正文文本，跳过。
 *
 * <p>契约遵循 G18：监听侧不得抛异常阻断主流程（全部 try/catch）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Component
public class SearchIndexListener {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexListener.class);

    @Autowired
    private LuceneSearchService luceneSearchService;

    @EventListener
    public void onContentChanged(ContentChangedEvent event) {
        try {
            String action = event.getAction();
            String modelCode = event.getModelCode();
            List<Long> ids = event.getIds();
            if (modelCode == null || modelCode.isBlank() || ids == null || ids.isEmpty()) {
                return;
            }
            if (action != null && action.startsWith("comment_")) {
                return;
            }
            for (Long id : ids) {
                if (id == null) {
                    continue;
                }
                if ("delete".equals(action)) {
                    luceneSearchService.remove(modelCode, id);
                } else {
                    luceneSearchService.upsert(modelCode, id);
                }
            }
        } catch (Exception e) {
            logger.warn("[lucene] 内容事件索引失败（不影响主流程）：{}", e.getMessage());
        }
    }
}
