<script lang="ts" setup>
import type { GuideRow } from '#/api/core/guide';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Checkbox,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Table,
  Tag,
  TreeSelect,
} from 'ant-design-vue';

import {
  deleteGuideApi,
  getGuideTreeApi,
  saveGuideApi,
  statusGuideApi,
} from '#/api/core/guide';
import { getCategoryOptionsApi, getModelListApi } from '#/api/core/model';
import type { ChannelRow } from '#/api/core/channel';
import { getChannelTreeApi } from '#/api/core/channel';

defineOptions({ name: 'SystemGuide' });

const { hasAccessByCodes } = useAccess();
const canSave = hasAccessByCodes(['/api/system/guide/save']);
const canDelete = hasAccessByCodes(['/api/system/guide/delete']);
const canStatus = hasAccessByCodes(['/api/system/guide/status']);

const TYPE_LABEL: Record<number, string> = {
  0: '自定义链接',
  1: '栏目',
  2: '模型分类',
};

interface TreeNode {
  children?: TreeNode[];
  title: string;
  value: number | string;
}

// ---------------- 导航树 ----------------
const rows = ref<GuideRow[]>([]);
const loading = ref(false);

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '类型', key: 'type', width: 110 },
  { title: '链接', key: 'url', width: 280 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 240 },
];

async function load() {
  loading.value = true;
  try {
    rows.value = prune(((await getGuideTreeApi()) ?? []) as GuideRow[]);
  } finally {
    loading.value = false;
  }
}

/** 空子级置 undefined，避免表格渲染无意义的展开箭头 */
function prune(list: GuideRow[]): GuideRow[] {
  return list.map((g) => {
    const kids = g.children?.length ? prune(g.children) : undefined;
    return { ...g, children: kids };
  });
}

// ---------------- 新增/编辑 ----------------
const open = ref(false);
const saving = ref(false);
const form = reactive({
  fatherId: 0 as number | string,
  id: '',
  link: '',
  name: '',
  refId: '' as number | string,
  sort: 0,
  status: 1,
  target: '',
  type: 0,
});

const editOpen = computed(() => !!form.id);

/** 父项选择树：编辑时排除自身及其后代（后端还有防环兜底） */
const fatherOptions = computed<TreeNode[]>(() => {
  const excluded = new Set<string>();
  if (editOpen.value) {
    collectSelfAndDescendants(rows.value, form.id, excluded);
  }
  const build = (list: GuideRow[]): TreeNode[] =>
    list
      .filter((g) => !excluded.has(String(g.id)))
      .map((g) => {
        const kids = build(g.children ?? []);
        return {
          children: kids.length ? kids : undefined,
          title: g.name,
          value: g.id,
        };
      });
  return [{ title: '顶级', value: 0 }, ...build(rows.value)];
});

function collectSelfAndDescendants(
  list: GuideRow[],
  id: string,
  out: Set<string>,
): boolean {
  for (const g of list) {
    if (String(g.id) === id) {
      out.add(String(g.id));
      const mark = (kids: GuideRow[]) => {
        for (const c of kids) {
          out.add(String(c.id));
          mark(c.children ?? []);
        }
      };
      mark(g.children ?? []);
      return true;
    }
    if (collectSelfAndDescendants(g.children ?? [], id, out)) return true;
  }
  return false;
}

// 绑定数据源：栏目树 / 模型列表 / 选中模型的分类树
const channelTreeData = ref<TreeNode[]>([]);
const models = ref<{ code: string; name: string }[]>([]);
const categoryTreeData = ref<TreeNode[]>([]);
const selectedModelCode = ref('');

async function loadRefs() {
  try {
    const [channels, modelPage] = await Promise.all([
      getChannelTreeApi(),
      getModelListApi({ p: 1, rows: 100 }),
    ]);
    channelTreeData.value = toTreeSelectData(channels ?? []);
    models.value = (modelPage?.list ?? []).map((m) => ({
      code: m.code,
      name: m.name,
    }));
  } catch {
    channelTreeData.value = [];
  }
}

