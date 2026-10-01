<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { Page } from '@vben/common-ui';
import { useAccessStore } from '@vben/stores';

import {
  Button,
  Drawer,
  Input,
  InputNumber,
  message,
  Modal,
  Pagination,
  Select,
  Table,
  Tag,
} from 'ant-design-vue';

import { getDictDataByTypeApi } from '#/api/core/dict';
import {
  type ContentVersionRow,
  deleteDataApi,
  type FormMeta,
  getDataListApi,
  getFormMetaApi,
  getModelByCodeApi,
  getPreviewTokenApi,
  getVersionDiffApi,
  getVersionListApi,
  type ModelFieldRow,
  type ModelRow,
  restoreVersionApi,
  updateDataStatusApi,
} from '#/api/core/model';
import { contentNoun } from '#/api/core/model';
import { useEditDrawer } from '#/utils/edit-drawer';

import FormModal from './form-modal.vue';

defineOptions({ name: 'SystemModelData' });

const route = useRoute();

/**
 * 通配权限码匹配（与后端 CheckUrlUtils 一致）：
 * 角色组绑定的可能是 /api/system/modelData/* 这类通配 action_key，
 * 前端精确匹配会漏判，这里做尾星号前缀匹配。
 */
function matchCode(codes: string[], need: string): boolean {
  return codes.some(
    (c) => c === need || (c.endsWith('*') && need.startsWith(c.slice(0, -1))),
  );
}

const accessStore = useAccessStore();
/** 按模型粒度的增删改查按钮权限（与后端锚点同源：add/edit/del@{modelId}） */
const canAdd = computed(() =>
  matchCode(accessStore.accessCodes, `/api/system/modelData/add@${model.value?.id ?? 0}`),
);
const canEdit = computed(() =>
  matchCode(accessStore.accessCodes, `/api/system/modelData/edit@${model.value?.id ?? 0}`),
);
const canDel = computed(() =>
  matchCode(accessStore.accessCodes, `/api/system/modelData/del@${model.value?.id ?? 0}`),
);

const model = ref<ModelRow | null>(null);
const fields = ref<ModelFieldRow[]>([]);
const categories = ref<{ id: number; name: string }[]>([]);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);

const title = ref('');
const status = ref<string>('');
const filterValues = ref<Record<string, any>>({});
const filterFields = computed(() => fields.value.filter((f) => f.isFilter === 1));
const listFields = computed(() => fields.value.filter((f) => f.isList === 1));

const STATUS_TEXT: Record<number, string> = {
  0: '待审核',
  1: '已发布',
  2: '未通过',
  3: '已删除',
  4: '待发布',
};

const [FormModalComp, formModalApi] = useEditDrawer({
  connectedComponent: FormModal,
  destroyOnClose: true,
});

function modelId() {
  return model.value?.id ?? 0;
}

const columns = computed(() => {
  const cols: Record<string, any>[] = [
    { title: '标题', dataIndex: 'title', key: 'title' },
  ];
  for (const f of listFields.value) {
    cols.push({ title: f.fieldLabel, dataIndex: f.fieldName, key: f.fieldName });
  }
  cols.push(
    { title: '浏览', dataIndex: 'countView', key: 'countView', width: 80 },
    { title: '状态', dataIndex: 'statusText', key: 'statusText', width: 90 },
    { title: '创建时间', dataIndex: 'createTimeText', key: 'createTimeText', width: 160 },
    { title: '操作', key: 'action', width: 200 },
  );
  return cols;
});

function parseOptions(optionsJson?: string) {
  if (!optionsJson) return [];
  try {
    return JSON.parse(optionsJson).map((o: any) =>
      typeof o === 'string' ? { label: o, value: o } : o,
    );
  } catch {
    return optionsJson.split(',').map((s) => ({ label: s, value: s }));
  }
}

// /////////// 字典绑定字段（若依式 dictType）：筛选候选项与值→标签显示 ///////////

const dictOptionsMap = ref<Record<string, { label: string; value: string }[]>>({});

