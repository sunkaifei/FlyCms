<script lang="ts" setup>
import type { UserMessageRow } from '#/api/core/message';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Input,
  InputNumber,
  message,
  Modal,
  Pagination,
  Select,
  Switch,
  Table,
  Tag,
  Textarea,
} from 'ant-design-vue';

import {
  MESSAGE_TYPE_COLOR,
  MESSAGE_TYPE_TEXT,
  deleteMessageApi,
  getMessagePageApi,
  readMessageApi,
  sendMessageApi,
} from '#/api/core/message';
import FieldItem from '../_shared/field-item.vue';

defineOptions({ name: 'SystemMessage' });

const { hasAccessByCodes } = useAccess();

const canSend = computed(() => hasAccessByCodes(['/api/system/message/send']));
const canDelete = computed(() => hasAccessByCodes(['/api/system/message/delete']));
const canRead = computed(() => hasAccessByCodes(['/api/system/message/read']));

const rows = ref<UserMessageRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const selectedKeys = ref<string[]>([]);

const userId = ref<number | undefined>(undefined);
const msgType = ref<number | undefined>(undefined);
const isRead = ref<number | undefined>(undefined);
const keyword = ref('');

const TYPE_OPTIONS = [
  { label: '系统通知', value: 1 },
  { label: '审核通知', value: 2 },
  { label: '互动提醒', value: 3 },
];

const READ_OPTIONS = [
  { label: '未读', value: 0 },
  { label: '已读', value: 1 },
];

