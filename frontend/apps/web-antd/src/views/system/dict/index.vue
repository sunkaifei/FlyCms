<script lang="ts" setup>
import type { DictDataRow, DictTypeRow } from '#/api/core/dict';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Pagination,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  deleteDictDataApi,
  deleteDictTypeApi,
  getDictDataListApi,
  getDictTypeListApi,
  saveDictDataApi,
  saveDictTypeApi,
  updateDictDataApi,
  updateDictTypeApi,
} from '#/api/core/dict';

defineOptions({ name: 'SystemDict' });

const { hasAccessByCodes } = useAccess();
const canTypeSave = hasAccessByCodes(['/api/system/dict/type/save']);
const canTypeUpdate = hasAccessByCodes(['/api/system/dict/type/update']);
const canTypeDelete = hasAccessByCodes(['/api/system/dict/type/delete']);
const canDataList = hasAccessByCodes(['/api/system/dict/data/list']);
const canDataSave = hasAccessByCodes(['/api/system/dict/data/save']);
const canDataUpdate = hasAccessByCodes(['/api/system/dict/data/update']);
const canDataDelete = hasAccessByCodes(['/api/system/dict/data/delete']);

// ---------------- 字典类型列表 ----------------
const rows = ref<DictTypeRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const keyword = ref('');

