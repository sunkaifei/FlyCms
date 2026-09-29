<script lang="ts" setup>
/**
 * G17 自动化规则管理：规则 = 事件 + 模型限定（可选）+ 条件（可选）+ 动作集（webhook/log）。
 * 执行引擎监听 G18 ContentChangedEvent，命中即依次执行动作。
 */
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Input,
  message,
  Modal,
  Select,
  Switch,
  Tag,
  Textarea,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteAutomationRuleApi,
  getAutomationListApi,
  saveAutomationRuleApi,
} from '#/api/core/automation';

defineOptions({ name: 'SystemAutomation' });

const { hasAccessByCodes } = useAccess();

const EVENT_OPTIONS = [
  { label: '内容新建 (insert)', value: 'insert' },
  { label: '内容更新 (update)', value: 'update' },
  { label: '内容删除 (delete)', value: 'delete' },
  { label: '状态切换 (status)', value: 'status' },
  { label: '评论发表 (comment_add)', value: 'comment_add' },
  { label: '评论审核 (comment_audit)', value: 'comment_audit' },
  { label: '评论删除 (comment_delete)', value: 'comment_delete' },
];

interface RuleRow {
  actions: string;
  conditions: string | null;
  createTime?: string;
  event: string;
  id: string;
  modelCode: string | null;
  ruleName: string;
  status: number;
}

const rows = ref<RuleRow[]>([]);
const loading = ref(false);

/** 编辑态（null=列表；对象=新增/编辑表单数据） */
const editing = ref<null | {
  actions: string;
  conditions: string;
  event: string;
  id?: string;
  modelCode: string;
  ruleName: string;
  status: number;
}>(null);

async function load() {
  loading.value = true;
  try {
    rows.value = (await getAutomationListApi()) ?? [];
  } finally {
    loading.value = false;
  }
}

function openAdd() {
  editing.value = {
    actions: JSON.stringify([{ type: 'log' }], null, 2),
    conditions: '',
    event: 'insert',
    modelCode: '',
    ruleName: '',
    status: 1,
  };
}

function openEdit(row: RuleRow) {
  editing.value = {
    actions: row.actions,
    conditions: row.conditions ?? '',
    event: row.event,
    id: row.id,
    modelCode: row.modelCode ?? '',
    ruleName: row.ruleName,
    status: row.status,
  };
}

async function save() {
  if (!editing.value) return;
  try {
    JSON.parse(editing.value.actions);
  } catch {
    message.error('动作 JSON 不合法');
    return;
  }
  await saveAutomationRuleApi({
    actions: editing.value.actions,
    conditions: editing.value.conditions || undefined,
    event: editing.value.event,
    id: editing.value.id,
    modelCode: editing.value.modelCode || undefined,
    ruleName: editing.value.ruleName,
    status: editing.value.status,
  });
  message.success('已保存');
  editing.value = null;
  load();
}

function onDelete(row: RuleRow) {
  Modal.confirm({
    content: `删除规则「${row.ruleName}」？`,
    okType: 'danger',
    onOk: async () => {
      await deleteAutomationRuleApi(row.id);
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

const gridOptions: VxeTableGridOptions<RuleRow> = {
  columns: [
    { title: '序号', type: 'seq', width: 60 },
    { field: 'ruleName', title: '规则名称' },
    { field: 'event', title: '事件', width: 150 },
    { field: 'modelCode', title: '限定模型', width: 110 },
    { field: 'actions', title: '动作（JSON）' },
    { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 160 },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async () => ({ items: rows.value, total: rows.value.length }),
    },
  },
  toolbarConfig: { refresh: true, zoom: true },
};

const [Grid] = useVbenVxeGrid({ gridOptions });

onMounted(load);
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="自动化规则（事件 → 动作）">
      <template #toolbar-tools>
        <Button
          v-if="hasAccessByCodes(['/api/system/automation/save'])"
          type="primary"
          @click="openAdd"
        >
          新增规则
        </Button>
      </template>
      <template #action="{ row }">
        <Button
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/automation/del'])"
          class="px-2"
          danger
          size="small"
          type="link"
          @click="onDelete(row)"
        >
          删除
        </Button>
      </template>
      <template #event="{ row }">
        <Tag color="blue">{{ row.event }}</Tag>
      </template>
    </Grid>

    <Modal
      :open="editing !== null"
      :title="editing?.id ? '编辑规则' : '新增规则'"
      :width="640"
      @ok="save"
      @cancel="editing = null"
    >
      <div v-if="editing" class="space-y-3 pt-2">
        <div>
          <div class="mb-1 text-sm">规则名称</div>
          <Input v-model:value="editing.ruleName" placeholder="如：新内容同步到企业微信" />
        </div>
        <div class="flex gap-3">
          <div class="flex-1">
            <div class="mb-1 text-sm">触发事件</div>
            <Select v-model:value="editing.event" :options="EVENT_OPTIONS" class="w-full" />
          </div>
          <div class="flex-1">
            <div class="mb-1 text-sm">限定模型（留空 = 全部模型）</div>
            <Input v-model:value="editing.modelCode" placeholder="如 articles" />
          </div>
        </div>
        <div>
          <div class="mb-1 text-sm">
            条件（可选，JSON 数组）——对首行内容求值，全部命中才执行
          </div>
          <Textarea
            v-model:value="editing.conditions"
            :rows="2"
            placeholder='[{"field":"status","op":"eq","value":"1"}]，op：eq/neq/gt/lt/contains'
          />
        </div>
        <div>
          <div class="mb-1 text-sm">动作（JSON 数组，依次执行）</div>
          <Textarea
            v-model:value="editing.actions"
            :rows="4"
            placeholder='[{"type":"webhook","url":"https://example.com/hook"},{"type":"log"}]'
          />
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm">启用</span>
          <Switch
            :checked="editing.status === 1"
            @change="(c: any) => (editing!.status = c ? 1 : 0)"
          />
        </div>
      </div>
    </Modal>
  </Page>
</template>
