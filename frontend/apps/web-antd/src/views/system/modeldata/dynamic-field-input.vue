<script lang="ts" setup>
import type { ModelFieldRow } from '#/api/core/model';

import { computed } from 'vue';

import {
  CheckboxGroup,
  DatePicker,
  Input,
  InputNumber,
  RangePicker,
  Rate,
  RadioGroup,
  Select,
  Switch,
  Textarea,
  TreeSelect,
} from 'ant-design-vue';

import FileSingleWidget from './file-single-widget.vue';
import GalleryWidget from './gallery-widget.vue';
import ImageSingleWidget from './image-single-widget.vue';

/**
 * 元数据驱动的单字段控件（P1 结构层抽出，顶层字段与 GROUP/REPEATER 子字段共用）。
 * 不含 editor（走主表 content 通道，由表单固定选项卡渲染）。
 * relate/relates/m2a 的候选项由父组件拉取后经 options 传入；
 * m2a 的 value 用 "model:id" 复合串（避免跨模型 id 冲突）。
 *
 * W 批次控件层：widget_conf（白名单 JSON）驱动控件形态——
 * 附件族三件套（单图头像式/文件卡片/网格画廊+拖拽排序）、时间格式/范围、
 * textarea 行数与计数、前后缀、选项布局、开关文案、星数、色板。
 */
const props = defineProps<{
  /** 当前值（v-model） */
  modelValue?: any;
  field: ModelFieldRow;
  /** relate/relates/m2a/user 的候选项（label/value） */
  options?: { label: string; value: string }[];
  /** 字典候选项（若依式 dictType 绑定，优先于 field.options） */
  dictOptions?: { label: string; value: string }[];
  /** category 的分类树（B1 绑定数据源，父组件按绑定模型组好树） */
  treeData?: any[];
}>();

const emit = defineEmits<(e: 'update:modelValue', v: any) => void>();

const inner = computed({
  get: () => props.modelValue,
  set: (v: any) => emit('update:modelValue', v),
});

