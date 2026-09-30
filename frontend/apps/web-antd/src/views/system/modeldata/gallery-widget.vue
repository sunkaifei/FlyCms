<script lang="ts" setup>
import { computed, ref, watch } from 'vue';

import { message } from 'ant-design-vue';

import { getAttachmentBatchApi } from '#/api/core/images';

import AttachmentPickerModal from './attachment-picker-modal.vue';

/**
 * 多图/多附件网格控件（W 批次）：缩略图网格 + HTML5 拖拽排序 + 数量上限 + 批量上传。
 * 存储：fly_images.id 数组 JSON（顺序即数组序，前台 {f}Urls 顺序一致）。
 */
const props = defineProps<{
  maxCount?: number;
  modelValue?: string;
  multiple?: boolean;
  sortMode?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

interface Item {
  id: string;
  name?: string;
  url?: string;
}

const pickerOpen = ref(false);
const items = ref<Item[]>([]);
const dragIndex = ref(-1);
const overIndex = ref(-1);

const limit = computed(() => props.maxCount ?? 20);
const draggable = computed(() => (props.sortMode ?? 'drag') === 'drag');

watch(
  () => props.modelValue,
  async (raw) => {
    let ids: string[] = [];
    try {
      ids = raw ? (JSON.parse(raw) as string[]) : [];
    } catch {
      ids = raw ? raw.split(',') : [];
    }
    // 与当前已回显的一致时跳过（避免拖拽排序后被 watch 重置）
    if (ids.join(',') === items.value.map((i) => i.id).join(',')) return;
    items.value = ids.map((id) => ({ id }));
    if (ids.length) {
      try {
        const list = (await getAttachmentBatchApi(ids)) ?? [];
        const map = new Map(list.map((r) => [String(r.id), r]));
        items.value = ids.map((id) => ({
          id,
          name: map.get(id)?.imgName,
          url: map.get(id)?.imgUrl,
        }));
      } catch {
        /* 回显失败退化为 id 占位 */
      }
    }
  },
  { immediate: true },
);

function persist() {
  emit('update:modelValue', JSON.stringify(items.value.map((i) => i.id)));
}

function onPicked(list: { id: string; name?: string; url?: string }[]) {
  const room = limit.value - items.value.length;
  if (room <= 0) {
    message.warning(`数量已达上限 ${limit.value} 张`);
    return;
  }
  const adding = list.slice(0, room);
  if (list.length > room) {
    message.warning(`最多还能添加 ${room} 张，已截取前 ${room} 张`);
  }
  const exist = new Set(items.value.map((i) => i.id));
  items.value.push(...adding.filter((a) => !exist.has(a.id)));
  persist();
}

function removeAt(index: number) {
  items.value.splice(index, 1);
  persist();
}

function onDragStart(index: number) {
  dragIndex.value = index;
}

function onDragOver(index: number) {
  overIndex.value = index;
}

function onDrop(index: number) {
  if (dragIndex.value < 0 || dragIndex.value === index) {
    dragIndex.value = -1;
    overIndex.value = -1;
    return;
  }
  const moved = items.value.splice(dragIndex.value, 1)[0];
  if (!moved) return;
  items.value.splice(index, 0, moved);
  dragIndex.value = -1;
  overIndex.value = -1;
  persist();
}
</script>

<template>
  <div class="w-full">
    <div class="flex flex-wrap gap-2">
      <div
        v-for="(item, index) in items"
        :key="item.id"
        :class="{
          'opacity-60': dragIndex === index,
          'border-blue-500': overIndex === index && dragIndex !== index,
        }"
        class="group relative rounded border"
        :draggable="draggable"
        @dragend="onDrop(overIndex)"
        @dragover.prevent="onDragOver(index)"
        @dragstart="onDragStart(index)"
        @drop.prevent="onDrop(index)"
      >
        <img
          v-if="item.url"
          :alt="item.name"
          :src="item.url"
          class="h-20 w-20 rounded object-cover"
        />
        <div
          v-else
          class="flex h-20 w-20 items-center justify-center rounded bg-gray-100 p-1 text-center text-[10px] text-gray-500"
        >
          {{ item.name || item.id }}
        </div>
        <span
          v-if="draggable && item.url"
          class="absolute left-1 top-1 cursor-move rounded bg-black/40 px-1 text-[10px] text-white"
          title="拖拽排序"
        >
          ⠿
        </span>
        <span
          class="absolute -right-1.5 -top-1.5 flex h-4 w-4 cursor-pointer items-center justify-center rounded-full bg-red-500 text-[10px] leading-none text-white"
          @click="removeAt(index)"
        >
          ×
        </span>
      </div>
      <div
        v-if="items.length < limit"
        class="flex h-20 w-20 cursor-pointer flex-col items-center justify-center rounded border border-dashed text-xs text-gray-400 hover:border-blue-400 hover:text-blue-500"
        @click="pickerOpen = true"
      >
        <span class="text-lg leading-none">＋</span>
        <span class="mt-1">添加</span>
      </div>
    </div>
    <div class="mt-1 text-xs text-gray-400">
      {{ items.length }}/{{ limit }}
      {{ draggable ? '（可拖拽排序）' : '' }}
    </div>
    <AttachmentPickerModal
      v-model:open="pickerOpen"
      :max-size="0"
      :multiple="true"
      @picked="onPicked"
    />
  </div>
</template>
