<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import type { ModelRow } from '#/api/core/model';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import { Button, message, Modal } from 'ant-design-vue';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteModelApi,
  exportModelApi,
  getModelListApi,
  importModelApi,
  regenTemplatesApi,
} from '#/api/core/model';

import CategoryDrawer from './category-drawer.vue';
import EditModal from './edit-modal.vue';

defineOptions({ name: 'SystemModel' });

const { hasAccessByCodes } = useAccess();
const router = useRouter();

const [EditModalComp, editModalApi] = useEditDrawer({
  connectedComponent: EditModal,
  destroyOnClose: true,
});

const gridOptions: VxeTableGridOptions<ModelRow> = {
  columns: [
    { title: '序号', type: 'seq', width: 60 },
    { field: 'name', title: '模型名称' },
    { field: 'code', title: '标识' },
    { field: 'titleLabel', title: '标题字段名' },
    { field: 'sort', title: '排序', width: 80 },
    {
      field: 'isSystem',
      formatter: ({ cellValue }) => (cellValue === 1 ? '内置' : '自定义'),
      title: '类型',
      width: 90,
    },
    {
      field: 'status',
      formatter: ({ cellValue }) => (cellValue === 1 ? '启用' : '禁用'),
      title: '状态',
      width: 80,
    },
    { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 320 },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async ({ page }) => {
        const res = await getModelListApi({ p: page.currentPage });
        return { items: res.list ?? [], total: res.count ?? 0 };
      },
    },
  },
  toolbarConfig: {
    refresh: true,
    zoom: true,
  },
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions });

function openAdd() {
  editModalApi.setData({ onSaved: () => gridApi.query() }).open();
}

function openEdit(row: ModelRow) {
  editModalApi.setData({ onSaved: () => gridApi.query(), record: row }).open();
}

function openFields(row: ModelRow) {
  router.push('/system/model/field/' + row.id);
}

function openData(row: ModelRow) {
  router.push('/modelData/' + row.code);
}

// /////////// P9 分类树管理 / E10 重新生成骨架 ///////////

const categoryDrawerVisible = ref(false);
const categoryModel = ref<null | ModelRow>(null);

const categoryDrawerSeq = ref(0);

function openCategories(row: ModelRow) {
  categoryModel.value = row;
  categoryDrawerVisible.value = true;
  categoryDrawerSeq.value += 1;
}

function onRegenTemplates(row: ModelRow) {
  Modal.confirm({
    content: `重新生成「${row.name}」的默认模板（list.html / detail.html）？将覆盖当前主题下的这两份文件（已手工定制的内容会丢失，建议先备份）。`,
    okText: '重新生成',
    onOk: async () => {
      await regenTemplatesApi(row.id);
      message.success('骨架已重新生成，含最新字段速查注释');
    },
    title: '重新生成骨架',
  });
}

// /////////// G20 模型导入导出 ///////////

const fileInput = ref<HTMLInputElement>();

async function onExport(row: ModelRow) {
  const def = await exportModelApi(row.id);
  const blob = new Blob([JSON.stringify(def, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${row.code}-model.json`;
  a.click();
  URL.revokeObjectURL(url);
  message.success(`已导出 ${row.code}-model.json（模型+字段+分类定义，可入 git / 跨环境导入）`);
}

function pickImportFile() {
  fileInput.value?.click();
}

async function onImportFile(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  const text = await file.text();
  let parsed: any;
  try {
    parsed = JSON.parse(text);
  } catch {
    message.error('不是合法的 JSON 文件');
    return;
  }
  const code = parsed?.model?.code ?? '?';
  Modal.confirm({
    content: `导入模型「${parsed?.model?.name ?? code}」？已存在同 code 模型时只更新元数据与字段（内容数据不受影响）；缺失字段会新建。`,
    okText: '导入',
    onOk: async () => {
      const res = await importModelApi(text);
      const r = res as any;
      message.success(
        `导入完成：模型${r.modelCreated ? '新建' : '更新'}，字段 +${r.fieldAdded}/更新 ${r.fieldUpdated}/失败 ${r.fieldFailed}，分类 +${r.categoryAdded}` +
          (r.errors?.length ? `；失败详情：${r.errors.join('；')}` : ''),
        6,
      );
      gridApi.query();
    },
    title: '导入模型定义',
  });
}

function onDelete(row: ModelRow) {
  Modal.confirm({
    content: `删除模型「${row.name}」将级联删除其数据表与全部字段定义，且不可恢复！`,
    onOk: async () => {
      await deleteModelApi(row.id);
      message.success('已删除');
      gridApi.query();
    },
    title: '删除确认',
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="内容模型">
      <template #toolbar-tools>
        <Button
          v-if="hasAccessByCodes(['/api/system/model/save'])"
          class="mr-2"
          type="primary"
          @click="openAdd"
        >
          新增模型
        </Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/model/save'])"
          class="mr-2"
          @click="pickImportFile"
        >
          导入模型
        </Button>
        <input
          ref="fileInput"
          accept=".json,application/json"
          class="hidden"
          type="file"
          @change="onImportFile"
        />
      </template>
      <template #action="{ row }">
        <Button class="mr-2 px-2" size="small" type="link" @click="openFields(row)">
          字段管理
        </Button>
        <Button class="mr-2 px-2" size="small" type="link" @click="openData(row)">
          内容管理
        </Button>
        <Button class="mr-2 px-2" size="small" type="link" @click="onExport(row)">
          导出
        </Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/modelCategory/save'])"
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="openCategories(row)"
        >
          分类
        </Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/model/update'])"
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="onRegenTemplates(row)"
        >
          重新生成模板
        </Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/model/update'])"
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/model/del']) && row.isSystem !== 1"
          class="px-2"
          danger
          size="small"
          type="link"
          @click="onDelete(row)"
        >
          删除
        </Button>
      </template>
    </Grid>
    <EditModalComp />
    <CategoryDrawer
      v-if="categoryDrawerVisible && categoryModel"
      :key="`${categoryModel.id}-${categoryDrawerSeq}`"
      :model-id="categoryModel.id"
      :model-name="categoryModel.name"
    />
  </Page>
</template>
