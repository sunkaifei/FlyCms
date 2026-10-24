<script lang="ts" setup>
import type { AuditArticleRow, ContentAuditLogRow } from '#/api/core/audit';
import type { ModelFieldRow, ModelRow } from '#/api/core/model';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Drawer,
  Empty,
  Input,
  message,
  Modal,
  Pagination,
  Select,
  Switch,
  Table,
  Tag,
  Textarea,
} from 'ant-design-vue';

import {
  auditArticleApi,
  batchAuditApi,
  getAuditDetailApi,
  getAuditLogsApi,
  getAuditPageAllApi,
  getAuditPageApi,
  getAuditSwitchApi,
  getPendingCountApi,
  setAuditSwitchApi,
} from '#/api/core/audit';
import {
  getFormMetaApi,
  getModelListApi,
  getUserOptionsApi,
} from '#/api/core/model';
import {
  getSearchIndexStatusApi,
  rebuildSearchIndexApi,
} from '#/api/core/search';
import { sanitizeHtml } from '#/utils/sanitize-html';

defineOptions({ name: 'SystemAudit' });

const { hasAccessByCodes } = useAccess();

/** 状态字典：全站单一来源（发布表单/列表筛选/列表文本三处此前各写一份，且 4 的叫法不一致） */
const STATUS = {
  0: { label: '待审核', color: 'orange' },
  1: { label: '已发布', color: 'green' },
  2: { label: '未通过', color: 'red' },
  3: { label: '已删除', color: 'default' },
  4: { label: '定时发布', color: 'blue' },
} as const;

const STATUS_OPTIONS = [
  { label: '待审核', value: 0 },
  { label: '已发布', value: 1 },
  { label: '未通过', value: 2 },
  { label: '定时发布', value: 4 },
  { label: '全部', value: -1 },
];

const rows = ref<AuditArticleRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const selectedKeys = ref<string[]>([]);
const status = ref(0);
const title = ref('');

const models = ref<ModelRow[]>([]);
/** 默认跨模型视图：modelId 为空串 = 全部模型 */
const modelId = ref<string>('');

/** 审核开关：1=先审后发 */
const auditSwitch = ref(false);
/** 待审总数（顶栏常驻，此前 getPendingCountApi 定义了却从未调用） */
const pendingCount = ref(0);

/** 全文检索索引 */
const indexedCount = ref(0);
const indexRebuilding = ref(false);
let indexPollTimer: ReturnType<typeof setInterval> | undefined;

/** 作者 id → 昵称（此前列表直接把雪花 ID 甩给审核员看） */
const userNames = ref<Record<string, string>>({});
/** 模型 id → 字段元数据（详情抽屉按显示名渲染自定义字段） */
const fieldMeta = ref<Record<string, ModelFieldRow[]>>({});

const canAudit = computed(() => hasAccessByCodes(['/api/system/audit/audit']));
const canBatch = computed(() => hasAccessByCodes(['/api/system/audit/batch']));

async function loadSearchIndexStatus() {
  try {
    const res = await getSearchIndexStatusApi();
    indexedCount.value = res?.indexed ?? 0;
    const rebuilding = Boolean(res?.rebuilding);
    const wasRebuilding = indexRebuilding.value;
    indexRebuilding.value = rebuilding;
    if (wasRebuilding && !rebuilding) {
      message.success('搜索索引重建完成');
    }
    if (rebuilding && !indexPollTimer) {
      indexPollTimer = setInterval(() => {
        if (indexRebuilding.value) {
          loadSearchIndexStatus();
        } else {
          clearInterval(indexPollTimer);
          indexPollTimer = undefined;
        }
      }, 2000);
    }
  } catch {
    // 状态获取失败不打扰主流程
  }
}

async function onRebuildIndex() {
  Modal.confirm({
    content:
      '将清空并按当前内容全量重建搜索索引，期间搜索自动走数据库兜底，完成后自动恢复。继续？',
    okText: '开始重建',
    title: '重建全文检索索引',
    onOk: async () => {
      try {
        await rebuildSearchIndexApi();
        message.info('索引重建已启动');
        indexRebuilding.value = true;
        loadSearchIndexStatus();
      } catch {
        message.error('重建启动失败');
      }
    },
  });
}

