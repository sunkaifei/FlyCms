<script lang="ts" setup>
import type { ModelFieldRow } from '#/api/core/model';

import { computed } from 'vue';

import {
  CheckboxGroup,
  DatePicker,
  Input,
  InputNumber,
  Rate,
  Select,
  Switch,
  Textarea,
} from 'ant-design-vue';

import AttachmentInput from './attachment-input.vue';

/**
 * 元数据驱动的单字段控件（P1 结构层抽出，顶层字段与 GROUP/REPEATER 子字段共用）。
 * 不含 editor（走主表 content 通道，由表单固定选项卡渲染）。
 * relate/relates/m2a 的候选项由父组件拉取后经 options 传入；
 * m2a 的 value 用 "model:id" 复合串（避免跨模型 id 冲突）。
 */
const props = defineProps<{
  /** 当前值（v-model） */
  modelValue?: any;
  field: ModelFieldRow;
  options?: { label: string; value: string }[];
}>();

const emit = defineEmits<(e: 'update:modelValue', v: any) => void>();

const inner = computed({
  get: () => props.modelValue,
  set: (v: any) => emit('update:modelValue', v),
});

/** 附件选择器要求字符串 id（结构体 JSON 里存的是数字，归一化） */
const attachmentValue = computed({
  get: () => (props.modelValue === undefined || props.modelValue === null ? '' : String(props.modelValue)),
  set: (v: any) => emit('update:modelValue', v),
});

function parseOptions(optionsJson?: string) {
  if (!optionsJson) return [];
  try {
    return JSON.parse(optionsJson).map((o: any) =>
      typeof o === 'string' ? { label: o, value: o } : o,
    );
  } catch {
    return optionsJson.split(',').map((s) => ({ label: s, value: s }));
  }
}

const controlOptions = computed(() => props.options ?? []);

function isSwitchChecked(v: any) {
  return v === true || v === '1' || v === 1;
}
</script>

<script lang="ts">
export default { inheritAttrs: false };
</script>

<template>
  <InputNumber
    v-if="field.fieldType === 'number'"
    v-model:value="inner"
    class="w-full"
    :placeholder="field.placeholder || field.fieldLabel"
  />
  <InputNumber
    v-else-if="field.fieldType === 'decimal'"
    v-model:value="inner"
    class="w-full"
    :placeholder="field.placeholder || field.fieldLabel"
    :step="0.01"
  />
  <Select
    v-else-if="field.fieldType === 'select' || field.fieldType === 'radio'"
    v-model:value="inner"
    allow-clear
    class="w-full"
    :options="parseOptions(field.options)"
    :placeholder="field.placeholder || field.fieldLabel"
  />
  <CheckboxGroup
    v-else-if="field.fieldType === 'checkbox'"
    v-model:value="inner"
    :options="parseOptions(field.options)"
  />
  <DatePicker
    v-else-if="field.fieldType === 'date'"
    v-model:value="inner"
    class="w-full"
    value-format="YYYY-MM-DD"
  />
  <DatePicker
    v-else-if="field.fieldType === 'datetime'"
    v-model:value="inner"
    show-time
    class="w-full"
    value-format="YYYY-MM-DD HH:mm:ss"
  />
  <AttachmentInput
    v-else-if="field.fieldType === 'image' || field.fieldType === 'file'"
    v-model:model-value="attachmentValue"
  />
  <AttachmentInput
    v-else-if="field.fieldType === 'images' || field.fieldType === 'files'"
    v-model:model-value="inner"
    multiple
  />
  <!-- E1 关联 / P2 任意关联：候选项由父组件传入 -->
  <Select
    v-else-if="field.fieldType === 'relate'"
    v-model:value="inner"
    allow-clear
    class="w-full"
    option-filter-prop="label"
    :options="controlOptions"
    :placeholder="`选择${field.fieldLabel}（${field.relateModel}）`"
    show-search
  />
  <Select
    v-else-if="field.fieldType === 'relates' || field.fieldType === 'm2a'"
    v-model:value="inner"
    allow-clear
    class="w-full"
    mode="multiple"
    option-filter-prop="label"
    :options="controlOptions"
    :placeholder="
      field.fieldType === 'm2a'
        ? `选择${field.fieldLabel}（可跨模型多选）`
        : `选择${field.fieldLabel}（${field.relateModel}）`
    "
    show-search
  />
  <!-- E3 直存 URL -->
  <div v-else-if="field.fieldType === 'image_url'" class="w-full">
    <Input
      v-model:value="inner"
      :placeholder="field.placeholder || '图片 URL，如 /upload/2026/09/a.png'"
    />
    <img
      v-if="inner"
      :alt="field.fieldLabel"
      class="mt-1 max-h-24 rounded border"
      :src="inner"
    />
  </div>
  <Input
    v-else-if="field.fieldType === 'file_url'"
    v-model:value="inner"
    :placeholder="field.placeholder || '附件 URL'"
  />
  <!-- P0：switch/rating/color -->
  <Switch
    v-else-if="field.fieldType === 'switch'"
    :checked="isSwitchChecked(inner)"
    @change="(checked: any) => emit('update:modelValue', checked ? '1' : '0')"
  />
  <Rate
    v-else-if="field.fieldType === 'rating'"
    v-model:value="inner"
    allow-clear
  />
  <div v-else-if="field.fieldType === 'color'" class="flex items-center gap-2">
    <input
      type="color"
      class="h-8 w-12 cursor-pointer rounded border"
      :value="inner || '#1677ff'"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <Input v-model:value="inner" class="w-40" placeholder="#RRGGBB" />
  </div>
  <Textarea
    v-else-if="field.fieldType === 'textarea'"
    v-model:value="inner"
    :placeholder="field.placeholder || field.fieldLabel"
    :rows="3"
  />
  <Input
    v-else
    v-model:value="inner"
    :maxlength="field.maxlength || undefined"
    :placeholder="field.placeholder || field.fieldLabel"
  />
</template>
