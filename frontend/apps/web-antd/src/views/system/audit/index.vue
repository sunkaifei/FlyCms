<script lang="ts" setup>
import type { AuditArticleRow } from '#/api/core/audit';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Input,
  message,
  Modal,
  Pagination,
  Select,
  Switch,
  Table,
  Textarea,
} from 'ant-design-vue';

import {
  auditArticleApi,
  batchAuditApi,
  getAuditPageApi,
  getAuditSwitchApi,
  setAuditSwitchApi,
} from '#/api/core/audit';
import { getModelListApi, type ModelRow } from '#/api/core/model';

defineOptions({ name: 'SystemAudit' });

const { hasAccessByCodes } = useAccess();

const STATUS_OPTIONS = [
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '未通过', value: 2 },
  { label: '全部', value: -1 },
];

const STATUS_TAG: Record<number, string> = {
  0: '待审核',
  1: '已通过',
  2: '未通过',
  3: '已删除',
};

const rows = ref<AuditArticleRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const selectedKeys = ref<string[]>([]);
const status = ref(0);
const title = ref('');

/** U3：审核对象为自定义模型内容，按模型过滤 */
const models = ref<ModelRow[]>([]);
const modelId = ref<number | string>();

/** 审核开关：1=先审后发 */
const auditSwitch = ref(false);

const rejectVisible = ref(false);
const rejectReason = ref('');
const rejectTarget = ref<string[]>([]);

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '作者ID', dataIndex: 'userId', key: 'userId', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 160 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getAuditPageApi({
      modelId: modelId.value,
      p: page.value,
      rows: 20,
      status: status.value >= 0 ? status.value : undefined,
      title: title.value || undefined,
    });
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

function openReject(ids: string[]) {
  if (!ids.length) {
    message.warning('请先选择内容');
    return;
  }
  rejectTarget.value = ids;
  rejectReason.value = '';
  rejectVisible.value = true;
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    message.warning('驳回必须填写原因');
    return;
  }
  const ids = rejectTarget.value;
  if (ids.length === 1) {
    await auditArticleApi(String(ids[0]), 2, rejectReason.value, modelId.value);
  } else {
    await batchAuditApi(ids, 2, rejectReason.value, modelId.value);
  }
  message.success('已驳回，并已站内信通知作者');
  rejectVisible.value = false;
  selectedKeys.value = [];
  load();
}

async function onPass(ids: string[]) {
  if (!ids.length) {
    message.warning('请先选择内容');
    return;
  }
  if (ids.length === 1) {
    await auditArticleApi(String(ids[0]), 1, undefined, modelId.value);
  } else {
    await batchAuditApi(ids, 1, undefined, modelId.value);
  }
  message.success('已通过');
  selectedKeys.value = [];
  load();
}

onMounted(async () => {
  await loadSwitch();
  try {
    const res = await getModelListApi({ p: 1, rows: 100 });
    models.value = res.list ?? [];
    if (models.value.length > 0) {
      modelId.value = models.value[0]!.id;
    }
  } catch {
    models.value = [];
  }
  load();
});
</script>

<template>
  <Page>
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="modelId"
        :options="(models ?? []).map((m) => ({ label: m.name, value: m.id }))"
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
        placeholder="标题"
        @press-enter="onSearch"
      />
      <Button @click="onSearch">搜索</Button>

      <div v-if="hasAccessByCodes(['/api/system/audit/switch'])" class="ml-auto flex items-center gap-2">
        <span class="text-sm text-gray-500">先审后发</span>
        <Switch
          v-model:checked="auditSwitch"
          size="small"
          @change="(c: any) => onToggleSwitch(Boolean(c))"
        />
      </div>
    </div>

    <div
      v-if="hasAccessByCodes(['/api/system/audit/batch'])"
      class="mb-3 flex gap-2"
    >
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
      <span v-if="selectedKeys.length" class="self-center text-xs text-gray-400">
        已选 {{ selectedKeys.length }} 条
      </span>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      :row-selection="{ selectedRowKeys: selectedKeys, onChange: (keys: any[]) => (selectedKeys = keys) }"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <span :class="record.status === 1 ? 'text-green-600' : 'text-orange-500'">
            {{ STATUS_TAG[record.status] ?? record.status }}
          </span>
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="hasAccessByCodes(['/api/system/audit/audit'])"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="onPass([String(record.id)])"
          >
            通过
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/audit/audit'])"
            danger
            size="small"
            type="link"
            @click="openReject([String(record.id)])"
          >
            驳回
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

    <Modal
      v-model:open="rejectVisible"
      title="驳回原因（将以站内信通知作者）"
      @ok="confirmReject"
    >
      <Textarea
        v-model:value="rejectReason"
        :rows="4"
        placeholder="请填写驳回原因，必填"
      />
    </Modal>
  </Page>
</template>
