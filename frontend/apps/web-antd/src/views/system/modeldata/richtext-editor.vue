<script lang="ts" setup>
import { watch } from 'vue';

import { VbenTiptap } from '@vben/plugins/tiptap';

/**
 * editor 字段的富文本编辑器封装（VbenTiptap，标准 modelValue 绑定）。
 * 作为 vben 表单未注册自定义组件使用（坑 15：schema 字段需 modelPropName: 'modelValue'）。
 */
const props = defineProps<{ modelValue?: string }>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

function onChange(v: string) {
  emit('update:modelValue', v);
}

// 内容异步回显（编辑弹窗 setValues 晚于挂载）
watch(
  () => props.modelValue,
  () => {},
  { immediate: true },
);
</script>

<template>
  <div class="w-full border rounded-md p-1">
    <VbenTiptap
      :model-value="modelValue ?? ''"
      @update:model-value="onChange"
    />
  </div>
</template>
