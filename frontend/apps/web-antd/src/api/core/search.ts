import { requestClient } from '#/api/request';

/**
 * 全文检索索引管理 API（Lucene，jieba 中文分词）。
 * 增量索引自动跟随内容生命周期（G18 事件）；本组接口只做状态查看与手动全量重建。
 */
export interface SearchIndexStatus {
  indexed: number;
  rebuilding: boolean;
}

export async function getSearchIndexStatusApi() {
  return requestClient.get<SearchIndexStatus>('/system/search/status');
}

export async function rebuildSearchIndexApi() {
  return requestClient.post('/system/search/rebuild');
}
