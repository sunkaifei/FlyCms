<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import type { FormMeta, ModelFieldRow, ModelRow } from '#/api/core/model';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import { Button, Input, message, Modal } from 'ant-design-vue';
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteFieldApi,
  getFieldListApi,
  getFormMetaApi,
  pickupComponentApi,
} from '#/api/core/model';

import ComponentDrawer from './component-drawer.vue';
import FieldModal from './field-modal.vue';
import FormPreviewModal from '../modeldata/form-modal.vue';

defineOptions({ name: 'SystemModelField' });

const { hasAccessByCodes } = useAccess();
const route = useRoute();
const router = useRouter();
// 雪花 ID 必须保持字符串（Number 化会丢末位精度，导致字段列表查询落空）
const modelId = route.params.modelId as string;

const [FieldModalComp, fieldModalApi] = useEditDrawer({
  connectedComponent: FieldModal,
  destroyOnClose: true,
});

// P1：建模后即时预览录入界面（PHPCMS 式「预览表单」，复用动态内容表单只读渲染）
const [FormPreviewComp, formPreviewApi] = useEditDrawer({
  connectedComponent: FormPreviewModal,
  destroyOnClose: true,
  footer: false,
});

const gridOptions: VxeTableGridOptions<ModelFieldRow> = {
  columns: [
    { title: '序号', type: 'seq', width: 60 },
    { field: 'fieldName', title: '字段名' },
    { field: 'fieldLabel', title: '显示名' },
    { field: 'fieldType', title: '类型', width: 100 },
    { field: 'columnType', title: '列定义' },
    { field: 'tabName', title: '表单选项卡', width: 100 },
    {
      field: 'isRequired',
      formatter: ({ cellValue }) => (cellValue === 1 ? '是' : '否'),
      title: '必填',
      width: 70,
    },
    {
      field: 'isList',
      formatter: ({ cellValue }) => (cellValue === 1 ? '是' : '否'),
      title: '列表显示',
      width: 90,
    },
    {
      field: 'isFilter',
      formatter: ({ cellValue }) => (cellValue === 1 ? '是' : '否'),
      title: '筛选',
      width: 70,
    },
    { field: 'sort', title: '排序', width: 70 },
    { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 190 },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async () => {
        const list = (await getFieldListApi(modelId)) ?? [];
        return { items: list, total: list.length };
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
  fieldModalApi
    .setData({ modelId, onSaved: () => gridApi.query() })
    .open();
}

function openAddChild(row: ModelFieldRow) {
  fieldModalApi
    .setData({
      modelId,
      onSaved: () => gridApi.query(),
      parentId: row.id,
      parentLabel: row.fieldLabel,
    })
    .open();
}

function openEdit(row: ModelFieldRow) {
  fieldModalApi
    .setData({ modelId, onSaved: () => gridApi.query(), record: row })
    .open();
}

async function openPreview() {
  try {
    const meta: FormMeta = await getFormMetaApi(modelId);
    formPreviewApi
      .setData({
        categories: meta.categories ?? [],
        fields: meta.fields ?? [],
        model: meta.model as unknown as ModelRow,
        preview: true,
      })
      .open();
  } catch {
    message.error('加载表单元数据失败');
  }
}

// /////////// G15 字段组库 ///////////

const componentVisible = ref(false);
const componentSeq = ref(0);
/** 存为字段组：行内弹出的命名表单 */
const pickupRow = ref<null | ModelFieldRow>(null);
const pickupCode = ref('');
const pickupName = ref('');
const pickupRemark = ref('');

function openComponents() {
  componentVisible.value = true;
  componentSeq.value += 1;
}

const pickupOpen = ref(false);
function startPickup(row: ModelFieldRow) {
  pickupRow.value = row;
  pickupCode.value = '';
  pickupName.value = `${row.fieldLabel}字段组`;
  pickupRemark.value = '';
  pickupOpen.value = true;
}

async function savePickup() {
  if (!pickupRow.value) return;
  const code = pickupCode.value.trim();
  if (!code) {
    message.warning('字段组标识不能为空（将作为应用时的 group 字段名）');
    return;
  }
  await pickupComponentApi({
    code,
    fieldId: pickupRow.value.id,
    modelId,
    name: pickupName.value.trim() || code,
    remark: pickupRemark.value.trim(),
  });
  message.success('字段组已入库，可在任意模型的「字段组库」中应用');
  pickupRow.value = null;
  pickupOpen.value = false;
}

async function onDelete(row: ModelFieldRow) {
  await deleteFieldApi(row.id);
  message.success('字段已删除，数据列已移除');
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="模型字段（字段名即数据表列名，创建后不可改）">
      <template #toolbar-tools>
        <Button class="mr-2" @click="openPreview">预览表单</Button>
        <Button class="mr-2" @click="router.push('/system/model/layout/' + modelId)">
          布局设计
        </Button>
        <Button class="mr-2" @click="openComponents">字段组库</Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/modelField/*'])"
          class="mr-2"
          type="primary"
          @click="openAdd"
        >
          新增字段
        </Button>
      </template>
      <template #action="{ row }">
        <Button
          v-if="['group', 'repeater'].includes(row.fieldType)"
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="openAddChild(row)"
        >
          子字段
        </Button>
        <Button
          v-if="row.fieldType === 'group'"
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="startPickup(row)"
        >
          存为字段组
        </Button>
        <Button
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/modelField/*'])"
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
    <FieldModalComp />
    <FormPreviewComp />
    <ComponentDrawer
      v-if="componentVisible"
      :key="componentSeq"
      :model-id="modelId"
      @applied="() => gridApi.query()"
    />
    <Modal
      v-model:open="pickupOpen"
      title="存为字段组"
      @ok="savePickup"
    >
      <div class="space-y-3 pt-2">
        <div>
          <div class="mb-1 text-sm">字段组标识（应用时作为 group 字段名，小写字母开头）</div>
          <Input v-model:value="pickupCode" placeholder="如 product_spec" />
        </div>
        <div>
          <div class="mb-1 text-sm">名称</div>
          <Input v-model:value="pickupName" />
        </div>
        <div>
          <div class="mb-1 text-sm">用途说明（可选）</div>
          <Input v-model:value="pickupRemark" />
        </div>
      </div>
    </Modal>
  </Page>
</template>
