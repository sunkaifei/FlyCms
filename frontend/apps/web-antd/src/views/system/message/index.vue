<script lang="ts" setup>
import type { MessageRow } from '#/api/core/message';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import { Button, Input, message, Modal, Pagination, Table } from 'ant-design-vue';

import { deleteMessageApi, getMessageListApi } from '#/api/core/message';

import SendModal from './send-modal.vue';

defineOptions({ name: 'SystemMessage' });

const { hasAccessByCodes } = useAccess();

const rows = ref<MessageRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const subject = ref('');

const [SendModalComp, sendModalApi] = useEditDrawer({
  connectedComponent: SendModal,
  destroyOnClose: true,
});

const columns = [
  { title: '发送时间', dataIndex: 'sendTime', key: 'sendTime', width: 160 },
  { title: '发件人', dataIndex: 'fromNickname', key: 'fromNickname', width: 110 },
  { title: '收件人', dataIndex: 'toNickname', key: 'toNickname', width: 110 },
  { title: '标题', dataIndex: 'subject', key: 'subject' },
  {
    title: '类型',
    dataIndex: 'isAdmin',
    key: 'isAdmin',
    width: 80,
    customRender: ({ text }: any) => (Number(text) === 1 ? '系统信息' : '用户私信'),
  },
  {
    title: '已读',
    dataIndex: 'hasView',
    key: 'hasView',
    width: 70,
    customRender: ({ text }: any) => (Number(text) === 1 ? '已读' : '未读'),
  },
  { title: '操作', key: 'action', width: 80 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getMessageListApi({
      p: page.value,
      subject: subject.value || undefined,
    });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

function openSend() {
  sendModalApi.setData({ onSaved: load }).open();
}

function onDelete(row: any) {
  Modal.confirm({
    content: `删除站内信「${row.subject}」？`,
    onOk: async () => {
      await deleteMessageApi(String(row.id));
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
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Input
        v-model:value="subject"
        allow-clear
        class="w-[240px]"
        placeholder="标题关键词"
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
      <Button
        v-if="hasAccessByCodes(['/api/system/message/send'])"
        class="ml-auto"
        type="primary"
        @click="openSend"
      >
        发送站内信
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
        <template v-if="column.key === 'action'">
          <Button
            v-if="hasAccessByCodes(['/api/system/message/delete'])"
            class="px-2"
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
    <SendModalComp />
  </Page>
</template>