const columns = [
  { title: '字典名称', dataIndex: 'dictName', key: 'dictName' },
  { title: '类型键', dataIndex: 'dictType', key: 'dictType' },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 200 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 200 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getDictTypeListApi({
      keyword: keyword.value || undefined,
      p: page.value,
      rows: 20,
    });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

// ---------------- 类型新增/编辑 ----------------
const typeOpen = ref(false);
const typeSaving = ref(false);
const typeForm = reactive({
  id: '',
  dictName: '',
  dictType: '',
  remark: '',
  sort: 0,
  status: 1,
});

function openTypeAdd() {
  Object.assign(typeForm, {
    id: '',
    dictName: '',
    dictType: '',
    remark: '',
    sort: 0,
    status: 1,
  });
  typeOpen.value = true;
}

function openTypeEdit(row: DictTypeRow) {
  Object.assign(typeForm, {
    id: String(row.id),
    dictName: row.dictName,
    dictType: row.dictType,
    remark: row.remark || '',
    sort: row.sort,
    status: row.status,
  });
  typeOpen.value = true;
}

async function onTypeSave() {
  if (!typeForm.dictName.trim()) {
    message.warning('字典名称不能为空');
    return;
  }
  if (!typeForm.id && !typeForm.dictType.trim()) {
    message.warning('类型键不能为空');
    return;
  }
  typeSaving.value = true;
  try {
    if (typeForm.id) {
      await updateDictTypeApi({ ...typeForm, id: typeForm.id });
      message.success('已更新（类型键创建后不可改）');
    } else {
      await saveDictTypeApi({ ...typeForm });
      message.success('已添加');
    }
    typeOpen.value = false;
    load();
  } finally {
    typeSaving.value = false;
  }
}

function onTypeDelete(row: DictTypeRow) {
  Modal.confirm({
    content: `删除字典「${row.dictName}」及其全部数据？被模型字段绑定时将拒绝删除。`,
    onOk: async () => {
      await deleteDictTypeApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

// ---------------- 字典数据管理（抽屉式弹窗） ----------------
const dataOpen = ref(false);
const dataLoading = ref(false);
const dataRows = ref<DictDataRow[]>([]);
const dataTotal = ref(0);
const dataPage = ref(1);
const currentType = ref<null | DictTypeRow>(null);

const dataColumns = [
  { title: '标签', dataIndex: 'dictLabel', key: 'dictLabel' },
  { title: '键值', dataIndex: 'dictValue', key: 'dictValue' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 140 },
];

async function openData(row: DictTypeRow) {
  currentType.value = row;
  dataOpen.value = true;
  dataPage.value = 1;
  await loadData();
}

async function loadData() {
  if (!currentType.value) return;
  dataLoading.value = true;
  try {
    const res = await getDictDataListApi({
      dictType: currentType.value.dictType,
      p: dataPage.value,
      rows: 50,
    });
    dataRows.value = res.list ?? [];
    dataTotal.value = res.count ?? 0;
  } finally {
    dataLoading.value = false;
  }
}

const dataEditOpen = ref(false);
const dataSaving = ref(false);
const dataForm = reactive({
  id: '',
  dictLabel: '',
  dictValue: '',
  remark: '',
  sort: 0,
  status: 1,
});

function openDataAdd() {
  Object.assign(dataForm, {
    id: '',
    dictLabel: '',
    dictValue: '',
    remark: '',
    sort: (dataRows.value?.length ?? 0) + 1,
    status: 1,
  });
  dataEditOpen.value = true;
}

function openDataEdit(row: DictDataRow) {
  Object.assign(dataForm, {
    id: String(row.id),
    dictLabel: row.dictLabel,
    dictValue: row.dictValue,
    remark: row.remark || '',
    sort: row.sort,
    status: row.status,
  });
  dataEditOpen.value = true;
}

async function onDataSave() {
  if (!currentType.value) return;
  if (!dataForm.dictLabel.trim() || !dataForm.dictValue.trim()) {
    message.warning('标签与键值不能为空');
    return;
  }
  dataSaving.value = true;
  try {
    if (dataForm.id) {
      await updateDictDataApi({ ...dataForm, id: dataForm.id });
      message.success('已更新');
    } else {
      await saveDictDataApi({
        ...dataForm,
        dictType: currentType.value.dictType,
      });
      message.success('已添加');
    }
    dataEditOpen.value = false;
    loadData();
  } finally {
    dataSaving.value = false;
  }
}

function onDataDelete(row: DictDataRow) {
  Modal.confirm({
    content: `删除数据「${row.dictLabel}」？`,
    onOk: async () => {
      await deleteDictDataApi(String(row.id));
      message.success('已删除');
      loadData();
    },
    title: '删除确认',
  });
}

onMounted(load);
</script>

<template>
  <Page
    title="字典管理"
    description="平台级数据字典：模型字段的下拉/单选/多选可绑定这里的类型，发布表单候选项随字典实时生效（字段自带选项作为回退）。"
  >
    <div class="mb-3 flex items-center gap-2">
      <Button v-if="canTypeSave" type="primary" @click="openTypeAdd">
        新增字典
      </Button>
      <Input
        v-model:value="keyword"
        allow-clear
        class="w-56"
        placeholder="名称 / 类型键"
        @press-enter="
          () => {
            page = 1;
            load();
          }
        "
      />
      <Button @click="load">刷新</Button>
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
        <template v-if="column.key === 'dictType'">
          <code class="rounded bg-gray-100 px-1.5 py-0.5 text-xs">
            {{ (record as DictTypeRow).dictType }}
          </code>
        </template>
        <template v-else-if="column.key === 'status'">
          <Tag :color="(record as DictTypeRow).status === 1 ? 'green' : 'default'">
            {{ (record as DictTypeRow).status === 1 ? '启用' : '停用' }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="canDataList"
            size="small"
            type="link"
            @click="openData(record as DictTypeRow)"
          >
            数据
          </Button>
          <Button
            v-if="canTypeUpdate"
            size="small"
            type="link"
            @click="openTypeEdit(record as DictTypeRow)"
          >
            编辑
          </Button>
          <Button
            v-if="canTypeDelete"
            danger
            size="small"
            type="link"
            @click="onTypeDelete(record as DictTypeRow)"
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
        :show-size-changer="false"
        :total="total"
        size="small"
        @change="load"
      />
    </div>

    <!-- 类型新增/编辑 -->
    <Modal
      v-model:open="typeOpen"
      :confirm-loading="typeSaving"
      :title="typeForm.id ? '编辑字典' : '新增字典'"
      @ok="onTypeSave"
    >
      <Form layout="vertical" class="pt-2">
        <FormItem label="字典名称" required>
          <Input v-model:value="typeForm.dictName" placeholder="如：运行平台" />
        </FormItem>
        <FormItem label="类型键" :required="!typeForm.id">
          <Input
            v-model:value="typeForm.dictType"
            :disabled="!!typeForm.id"
            placeholder="小写字母开头，如 app_os（创建后不可改）"
          />
        </FormItem>
        <FormItem label="排序">
          <InputNumber v-model:value="typeForm.sort" class="w-full" />
        </FormItem>
        <FormItem label="备注">
          <Input v-model:value="typeForm.remark" />
        </FormItem>
        <FormItem label="状态">
          <Tag :color="typeForm.status === 1 ? 'green' : 'default'">
            {{ typeForm.status === 1 ? '启用' : '停用' }}
          </Tag>
          <Button
            size="small"
            @click="typeForm.status = typeForm.status === 1 ? 0 : 1"
          >
            {{ typeForm.status === 1 ? '点击停用' : '点击启用' }}
          </Button>
        </FormItem>
      </Form>
    </Modal>

    <!-- 字典数据管理 -->
    <Modal
      v-model:open="dataOpen"
      :footer="null"
      :title="`字典数据 — ${currentType?.dictName ?? ''}（${currentType?.dictType ?? ''}）`"
      width="760px"
    >
      <div class="mb-3 flex items-center gap-2 pt-2">
        <Button v-if="canDataSave" size="small" type="primary" @click="openDataAdd">
          新增数据
        </Button>
        <Button size="small" @click="loadData">刷新</Button>
      </div>
      <Table
        :columns="dataColumns"
        :data-source="dataRows"
        :loading="dataLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag
              :color="(record as DictDataRow).status === 1 ? 'green' : 'default'"
            >
              {{ (record as DictDataRow).status === 1 ? '启用' : '停用' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Button
              v-if="canDataUpdate"
              size="small"
              type="link"
              @click="openDataEdit(record as DictDataRow)"
            >
              编辑
            </Button>
            <Button
              v-if="canDataDelete"
              danger
              size="small"
              type="link"
              @click="onDataDelete(record as DictDataRow)"
            >
              删除
            </Button>
          </template>
        </template>
      </Table>
      <div class="mt-3 flex justify-end">
        <Pagination
          v-model:current="dataPage"
          :page-size="50"
          :show-size-changer="false"
          :total="dataTotal"
          size="small"
          @change="loadData"
        />
      </div>

      <!-- 数据新增/编辑 -->
      <Modal
        v-model:open="dataEditOpen"
        :confirm-loading="dataSaving"
        :title="dataForm.id ? '编辑数据' : '新增数据'"
        @ok="onDataSave"
      >
        <Form layout="vertical" class="pt-2">
          <FormItem label="标签（表单显示）" required>
            <Input v-model:value="dataForm.dictLabel" placeholder="如：Windows" />
          </FormItem>
          <FormItem label="键值（入库存储）" required>
            <Input v-model:value="dataForm.dictValue" placeholder="可与标签相同" />
          </FormItem>
          <FormItem label="排序">
            <InputNumber v-model:value="dataForm.sort" class="w-full" />
          </FormItem>
          <FormItem label="备注">
            <Input v-model:value="dataForm.remark" />
          </FormItem>
          <FormItem label="状态">
            <Tag :color="dataForm.status === 1 ? 'green' : 'default'">
              {{ dataForm.status === 1 ? '启用' : '停用' }}
            </Tag>
            <Button
              size="small"
              @click="dataForm.status = dataForm.status === 1 ? 0 : 1"
            >
              {{ dataForm.status === 1 ? '点击停用' : '点击启用' }}
            </Button>
          </FormItem>
        </Form>
      </Modal>
    </Modal>
  </Page>
</template>