async function loadPending() {
  try {
    pendingCount.value = Number(await getPendingCountApi()) || 0;
  } catch {
    pendingCount.value = 0;
  }
}

async function load() {
  loading.value = true;
  try {
    const common = {
      p: page.value,
      rows: 20,
      status: status.value >= 0 ? status.value : undefined,
      title: title.value || undefined,
    };
    const res = modelId.value
      ? await getAuditPageApi({ ...common, modelId: modelId.value })
      : await getAuditPageAllApi(common);
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

async function loadSwitch() {
  const v = await getAuditSwitchApi();
  auditSwitch.value = Number(v) === 1;
}

function onSearch() {
  page.value = 1;
  load();
}

async function onToggleSwitch(checked: boolean) {
  await setAuditSwitchApi(checked ? 1 : 0);
  message.success(checked ? '已开启先审后发' : '已切换为直接发布');
}

/** 行归属的模型 id（单模型视图下行里没有 _modelId） */
function rowModelId(row: Record<string, any>): string {
  return String(row._modelId || modelId.value || '');
}

function rowModelName(row: Record<string, any>): string {
  return String(row._modelName || '');
}

function authorName(row: Record<string, any>): string {
  const id = String(row.userId ?? '');
  if (!id) return '—';
  return userNames.value[id] || id;
}

// /////////// 审核动作 ///////////

const rejectVisible = ref(false);
const rejectReason = ref('');
const rejectTarget = ref<string[]>([]);
/** 是否同时给作者发站内信（默认开：审核结果必须送到人手里） */
const rejectNotify = ref(true);

function openReject(ids: string[]) {
  if (!ids.length) {
    message.warning('请先选择内容');
    return;
  }
  rejectTarget.value = ids;
  rejectReason.value = '';
  rejectNotify.value = true;
  rejectVisible.value = true;
}

async function confirmReject() {
  const reason = rejectReason.value.trim();
  if (!reason) {
    message.warning('驳回必须填写原因');
    return;
  }
  const ids = rejectTarget.value;
  await runAudit(ids, 2, reason);
  rejectVisible.value = false;
  detailOpen.value = false;
}

/**
 * 批量通过需二次确认：一次点错就是批量内容直接上线，
 * 此前没有任何确认直接改状态。
 */
function onPass(ids: string[]) {
  if (!ids.length) {
    message.warning('请先选择内容');
    return;
  }
  if (ids.length === 1) {
    void runAudit(ids, 1);
    return;
  }
  Modal.confirm({
    content: `确认通过选中的 ${ids.length} 条内容？通过后它们将立即在前台可见。`,
    okText: '确认通过',
    title: '批量通过',
    onOk: async () => {
      await runAudit(ids, 1);
    },
  });
}

async function runAudit(ids: string[], target: 1 | 2, reason?: string) {
  try {
    const notify = rejectNotify.value ? undefined : 0;
    const row = rows.value.find((r) => String(r.id) === ids[0]);
    // 后端按实际送达情况回执（含「内容无作者」这类正常跳过），不再由前端编造提示
    const text = ids.length === 1
      ? await auditArticleApi(ids[0]!, target, reason, rowModelId(row ?? {}), notify)
      : await batchAuditApi(ids, target, reason, rowModelId(row ?? {}), notify);
    message.success(text || (target === 2 ? '已驳回' : '已通过'));
    selectedKeys.value = [];
    await load();
    loadPending();
  } catch (e: any) {
    message.error(e?.message ?? '操作失败');
  }
}

// /////////// 详情抽屉（审核员必须先看到正文才能做决定） ///////////

const detailOpen = ref(false);
const detailLoading = ref(false);
const detail = ref<Record<string, any>>({});
const detailRow = ref<Record<string, any>>({});
/** 清洗后的正文（投稿内容不可信，渲染前必须过白名单） */
const safeContent = computed(() => sanitizeHtml(String(detail.value.content ?? '')));

/** 详情里的自定义字段（按字段元数据显示名渲染） */
const detailFields = computed(() => {
  const mid = rowModelId(detailRow.value);
  const list = fieldMeta.value[mid] ?? [];
  const out: { label: string; value: any }[] = [];
  const SKIP = new Set([
    'id',
    'title',
    'content',
    'thumbnail',
    'shortUrl',
    'short_url',
    'createTime',
    'create_time',
    'updateTime',
    'update_time',
    'publishTime',
    'publish_time',
    'status',
    'userId',
    'user_id',
    'categoryId',
    'category_id',
    'isDelete',
    'is_delete',
    'countView',
    'count_view',
    'countComment',
    'count_comment',
  ]);
  for (const f of list) {
    if (SKIP.has(f.fieldName)) continue;
    const raw = detail.value[f.fieldName] ?? detail.value[toSnake(f.fieldName)];
    if (raw === undefined || raw === null || raw === '') continue;
    if (f.fieldType === 'editor' || f.fieldType === 'formula') continue;
    out.push({ label: f.fieldLabel, value: raw });
  }
  // 元数据没覆盖到的列（如新建后未登记字段）按原样列出，宁多勿漏
  const known = new Set(list.map((f) => f.fieldName));
  for (const [k, v] of Object.entries(detail.value)) {
    if (SKIP.has(k) || k.startsWith('_') || k.endsWith('List') || k.endsWith('Obj')) continue;
    if (known.has(k) || known.has(toCamel(k))) continue;
    if (v === null || v === undefined || v === '') continue;
    if (typeof v === 'object') continue;
    out.push({ label: k, value: v });
  }
  return out;
});

function toSnake(s: string) {
  return s.replaceAll(/([A-Z])/g, (m) => `_${m.toLowerCase()}`);
}
function toCamel(s: string) {
  return s
    .split('_')
    .map((p, i) => (i === 0 ? p : p.charAt(0).toUpperCase() + p.slice(1)))
    .join('');
}

async function ensureMeta(mid: string) {
  if (!mid || fieldMeta.value[mid]) return;
  try {
    const meta = await getFormMetaApi(mid);
    fieldMeta.value[mid] = meta.fields ?? [];
  } catch {
    fieldMeta.value[mid] = [];
  }
}

async function openDetail(row: Record<string, any>) {
  const mid = rowModelId(row);
  if (!mid) {
    message.warning('请先选择内容模型');
    return;
  }
  detailRow.value = row;
  detail.value = {};
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    await ensureMeta(mid);
    const res = await getAuditDetailApi({ id: String(row.id), modelId: mid });
    detail.value = res ?? {};
  } catch (e: any) {
    message.error(e?.message ?? '内容详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

/** 在抽屉里直接审：看完即决，不用关掉再回列表找按钮 */
function passFromDetail() {
  void runAudit([String(detailRow.value.id)], 1);
  detailOpen.value = false;
}
function rejectFromDetail() {
  openReject([String(detailRow.value.id)]);
}

// /////////// 审核历史 ///////////

const logOpen = ref(false);
const logLoading = ref(false);
const logs = ref<ContentAuditLogRow[]>([]);
const logRow = ref<Record<string, any>>({});

async function openLogs(row: Record<string, any>) {
  const mid = rowModelId(row);
  if (!mid) {
    message.warning('请先选择内容模型');
    return;
  }
  logRow.value = row;
  logOpen.value = true;
  logLoading.value = true;
  try {
    const res = await getAuditLogsApi({
      id: String(row.id),
      modelId: mid,
      p: 1,
      rows: 50,
    });
    logs.value = res.list ?? [];
  } catch (e: any) {
    message.error(e?.message ?? '审核历史加载失败');
    logs.value = [];
  } finally {
    logLoading.value = false;
  }
}

function fmtTime(v?: string) {
  return (v ?? '').replace('T', ' ').slice(0, 16);
}

const columns = computed(() => {
  const cols: Record<string, any>[] = [
    { title: '标题', dataIndex: 'title', key: 'title' },
  ];
  if (!modelId.value) {
    cols.push({ title: '模型', key: 'model', width: 110 });
  }
  cols.push(
    { title: '作者', key: 'author', width: 120 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
    { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
    { title: '操作', key: 'action', width: 220 },
  );
  return cols;
});

async function loadUsers() {
  try {
    const list = (await getUserOptionsApi()) ?? [];
    userNames.value = Object.fromEntries(
      list.map((u: any) => [String(u.id), String(u.label ?? u.nickName ?? u.id)]),
    );
  } catch {
    userNames.value = {};
  }
}

onMounted(async () => {
  await loadSwitch();
  try {
    const res = await getModelListApi({ p: 1, rows: 100 });
    models.value = res.list ?? [];
  } catch {
    models.value = [];
  }
  loadUsers();
  load();
  loadPending();
  loadSearchIndexStatus();
});
</script>

<template>
  <Page
    title="投稿审核"
    description="按内容模型归并的审核队列：先看内容，再决定通过或驳回（驳回必须填写原因，原因会存档可追溯）。"
  >
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="modelId"
        :options="[
          { label: '全部模型', value: '' },
          ...models.map((m) => ({ label: m.name, value: String(m.id) })),
        ]"
        class="w-40"
        placeholder="内容模型"
        @change="onSearch"
      />
      <Select
        v-model:value="status"
        :options="STATUS_OPTIONS"
        class="w-32"
        @change="onSearch"
      />
      <Input
        v-model:value="title"
        allow-clear
        class="w-56"
        placeholder="标题关键词"
        @press-enter="onSearch"
      />
      <Button type="primary" @click="onSearch">搜索</Button>

      <div
        v-if="hasAccessByCodes(['/api/system/audit/switch'])"
        class="ml-auto flex items-center gap-2"
      >
        <span class="text-sm text-gray-500">先审后发</span>
        <Switch
          v-model:checked="auditSwitch"
          size="small"
          @change="(c: any) => onToggleSwitch(Boolean(c))"
        />
      </div>
    </div>

    <div class="mb-3 flex flex-wrap items-center gap-4 text-sm">
      <span>
        待审核：
        <b :class="pendingCount > 0 ? 'text-orange-600' : 'text-gray-500'">
          {{ pendingCount }}
        </b>
        条
      </span>
      <span class="text-gray-400">
        全文检索索引 {{ indexedCount }} 条
        <Button :loading="indexRebuilding" size="small" type="link" @click="onRebuildIndex">
          {{ indexRebuilding ? '重建中…' : '重建索引' }}
        </Button>
      </span>
      <span class="text-xs text-gray-400">
        内容保存、审核通过、删除时自动增量索引；此处仅用于索引异常后手动全量重建
      </span>
    </div>

    <div v-if="canBatch" class="mb-3 flex flex-wrap items-center gap-2">
      <Button
        :disabled="!selectedKeys.length"
        size="small"
        type="primary"
        @click="onPass(selectedKeys)"
      >
        批量通过
      </Button>
      <Button
        :disabled="!selectedKeys.length"
        danger
        size="small"
        @click="openReject(selectedKeys)"
      >
        批量驳回
      </Button>
      <span v-if="selectedKeys.length" class="text-xs text-gray-400">
        已选 {{ selectedKeys.length }} 条
      </span>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      :row-selection="{
        selectedRowKeys: selectedKeys,
        onChange: (keys: any[]) => (selectedKeys = keys),
      }"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'title'">
          <a class="cursor-pointer" @click="openDetail(record)">{{ record.title }}</a>
        </template>
        <template v-else-if="column.key === 'model'">
          <Tag color="blue">{{ rowModelName(record) || rowModelId(record) }}</Tag>
        </template>
        <template v-else-if="column.key === 'author'">
          {{ authorName(record) }}
        </template>
        <template v-else-if="column.key === 'status'">
          <Tag :color="STATUS[record.status as 0]?.color ?? 'default'">
            {{ STATUS[record.status as 0]?.label ?? record.status }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ fmtTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="openDetail(record)"
          >
            查看
          </Button>
          <Button
            v-if="canAudit"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="onPass([String(record.id)])"
          >
            通过
          </Button>
          <Button
            v-if="canAudit"
            class="mr-1 px-2"
            danger
            size="small"
            type="link"
            @click="openReject([String(record.id)])"
          >
            驳回
          </Button>
          <Button size="small" type="link" @click="openLogs(record)">历史</Button>
        </template>
      </template>
    </Table>

    <div class="mt-3 flex justify-end">
      <Pagination
        v-model:current="page"
        :page-size="20"
        :show-total="(t: number) => `共 ${t} 条`"
        :total="total"
        size="small"
        @change="load"
      />
    </div>

    <!-- 驳回原因 -->
    <Modal
      v-model:open="rejectVisible"
      title="驳回原因（将存档，并可按需通知作者）"
      @ok="confirmReject"
    >
      <Textarea
        v-model:value="rejectReason"
        :rows="4"
        :maxlength="500"
        placeholder="请说明驳回理由，必填（最多 500 字）"
        show-count
      />
      <div class="mt-3 flex items-center gap-2">
        <Switch v-model:checked="rejectNotify" />
        <span class="text-sm">同时给作者发送站内信</span>
      </div>
      <div class="mt-1 text-xs text-gray-400">
        站内信会带上驳回原因，作者登录后在「个人中心 → 我的消息」查看。
        内容没有作者（后台代发）时自动跳过。
      </div>
    </Modal>

    <!-- 内容详情：审核前先看正文 -->
    <Drawer
      v-model:open="detailOpen"
      :title="`审核 — ${detailRow.title ?? ''}`"
      :width="720"
    >
      <div v-if="detailLoading" class="py-10 text-center text-gray-400">
        加载中…
      </div>
      <div v-else class="space-y-4">
        <div v-if="detail.thumbnail" class="flex justify-center">
          <img
            :src="String(detail.thumbnail)"
            alt=""
            class="max-h-48 rounded border object-cover"
          />
        </div>

        <div>
          <div class="mb-1 text-xs text-gray-400">
            正文（投稿内容，已按白名单清洗后渲染）
          </div>
          <!-- 投稿正文未经服务端清洗，直接 v-html 会把存储型 XSS 送进管理员会话 -->
          <div
            v-if="safeContent"
            class="prose max-w-none rounded border bg-gray-50 p-3 text-sm"
            v-html="safeContent"
          />
          <div v-else class="rounded border bg-gray-50 px-3 py-6 text-center text-gray-400">
            <Empty :image="undefined" description="该内容没有正文" />
          </div>
        </div>

        <div v-if="detailFields.length">
          <div class="mb-1 text-xs text-gray-400">其他字段</div>
          <Table
            :columns="[
              { title: '字段', dataIndex: 'label', key: 'label', width: 140 },
              { title: '值', dataIndex: 'value', key: 'value' },
            ]"
            :data-source="detailFields"
            :pagination="false"
            row-key="label"
            size="small"
          />
        </div>

        <div class="flex items-center gap-2 border-t pt-3">
          <Tag :color="STATUS[Number(detail.status) as 0]?.color ?? 'default'">
            {{ STATUS[Number(detail.status) as 0]?.label ?? '未知' }}
          </Tag>
          <span class="text-xs text-gray-400">
            提交于 {{ fmtTime(String(detail.createTime ?? detailRow.createTime ?? '')) }}
          </span>
          <div v-if="canAudit" class="ml-auto flex gap-2">
            <Button danger size="small" @click="rejectFromDetail">驳回</Button>
            <Button size="small" type="primary" @click="passFromDetail">
              通过并发布
            </Button>
          </div>
        </div>
      </div>
    </Drawer>

    <!-- 审核历史 -->
    <Drawer
      v-model:open="logOpen"
      :title="`审核历史 — ${logRow.title ?? ''}`"
      :width="560"
    >
      <div v-if="logLoading" class="py-10 text-center text-gray-400">加载中…</div>
      <div v-else-if="!logs.length" class="py-10 text-center text-gray-400">
        该内容还没有审核记录
      </div>
      <div v-else class="space-y-2">
        <div
          v-for="l in logs"
          :key="l.id"
          class="rounded border p-2"
          :class="l.action === 2 ? 'border-red-200 bg-red-50' : 'border-green-200 bg-green-50'"
        >
          <div class="flex items-center gap-2 text-sm">
            <Tag :color="l.action === 2 ? 'red' : 'green'">
              {{ l.action === 2 ? '驳回' : '通过' }}
            </Tag>
            <span class="text-gray-500">
              {{ l.auditorName || '系统' }} · {{ fmtTime(l.createTime) }}
            </span>
          </div>
          <div v-if="l.reason" class="mt-1 text-sm text-gray-700">
            原因：{{ l.reason }}
          </div>
        </div>
      </div>
    </Drawer>
  </Page>
</template>