function toTreeSelectData(list: ChannelRow[]): TreeNode[] {
  return list.map((c) => ({
    children: c.children?.length
      ? toTreeSelectData(c.children)
      : undefined,
    title: c.channelName,
    value: c.id,
  }));
}

async function loadCategoryTree(modelCode: string) {
  categoryTreeData.value = [];
  if (!modelCode) return;
  const flat = (await getCategoryOptionsApi(modelCode)) ?? [];
  const byId = new Map<string, TreeNode>();
  const tree: TreeNode[] = [];
  for (const c of flat) {
    byId.set(String(c.id), { title: c.name, value: String(c.id) });
  }
  for (const c of flat) {
    const node = byId.get(String(c.id))!;
    const father = byId.get(String(c.fatherId));
    if (father) {
      father.children = father.children ?? [];
      father.children.push(node);
    } else {
      tree.push(node);
    }
  }
  categoryTreeData.value = tree;
}

async function onModelChange(code: string) {
  selectedModelCode.value = code;
  form.refId = '';
  await loadCategoryTree(code);
}

function openAdd(fatherId: number | string = 0) {
  Object.assign(form, {
    fatherId,
    id: '',
    link: '',
    name: '',
    refId: '',
    sort: 0,
    status: 1,
    target: '',
    type: 0,
  });
  selectedModelCode.value = '';
  categoryTreeData.value = [];
  open.value = true;
}

async function openEdit(row: GuideRow) {
  Object.assign(form, {
    fatherId: row.fatherId,
    id: String(row.id),
    link: row.link || '',
    name: row.name,
    refId: row.refId ?? '',
    sort: row.sort,
    status: row.status,
    target: row.target || '',
    type: row.type,
  });
  selectedModelCode.value = row.refModel || '';
  // 模型分类绑定：先加载该模型分类树再回显选中值
  if (row.type === 2 && row.refModel) {
    await loadCategoryTree(row.refModel);
  } else {
    categoryTreeData.value = [];
  }
  open.value = true;
}

async function onSave() {
  if (!form.name.trim()) {
    message.warning('导航名称不能为空');
    return;
  }
  if (form.type === 0 && !form.link.trim()) {
    message.warning('自定义链接不能为空');
    return;
  }
  if (form.type !== 0 && !form.refId) {
    message.warning(form.type === 1 ? '请选择绑定的栏目' : '请选择绑定的分类');
    return;
  }
  saving.value = true;
  try {
    await saveGuideApi({
      fatherId: form.fatherId,
      id: form.id || undefined,
      link: form.type === 0 ? form.link : '',
      name: form.name,
      refId: form.type === 0 ? undefined : form.refId,
      sort: form.sort,
      status: form.status,
      target: form.target ? '_blank' : '',
      type: form.type,
    });
    message.success(form.id ? '已更新' : '已添加');
    open.value = false;
    load();
  } finally {
    saving.value = false;
  }
}

function onToggleStatus(row: GuideRow) {
  statusGuideApi(String(row.id), row.status === 1 ? 0 : 1).then(() => {
    message.success(row.status === 1 ? '已隐藏' : '已显示');
    load();
  });
}

