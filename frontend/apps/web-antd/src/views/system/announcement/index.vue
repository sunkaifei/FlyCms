<script lang="ts" setup>
import type { AnnouncementRow } from '#/api/core/message';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, message, Modal, Pagination, Switch, Table, Textarea } from 'ant-design-vue';

import {
  deleteAnnouncementApi,
  getAnnouncementListApi,
  updateAnnouncementApi,
} from '#/api/core/message';

defineOptions({ name: 'SystemAnnouncement' });

const { hasAccessByCodes } = useAccess();
const canSave = hasAccessByCodes(['/api/system/announcement/save']);

const rows = ref<AnnouncementRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);

/** 行内编辑态（单行轻编辑：标题/排序） */
const editing = ref<null | string>(null);

// 复杂表单仍走简单弹窗（内容 Textarea + 时间输入）
const editOpen = ref(false);
const editId = ref('');
const editTitle = ref('');
const editContent = ref('');
const editLink = ref('');
const editStart = ref('');
const editEnd = ref('');
const editSort = ref(0);

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '链接', dataIndex: 'linkUrl', key: 'linkUrl' },
  { title: '上线时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
  { title: '下线时间', dataIndex: 'endTime', key: 'endTime', width: 160 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  { title: '显示', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 150 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getAnnouncementListApi({ p: page.value });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

function openAdd() {
  editId.value = '';
  editTitle.value = '';
  editContent.value = '';
  editLink.value = '';
  editStart.value = '';
  editEnd.value = '';
  editSort.value = 0;
  editOpen.value = true;
}

function openEdit(row: any) {
  editId.value = String(row.id);
  editTitle.value = row.title;
  editContent.value = row.content || '';
  editLink.value = row.linkUrl || '';
  editStart.value = (row.startTime || '').replace('T', ' ').slice(0, 19);
  editEnd.value = (row.endTime || '').replace('T', ' ').slice(0, 19);
  editSort.value = row.sort;
  editOpen.value = true;
}

async function onSave() {
  if (!editTitle.value.trim()) {
    message.warning('公告标题不能为空');
    return;
  }
  const payload = {
    content: editContent.value,
    endTime: editEnd.value,
    linkUrl: editLink.value,
    startTime: editStart.value,
    sort: editSort.value,
    status: 1,
    title: editTitle.value,
  };
  if (editId.value) {
    await updateAnnouncementApi({ ...payload, id: editId.value });
  } else {
    const { saveAnnouncementApi } = await import('#/api/core/message');
    await saveAnnouncementApi(payload);
  }
  message.success('保存成功');
  editOpen.value = false;
  load();
}

function onDelete(row: any) {
  Modal.confirm({
    content: `删除公告「${row.title}」？`,
    onOk: async () => {
      await deleteAnnouncementApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

async function onToggle(row: any, checked: boolean) {
  await updateAnnouncementApi({
    endTime: row.endTime || '',
    id: row.id,
    linkUrl: row.linkUrl || '',
    sort: row.sort,
    startTime: row.startTime || '',
    status: checked ? 1 : 0,
    title: row.title,
  });
  message.success(checked ? '已显示' : '已隐藏');
  load();
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="mb-3 flex items-center gap-2">
      <span class="text-sm text-gray-400">
        前台模板用 &lt;@fly_announcement_model rows="5"&gt; 调用，仅显示时间窗内的公告
      </span>
      <Button
        v-if="canSave"
        class="ml-auto"
        type="primary"
        @click="openAdd"
      >
        新增公告
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
        <template v-if="column.key === 'title'">
          {{ record.title }}
          <Textarea
            v-if="editing === String(record.id)"
            v-model:value="editTitle"
            :rows="1"
            class="mt-1"
          />
        </template>
        <template v-else-if="column.key === 'sort'">
          <template v-if="editing === String(record.id)">
            <Input v-model:value="editSort" class="w-[70px]" type="number" />
          </template>
          <template v-else>{{ record.sort }}</template>
        </template>
        <template v-else-if="column.key === 'status'">
          <Switch
            :checked="Number(record.status) === 1"
            :disabled="!canSave"
            size="small"
            @change="(checked: any) => onToggle(record, Boolean(checked))"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <template v-if="editing === String(record.id)">
            <Button
              class="mr-1 px-2"
              size="small"
              type="link"
              @click="
                () => {
                  editing = null;
                  load();
                }
              "
            >
              取消
            </Button>
            <Button
              class="px-2"
              size="small"
              type="link"
              @click="
                () => {
                  record.title = editTitle;
                  record.sort = Number(editSort);
                  editing = null;
                  updateAnnouncementApi({
                    content: record.content || '',
                    endTime: record.endTime || '',
                    id: record.id,
                    linkUrl: record.linkUrl || '',
                    sort: Number(editSort),
                    startTime: record.startTime || '',
                    status: Number(record.status),
                    title: editTitle,
                  });
                  message.success('已保存');
                }
              "
            >
              保存
            </Button>
          </template>
          <template v-else>
            <Button
              class="mr-1 px-2"
              size="small"
              type="link"
              @click="
                () => {
                  editing = String(record.id);
                  editTitle = record.title;
                  editSort = record.sort;
                }
              "
            >
              快改
            </Button>
            <Button
              v-if="canSave"
              class="mr-1 px-2"
              size="small"
              type="link"
              @click="openEdit(record)"
            >
              编辑
            </Button>
            <Button
              v-if="hasAccessByCodes(['/api/system/announcement/delete'])"
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
        <template v-else-if="column.key === 'startTime' || column.key === 'endTime'">
          {{ (record[column.key] || '-').replace('T', ' ').slice(0, 16) }}
        </template>
        <template v-else>
          {{ record[column.key as string] || '-' }}
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
      v-model:open="editOpen"
      :title="editId ? '编辑公告' : '新增公告'"
      width="720px"
      @ok="onSave"
    >
      <div class="grid grid-cols-1 gap-y-3">
        <div>
          <div class="mb-1 text-sm">公告标题</div>
          <Input v-model:value="editTitle" :maxlength="200" />
        </div>
        <div>
          <div class="mb-1 text-sm">跳转链接（空=仅展示内容）</div>
          <Input v-model:value="editLink" placeholder="https://..." />
        </div>
        <div>
          <div class="mb-1 text-sm">公告内容</div>
          <Textarea v-model:value="editContent" :rows="5" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <div class="mb-1 text-sm">上线时间（空=立即）</div>
            <Input v-model:value="editStart" placeholder="yyyy-MM-dd HH:mm:ss" />
          </div>
          <div>
            <div class="mb-1 text-sm">下线时间（空=长期）</div>
            <Input v-model:value="editEnd" placeholder="yyyy-MM-dd HH:mm:ss" />
          </div>
          <div>
            <div class="mb-1 text-sm">排序</div>
            <Input v-model:value="editSort" type="number" />
          </div>
        </div>
      </div>
    </Modal>
  </Page>
</template>
