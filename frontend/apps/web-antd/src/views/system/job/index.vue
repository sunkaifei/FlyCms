<script lang="ts" setup>
import type { JobLogRow, JobRow } from '#/api/core/job';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  message,
  Modal,
  Pagination,
  Switch,
  Table,
  Tag,
  Input,
  Form,
  FormItem,
} from 'ant-design-vue';

import {
  deleteJobApi,
  getJobListApi,
  getJobLogListApi,
  runJobApi,
  saveJobApi,
  updateJobApi,
  updateJobStatusApi,
} from '#/api/core/job';

defineOptions({ name: 'SystemJob' });

const { hasAccessByCodes } = useAccess();
const canSave = hasAccessByCodes(['/api/system/job/save']);
const canUpdate = hasAccessByCodes(['/api/system/job/update']);
const canStatus = hasAccessByCodes(['/api/system/job/status']);
const canRun = hasAccessByCodes(['/api/system/job/run']);
const canDelete = hasAccessByCodes(['/api/system/job/delete']);
const canLog = hasAccessByCodes(['/api/system/job/logList']);

const rows = ref<JobRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);

const columns = [
  { title: '任务', key: 'task' },
  { title: 'cron 表达式', dataIndex: 'cronExpression', key: 'cron' },
  { title: '参数', dataIndex: 'params', key: 'params', width: 110 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 140 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 230 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getJobListApi({ p: page.value, rows: 20 });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

// ---------------- 新增/编辑弹窗 ----------------
const editOpen = ref(false);
const editSaving = ref(false);
const editForm = reactive({
  id: '',
  beanName: '',
  methodName: '',
  cronExpression: '',
  params: '',
  remark: '',
  status: '0',
});

function openAdd() {
  Object.assign(editForm, {
    id: '',
    beanName: '',
    methodName: '',
    cronExpression: '',
    params: '',
    remark: '',
    status: '0',
  });
  editOpen.value = true;
}

function openEdit(row: JobRow) {
  Object.assign(editForm, {
    id: String(row.id),
    beanName: row.beanName,
    methodName: row.methodName,
    cronExpression: row.cronExpression,
    params: row.params || '',
    remark: row.remark || '',
    status: row.status,
  });
  editOpen.value = true;
}

async function onSave() {
  if (!editForm.beanName.trim() || !editForm.methodName.trim()) {
    message.warning('bean 名称与方法名不能为空');
    return;
  }
  if (!editForm.cronExpression.trim()) {
    message.warning('cron 表达式不能为空');
    return;
  }
  editSaving.value = true;
  try {
    if (editForm.id) {
      await updateJobApi({ ...editForm, id: editForm.id });
      message.success('已更新，调度已同步');
    } else {
      await saveJobApi({ ...editForm });
      message.success(
        editForm.status === '1' ? '已新增并启用' : '已新增（暂停状态，可手动启用）',
      );
    }
    editOpen.value = false;
    load();
  } finally {
    editSaving.value = false;
  }
}

// ---------------- 状态/执行/删除 ----------------
async function onStatus(row: JobRow, checked: boolean) {
  const next = checked ? '1' : '0';
  await updateJobStatusApi(String(row.id), next);
  row.status = next;
  message.success(checked ? '已启用' : '已暂停');
}

function onRun(row: JobRow) {
  Modal.confirm({
    content: `立即执行「${row.beanName}.${row.methodName}」一次？`,
    onOk: async () => {
      const res = await runJobApi(String(row.id));
      message.success(res || '已触发执行');
    },
    title: '立即执行',
  });
}

function onDelete(row: JobRow) {
  Modal.confirm({
    content: `删除任务「${row.beanName}.${row.methodName}」？同时会从调度器移除。`,
    onOk: async () => {
      await deleteJobApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

// ---------------- 执行日志弹窗 ----------------
const logOpen = ref(false);
const logLoading = ref(false);
const logRows = ref<JobLogRow[]>([]);
const logTotal = ref(0);
const logPage = ref(1);

const logColumns = [
  { title: '时间', dataIndex: 'createTime', key: 'time', width: 170 },
  { title: '任务', key: 'task' },
  { title: '耗时', key: 'times', width: 90 },
  { title: '结果', key: 'result', width: 80 },
  { title: '错误信息', dataIndex: 'errorMsg', key: 'error' },
];

async function openLog() {
  logOpen.value = true;
  logPage.value = 1;
  await loadLog();
}

async function loadLog() {
  logLoading.value = true;
  try {
    const res = await getJobLogListApi({ p: logPage.value, rows: 10 });
    logRows.value = res.list ?? [];
    logTotal.value = res.count ?? 0;
  } finally {
    logLoading.value = false;
  }
}

function fmtTimes(row: JobLogRow) {
  const t = row.times;
  return t === undefined || t === null ? '-' : `${t} ms`;
}

onMounted(load);
</script>

<template>
  <Page
    title="定时任务"
    description="基于 Quartz 的任务调度：bean 名称填 Spring 容器中的组件名（如 myTaskTest），方法名无参或仅一个 String 参数。"
  >
    <div class="mb-3 flex items-center gap-2">
      <Button v-if="canSave" type="primary" @click="openAdd">新增任务</Button>
      <Button v-if="canLog" @click="openLog">执行日志</Button>
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
        <template v-if="column.key === 'task'">
          <span class="font-medium">{{ (record as JobRow).beanName }}</span>
          <span class="text-gray-400">.</span>
          <span>{{ (record as JobRow).methodName }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <Switch
            v-if="canStatus"
            :checked="(record as JobRow).status === '1'"
            checked-children="启用"
            un-checked-children="暂停"
            @change="(v: any) => onStatus(record as JobRow, !!v)"
          />
          <Tag v-else :color="(record as JobRow).status === '1' ? 'green' : 'default'">
            {{ (record as JobRow).status === '1' ? '启用' : '暂停' }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="canRun"
            size="small"
            type="link"
            @click="onRun(record as JobRow)"
          >
            执行
          </Button>
          <Button
            v-if="canUpdate"
            size="small"
            type="link"
            @click="openEdit(record as JobRow)"
          >
            编辑
          </Button>
          <Button
            v-if="canDelete"
            danger
            size="small"
            type="link"
            @click="onDelete(record as JobRow)"
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

    <!-- 新增/编辑 -->
    <Modal
      v-model:open="editOpen"
      :confirm-loading="editSaving"
      :title="editForm.id ? '编辑任务' : '新增任务'"
      @ok="onSave"
    >
      <Form layout="vertical" class="pt-2">
        <FormItem label="bean 名称（Spring 组件名）" required>
          <Input v-model:value="editForm.beanName" placeholder="如 myTaskTest" />
        </FormItem>
        <FormItem label="方法名" required>
          <Input v-model:value="editForm.methodName" placeholder="无参方法或仅一个 String 参数" />
        </FormItem>
        <FormItem label="cron 表达式" required>
          <Input v-model:value="editForm.cronExpression" placeholder="如 0/10 * * * * ?（每 10 秒）" />
        </FormItem>
        <FormItem label="参数（可选，String 类型）">
          <Input v-model:value="editForm.params" />
        </FormItem>
        <FormItem label="备注">
          <Input v-model:value="editForm.remark" />
        </FormItem>
        <FormItem v-if="!editForm.id" label="创建后状态">
          <Switch
            :checked="editForm.status === '1'"
            checked-children="启用"
            un-checked-children="暂停"
            @change="(v: any) => (editForm.status = v ? '1' : '0')"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 执行日志 -->
    <Modal
      v-model:open="logOpen"
      :footer="null"
      title="任务执行日志"
      width="820px"
    >
      <Table
        :columns="logColumns"
        :data-source="logRows"
        :loading="logLoading"
        :pagination="false"
        row-key="id"
        size="small"
        class="pt-2"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'task'">
            {{ (record as JobLogRow).beanName }}.{{
              (record as JobLogRow).methodName
            }}
          </template>
          <template v-else-if="column.key === 'times'">
            {{ fmtTimes(record as JobLogRow) }}
          </template>
          <template v-else-if="column.key === 'result'">
            <Tag :color="(record as JobLogRow).status === '1' ? 'green' : 'red'">
              {{ (record as JobLogRow).status === '1' ? '成功' : '失败' }}
            </Tag>
          </template>
        </template>
      </Table>
      <div class="mt-3 flex justify-end">
        <Pagination
          v-model:current="logPage"
          :page-size="10"
          :show-size-changer="false"
          :total="logTotal"
          size="small"
          @change="loadLog"
        />
      </div>
    </Modal>
  </Page>
</template>