async function loadDictOptions() {
  const keys = new Set<string>();
  for (const f of fields.value) {
    if (f.dictType) keys.add(f.dictType);
  }
  for (const key of keys) {
    if (dictOptionsMap.value[key]) continue;
    try {
      const rows = (await getDictDataByTypeApi(key)) ?? [];
      dictOptionsMap.value[key] = rows.map((r) => ({
        label: r.dictLabel,
        value: r.dictValue,
      }));
    } catch {
      dictOptionsMap.value[key] = [];
    }
  }
}

/** 筛选下拉候选项：绑了字典用字典（字典无数据时回退），否则字段自带 options */
function fieldOptions(f: ModelFieldRow) {
  return f.dictType && dictOptionsMap.value[f.dictType ?? '']?.length
    ? dictOptionsMap.value[f.dictType ?? '']
    : parseOptions(f.options);
}

function cellText(record: Record<string, any>, column: any): string {
  const key = column?.key as string;
  // E1 关联引用：优先显示展开后的目标内容标题，而非裸 id
  const obj = key ? record[`${key}Obj`] : undefined;
  if (obj && typeof obj === 'object') {
    // E1 关联显示目标标题；B1 绑定实体显示昵称/分类名（用户/分类没有 title）
    const o = obj as Record<string, any>;
    return String(o.title ?? o.nickName ?? o.name ?? o.userName ?? '');
  }
  const list = key ? record[`${key}List`] : undefined;
  if (Array.isArray(list)) {
    return list
      .map((r: any) => String(r?.title ?? ''))
      .filter(Boolean)
      .join('、');
  }
  const v = key ? record[key] : undefined;
  if (v === undefined || v === null) return '-';
  if (typeof v === 'object') {
    return Array.isArray(v) ? v.join(',') : JSON.stringify(v);
  }
  // 字典绑定字段：值→标签显示（checkbox 为 JSON 数组串，逐项映射）
  const f = listFields.value.find((x) => x.fieldName === key);
  if (f?.dictType) {
    const opts = dictOptionsMap.value[f.dictType] ?? [];
    if (opts.length > 0) {
      const toLabel = (val: any) =>
        opts.find((o) => String(o.value) === String(val))?.label ?? String(val);
      let arr: any[] = [v];
      if (typeof v === 'string' && v.trim().startsWith('[')) {
        try {
          arr = JSON.parse(v);
        } catch {
          arr = [v];
        }
      }
      return arr.map(toLabel).join(',');
    }
  }
  return String(v);
}

function normalize(row: Record<string, any>): Record<string, any> {
  const out: Record<string, any> = { ...row };
  for (const [key, value] of Object.entries(row)) {
    if (key.includes('_')) {
      const camel = key
        .split('_')
        .map((p, i) => (i === 0 ? p : p.charAt(0).toUpperCase() + p.slice(1)))
        .join('');
      out[camel] = value;
    }
  }
  out.statusText = STATUS_TEXT[Number(row.status)] ?? String(row.status);
  out.createTimeText = (row.createTime == null
    ? ''
    : String(row.createTime)
  )
    .replace('T', ' ')
    .slice(0, 16);
  return out;
}

