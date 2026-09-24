<script lang="ts" setup>
import type { FormFieldRow, FormRow } from '#/api/core/form';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import {
  Button,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Table,
  Textarea,
} from 'ant-design-vue';

import {
  FIELD_TYPES,
  deleteFormFieldApi,
  getFormFieldListApi,
  saveFormFieldApi,
} from '#/api/core/form';

/** 字段管理抽屉：一个表单对应若干字段 */
const form = ref<null | FormRow>(null);
const rows = ref<FormFieldRow[]>([]);
const loading = ref(false);
const editingId = ref('');

const fieldCode = ref('');
const fieldName = ref('');
const fieldType = ref('text');
const required = ref(false);
const defaultValue = ref('');
const placeholder = ref('');
const options = ref('');
const sort = ref(0);

const [Drawer, drawerApi] = useEditDrawer({ title: '字段管理' });

const columns = [
  { title: '字段名', dataIndex: 'fieldName', key: 'fieldName', width: 140 },
  { title: '字段key', dataIndex: 'fieldCode', key: 'fieldCode', width: 140 },
  { title: '类型', dataIndex: 'fieldType', key: 'fieldType', width: 100 },
  { title: '必填', dataIndex: 'required', key: 'required', width: 60 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 60 },
  { title: '操作', key: 'action', width: 120 },
];

async function load() {
  if (!form.value) return;
  loading.value = true;
  try {
    rows.value = await getFormFieldListApi(String(form.value.id));
  } finally {
    loading.value = false;
  }
}

function resetEditor() {
  editingId.value = '';
  fieldCode.value = '';
  fieldName.value = '';
  fieldType.value = 'text';
  required.value = false;
  defaultValue.value = '';
  placeholder.value = '';
  options.value = '';
  sort.value = rows.value.length;
}

function onEdit(row: any) {
  editingId.value = String(row.id);
  fieldCode.value = row.fieldCode;
  fieldName.value = row.fieldName;
  fieldType.value = row.fieldType;
  required.value = row.required === 1;
  defaultValue.value = row.defaultValue || '';
  placeholder.value = row.placeholder || '';
  options.value = row.options || '';
  sort.value = row.sort ?? 0;
}

async function onSave() {
  if (!fieldName.value.trim()) {
    message.warning('请填写字段名');
    return;
  }
  if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(fieldCode.value)) {
    message.warning('字段key 只能为字母/数字/下划线，且以字母开头');
    return;
  }
  await saveFormFieldApi({
    defaultValue: defaultValue.value,
    fieldCode: fieldCode.value,
    fieldName: fieldName.value,
    fieldType: fieldType.value,
    formId: form.value?.id,
    id: editingId.value || undefined,
    options: options.value,
    placeholder: placeholder.value,
    required: required.value ? 1 : 0,
    sort: sort.value,
  });
  message.success('保存成功');
  resetEditor();
  load();
}

function onDelete(row: any) {
  Modal.confirm({
    content: `确认删除字段「${row.fieldName}」？已提交数据不受影响。`,
    onOk: async () => {
      await deleteFormFieldApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

const needOptions = (t: string) =>
  t === 'radio' || t === 'checkbox' || t === 'select';

onMounted(() => {
  const data = drawerApi.getData() as { record?: FormRow } | undefined;
  form.value = data?.record ?? null;
  if (form.value) {
    drawerApi.setState({ title: `字段管理 — ${form.value.formName}` });
    load();
  }
});
</script>

<template>
  <Drawer>
    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      class="mb-4"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'fieldCode'">
          <code class="text-xs">{{ record.fieldCode }}</code>
        </template>
        <template v-if="column.key === 'required'">
          {{ record.required === 1 ? '是' : '否' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <Button class="mr-1 px-2" size="small" type="link" @click="onEdit(record)">
            编辑
          </Button>
          <Button danger size="small" type="link" @click="onDelete(record)">
            删除
          </Button>
        </template>
      </template>
    </Table>

    <div class="rounded border p-3">
      <div class="mb-2 text-sm font-medium">
        {{ editingId ? '编辑字段' : '新增字段' }}
      </div>
      <div class="grid grid-cols-2 gap-x-4 gap-y-3">
        <div>
          <div class="mb-1 text-sm">字段名（中文）</div>
          <Input v-model:value="fieldName" placeholder="如：您的姓名" />
        </div>
        <div>
          <div class="mb-1 text-sm">字段key（英文）</div>
          <Input v-model:value="fieldCode" placeholder="name" />
        </div>
        <div>
          <div class="mb-1 text-sm">类型</div>
          <Select v-model:value="fieldType" :options="FIELD_TYPES" class="w-full" />
        </div>
        <div>
          <div class="mb-1 text-sm">排序</div>
          <InputNumber v-model:value="sort" class="w-full" />
        </div>
        <div>
          <div class="mb-1 text-sm">默认值</div>
          <Input v-model:value="defaultValue" />
        </div>
        <div>
          <div class="mb-1 text-sm">输入提示</div>
          <Input v-model:value="placeholder" />
        </div>
        <div v-if="needOptions(fieldType)" class="col-span-2">
          <div class="mb-1 text-sm">
            选项（每行一个，或 JSON 数组 ["男","女"]）
          </div>
          <Textarea v-model:value="options" :rows="3" />
        </div>
        <div class="flex items-end gap-2 pb-1">
          <input id="field-required" v-model="required" type="checkbox" />
          <label class="text-sm" for="field-required">必填</label>
        </div>
      </div>
      <div class="mt-3 flex gap-2">
        <Button type="primary" @click="onSave">保存字段</Button>
        <Button v-if="editingId" @click="resetEditor">取消编辑</Button>
      </div>
    </div>
  </Drawer>
</template>
