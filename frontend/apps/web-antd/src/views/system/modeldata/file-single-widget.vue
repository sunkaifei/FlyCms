<script lang="ts" setup>
import { ref, watch } from 'vue';

import { Button } from 'ant-design-vue';

import { getAttachmentBatchApi } from '#/api/core/images';

import AttachmentPickerModal from './attachment-picker-modal.vue';

/**
 * 单文件卡片控件（W 批次）：文件图标 + 文件名 + 更换/删除。存储：fly_images.id。
 */
const props = defineProps<{
  maxSize?: number;
  modelValue?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const pickerOpen = ref(false);
const name = ref('');
const url = ref('');

watch(
  () => props.modelValue,
  async (id) => {
    name.value = '';
    url.value = '';
    if (!id) return;
    try {
      const list = (await getAttachmentBatchApi([id])) ?? [];
      name.value = list[0]?.imgName || String(id);
      url.value = list[0]?.imgUrl ?? '';
    } catch {
      name.value = String(id);
    }
  },
  { immediate: true },
);
</script>

<template>
  <div class="w-full">
    <div
      v-if="modelValue"
      class="flex w-full max-w-md items-center gap-2 rounded border px-3 py-2"
    >
      <span class="text-lg leading-none">📄</span>
      <a
        v-if="url"
        :href="url"
        class="truncate text-sm text-blue-600"
        target="_blank"
      >
        {{ name }}
      </a>
      <span v-else class="truncate text-sm">{{ name }}</span>
      <span class="ml-auto flex items-center gap-1">
        <Button size="small" type="link" @click="pickerOpen = true">更换</Button>
        <Button
          danger
          size="small"
          type="link"
          @click="emit('update:modelValue', '')"
        >
          删除
        </Button>
      </span>
    </div>
    <Button v-else class="w-full max-w-md" type="dashed" @click="pickerOpen = true">
      点击上传附件
    </Button>
    <AttachmentPickerModal
      v-model:open="pickerOpen"
      :max-size="maxSize"
      @picked="
        (list) => {
          if (list[0]) emit('update:modelValue', list[0].id);
        }
      "
    />
  </div>
</template>
