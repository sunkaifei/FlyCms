<script lang="ts" setup>
import type { ModelFieldRow } from '#/api/core/model';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  getFieldListApi,
  getModelListApi,
  saveFieldApi,
  updateFieldApi,
} from '#/api/core/model';

/**
 * 新增/编辑字段弹窗。
 * - field_name/field_type 创建后不可改（=列名与列定义）
 * - column_type 由后端 FieldTypeEnum 推导，前端不传
 * - 类型联动：maxlength 仅 input；options 仅 select/radio/checkbox；regex 仅 input；
 *   relateModel 仅 relate/relates（E1）；rollup 配置仅 rollup（P2）；
 *   lookupFields 仅 relate/relates（P2）；条件显隐仅顶层字段（P1）
 * - 子字段：modal data.parentId > 0 时为 GROUP/REPEATER 添加子字段（不建物理列）
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
  { label: '关联单条内容 (relate)', value: 'relate' },
  { label: '关联多条内容 (relates)', value: 'relates' },
  { label: '图片URL (image_url，直存链接)', value: 'image_url' },
  { label: '附件URL (file_url，直存链接)', value: 'file_url' },
  { label: '开关 (switch)', value: 'switch' },
  { label: '邮箱 (email)', value: 'email' },
  { label: '网址 (url)', value: 'url' },
  { label: '手机号 (phone)', value: 'phone' },
  { label: '颜色 (color)', value: 'color' },
  { label: '评分 (rating)', value: 'rating' },
  { label: 'URL片段 (slug，常配唯一)', value: 'slug' },
  { label: '字段组 (group，JSON 子字段)', value: 'group' },
  { label: '重复行 (repeater，可增行)', value: 'repeater' },
  { label: '聚合统计 (rollup，虚拟列)', value: 'rollup' },
  { label: '任意关联 (m2a，可跨模型)', value: 'm2a' },
];

const RELATION_TYPES = ['relate', 'relates'];
const STRUCTURE_TYPES = ['group', 'repeater'];
const VIS_OPS = [
  { label: '等于 (eq)', value: 'eq' },
  { label: '不等于 (neq)', value: 'neq' },
  { label: '属于 (in，逗号分隔)', value: 'in' },
  { label: '不属于 (notin)', value: 'notin' },
  { label: '为空 (empty)', value: 'empty' },
  { label: '不为空 (notempty)', value: 'notempty' },
];
const ROLLUP_FUNCS = ['COUNT', 'SUM', 'AVG', 'MIN', 'MAX'].map((f) => ({
  label: f,
  value: f,
}));

const modelId = ref<number | string>(0);
const editing = ref<null | ModelFieldRow>(null);
/** >0 = 正在为某个 GROUP/REPEATER 添加子字段 */
const parentFieldId = ref<number>(0);
/** 父字段显示名（子字段模式下的提示） */
const parentLabel = ref('');
let onSaved: (() => void) | undefined;

const fieldType = ref<string>('input');
/** 可被引用的模型（含「本模型」，用于自关联树，如回答的 parent_id） */
const relateOptions = ref<{ label: string; value: string }[]>([]);
/** 本模型顶层字段（条件显隐引用源） */
const visFieldOptions = ref<{ label: string; value: string }[]>([]);
/** 本模型 relate 字段（rollup 数据源） */
const rollupSourceOptions = ref<{ label: string; value: string }[]>([]);

async function loadRelateOptions(currentModelId?: number | string) {
  try {
    const res = await getModelListApi({ p: 1 });
    const list = res.list ?? [];
    relateOptions.value = list.map((m) => ({
      label: `${m.name}（${m.code}）`,
      value: m.code,
    }));
    // 本模型始终置顶可选：relate 指向本模型 = 自关联树
    const self = list.find((m) => String(m.id) === String(currentModelId));
    if (self) {
      relateOptions.value = [
        { label: `本模型（${self.code}）`, value: self.code },
        ...relateOptions.value.filter((o) => o.value !== self.code),
      ];
    }
  } catch {
    relateOptions.value = [];
  }
}

