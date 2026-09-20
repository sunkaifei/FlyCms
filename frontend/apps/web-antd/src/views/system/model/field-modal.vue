<script lang="ts" setup>
import type { ModelFieldRow } from '#/api/core/model';

import { onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { saveFieldApi, updateFieldApi } from '#/api/core/model';

/**
 * 新增/编辑字段弹窗。
 * - field_name/field_type 创建后不可改（=列名与列定义）
 * - column_type 由后端 FieldTypeEnum 推导，前端不传
 * - 类型联动：maxlength 仅 input；options 仅 select/radio/checkbox；regex 仅 input
 */
const FIELD_TYPES = [
  { label: '单行文本 (input)', value: 'input' },
  { label: '多行文本 (textarea)', value: 'textarea' },
  { label: '富文本 (editor，存主表正文)', value: 'editor' },
  { label: '整数 (number)', value: 'number' },
  { label: '小数 (decimal)', value: 'decimal' },
  { label: '日期 (date)', value: 'date' },
  { label: '日期时间 (datetime)', value: 'datetime' },
  { label: '下拉 (select)', value: 'select' },
  { label: '单选 (radio)', value: 'radio' },
  { label: '多选 (checkbox)', value: 'checkbox' },
  { label: '单图 (image)', value: 'image' },
  { label: '图组 (images)', value: 'images' },
  { label: '附件 (file)', value: 'file' },
  { label: '多附件 (files)', value: 'files' },
  { label: '地区 (region)', value: 'region' },
];

const modelId = ref<number>(0);
const editing = ref<null | ModelFieldRow>(null);
let onSaved: (() => void) | undefined;

const fieldType = ref<string>('input');

const [Form, formApi] = useVbenForm({
  schema: [
    {
      component: 'Input',
      componentProps: { placeholder: '小写字母开头，如 file_size' },
      fieldName: 'fieldName',
      label: '字段名',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: {
        options: FIELD_TYPES,
        placeholder: '选择类型（创建后不可改）',
      },
      fieldName: 'fieldType',
      label: '字段类型',
      rules: 'selectRequired',
    },
    {
      component: 'Input',
      fieldName: 'fieldLabel',
      label: '显示名',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { min: 1, max: 4000 },
      dependencies: {
        show: () => fieldType.value === 'input',
        triggerFields: ['fieldType'],
      },
      fieldName: 'maxlength',
      label: '长度上限',
    },
    {
      component: 'Textarea',
      componentProps: {
        placeholder: 'JSON 数组，如 ["Windows","Linux","macOS"]',
        rows: 2,
      },
      dependencies: {
        show: () => ['select', 'radio', 'checkbox'].includes(fieldType.value),
        triggerFields: ['fieldType'],
      },
      fieldName: 'options',
      label: '选项',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '正则表达式（可选）' },
      dependencies: {
        show: () => fieldType.value === 'input',
        triggerFields: ['fieldType'],
      },
      fieldName: 'regex',
      label: '校验正则',
    },
    {
      component: 'Input',
      fieldName: 'placeholder',
      label: '占位提示',
    },
    {
      component: 'InputNumber',
      fieldName: 'sort',
      label: '排序',
    },
    {
      component: 'Checkbox',
      fieldName: 'isRequired',
      label: '必填',
    },
    {
      component: 'Checkbox',
      fieldName: 'isList',
      label: '列表显示',
    },
    {
      component: 'Checkbox',
      fieldName: 'isFilter',
      label: '列表筛选',
    },
  ],
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    modalApi.lock();
    try {
      if (editing.value) {
        await updateFieldApi({ ...values, id: editing.value.id });
      } else {
        await saveFieldApi({ ...values, modelId: modelId.value });
      }
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '模型字段',
});

onMounted(() => {
  const data = modalApi.getData() as
    | { modelId: number; onSaved?: () => void; record?: ModelFieldRow }
    | undefined;
  onSaved = data?.onSaved;
  modelId.value = data?.modelId ?? 0;
  editing.value = data?.record ?? null;
  if (editing.value) {
    fieldType.value = editing.value.fieldType;
    modalApi.setState({ title: '编辑字段' });
    formApi.setValues({
      fieldLabel: editing.value.fieldLabel,
      fieldName: editing.value.fieldName,
      fieldType: editing.value.fieldType,
      isFilter: editing.value.isFilter === 1,
      isList: editing.value.isList === 1,
      isRequired: editing.value.isRequired === 1,
      maxlength: editing.value.maxlength,
      options: editing.value.options,
      placeholder: editing.value.placeholder,
      regex: editing.value.regex,
      sort: editing.value.sort,
    });
    formApi.updateSchema([
      { componentProps: { disabled: true }, fieldName: 'fieldName' },
      { componentProps: { disabled: true }, fieldName: 'fieldType' },
    ]);
  } else {
    modalApi.setState({ title: '新增字段' });
    formApi.setValues({ fieldType: 'input', isList: true, sort: 0 });
  }
});
</script>

<template>
  <Modal>
    <Form />
  </Modal>
</template>