/** 控件配置（widget_conf JSON 解析，坏 JSON 静默回默认） */
const conf = computed<Record<string, any>>(() => {
  try {
    return props.field.widgetConf ? JSON.parse(props.field.widgetConf) : {};
  } catch {
    return {};
  }
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

/** select/radio/checkbox 候选项：绑了字典用字典（字典无数据时回退），否则字段自带 options */
const choiceOptions = computed(() =>
  props.field.dictType && props.dictOptions?.length
    ? props.dictOptions
    : parseOptions(props.field.options),
);

/** W：select/radio 选项布局（默认下拉；row/column/button 用单选组渲染） */
const selectLayout = computed(() => String(conf.value.layout ?? 'select'));
const radioStyleMode = computed(() =>
  ['row', 'column', 'button'].includes(selectLayout.value),
);

function isSwitchChecked(v: any) {
  return v === true || v === '1' || v === 1;
}

/** W：色板色块点击（conf.palette = JSON 颜色数组） */
const palette = computed<string[]>(() => {
  const p = conf.value.palette;
  if (!p) return [];
  try {
    const arr = typeof p === 'string' ? JSON.parse(p) : p;
    return Array.isArray(arr) ? arr.map(String) : [];
  } catch {
    return [];
  }
});

/** W：date 范围值归一化（RangePicker 数组 ↔ 表单字符串存储 json） */
const rangeValue = computed<any>({
  get: () => {
    const raw = props.modelValue;
    if (!raw) return undefined;
    try {
      const arr = typeof raw === 'string' ? JSON.parse(raw) : raw;
      return Array.isArray(arr) && arr.length === 2 ? arr : undefined;
    } catch {
      return undefined;
    }
  },
  set: (v: any) => {
    if (Array.isArray(v) && v.length === 2) {
      emit('update:modelValue', JSON.stringify(v));
    } else {
      emit('update:modelValue', '');
    }
  },
});

/** W：date 配置格式 → 精度（月/年选择器） */
const datePickerPrecision = computed(() => {
  const fmt = String(conf.value.format ?? 'YYYY-MM-DD');
  return fmt === 'YYYY-MM' ? 'month' : fmt === 'YYYY' ? 'year' : 'date';
});
</script>

<script lang="ts">
export default { inheritAttrs: false };
</script>

<template>
  <InputNumber
    v-if="field.fieldType === 'number'"
    v-model:value="inner"
    :addon-after="conf.addonAfter"
    :placeholder="field.placeholder || field.fieldLabel"
    :precision="0"
    class="w-full"
  />
  <InputNumber
    v-else-if="field.fieldType === 'decimal'"
    :addon-after="conf.addonAfter"
    :placeholder="field.placeholder || field.fieldLabel"
    :step="conf.step ?? 0.01"
    class="w-full"
    v-model:value="inner"
  />
  <!-- W：select/radio 选项布局（row/column/button 用单选组；默认下拉） -->
  <RadioGroup
    v-else-if="
      (field.fieldType === 'select' || field.fieldType === 'radio') &&
      radioStyleMode
    "
    v-model:value="inner"
    :option-type="selectLayout === 'button' ? 'button' : 'default'"
    :options="choiceOptions"
    :class="selectLayout === 'column' ? 'flex flex-col gap-1' : ''"
  />
  <Select
    v-else-if="field.fieldType === 'select' || field.fieldType === 'radio'"
    v-model:value="inner"
    :options="choiceOptions"
    :placeholder="field.placeholder || `请选择${field.fieldLabel}`"
    class="w-full"
    allow-clear
  />
  <!-- W：checkbox 标签组布局 -->
  <CheckboxGroup
    v-else-if="field.fieldType === 'checkbox' && conf.layout === 'button'"
    v-model:value="inner"
    :options="choiceOptions"
    option-type="button"
  />
  <CheckboxGroup
    v-else-if="field.fieldType === 'checkbox'"
    v-model:value="inner"
    :options="choiceOptions"
  />
  <!-- W：date/datetime 格式配置（月/年精度联动、到分/到秒） -->
  <DatePicker
    v-else-if="field.fieldType === 'date'"
    v-model:value="inner"
    :picker="datePickerPrecision"
    :value-format="conf.format || 'YYYY-MM-DD'"
    class="w-full"
  />
  <DatePicker
    v-else-if="field.fieldType === 'datetime'"
    v-model:value="inner"
    :show-time="{ format: String(conf.format || 'YYYY-MM-DD HH:mm').includes('ss') ? 'HH:mm:ss' : 'HH:mm' }"
    :value-format="conf.format || 'YYYY-MM-DD HH:mm'"
    class="w-full"
  />
  <!-- W：时间范围（新类型，存 ["start","end"] JSON） -->
  <RangePicker
    v-else-if="field.fieldType === 'date_range'"
    v-model:value="rangeValue"
    class="w-full"
    value-format="YYYY-MM-DD"
  />
  <RangePicker
    v-else-if="field.fieldType === 'datetime_range'"
    v-model:value="rangeValue"
    class="w-full"
    show-time
    value-format="YYYY-MM-DD HH:mm:ss"
  />
  <!-- W 附件族：单图头像式 / 单文件卡片 / 网格画廊（拖拽排序+上限+直传） -->
  <ImageSingleWidget
    v-else-if="field.fieldType === 'image'"
    v-model:model-value="attachmentValue"
    :max-size="Number(conf.maxSize) || 0"
    :shape="conf.shape"
  />
  <FileSingleWidget
    v-else-if="field.fieldType === 'file'"
    v-model:model-value="attachmentValue"
  />
  <GalleryWidget
    v-else-if="field.fieldType === 'images'"
    v-model:model-value="inner"
    :max-count="Number(conf.maxCount) || 20"
    :sort-mode="conf.sortMode"
  />
  <GalleryWidget
    v-else-if="field.fieldType === 'files'"
    v-model:model-value="inner"
    :max-count="Number(conf.maxCount) || 20"
    :sort-mode="conf.sortMode"
  />
  <!-- B1 绑定数据源：用户选择器（显示昵称，提交存 user_id） -->
  <Select
    v-else-if="field.fieldType === 'user'"
    v-model:value="inner"
    allow-clear
    class="w-full"
    option-filter-prop="label"
    :options="controlOptions"
    :placeholder="field.placeholder || `选择${field.fieldLabel}`"
    show-search
  />
  <!-- B1 绑定数据源：分类树下拉（提交存分类 id） -->
  <TreeSelect
    v-else-if="field.fieldType === 'category'"
    v-model:value="inner"
    allow-clear
    class="w-full"
    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
    :placeholder="field.placeholder || `选择${field.fieldLabel}`"
    show-search
    :tree-data="props.treeData ?? []"
    tree-default-expand-all
    tree-node-filter-prop="label"
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
    :checked-children="conf.checkedText || undefined"
    :un-checked-children="conf.unCheckedText || undefined"
    @change="(checked: any) => emit('update:modelValue', checked ? '1' : '0')"
  />
  <Rate
    v-else-if="field.fieldType === 'rating'"
    v-model:value="inner"
    :count="Number(conf.count) || 5"
    :allow-half="conf.allowHalf === true"
    allow-clear
  />
  <div v-else-if="field.fieldType === 'color'" class="flex items-center gap-2">
    <span
      v-for="c in palette"
      :key="c"
      :style="{ background: c }"
      class="h-6 w-6 cursor-pointer rounded border"
      :title="c"
      @click="emit('update:modelValue', c)"
    />
    <input
      type="color"
      class="h-8 w-12 cursor-pointer rounded border"
      :value="inner || '#1677ff'"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <Input v-model:value="inner" class="w-40" placeholder="#RRGGBB" />
  </div>
  <!-- W：多行文本行数/自动增高/字数统计（maxlength 接通） -->
  <Textarea
    v-else-if="field.fieldType === 'textarea'"
    v-model:value="inner"
    :auto-size="
      conf.autoSize === true
        ? { minRows: 2, maxRows: Number(conf.rows) || 12 }
        : false
    "
    :maxlength="field.maxlength || undefined"
    :placeholder="field.placeholder || field.fieldLabel"
    :rows="Number(conf.rows) || 3"
    :show-count="conf.showCount === true"
  />
  <!-- W：单行文本前后缀（纯装饰不入库，antd Input 原生 addon） -->
  <Input
    v-else-if="field.fieldType === 'input' && (conf.addonBefore || conf.addonAfter)"
    v-model:value="inner"
    :addon-before="conf.addonBefore"
    :addon-after="conf.addonAfter"
    :maxlength="field.maxlength || undefined"
    :placeholder="field.placeholder || field.fieldLabel"
  />
  <!-- W：slug 展示前缀 -->
  <Input
    v-else-if="field.fieldType === 'slug' && conf.prefix"
    v-model:value="inner"
    :addon-before="conf.prefix"
    :placeholder="field.placeholder || field.fieldLabel"
  />
  <Input
    v-else
    v-model:value="inner"
    :maxlength="field.maxlength || undefined"
    :placeholder="field.placeholder || field.fieldLabel"
  />
</template>