async function load() {
  if (!model.value) return;
  loading.value = true;
  try {
    const params: Record<string, any> = { p: page.value };
    if (title.value) params.title = title.value;
    if (status.value !== '') params.status = status.value;
    for (const [k, v] of Object.entries(filterValues.value)) {
      if (v !== undefined && v !== null && v !== '') params[k] = String(v);
    }
    const res = await getDataListApi(modelId(), params);
    rows.value = (res.list ?? []).map((r) => normalize(r));
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  title.value = '';
  status.value = '';
  filterValues.value = {};
  page.value = 1;
  load();
}

function openAdd() {
  formModalApi
    .setData({
      categories: categories.value,
      fields: fields.value,
      model: model.value,
      onSaved: () => load(),
    })
    .open();
}

function openEdit(row: Record<string, any>) {
  formModalApi
    .setData({
      categories: categories.value,
      fields: fields.value,
      model: model.value,
      onSaved: () => load(),
      record: row,
    })
    .open();
}

// /////////// G16 草稿预览 ///////////

async function openPreview(row: Record<string, any>) {
  try {
    const res = await getPreviewTokenApi(modelId(), String(row.id));
    window.open(res.url, '_blank');
  } catch {
    message.error('预览令牌签发失败');
  }
}

// /////////// G12 内容版本 ///////////

const versionVisible = ref(false);
const versionRow = ref<null | Record<string, any>>(null);
const versionLoading = ref(false);
const versions = ref<ContentVersionRow[]>([]);
const versionTotal = ref(0);
const versionPage = ref(1);
/** 版本恢复确认 */
const restoreTarget = ref<ContentVersionRow | null>(null);

function openVersions(row: Record<string, any>) {
  versionRow.value = row;
  versionVisible.value = true;
  versionPage.value = 1;
  loadVersions();
}

async function loadVersions() {
  if (!versionRow.value) return;
  versionLoading.value = true;
  try {
    const res = await getVersionListApi(modelId(), String(versionRow.value.id), {
      p: versionPage.value,
      rows: 10,
    });
    versions.value = res.list ?? [];
    versionTotal.value = res.count ?? 0;
  } finally {
    versionLoading.value = false;
  }
}

const diffRows = ref<{ field: string; from: string; to: string }[]>([]);
const diffVisible = ref(false);

/** 与紧邻的旧版本对比（v1 无上一版时按钮禁用） */
async function openDiff(v: ContentVersionRow) {
  const res = await getVersionDiffApi(
    modelId(),
    String(versionRow.value?.id),
    v.version - 1,
    v.version,
  );
  diffRows.value = res ?? [];
  diffVisible.value = true;
}

function confirmRestore(v: ContentVersionRow) {
  restoreTarget.value = v;
  Modal.confirm({
    content: `将内容恢复到 v${v.version}（${v.createTime ?? ''}）？恢复动作会另存为新版本，历史不丢失。`,
    okText: '恢复',
    onOk: async () => {
      await restoreVersionApi(modelId(), String(versionRow.value?.id), v.version);
      message.success(`已恢复到 v${v.version}`);
      restoreTarget.value = null;
      await loadVersions();
      load();
    },
    title: '版本恢复确认',
  });
}

function onDelete(row: Record<string, any>) {
  Modal.confirm({
    content: `删除「${row.title}」？`,
    onOk: async () => {
      await deleteDataApi(modelId(), String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

async function toggleStatus(row: Record<string, any>) {
  const next = Number(row.status) === 1 ? 0 : 1;
  await updateDataStatusApi(modelId(), String(row.id), next);
  message.success(next === 1 ? '已发布' : '已下架');
  load();
}

onMounted(async () => {
  // 路由 /modelData/{code}（后端动态菜单生成），code 取路径末段
  const code = route.path.split('/').pop() || '';
  model.value = (await getModelByCodeApi(code)) as unknown as ModelRow;
  const meta: FormMeta = await getFormMetaApi(model.value.id);
  fields.value = meta.fields ?? [];
  categories.value = meta.categories ?? [];
  loadDictOptions();
  await load();
});
</script>

<template>
  <Page>
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Input
        v-model:value="title"
        class="w-[200px]"
        :placeholder="`${model?.titleLabel || '标题'}关键词`"
        @press-enter="
          () => {
            page = 1;
            load();
          }
        "
      />
      <template v-for="f in filterFields" :key="f.fieldName">
        <Select
          v-if="f.fieldType === 'select' || f.fieldType === 'radio'"
          v-model:value="filterValues[f.fieldName]"
          allow-clear
          class="w-[160px]"
          :options="fieldOptions(f)"
          :placeholder="f.fieldLabel"
        />
        <InputNumber
          v-else-if="f.fieldType === 'number' || f.fieldType === 'decimal'"
          v-model:value="filterValues[f.fieldName]"
          class="w-[140px]"
          :placeholder="f.fieldLabel"
        />
        <Input
          v-else
          v-model:value="filterValues[f.fieldName]"
          class="w-[160px]"
          :placeholder="f.fieldLabel"
        />
      </template>
      <Select
        v-model:value="status"
        allow-clear
        class="w-[120px]"
        :options="[
          { label: '待审核', value: '0' },
          { label: '已发布', value: '1' },
          { label: '未通过', value: '2' },
          { label: '待发布', value: '4' },
        ]"
        placeholder="状态"
      />
      <Button
        type="primary"
        @click="
          () => {
            page = 1;
            load();
          }
        "
      >
        查询
      </Button>
      <Button @click="resetFilters">重置</Button>
      <Button
        v-if="canAdd && model?.adminCreate !== 0"
        class="ml-auto"
        type="primary"
        @click="openAdd"
      >
        添加{{ contentNoun(model?.titleLabel) }}
      </Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'title'">
          <a class="cursor-pointer" @click="openEdit(record)">
            {{ record.title }}
          </a>
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="canEdit"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="openEdit(record)"
          >
            编辑
          </Button>
          <Button
            v-if="canEdit"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="openPreview(record)"
          >
            预览
          </Button>
          <Button
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="openVersions(record)"
          >
            版本
          </Button>
          <Button
            v-if="canEdit"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="toggleStatus(record)"
          >
            {{ Number(record.status) === 1 ? '下架' : '发布' }}
          </Button>
          <Button
            v-if="canDel"
            class="px-2"
            danger
            size="small"
            type="link"
            @click="onDelete(record)"
          >
            删除
          </Button>
        </template>
        <template v-else>
          {{ cellText(record, column) }}
        </template>
      </template>
    </Table>

    <!-- G12 内容版本抽屉 -->
    <Drawer
      v-model:open="versionVisible"
      :title="`内容版本 - ${versionRow?.title ?? ''}`"
      :width="520"
    >
      <Table
        :columns="[
          { title: '版本', dataIndex: 'version', key: 'version', width: 70 },
          { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
          { title: '备注', dataIndex: 'remark', key: 'remark' },
          { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
          { title: '操作', key: 'vaction', width: 80 },
        ]"
        :data-source="versions"
        :loading="versionLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'version'">
            <Tag color="blue">v{{ record.version }}</Tag>
          </template>
          <template v-else-if="column.key === 'status'">
            {{ Number(record.status) === 1 ? '发布' : Number(record.status) === 0 ? '待审' : '其他' }}
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ (record.createTime ?? '').replace('T', ' ').slice(0, 16) }}
          </template>
          <template v-else-if="column.key === 'vaction'">
            <Button
              :disabled="record.version <= 1"
              size="small"
              type="link"
              @click="openDiff(record as ContentVersionRow)"
            >
              对比
            </Button>
            <Button
              :disabled="restoreTarget !== null"
              size="small"
              type="link"
              @click="confirmRestore(record as ContentVersionRow)"
            >
              恢复
            </Button>
          </template>
        </template>
      </Table>
      <Modal
        v-model:open="diffVisible"
        :footer="null"
        title="版本差异（上一版 → 本版）"
        :width="560"
      >
        <Table
          :columns="[
            { title: '字段', dataIndex: 'field', key: 'field', width: 140 },
            { title: '旧值', dataIndex: 'from', key: 'from' },
            { title: '新值', dataIndex: 'to', key: 'to' },
          ]"
          :data-source="diffRows"
          :pagination="false"
          row-key="field"
          size="small"
        />
      </Modal>
      <div class="mt-3 flex justify-end">
        <Pagination
          v-model:current="versionPage"
          :page-size="10"
          :show-total="(t: number) => `共 ${t} 版`"
          :total="versionTotal"
          size="small"
          @change="loadVersions"
        />
      </div>
    </Drawer>
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
    <FormModalComp />
  </Page>
</template>