function onDelete(row: GuideRow) {
  Modal.confirm({
    content: `删除导航项「${row.name}」？有下级时需先删下级。`,
    onOk: async () => {
      await deleteGuideApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

onMounted(() => {
  load();
  loadRefs();
});
</script>

<template>
  <Page
    title="导航管理"
    description="站点导航（模板标签 fly_guide）：支持自定义链接、栏目、模型分类三种来源，url 由后端按来源自动计算；树形支持下级菜单。"
  >
    <div class="mb-3 flex items-center gap-2">
      <Button v-if="canSave" type="primary" @click="openAdd(0)">
        新增导航项
      </Button>
      <Button @click="load">刷新</Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      default-expand-all-rows
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <Tag
            :color="
              (record as GuideRow).type === 1
                ? 'blue'
                : (record as GuideRow).type === 2
                  ? 'purple'
                  : 'default'
            "
          >
            {{ TYPE_LABEL[(record as GuideRow).type] ?? '未知' }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'url'">
          <code class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-900">
            {{ (record as GuideRow).url || '-' }}
          </code>
        </template>
        <template v-else-if="column.key === 'status'">
          <Tag :color="(record as GuideRow).status === 1 ? 'green' : 'default'">
            {{ (record as GuideRow).status === 1 ? '显示' : '隐藏' }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="canSave"
            size="small"
            type="link"
            @click="openAdd((record as GuideRow).id)"
          >
            加下级
          </Button>
          <Button
            v-if="canSave"
            size="small"
            type="link"
            @click="openEdit(record as GuideRow)"
          >
            编辑
          </Button>
          <Button
            v-if="canStatus"
            size="small"
            type="link"
            @click="onToggleStatus(record as GuideRow)"
          >
            {{ (record as GuideRow).status === 1 ? '隐藏' : '显示' }}
          </Button>
          <Button
            v-if="canDelete"
            danger
            size="small"
            type="link"
            @click="onDelete(record as GuideRow)"
          >
            删除
          </Button>
        </template>
      </template>
    </Table>

    <!-- 新增/编辑 -->
    <Modal
      v-model:open="open"
      :confirm-loading="saving"
      :title="form.id ? '编辑导航项' : '新增导航项'"
      @ok="onSave"
    >
      <Form layout="vertical" class="pt-2">
        <FormItem label="名称" required>
          <Input v-model:value="form.name" placeholder="导航显示文字" />
        </FormItem>
        <FormItem label="上级导航项">
          <TreeSelect
            v-model:value="form.fatherId"
            :dropdown-style="{ maxHeight: '320px', overflow: 'auto' }"
            :tree-data="fatherOptions"
            placeholder="顶级"
            tree-default-expand-all
            tree-node-filter-prop="title"
          />
        </FormItem>
        <FormItem label="类型">
          <Select
            v-model:value="form.type"
            :options="[
              { label: '自定义链接', value: 0 },
              { label: '栏目', value: 1 },
              { label: '模型分类', value: 2 },
            ]"
            @change="
              () => {
                form.refId = '';
              }
            "
          />
        </FormItem>

        <FormItem v-if="form.type === 0" label="链接地址" required>
          <Input v-model:value="form.link" placeholder="如 /about/ 或 https://…" />
        </FormItem>
        <FormItem v-else-if="form.type === 1" label="绑定栏目" required>
          <TreeSelect
            v-model:value="form.refId"
            :dropdown-style="{ maxHeight: '320px', overflow: 'auto' }"
            :tree-data="channelTreeData"
            placeholder="选择栏目"
            tree-default-expand-all
            tree-node-filter-prop="title"
          />
        </FormItem>
        <template v-else>
          <FormItem label="绑定模型" required>
            <Select
              :options="models.map((m) => ({ label: m.name, value: m.code }))"
              :value="selectedModelCode || undefined"
              placeholder="选择模型"
              show-search
              option-filter-prop="label"
              @change="(v: unknown) => onModelChange(String(v ?? ''))"
            />
          </FormItem>
          <FormItem label="绑定分类" required>
            <TreeSelect
              v-model:value="form.refId"
              :dropdown-style="{ maxHeight: '320px', overflow: 'auto' }"
              :tree-data="categoryTreeData"
              :placeholder="categoryTreeData.length ? '选择分类' : '请先选择模型'"
              tree-default-expand-all
              tree-node-filter-prop="title"
            />
          </FormItem>
        </template>

        <FormItem label="打开方式">
          <Checkbox
            :checked="form.target === '_blank'"
            @update:checked="(v: boolean) => (form.target = v ? '_blank' : '')"
          >
            新窗口打开
          </Checkbox>
        </FormItem>
        <FormItem label="排序">
          <InputNumber v-model:value="form.sort" class="w-full" />
        </FormItem>
        <FormItem label="状态">
          <Tag :color="form.status === 1 ? 'green' : 'default'">
            {{ form.status === 1 ? '显示' : '隐藏' }}
          </Tag>
          <Button size="small" @click="form.status = form.status === 1 ? 0 : 1">
            {{ form.status === 1 ? '点击隐藏' : '点击显示' }}
          </Button>
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