async function loadSiblingFields(excludeId?: number) {
  try {
    const list = (await getFieldListApi(modelId.value)) ?? [];
    visFieldOptions.value = list
      .filter(
        (f) =>
          (f.parentId ?? 0) === 0 &&
          String(f.id) !== String(excludeId ?? -1),
      )
      .map((f) => ({ label: `${f.fieldLabel}（${f.fieldName}）`, value: f.fieldName }));
    rollupSourceOptions.value = list
      .filter((f) => f.fieldType === 'relate' && (f.parentId ?? 0) === 0)
      .map((f) => ({ label: `${f.fieldLabel}（${f.fieldName}）`, value: f.fieldName }));
  } catch {
    visFieldOptions.value = [];
    rollupSourceOptions.value = [];
  }
}

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
      component: 'Select',
      componentProps: {
        options: [],
        placeholder: '选择被引用的模型（关联本模型 = 自关联树）',
        showSearch: true,
        optionFilterProp: 'label',
      },
      dependencies: {
        show: () => RELATION_TYPES.includes(fieldType.value),
        triggerFields: ['fieldType'],
      },
      fieldName: 'relateModel',
      label: '被引用模型',
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
      componentProps: { placeholder: '默认：基础信息' },
      dependencies: {
        show: () => parentFieldId.value === 0,
        triggerFields: [],
      },
      fieldName: 'tabName',
      label: '表单选项卡',
    },
    // /////////// P2 Rollup 配置（仅 rollup 类型） ///////////
    {
      component: 'Select',
      componentProps: {
        options: [],
        placeholder: '选择本模型的单值关联字段（relate）作为数据源',
      },
      dependencies: {
        show: () => fieldType.value === 'rollup',
        triggerFields: ['fieldType'],
      },
      fieldName: 'rollupSource',
      label: '聚合来源',
    },
    {
      component: 'Select',
      componentProps: { options: ROLLUP_FUNCS },
      dependencies: {
        show: () => fieldType.value === 'rollup',
        triggerFields: ['fieldType'],
      },
      defaultValue: 'COUNT',
      fieldName: 'rollupFunc',
      label: '聚合函数',
    },
    {
      component: 'Input',
      componentProps: { placeholder: 'SUM/AVG/MIN/MAX 需要填目标模型的数值列名' },
      dependencies: {
        show: () => fieldType.value === 'rollup',
        triggerFields: ['fieldType'],
      },
      fieldName: 'rollupColumn',
      label: '聚合列',
    },
    // /////////// P2 Lookup 展示列（仅 relate/relates） ///////////
    {
      component: 'Textarea',
      componentProps: {
        placeholder: 'JSON 数组，如 ["price","cover"]；目标行展开时额外携带的列',
        rows: 2,
      },
      dependencies: {
        show: () => RELATION_TYPES.includes(fieldType.value),
        triggerFields: ['fieldType'],
      },
      fieldName: 'lookupFields',
      label: 'Lookup展示列',
    },
    // /////////// P1 条件显隐（仅顶层字段；子字段不支持） ///////////
    {
      component: 'Select',
      componentProps: {
        options: [],
        allowClear: true,
        placeholder: '不选 = 始终显示',
        showSearch: true,
        optionFilterProp: 'label',
      },
      dependencies: {
        show: () => parentFieldId.value === 0,
        triggerFields: [],
      },
      fieldName: 'visField',
      label: '显隐条件',
    },
    {
      component: 'Select',
      componentProps: { options: VIS_OPS },
      defaultValue: 'eq',
      dependencies: {
        show: () => parentFieldId.value === 0 && Boolean(visField.value),
        triggerFields: ['visField'],
      },
      fieldName: 'visOp',
      label: '操作符',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '比较值（eq/neq/in 用；in 逗号分隔）' },
      dependencies: {
        show: () =>
          parentFieldId.value === 0 &&
          Boolean(visField.value) &&
          !['empty', 'notempty'].includes(String(visOp.value)),
        triggerFields: ['visField', 'visOp'],
      },
      fieldName: 'visValue',
      label: '比较值',
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
    {
      component: 'Checkbox',
      componentProps: { title: '开启后写入时校验全模型唯一（slug/编号类字段）' },
      dependencies: {
        show: () => !STRUCTURE_TYPES.includes(fieldType.value) && fieldType.value !== 'rollup' && fieldType.value !== 'm2a',
        triggerFields: ['fieldType'],
      },
      fieldName: 'isUnique',
      label: '值唯一',
    },
    {
      component: 'InputNumber',
      componentProps: { step: 0.01 },
      dependencies: {
        show: () => ['number', 'decimal', 'rating'].includes(fieldType.value),
        triggerFields: ['fieldType'],
      },
      fieldName: 'minValue',
      label: '最小值',
    },
    {
      component: 'InputNumber',
      componentProps: { step: 0.01 },
      dependencies: {
        show: () => ['number', 'decimal', 'rating'].includes(fieldType.value),
        triggerFields: ['fieldType'],
      },
      fieldName: 'maxValue',
      label: '最大值',
    },
  ],
  showDefaultActions: false,
});

/** 显隐条件引用源（用于 dependencies 联动展示） */
const visField = ref<string>('');
const visOp = ref<string>('eq');

function serializeVisibleWhen(values: Record<string, any>): string {
  const f = values.visField;
  if (!f) return '';
  return JSON.stringify({
    field: f,
    op: values.visOp || 'eq',
    value: values.visValue === undefined || values.visValue === null ? '' : String(values.visValue),
  });
}

