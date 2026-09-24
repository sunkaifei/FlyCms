<script lang="ts" setup>
import type { ImageRow } from '#/api/core/images';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, Input, message, Modal, Pagination, Table } from 'ant-design-vue';

import {
  deleteOrphanImagesApi,
  getImagesPageApi,
  getOrphanCountApi,
} from '#/api/core/images';

defineOptions({ name: 'SystemImages' });

const { hasAccessByCodes } = useAccess();

const rows = ref<ImageRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const keyword = ref('');
const onlyOrphan = ref(false);
const orphanCount = ref(0);

const columns = [
  { title: '预览', key: 'preview', width: 90 },
  { title: '文件名', dataIndex: 'imgName', key: 'imgName' },
  { title: '地址', dataIndex: 'imgUrl', key: 'imgUrl' },
  { title: '被引次数', dataIndex: 'infoCount', key: 'infoCount', width: 100 },
  { title: '上传时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getImagesPageApi({
      keyword: keyword.value || undefined,
      onlyOrphan: onlyOrphan.value ? 1 : 0,
      p: page.value,
      rows: 20,
    });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
    const cnt = await getOrphanCountApi();
    orphanCount.value = cnt.count ?? 0;
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  page.value = 1;
  load();
}

function onCleanOrphan() {
  if (orphanCount.value <= 0) {
    message.info('没有孤儿附件');
    return;
  }
  Modal.confirm({
    content: `确认清理 ${orphanCount.value} 个未被任何内容引用的附件？该操作只删除数据库记录，不影响磁盘文件。`,
    onOk: async () => {
      await deleteOrphanImagesApi();
      message.success('清理完成');
      load();
    },
    title: '清理孤儿附件',
  });
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Input
        v-model:value="keyword"
        allow-clear
        class="w-56"
        placeholder="文件名/描述关键词"
        @press-enter="onSearch"
      />
      <Button @click="onSearch">搜索</Button>
      <label class="ml-2 flex items-center gap-1 text-sm">
        <input v-model="onlyOrphan" type="checkbox" @change="onSearch" />
        只看孤儿附件
      </label>

      <div v-if="hasAccessByCodes(['/api/system/images/deleteOrphan'])" class="ml-auto">
        <Button danger size="small" @click="onCleanOrphan">
          清理孤儿附件（{{ orphanCount }}）
        </Button>
      </div>
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
        <template v-if="column.key === 'preview'">
          <img
            :src="record.imgUrl"
            class="h-10 w-16 rounded border object-cover"
            alt=""
          />
        </template>
        <template v-else-if="column.key === 'imgUrl'">
          <span class="text-xs text-gray-500">{{ record.imgUrl }}</span>
        </template>
        <template v-else-if="column.key === 'infoCount'">
          <span :class="record.infoCount > 0 ? '' : 'text-red-500'">
            {{ record.infoCount ?? 0 }}
          </span>
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
  </Page>
</template>