const columns = [
  { title: '接收用户', dataIndex: 'userId', key: 'userId', width: 140 },
  { title: '类型', dataIndex: 'msgType', key: 'msgType', width: 100 },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '发送者', dataIndex: 'senderName', key: 'senderName', width: 100 },
  { title: '状态', dataIndex: 'isRead', key: 'isRead', width: 80 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getMessagePageApi({
      isRead: isRead.value,
      keyword: keyword.value || undefined,
      msgType: msgType.value,
      p: page.value,
      rows: 20,
      userId: userId.value,
    });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

function reset() {
  userId.value = undefined;
  msgType.value = undefined;
  isRead.value = undefined;
  keyword.value = '';
  page.value = 1;
  load();
}

// /////////// 发送 ///////////

const sendVisible = ref(false);
const sendTitle = ref('');
const sendContent = ref('');
const sendUserId = ref<number | undefined>(undefined);
const sendUserIds = ref('');
const sendAll = ref(false);
const sending = ref(false);

function openSend() {
  sendTitle.value = '';
  sendContent.value = '';
  sendUserId.value = undefined;
  sendUserIds.value = '';
  sendAll.value = false;
  sendVisible.value = true;
}

async function confirmSend() {
  if (!sendTitle.value.trim()) {
    message.warning('请填写标题');
    return;
  }
  if (!sendAll.value && sendUserId.value === undefined && !sendUserIds.value.trim()) {
    message.warning('请指定接收用户，或勾选「发送给全体用户」');
    return;
  }
  sending.value = true;
  try {
    const res = await sendMessageApi({
      content: sendContent.value || undefined,
      title: sendTitle.value.trim(),
      userId: sendAll.value ? undefined : sendUserId.value,
      userIds: sendAll.value ? '' : sendUserIds.value.trim() || undefined,
    });
    message.success(`已发送 ${res ?? 0} 条站内信`);
    sendVisible.value = false;
    page.value = 1;
    await load();
  } catch (e: any) {
    message.error(e?.message ?? '发送失败');
  } finally {
    sending.value = false;
  }
}

// /////////// 删除 / 已读 ///////////

function onDelete(ids: string[]) {
  if (!ids.length) {
    message.warning('请先选择站内信');
    return;
  }
  Modal.confirm({
    content: `确认删除选中的 ${ids.length} 条站内信？删除后用户侧不再可见。`,
    okText: '确认删除',
    title: '删除站内信',
    onOk: async () => {
      try {
        await deleteMessageApi(ids);
        message.success('已删除');
        selectedKeys.value = [];
        await load();
      } catch (e: any) {
        message.error(e?.message ?? '删除失败');
      }
    },
  });
}

async function onRead() {
  if (!selectedKeys.value.length) {
    message.warning('请先选择站内信');
    return;
  }
  const target = rows.value.filter((r) => String(r.userId) && r.isRead === 0);
  if (!target.length) {
    message.info('选中的记录都已是已读');
    return;
  }
  try {
    await Promise.all(
      [...new Set(target.map((r) => r.userId))].map((uid) =>
        readMessageApi({ ids: selectedKeys.value.join(','), userId: uid }),
      ),
    );
    message.success('已标记为已读');
    selectedKeys.value = [];
    await load();
  } catch (e: any) {
    message.error(e?.message ?? '操作失败');
  }
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <InputNumber
        v-model:value="userId"
        :min="1"
        class="w-[180px]"
        placeholder="接收用户 ID"
      />
      <Select
        v-model:value="msgType"
        allow-clear
        class="w-[130px]"
        placeholder="类型"
        :options="TYPE_OPTIONS"
      />
      <Select
        v-model:value="isRead"
        allow-clear
        class="w-[110px]"
        placeholder="状态"
        :options="READ_OPTIONS"
      />
      <Input
        v-model:value="keyword"
        allow-clear
        class="w-[200px]"
        placeholder="标题/正文关键字"
        @press-enter="
          () => {
            page = 1;
            load();
          }
        "
      />
      <Button
        type="primary"
        @click="
          () => {
            page = 1;
            load();
          }
        "
      >
        查询
      </Button>
      <Button @click="reset">重置</Button>
      <Button v-if="canSend" class="ml-auto" type="primary" @click="openSend">
        发送站内信
      </Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      :row-selection="
        canDelete || canRead
          ? { selectedRowKeys: selectedKeys, onChange: (keys: any[]) => (selectedKeys = keys) }
          : undefined
      "
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'msgType'">
          <Tag :color="MESSAGE_TYPE_COLOR[record.msgType] ?? 'default'">
            {{ MESSAGE_TYPE_TEXT[record.msgType] ?? record.msgType }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'isRead'">
          <Tag :color="record.isRead === 1 ? 'default' : 'orange'">
            {{ record.isRead === 1 ? '已读' : '未读' }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'senderName'">
          {{ record.senderName || '系统' }}
        </template>
        <template v-else-if="column.key === 'userId'">
          <span class="font-mono text-xs">{{ record.userId }}</span>
        </template>
      </template>
    </Table>

    <div class="mt-3 flex items-center justify-between">
      <div v-if="canDelete || canRead" class="flex gap-2">
        <Button v-if="canRead" :disabled="!selectedKeys.length" @click="onRead">
          标记已读
        </Button>
        <Button v-if="canDelete" danger :disabled="!selectedKeys.length" @click="onDelete(selectedKeys)">
          删除
        </Button>
        <span v-if="selectedKeys.length" class="text-xs text-gray-400">
          已选 {{ selectedKeys.length }} 条
        </span>
      </div>
      <Pagination
        v-model:current="page"
        :page-size="20"
        :show-total="(t: number) => `共 ${t} 条`"
        :total="total"
        class="ml-auto"
        size="small"
        @change="load"
      />
    </div>

    <Modal
      v-model:open="sendVisible"
      :confirm-loading="sending"
      ok-text="发送"
      title="发送站内信"
      width="560"
      @ok="confirmSend"
    >
      <div class="grid grid-cols-2 gap-x-4 gap-y-1 pt-2">
        <FieldItem label="标题" name="title" required>
          <Input v-model:value="sendTitle" placeholder="一句话说明是什么事" />
        </FieldItem>
        <FieldItem label="发送给全体用户" name="sendAll" tips="勾选后忽略下方指定用户">
          <div class="pt-1">
            <Switch v-model:checked="sendAll" />
          </div>
        </FieldItem>
        <FieldItem label="指定用户 ID" name="userId">
          <InputNumber
            v-model:value="sendUserId"
            :disabled="sendAll"
            :min="1"
            class="w-full"
            placeholder="接收用户 user_id"
          />
        </FieldItem>
        <FieldItem label="多个用户 ID" name="userIds" tips="逗号分隔，最多 5000 人">
          <Input
            v-model:value="sendUserIds"
            :disabled="sendAll"
            placeholder="1001,1002,1003"
          />
        </FieldItem>
        <FieldItem label="正文" name="content" full tips="纯文本，按行分隔；前台不渲染 HTML">
          <Textarea
            v-model:value="sendContent"
            :rows="5"
            placeholder="例如：您的内容未通过审核，原因：含违规外链"
          />
        </FieldItem>
      </div>
    </Modal>
  </Page>
</template>
