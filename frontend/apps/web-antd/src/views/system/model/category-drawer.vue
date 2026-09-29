<script lang="ts" setup>
/**
 * P9 模型分类树管理抽屉：新增（根/子级）/改名 / 排序 / 删除。
 * 数据源 /api/system/modelCategory/tree/{modelId}，写走 save（传 id 即更新）/ del。
 */
import type { ModelCategoryRow } from '#/api/core/model';

import { ref } from 'vue';

import { Button, Drawer, Input, InputNumber, message, Modal, Tree } from 'ant-design-vue';

import {
  deleteCategoryApi,
  getCategoryTreeApi,
  saveCategoryApi,
} from '#/api/core/model';

const props = defineProps<{
  modelId: number | string;
  modelName?: string;
}>();

const open = ref(true);
const loading = ref(false);
const treeData = ref<any[]>([]);
const expandedKeys = ref<number[]>([]);

/** 编辑态：'' = 关闭；'new:0' 新根 / 'new:{fatherId}' 新子级 / '{id}' 改名 */
const editing = ref('');
const editName = ref('');
const editSort = ref<number>();

async function load() {
  loading.value = true;
  try {
    const list = (await getCategoryTreeApi(Number(props.modelId))) ?? [];
    treeData.value = build(list);
  } finally {
    loading.value = false;
  }
}

function build(
  rows: ModelCategoryRow[],
): any[] {
  const nodes = new Map<number, any>();
  for (const r of rows) {
    nodes.set(Number(r.id), {
      id: Number(r.id),
      fatherId: Number(r.fatherId ?? 0),
      name: r.name,
      sort: Number(r.sort ?? 0),
      children: [],
    });
  }
  const roots: any[] = [];
  for (const r of rows) {
    const node = nodes.get(Number(r.id));
    const parent = nodes.get(Number(r.fatherId ?? 0));
    if (r.fatherId && parent && Number(r.fatherId) !== Number(r.id)) {
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  }
  sortNodes(roots);
  expandedKeys.value = collectIds(roots);
  return roots;
}

function sortNodes(nodes: any[]) {
  nodes.sort((a, b) => a.sort - b.sort || a.id - b.id);
  for (const n of nodes) sortNodes(n.children);
}

function collectIds(nodes: any[]): number[] {
  const out: number[] = [];
  const walk = (list: any[]) => {
    for (const n of list) {
      out.push(n.id);
      walk(n.children);
    }
  };
  walk(nodes);
  return out;
}

function startAdd(fatherId: number) {
  editing.value = `new:${fatherId}`;
  editName.value = '';
  editSort.value = 0;
}

function startRename(node: any) {
  editing.value = String(node.id);
  editName.value = node.name;
  editSort.value = node.sort;
}

async function saveEdit() {
  const name = editName.value.trim();
  if (!name) {
    message.warning('分类名称不能为空');
    return;
  }
  const [mode, arg] = editing.value.split(':');
  if (mode === 'new') {
    await saveCategoryApi({
      fatherId: arg || 0,
      modelId: props.modelId,
      name,
      sort: editSort.value ?? 0,
    });
    message.success('已新增');
  } else {
    await saveCategoryApi({
      id: editing.value,
      name,
      sort: editSort.value ?? 0,
    });
    message.success('已保存');
  }
  editing.value = '';
  await load();
}

function onDelete(node: any) {
  Modal.confirm({
    content: `删除分类「${node.name}」？其子分类将一并删除（内容不受影响，仅归到未分类）。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteCategoryApi(node.id);
      message.success('已删除');
      await load();
    },
    title: '删除确认',
  });
}

function cancelEdit() {
  editing.value = '';
}

defineExpose({ open: () => open.value = true, reload: load });

load();
</script>

<template>
  <Drawer
    v-model:open="open"
    :title="`分类树 - ${props.modelName ?? ''}`"
    :width="480"
  >
    <div class="mb-2">
      <Button size="small" type="primary" @click="startAdd(0)">+ 新增根分类</Button>
    </div>
    <div v-if="editing === 'new:0'" class="mb-3 flex items-center gap-2">
      <Input v-model:value="editName" class="flex-1" placeholder="分类名称" size="small" />
      <InputNumber v-model:value="editSort" :precision="0" class="w-20" placeholder="排序" size="small" />
      <Button size="small" type="primary" @click="saveEdit">保存</Button>
      <Button size="small" @click="cancelEdit">取消</Button>
    </div>

    <Tree
      v-if="treeData.length > 0"
      :expanded-keys="expandedKeys"
      :tree-data="treeData"
      block-node
    >
      <template #title="{ dataRef }">
        <div class="flex min-h-7 flex-1 items-center justify-between gap-2">
          <span>{{ dataRef.name }} <span class="text-xs text-gray-300">#{{ dataRef.sort }}</span></span>
          <span class="shrink-0">
            <template v-if="editing === String(dataRef.id)">
              <Input v-model:value="editName" class="mr-1 w-32" size="small" />
              排序
              <InputNumber v-model:value="editSort" :precision="0" class="mx-1 w-16" size="small" />
              <Button class="mr-1" size="small" type="link" @click="saveEdit">存</Button>
              <Button size="small" type="link" @click="cancelEdit">取消</Button>
            </template>
            <template v-else>
              <Button class="px-1" size="small" type="link" @click="startAdd(dataRef.id)">+子级</Button>
              <Button class="px-1" size="small" type="link" @click="startRename(dataRef)">改名</Button>
              <Button class="px-1" danger size="small" type="link" @click="onDelete(dataRef)">删除</Button>
            </template>
          </span>
        </div>
        <div
          v-if="editing === `new:${dataRef.id}`"
          class="mb-1 ml-4 flex items-center gap-2"
        >
          <Input v-model:value="editName" class="flex-1" placeholder="子分类名称" size="small" />
          <InputNumber v-model:value="editSort" :precision="0" class="w-20" placeholder="排序" size="small" />
          <Button size="small" type="primary" @click="saveEdit">保存</Button>
          <Button size="small" @click="cancelEdit">取消</Button>
        </div>
      </template>
    </Tree>
    <div v-else-if="!loading" class="py-8 text-center text-gray-400">
      还没有分类，点上方按钮新增
    </div>
  </Drawer>
</template>
