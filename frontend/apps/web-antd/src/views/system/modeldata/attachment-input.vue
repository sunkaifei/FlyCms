<script lang="ts" setup>
import { computed, ref } from 'vue';

import { Button, Input, Modal, Pagination } from 'ant-design-vue';

import { getAttachmentListApi } from '#/api/core/model';

/**
 * 附件引用输入（image/file 单值；images/files 多值 JSON 数组字符串）。
 * 绑定契约：vben 表单未注册自定义组件走默认 modelValue（手册坑 15）。
 * MVP：回显缩略图 + 从附件库（fly_images）分页选择；上传走既有 UpLoad 通道后录入 id。
 */
const props = defineProps<{
  multiple?: boolean;
  modelValue?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const pickerOpen = ref(false);
const pickerPage = ref(1);
const pickerRows = ref<{ id: number; imgUrl?: string; imgName?: string }[]>([]);
const pickerTotal = ref(0);
const pickedUrlMap = ref<Record<number, string>>({});

const multiIds = computed<string[]>(() => {
  if (!props.multiple || !props.modelValue) return [];
  try {
    return JSON.parse(props.modelValue);
  } catch {
    return props.modelValue.split(',');
  }
});

const previewIds = computed<string[]>(() => {
  if (props.multiple) return multiIds.value;
  return props.modelValue ? [props.modelValue] : [];
});

function urlOf(id: string): string {
  return pickedUrlMap.value[Number(id)] || '';
}

async function loadPicker() {
  const res = await getAttachmentListApi({ p: pickerPage.value });
  pickerRows.value = res.list ?? [];
  pickerTotal.value = res.count ?? 0;
}

function openPicker() {
  pickerOpen.value = true;
  loadPicker();
}

function togglePick(id: number, url?: string) {
  if (url) pickedUrlMap.value[id] = url;
  if (props.multiple) {
    const ids = new Set(multiIds.value);
    if (ids.has(String(id))) {
      ids.delete(String(id));
    } else {
      ids.add(String(id));
    }
    emit('update:modelValue', JSON.stringify([...ids]));
  } else {
    emit('update:modelValue', String(id));
    pickerOpen.value = false;
  }
}

function removeOne(id: string) {
  if (props.multiple) {
    const ids = multiIds.value.filter((x) => x !== id);
    emit('update:modelValue', JSON.stringify(ids));
  } else {
    emit('update:modelValue', '');
  }
}
</script>

<template>
  <div class="w-full">
    <div v-if="previewIds.length" class="mb-1 flex flex-wrap gap-2">
      <div
        v-for="id in previewIds"
        :key="id"
        class="relative inline-block"
      >
        <img
          v-if="urlOf(id)"
          :src="urlOf(id)"
          class="h-14 w-14 rounded border object-cover"
        />
        <span
          v-else
          class="flex h-14 w-14 items-center justify-center rounded border bg-gray-50 text-xs text-gray-500"
        >
          {{ id }}
        </span>
        <span
          class="absolute -right-1 -top-1 flex h-4 w-4 cursor-pointer items-center justify-center rounded-full bg-red-500 text-[10px] text-white"
          @click="removeOne(id)"
        >
          ×
        </span>
      </div>
    </div>
    <div class="flex items-center gap-2">
      <Input
        :placeholder="multiple ? '附件ID（JSON数组）' : '附件ID（fly_images.id）'"
        :value="modelValue"
        @update:value="(v: string) => emit('update:modelValue', v)"
      />
      <Button type="primary" @click="openPicker">选择</Button>
    </div>

    <Modal
      v-model:open="pickerOpen"
      :footer="null"
      title="选择附件（fly_images）"
      width="720px"
    >
      <div class="mb-2 flex flex-wrap gap-2">
        <div
          v-for="row in pickerRows"
          :key="row.id"
          class="cursor-pointer rounded border p-1 hover:border-blue-400"
          :class="{
            'border-blue-500 ring-1 ring-blue-400':
              (multiple && multiIds.includes(String(row.id))) ||
              (!multiple && modelValue === String(row.id)),
          }"
          @click="togglePick(row.id, row.imgUrl)"
        >
          <img
            v-if="row.imgUrl"
            :src="row.imgUrl"
            class="h-20 w-20 rounded object-cover"
          />
          <div
            v-else
            class="flex h-20 w-20 items-center justify-center rounded bg-gray-100 text-xs"
          >
            {{ row.imgName || row.id }}
          </div>
          <div class="w-20 truncate text-center text-xs text-gray-500">
            ID: {{ row.id }}
          </div>
        </div>
        <div v-if="pickerRows.length === 0" class="p-4 text-gray-400">
          附件库为空
        </div>
      </div>
      <Pagination
        v-model:current="pickerPage"
        :page-size="20"
        :total="pickerTotal"
        size="small"
        @change="loadPicker"
      />
    </Modal>
  </div>
</template>
