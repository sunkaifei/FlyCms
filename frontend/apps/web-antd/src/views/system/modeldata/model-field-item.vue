<script lang="ts" setup>
import type { ModelFieldRow } from '#/api/core/model';

import { computed } from 'vue';

import { Button } from 'ant-design-vue';

import DynamicFieldInput from './dynamic-field-input.vue';
import FieldItem from '../_shared/field-item.vue';

/**
 * 单个模型字段的渲染单元（2026-10-23 从 form-modal.vue 抽出）。
 *
 * <p>此前「base 页签自定义字段」「自定义页签字段」「GROUP/REPEATER 子字段」
 * 三块渲染逻辑在 form-modal.vue 里被复制了三份（约 250 行），改一处要改三处，
 * 是表单持续劣化的源头。这里统一：本组件递归处理 GROUP/REPEATER，
 * 调用方只传顶层字段即可。
 *
 * <p>候选项按「字段名 → 候选项」的映射传入（而非单个数组），
 * 这样子字段也能拿到各自正确的候选（子字段可能是 user 字段，需要用户候选）。
 */
type Opt = { label: string; value: string }[];

const props = defineProps<{
  field: ModelFieldRow;
  /** 当前值（v-model）：GROUP 为对象、REPEATER 为数组、其余为标量 */
  modelValue?: any;
  /** 子字段列表（GROUP/REPEATER 用） */
  children?: ModelFieldRow[];
  /** 字段名 → 候选项（relate/relates/m2a/user） */
  optionMap?: Record<string, Opt>;
  /** dictType → 候选项 */
  dictOptionMap?: Record<string, Opt>;
  /** 字段名 → 分类树（category） */
  treeDataMap?: Record<string, any[]>;
  /** 顶层字段的错误文案 */
  error?: string;
  /** 子字段错误：{ 字段名: 文案 } */
  childErrors?: Record<string, string>;
}>();

const emit = defineEmits<{ (e: 'update:modelValue', v: any): void }>();

const value = computed({
  get: () => props.modelValue,
  set: (v: any) => emit('update:modelValue', v),
});

/** 长控件独占整行（两列栅格下 col-span-2） */
const LONG_TYPES = [
  'textarea',
  'images',
  'files',
  'relates',
  'image_url',
  'repeater',
  'm2a',
  'editor',
];

const repeaterRows = computed(() =>
  Array.isArray(props.modelValue) ? (props.modelValue as Record<string, any>[]) : [],
);

function addRow() {
  const rows = Array.isArray(props.modelValue) ? [...props.modelValue] : [];
  const row: Record<string, any> = {};
  for (const c of props.children ?? []) row[c.fieldName] = undefined;
  rows.push(row);
  emit('update:modelValue', rows);
}

function removeRow(index: number) {
  const rows = Array.isArray(props.modelValue) ? [...props.modelValue] : [];
  rows.splice(index, 1);
  emit('update:modelValue', rows);
}
</script>

<template>
  <FieldItem
    :error="props.error"
    :full="LONG_TYPES.includes(props.field.fieldType)"
    :label="props.field.fieldLabel"
    :name="props.field.fieldName"
    :required="props.field.isRequired === 1"
    :tips="props.field.tips"
  >
    <!-- P1 GROUP 字段组：子字段网格 -->
    <div
      v-if="props.field.fieldType === 'group'"
      class="rounded border border-gray-200 p-3"
    >
      <div v-if="!(props.children ?? []).length" class="text-xs text-gray-400">
        该字段组还没有子字段（在「字段管理」里点「子字段」添加）
      </div>
      <div v-else class="grid grid-cols-2 gap-x-4 gap-y-3">
        <div v-for="child in props.children ?? []" :key="child.id">
          <FieldItem
            :error="props.childErrors?.[child.fieldName]"
            :label="child.fieldLabel"
            :name="child.fieldName"
            :required="child.isRequired === 1"
            :tips="child.tips"
          >
            <DynamicFieldInput
              v-model="value[child.fieldName]"
              :dict-options="
                child.dictType ? props.dictOptionMap?.[child.dictType] : undefined
              "
              :field="child"
              :options="props.optionMap?.[child.fieldName] ?? []"
              :tree-data="props.treeDataMap?.[child.fieldName]"
            />
          </FieldItem>
        </div>
      </div>
    </div>

    <!-- P1 REPEATER 重复行：可增删行 -->
    <div v-else-if="props.field.fieldType === 'repeater'" class="space-y-2">
      <div
        v-for="(row, rowIndex) in repeaterRows"
        :key="rowIndex"
        class="relative rounded border border-gray-200 p-3"
      >
        <Button
          class="absolute right-2 top-2"
          danger
          size="small"
          type="link"
          @click="removeRow(rowIndex)"
        >
          删除本行
        </Button>
        <div class="grid grid-cols-2 gap-x-4 gap-y-3">
          <div v-for="child in props.children ?? []" :key="child.id">
            <FieldItem
              :error="props.childErrors?.[child.fieldName]"
              :label="child.fieldLabel"
              :name="child.fieldName"
              :required="child.isRequired === 1"
              :tips="child.tips"
            >
              <DynamicFieldInput
                v-model="row[child.fieldName]"
                :dict-options="
                  child.dictType ? props.dictOptionMap?.[child.dictType] : undefined
                "
                :field="child"
                :options="props.optionMap?.[child.fieldName] ?? []"
                :tree-data="props.treeDataMap?.[child.fieldName]"
              />
            </FieldItem>
          </div>
        </div>
      </div>
      <Button class="w-full" size="small" @click="addRow">
        + 添加一行（{{ repeaterRows.length }} 行）
      </Button>
    </div>

    <!-- 其余类型走通用控件 -->
    <DynamicFieldInput
      v-else
      v-model="value"
      :dict-options="
        props.field.dictType ? props.dictOptionMap?.[props.field.dictType] : undefined
      "
      :field="props.field"
      :options="props.optionMap?.[props.field.fieldName] ?? []"
      :tree-data="props.treeDataMap?.[props.field.fieldName]"
    />
  </FieldItem>
</template>