function serializeRollupExpr(values: Record<string, any>): string {
  if (values.fieldType !== 'rollup') return '';
  const src = values.rollupSource;
  if (!src) return '';
  const expr: Record<string, string> = {
    source: src,
    func: values.rollupFunc || 'COUNT',
  };
  const col = values.rollupColumn;
  if (expr.func !== 'COUNT') {
    if (!col) return '';
    expr.column = String(col);
  }
  return JSON.stringify(expr);
}

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = (await formApi.getValues()) as Record<string, any>;
    // E1：关联字段必须显式选择被引用模型（后端仍为权威校验）
    if (RELATION_TYPES.includes(String(values.fieldType)) && !values.relateModel) {
      message.warning('请选择被引用模型（关联本模型请选择「本模型」）');
      return;
    }
    if (values.fieldType === 'rollup' && !values.rollupSource) {
      message.warning('聚合来源必须选择本模型的一个 relate 字段');
      return;
    }
    modalApi.lock();
    const payload: Record<string, any> = { ...values };
    payload.visibleWhen = serializeVisibleWhen(values);
    payload.rollupExpr = serializeRollupExpr(values);
    delete payload.visField;
    delete payload.visOp;
    delete payload.visValue;
    delete payload.rollupSource;
    delete payload.rollupFunc;
    delete payload.rollupColumn;
    try {
      if (editing.value) {
        await updateFieldApi({ ...payload, id: editing.value.id });
      } else {
        await saveFieldApi({
          ...payload,
          modelId: modelId.value,
          parentId: parentFieldId.value || undefined,
        });
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

onMounted(async () => {
  const data = modalApi.getData() as
    | {
        modelId: number | string;
        onSaved?: () => void;
        parentId?: number;
        parentLabel?: string;
        record?: ModelFieldRow;
      }
    | undefined;
  onSaved = data?.onSaved;
  modelId.value = data?.modelId ?? 0;
  editing.value = data?.record ?? null;
  parentFieldId.value = data?.parentId ?? 0;
  parentLabel.value = data?.parentLabel ?? '';
  await loadRelateOptions(modelId.value);
  await loadSiblingFields(editing.value?.id);
  formApi.updateSchema([
    {
      componentProps: { options: relateOptions.value },
      fieldName: 'relateModel',
    },
    {
      componentProps: { options: visFieldOptions.value },
      fieldName: 'visField',
    },
    {
      componentProps: { options: rollupSourceOptions.value },
      fieldName: 'rollupSource',
    },
  ]);
  if (editing.value) {
    fieldType.value = editing.value.fieldType;
    modalApi.setState({ title: '编辑字段' });
    // P1 条件显隐回填
    let vis: any = {};
    try {
      vis = editing.value.visibleWhen ? JSON.parse(editing.value.visibleWhen) : {};
    } catch {
      vis = {};
    }
    let rollup: any = {};
    try {
      rollup = editing.value.rollupExpr ? JSON.parse(editing.value.rollupExpr) : {};
    } catch {
      rollup = {};
    }
    visField.value = vis.field ?? '';
    visOp.value = vis.op ?? 'eq';
    formApi.setValues({
      fieldLabel: editing.value.fieldLabel,
      fieldName: editing.value.fieldName,
      fieldType: editing.value.fieldType,
      relateModel: editing.value.relateModel,
      tabName: editing.value.tabName || '基础信息',
      isFilter: editing.value.isFilter === 1,
      isList: editing.value.isList === 1,
      isRequired: editing.value.isRequired === 1,
      isUnique: editing.value.isUnique === 1,
      minValue: editing.value.minValue ?? undefined,
      maxValue: editing.value.maxValue ?? undefined,
      maxlength: editing.value.maxlength,
      options: editing.value.options,
      placeholder: editing.value.placeholder,
      regex: editing.value.regex,
      sort: editing.value.sort,
      lookupFields: editing.value.lookupFields,
      visField: vis.field,
      visOp: vis.op ?? 'eq',
      visValue: vis.value,
      rollupSource: rollup.source,
      rollupFunc: rollup.func ?? 'COUNT',
      rollupColumn: rollup.column,
    });
    formApi.updateSchema([
      { componentProps: { disabled: true }, fieldName: 'fieldName' },
      { componentProps: { disabled: true }, fieldName: 'fieldType' },
    ]);
  } else {
    modalApi.setState({
      title: parentFieldId.value > 0 ? `为「${parentLabel.value}」添加子字段` : '新增字段',
    });
    formApi.setValues({ fieldType: 'input', isList: true, sort: 0 });
  }
});
</script>

<template>
  <Modal>
    <div v-if="parentFieldId > 0" class="mb-2 rounded bg-blue-50 px-3 py-1.5 text-xs text-blue-600">
      子字段模式：子字段存于「{{ parentLabel }}」的 JSON 结构内，不创建独立数据表列
    </div>
    <Form />
  </Modal>
</template>
