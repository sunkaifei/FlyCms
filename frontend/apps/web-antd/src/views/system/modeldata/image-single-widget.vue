<script lang="ts" setup>
import { ref, watch } from 'vue';

import { Image as AImage } from 'ant-design-vue';

import { getAttachmentBatchApi } from '#/api/core/images';

import AttachmentPickerModal from './attachment-picker-modal.vue';

/**
 * 单图头像式控件（W 批次）：空态虚线框点击上传/选图；有值大缩略图 + 悬停遮罩（更换/删除）。
 * 存储：fly_images.id（bigint 字符串）。回显走批量映射接口（修刷新丢图）。
 */
const props = defineProps<{
  maxSize?: number;
  modelValue?: string;
  shape?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const pickerOpen = ref(false);
const url = ref('');

watch(
  () => props.modelValue,
  async (id) => {
    url.value = '';
    if (!id) return;
    try {
      const list = (await getAttachmentBatchApi([id])) ?? [];
      url.value = list[0]?.imgUrl ?? '';
    } catch {
      url.value = '';
    }
  },
  { immediate: true },
);
</script>

<template>
  <div class="flex items-center gap-3">
    <div v-if="modelValue" class="group relative">
      <AImage
        :height="96"
        :preview="true"
        :src="url"
        :width="shape === 'circle' ? 96 : 128"
        class="cursor-pointer border object-cover"
        :class="shape === 'circle' ? 'rounded-full' : 'rounded-lg'"
        fallback="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iOTYiPjxyZWN0IHdpZHRoPSIxMjgiIGhlaWdodD0iOTYiIGZpbGw9IiNlZWUiLz48L3N2Zz4="
      />
      <div
        class="absolute inset-0 flex items-center justify-center gap-2 rounded-lg bg-black/50 opacity-0 transition group-hover:opacity-100"
        :class="shape === 'circle' ? 'rounded-full' : 'rounded-lg'"
      >
        <span
          class="cursor-pointer text-xs text-white underline"
          @click.stop="pickerOpen = true"
        >
          更换
        </span>
        <span
          class="cursor-pointer text-xs text-red-300 underline"
          @click.stop="emit('update:modelValue', '')"
        >
          删除
        </span>
      </div>
    </div>
    <div
      v-else
      class="flex h-24 w-32 cursor-pointer flex-col items-center justify-center rounded-lg border border-dashed text-xs text-gray-400 hover:border-blue-400 hover:text-blue-500"
      :class="shape === 'circle' ? 'rounded-full' : ''"
      @click="pickerOpen = true"
    >
      <span class="text-lg leading-none">＋</span>
      <span class="mt-1">点击上传</span>
    </div>
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
