/**
 * 内容状态字典 —— 全站单一来源（2026-10-23）。
 *
 * 此前同一件事在三处各写一份，且叫法不一致：
 *   · modeldata/form-modal.vue（发布状态下拉）：0待审核 / 1发布 / 2未通过 / 4定时发布（缺 3）
 *   · modeldata/list.vue（列表筛选）：        0待审核 / 1已发布 / 2未通过 / 4待发布
 *   · modeldata/list.vue（列表文本）：        0待审核 / 1已发布 / 2未通过 / 3已删除 / 4待发布
 * 同一个 4，下拉里叫「定时发布」、筛选里叫「待发布」。
 *
 * 权威口径：0 待审核 / 1 已发布 / 2 未通过 / 3 已删除 / 4 定时发布（等定时任务到点转 1）。
 *
 * 后端同名概念见 ApiAuditController 与 TimingPublishJob。
 */

export const CONTENT_STATUS = {
  PENDING: 0,
  PUBLISHED: 1,
  REJECTED: 2,
  DELETED: 3,
  SCHEDULED: 4,
} as const;

export type ContentStatusValue =
  (typeof CONTENT_STATUS)[keyof typeof CONTENT_STATUS];

/** 值 → 中文标签 */
export const CONTENT_STATUS_TEXT: Record<number, string> = {
  0: '待审核',
  1: '已发布',
  2: '未通过',
  3: '已删除',
  4: '定时发布',
};

/** 值 → antd Tag 颜色 */
export const CONTENT_STATUS_COLOR: Record<number, string> = {
  0: 'orange',
  1: 'green',
  2: 'red',
  3: 'default',
  4: 'blue',
};

/** 列表筛选下拉（不列"已删除"：软删内容由内容列表的回收逻辑处理，不在常规筛选里） */
export const CONTENT_STATUS_FILTER_OPTIONS = [
  { label: '待审核', value: 0 },
  { label: '已发布', value: 1 },
  { label: '未通过', value: 2 },
  { label: '定时发布', value: 4 },
].map((o) => ({ label: o.label, value: String(o.value) }));

/** 列表筛选下拉（值保持字符串，与 Select 的 string 口径一致） */
export const CONTENT_STATUS_FILTER = [
  { label: '待审核', value: '0' },
  { label: '已发布', value: '1' },
  { label: '未通过', value: '2' },
  { label: '定时发布', value: '4' },
];

/**
 * 发布表单的状态选项：按场景收敛（此前新建内容也能选「未通过」，语义荒谬）。
 *
 * @param editing 是否为编辑已有内容
 */
export function statusOptionsFor(editing: boolean) {
  return editing
    ? [
        { label: '待审核', value: '0' },
        { label: '已发布', value: '1' },
        { label: '未通过', value: '2' },
        { label: '定时发布', value: '4' },
      ]
    : [
        { label: '待审核', value: '0' },
        { label: '立即发布', value: '1' },
        { label: '定时发布', value: '4' },
      ];
}
