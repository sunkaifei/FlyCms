<script lang="ts" setup>
import type { BlockRow } from '#/api/core/block';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Input, InputNumber, message, Select, Textarea } from 'ant-design-vue';

import { saveBlockApi } from '#/api/core/block';

/**
 * 新增/编辑碎片位弹窗。blockKey 创建后锁定（模板调用依赖）。
 */
const TYPE_OPTIONS = [
  { label: '富文本', value: 0 },
  { label: '图片', value: 1 },
  { label: '推荐位列表', value: 2 },
  { label: '模板碎片', value: 3 },
];

const editing = ref<null | BlockRow>(null);
let onSaved: (() => void) | undefined;

const blockKey = ref('');
const blockName = ref('');
const blockType = ref(0);
const content = ref('');
const itemCount = ref(10);
const cacheSeconds = ref(0);
const sort = ref(0);
const status = ref(true);

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    if (!blockName.value.trim()) {
      message.warning('名称不能为空');
      return;
    }
    if (!editing.value && !blockKey.value.trim()) {
      message.warning('调用键不能为空');
      return;
    }
    modalApi.lock();
    try {
      await saveBlockApi({
        blockKey: blockKey.value,
        blockName: blockName.value,
        blockType: blockType.value,
        cacheSeconds: cacheSeconds.value,
        content: content.value,
        id: editing.value?.id,
        itemCount: itemCount.value,
        sort: sort.value,
        status: status.value ? 1 : 0,
      });
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '碎片位',
});

onMounted(() => {
  const data = modalApi.getData() as
    | { onSaved?: () => void; record?: BlockRow }
    | undefined;
  onSaved = data?.onSaved;
  if (data?.record) {
    editing.value = data.record;
    blockKey.value = data.record.blockKey;
    blockName.value = data.record.blockName;
    blockType.value = data.record.blockType;
    content.value = data.record.content || '';
    itemCount.value = data.record.itemCount;
    cacheSeconds.value = data.record.cacheSeconds;
    sort.value = data.record.sort;
    status.value = data.record.status === 1;
    modalApi.setState({ title: '编辑碎片位' });
  } else {
    modalApi.setState({ title: '新增碎片位' });
  }
});
</script>

<template>
  <Modal>
    <div class="grid grid-cols-2 gap-x-4 gap-y-3">
      <div>
        <div class="mb-1 text-sm">名称</div>
        <Input v-model:value="blockName" placeholder="如：首页焦点图" />
      </div>
      <div>
        <div class="mb-1 text-sm">调用键</div>
        <Input v-model:value="blockKey" :disabled="!!editing" placeholder="home_focus" />
      </div>
      <div>
        <div class="mb-1 text-sm">类型</div>
        <Select v-model:value="blockType" :options="TYPE_OPTIONS" class="w-full" />
      </div>
      <div>
        <div class="mb-1 text-sm">展示条数（推荐位）</div>
        <InputNumber v-model:value="itemCount" :min="1" class="w-full" />
      </div>
      <div class="col-span-2" v-if="blockType === 0 || blockType === 3">
        <div class="mb-1 text-sm">
          {{ blockType === 3 ? '模板内容（Freemarker 片段）' : '富文本内容' }}
        </div>
        <Textarea v-model:value="content" :rows="5" />
      </div>
      <div>
        <div class="mb-1 text-sm">排序</div>
        <InputNumber v-model:value="sort" class="w-full" />
      </div>
      <div class="flex items-end gap-2 pb-1">
        <input v-model="status" type="checkbox" id="blk-status" />
        <label for="blk-status" class="text-sm">显示</label>
      </div>
    </div>
  </Modal>
</template>
