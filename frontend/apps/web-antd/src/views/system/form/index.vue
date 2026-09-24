<script lang="ts" setup>
import type { FormRow } from '#/api/core/form';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import { Button, Input, message, Modal, Pagination, Switch, Table } from 'ant-design-vue';

import { deleteFormApi, getFormPageApi } from '#/api/core/form';

import FieldDrawer from './field-drawer.vue';
import FormModal from './form-modal.vue';

defineOptions({ name: 'SystemForm' });

const { hasAccessByCodes } = useAccess();

const rows = ref<FormRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const keyword = ref('');

const [FormModalComp, formModalApi] = useEditDrawer({
  connectedComponent: FormModal,
  destroyOnClose: true,
});

const [FieldDrawerComp, fieldDrawerApi] = useEditDrawer({
  connectedComponent: FieldDrawer,
  destroyOnClose: true,
});

const columns = [
  { title: '表单名称', dataIndex: 'formName', key: 'formName' },
  { title: '调用码', dataIndex: 'formCode', key: 'formCode', width: 160 },
  { title: '上限/日', dataIndex: 'submitLimit', key: 'submitLimit', width: 90 },
  { title: '验证码', dataIndex: 'needCaptcha', key: 'needCaptcha', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 200 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getFormPageApi({
      formName: keyword.value || undefined,
      p: page.value,
      rows: 20,
    });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  page.value = 1;
  load();
}

function openAdd() {
  formModalApi.setData({ onSaved: load }).open();
}

function openEdit(row: any) {
  formModalApi.setData({ onSaved: load, record: row }).open();
}

function openFields(row: any) {
  fieldDrawerApi.setData({ record: row }).open();
}

function onDelete(row: any) {
  Modal.confirm({
    content: `确认删除表单「${row.formName}」？该表单已提交的数据会一并删除。`,
    onOk: async () => {
      await deleteFormApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="mb-3 flex items-center gap-2">
      <Input
        v-model:value="keyword"
        allow-clear
        class="w-56"
        placeholder="表单名称"
        @press-enter="onSearch"
      />
      <Button @click="onSearch">搜索</Button>
      <Button
        v-if="hasAccessByCodes(['/api/system/form/save'])"
        class="ml-auto"
        type="primary"
        @click="openAdd"
      >
        新增表单
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
        <template v-if="column.key === 'formCode'">
          <code class="text-xs">{{ record.formCode }}</code>
        </template>
        <template v-else-if="column.key === 'needCaptcha'">
          {{ record.needCaptcha === 1 ? '开启' : '关闭' }}
        </template>
        <template v-else-if="column.key === 'status'">
          <Switch
            :checked="Number(record.status) === 1"
            disabled
            size="small"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <Button class="mr-1 px-2" size="small" type="link" @click="openFields(record)">
            字段管理
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/form/save'])"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="openEdit(record)"
          >
            编辑
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/form/delete'])"
            danger
            size="small"
            type="link"
            @click="onDelete(record)"
          >
            删除
          </Button>
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
    <FormModalComp />
    <FieldDrawerComp />
  </Page>
</template>
