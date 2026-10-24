<script lang="ts" setup>
import type { ModelFieldRow } from '#/api/core/model';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { getDictTypeOptionsApi } from '#/api/core/dict';
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
 * - 子字段：modal data.parentId > 0 时为 GROUP/REPEATER 添加子字段（不建物理列）
 *
 * 2026-10-23 表单改造：
 * - 45 个 schema 项从「一列平铺」改为 4 个可折叠分组（基础 / 校验与选项 /
 *   高级 / 控件外观），填一个字段不必从上滚到下；
 * - 修「类型切换后残留值入库」：切换类型时清掉不属于新类型的 w_* 控件配置；
 * - 修 w_format 在 schema 里重复登记两次（date 一份、datetime 一份）的隐患，
 *   合并为一项、候选按类型联动；
 * - 修 dependencies 里混用「响应式 ref」与「values 参数」导致
 *   新建字段时类型联动失效（只有编辑既有字段才生效）的老 bug：
 *   选了 image 却不出「单图形态」、选了 rollup 却不出聚合配置。
 */
const FIELD_TYPES = [
  { label: '单行文本 (input)', value: 'input' },
  { label: '多行文本 (textarea)', value: 'textarea' },
  { label: '富文本 (editor，存主表正文)', value: 'editor' },
  { label: '整数 (number)', value: 'number' },
  { label: '小数 (decimal)', value: 'decimal' },
  { label: '日期 (date)', value: 'date' },
  { label: '日期时间 (datetime)', value: 'datetime' },
  { label: '日期范围 (date_range，存起止)', value: 'date_range' },
  { label: '时间范围 (datetime_range，存起止)', value: 'datetime_range' },
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
  { label: '用户选择器 (user，存用户id显示昵称)', value: 'user' },
  { label: '分类选择器 (category，绑分类树存分类id)', value: 'category' },
  { label: '公式 (formula，行内计算不落库)', value: 'formula' },
  { label: '字段组 (group，JSON 子字段)', value: 'group' },
  { label: '重复行 (repeater，可增行)', value: 'repeater' },
  { label: '聚合统计 (rollup，虚拟列)', value: 'rollup' },
  { label: '任意关联 (m2a，可跨模型)', value: 'm2a' },
];

const RELATION_TYPES = ['relate', 'relates'];
/** B1 绑定数据源中复用 relateModel 配置列的类型（category 绑定目标分类树所属模型，留空=本模型） */
const BOUND_TYPES = ['category'];
const STRUCTURE_TYPES = ['group', 'repeater'];
/** 可绑定数据字典的类型（若依式）：候选项优先取字典启用数据，options 作回退 */
const OPTION_TYPES = ['select', 'radio', 'checkbox'];
const DICT_SOURCES = [
  { label: '自定义选项', value: 'options' },
  { label: '绑定字典', value: 'dict' },
];
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

/** 各字段类型可用的控件配置键（切换类型时据此清残留） */
const W_KEYS: Record<string, string[]> = {
  image: ['w_shape', 'w_maxSize'],
  images: ['w_maxCount', 'w_sortMode'],
  files: ['w_maxCount', 'w_sortMode'],
  date: ['w_format'],
  datetime: ['w_format'],
  textarea: ['w_rows', 'w_autoSize', 'w_showCount'],
  input: ['w_addonBefore', 'w_addonAfter'],
  number: ['w_addonAfter'],
  decimal: ['w_addonAfter', 'w_step'],
  slug: ['w_prefix'],
  select: ['w_layout'],
  radio: ['w_layout'],
  checkbox: ['w_layout'],
  switch: ['w_checkedText', 'w_unCheckedText'],
  rating: ['w_count', 'w_allowHalf'],
  color: ['w_palette'],
};
const ALL_W_KEYS = [
  'w_shape',
  'w_maxSize',
  'w_maxCount',
  'w_sortMode',
  'w_format',
  'w_rows',
  'w_autoSize',
  'w_showCount',
  'w_addonBefore',
  'w_addonAfter',
  'w_step',
  'w_prefix',
  'w_layout',
  'w_checkedText',
  'w_unCheckedText',
  'w_count',
  'w_allowHalf',
  'w_palette',
];
/** 布尔型控件配置（清空值为 false 而非空串） */
const BOOL_W_KEYS = new Set(['w_autoSize', 'w_showCount', 'w_allowHalf']);

const modelId = ref<number | string>(0);
const editing = ref<null | ModelFieldRow>(null);
/** >0 = 正在为某个 GROUP/REPEATER 添加子字段 */
const parentFieldId = ref<number>(0);
/** 父字段显示名（子字段模式下的提示） */
const parentLabel = ref('');
let onSaved: (() => void) | undefined;

/** 当前选择的字段类型（联动控件外观与高级配置的显隐） */
const fieldType = ref<string>('input');
/** 可被引用的模型（含「本模型」，用于自关联树，如回答的 parent_id） */
const relateOptions = ref<{ label: string; value: string }[]>([]);
/** 本模型顶层字段（条件显隐引用源） */
const visFieldOptions = ref<{ label: string; value: string }[]>([]);
/** 本模型 relate 字段（rollup 数据源） */
const rollupSourceOptions = ref<{ label: string; value: string }[]>([]);
/** 平台字典类型（绑定字典下拉） */
const dictTypeOptions = ref<{ label: string; value: string }[]>([]);

async function loadDictTypeOptions() {
  try {
    const list = (await getDictTypeOptionsApi()) ?? [];
    dictTypeOptions.value = list.map((t) => ({
      label: `${t.dictName}（${t.dictType}）`,
      value: t.dictType,
    }));
  } catch {
    dictTypeOptions.value = [];
  }
}

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
        (f) => (f.parentId ?? 0) === 0 && String(f.id) !== String(excludeId ?? -1),
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

// /////////// 字段 schema（分组组织） ///////////

/** 基础信息组 */
function basicFields(isChild: boolean) {
  return [
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
        onChange: (v: any) => onFieldTypeChange(v),
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
    ...(isChild
      ? []
      : [
          {
            component: 'Input',
            componentProps: { placeholder: '默认：基础信息' },
            fieldName: 'tabName',
            label: '表单选项卡',
          },
        ]),
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
  ];
}

/** 校验与选项组 */
function validateFields() {
  return [
    {
      component: 'InputNumber',
      componentProps: { min: 1, max: 4000 },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'input',
        triggerFields: ['fieldType'],
      },
      fieldName: 'maxlength',
      label: '长度上限',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '正则表达式（可选）' },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'input',
        triggerFields: ['fieldType'],
      },
      fieldName: 'regex',
      label: '校验正则',
    },
    {
      component: 'InputNumber',
      componentProps: { step: 0.01 },
      dependencies: {
        show: (values: Record<string, any>) =>
          ['number', 'decimal', 'rating'].includes(String(values.fieldType)),
        triggerFields: ['fieldType'],
      },
      fieldName: 'minValue',
      label: '最小值',
    },
    {
      component: 'InputNumber',
      componentProps: { step: 0.01 },
      dependencies: {
        show: (values: Record<string, any>) =>
          ['number', 'decimal', 'rating'].includes(String(values.fieldType)),
        triggerFields: ['fieldType'],
      },
      fieldName: 'maxValue',
      label: '最大值',
    },
    {
      component: 'Checkbox',
      componentProps: { title: '开启后写入时校验全模型唯一（slug/编号类字段）' },
      dependencies: {
        show: (values: Record<string, any>) =>
          !STRUCTURE_TYPES.includes(String(values.fieldType)) &&
          values.fieldType !== 'rollup' &&
          values.fieldType !== 'm2a',
        triggerFields: ['fieldType'],
      },
      fieldName: 'isUnique',
      label: '值唯一',
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
      componentProps: {
        title: '关闭后该字段不出现在内容表单（不校验不提交），仅存储/列表/详情可见',
      },
      defaultValue: true,
      fieldName: 'isForm',
      label: '表单显示',
    },
    {
      component: 'Checkbox',
      fieldName: 'isFilter',
      label: '列表筛选',
    },
    {
      component: 'RadioGroup',
      componentProps: { options: DICT_SOURCES },
      defaultValue: 'options',
      dependencies: {
        show: (values: Record<string, any>) =>
          OPTION_TYPES.includes(String(values.fieldType)),
        triggerFields: ['fieldType'],
      },
      fieldName: 'dictSource',
      label: '候选来源',
    },
    {
      component: 'Select',
      componentProps: {
        options: [],
        placeholder: '选择平台字典（「字典管理」里维护候选项）',
        showSearch: true,
        optionFilterProp: 'label',
      },
      dependencies: {
        show: (values: Record<string, any>) =>
          OPTION_TYPES.includes(String(values.fieldType)) &&
          String(values.dictSource) === 'dict',
        triggerFields: ['fieldType', 'dictSource'],
      },
      fieldName: 'dictType',
      label: '绑定字典',
    },
    {
      component: 'Textarea',
      componentProps: {
        placeholder: 'JSON 数组，如 ["Windows","Linux","macOS"]',
        rows: 2,
      },
      dependencies: {
        show: (values: Record<string, any>) =>
          OPTION_TYPES.includes(String(values.fieldType)) &&
          String(values.dictSource) !== 'dict',
        triggerFields: ['fieldType', 'dictSource'],
      },
      fieldName: 'options',
      label: '选项',
    },
  ];
}

/** 高级组（仅顶层字段：子字段不建物理列，关联/聚合/显隐都不适用） */
function advancedFields() {
  return [
    {
      component: 'Select',
      componentProps: {
        options: [],
        placeholder: '被引用/绑定的模型（关联本模型或分类留空 = 本模型）',
        showSearch: true,
        optionFilterProp: 'label',
        allowClear: true,
      },
      dependencies: {
        show: (values: Record<string, any>) =>
          RELATION_TYPES.includes(String(values.fieldType)) ||
          BOUND_TYPES.includes(String(values.fieldType)),
        triggerFields: ['fieldType'],
      },
      fieldName: 'relateModel',
      label: '绑定模型',
    },
    {
      component: 'Select',
      componentProps: {
        options: [],
        placeholder: '选择本模型的单值关联字段（relate）作为数据源',
      },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'rollup',
        triggerFields: ['fieldType'],
      },
      fieldName: 'rollupSource',
      label: '聚合来源',
    },
    {
      component: 'Select',
      componentProps: { options: ROLLUP_FUNCS },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'rollup',
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
        show: (values: Record<string, any>) => values.fieldType === 'rollup',
        triggerFields: ['fieldType'],
      },
      fieldName: 'rollupColumn',
      label: '聚合列',
    },
    {
      component: 'Input',
      componentProps: {
        placeholder: '行内表达式，如 price * 0.88；变量=本模型字段名，仅四则+括号+数字',
      },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'formula',
        triggerFields: ['fieldType'],
      },
      fieldName: 'formula',
      label: '计算公式',
    },
    {
      component: 'Textarea',
      componentProps: {
        placeholder: 'JSON 数组，如 ["price","cover"]；目标行展开时额外携带的列',
        rows: 2,
      },
      dependencies: {
        show: (values: Record<string, any>) =>
          RELATION_TYPES.includes(String(values.fieldType)),
        triggerFields: ['fieldType'],
      },
      fieldName: 'lookupFields',
      label: 'Lookup展示列',
    },
    {
      component: 'Select',
      componentProps: {
        options: [],
        allowClear: true,
        placeholder: '不选 = 始终显示',
        showSearch: true,
        optionFilterProp: 'label',
      },
      fieldName: 'visField',
      label: '显隐条件',
    },
    {
      component: 'Select',
      componentProps: { options: VIS_OPS },
      defaultValue: 'eq',
      dependencies: {
        show: (values: Record<string, any>) => Boolean(values.visField),
        triggerFields: ['visField'],
      },
      fieldName: 'visOp',
      label: '操作符',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '比较值（eq/neq/in 用；in 逗号分隔）' },
      dependencies: {
        show: (values: Record<string, any>) =>
          Boolean(values.visField) &&
          !['empty', 'notempty'].includes(String(values.visOp)),
        triggerFields: ['visField', 'visOp'],
      },
      fieldName: 'visValue',
      label: '比较值',
    },
  ];
}

/** 控件外观组（widget_conf，与后端 FieldWidgetConfUtil 同源） */
function widgetFields() {
  return [
    {
      component: 'Select',
      componentProps: {
        options: [
          { label: '方形 (square)', value: 'square' },
          { label: '圆形 (circle，头像场景)', value: 'circle' },
        ],
        placeholder: '默认方形',
        allowClear: true,
      },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'image',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_shape',
      label: '单图形态',
    },
    {
      component: 'InputNumber',
      componentProps: { min: 1, max: 999 },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'image',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_maxSize',
      label: '大小上限(MB)',
    },
    {
      component: 'InputNumber',
      componentProps: { min: 1, max: 50 },
      dependencies: {
        show: (values: Record<string, any>) =>
          ['images', 'files'].includes(String(values.fieldType)),
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_maxCount',
      label: '数量上限',
    },
    {
      component: 'Select',
      componentProps: {
        options: [
          { label: '拖拽排序 (drag)', value: 'drag' },
          { label: '固定顺序 (fixed)', value: 'fixed' },
        ],
        placeholder: '默认拖拽排序',
        allowClear: true,
      },
      dependencies: {
        show: (values: Record<string, any>) =>
          ['images', 'files'].includes(String(values.fieldType)),
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_sortMode',
      label: '排序方式',
    },
    {
      // date / datetime 共用一项（此前登记了两份同名项，updateSchema 按 fieldName
      // 匹配时会打架），候选随类型联动
      component: 'Select',
      componentProps: { options: [], placeholder: '默认到日', allowClear: true },
      dependencies: {
        show: (values: Record<string, any>) =>
          values.fieldType === 'date' || values.fieldType === 'datetime',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_format',
      label: '时间格式',
    },
    {
      component: 'InputNumber',
      componentProps: { min: 1, max: 30 },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'textarea',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_rows',
      label: '行数',
    },
    {
      component: 'Checkbox',
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'textarea',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_autoSize',
      label: '自动增高',
    },
    {
      component: 'Checkbox',
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'textarea',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_showCount',
      label: '字数统计',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '如 https://' },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'input',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_addonBefore',
      label: '前缀',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '如 https:// 或 单位“元”' },
      dependencies: {
        show: (values: Record<string, any>) =>
          ['input', 'number', 'decimal'].includes(String(values.fieldType)),
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_addonAfter',
      label: '后缀',
    },
    {
      component: 'InputNumber',
      componentProps: { step: 0.01, min: 0.0001 },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'decimal',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_step',
      label: '步长',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '如 /downloads/（仅展示装饰）' },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'slug',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_prefix',
      label: '展示前缀',
    },
    {
      component: 'Select',
      componentProps: {
        options: [],
        placeholder: '默认下拉',
        allowClear: true,
      },
      dependencies: {
        show: (values: Record<string, any>) =>
          ['select', 'radio', 'checkbox'].includes(String(values.fieldType)),
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_layout',
      label: '选项布局',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '如 显示' },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'switch',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_checkedText',
      label: '激活文案',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '如 隐藏' },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'switch',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_unCheckedText',
      label: '关闭文案',
    },
    {
      component: 'InputNumber',
      componentProps: { min: 1, max: 10 },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'rating',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_count',
      label: '星数',
    },
    {
      component: 'Checkbox',
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'rating',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_allowHalf',
      label: '允许半星',
    },
    {
      component: 'Textarea',
      componentProps: {
        placeholder: 'JSON 颜色数组，如 ["#1677ff","#f5222d","#52c41a"]',
        rows: 2,
      },
      dependencies: {
        show: (values: Record<string, any>) => values.fieldType === 'color',
        triggerFields: ['fieldType'],
      },
      fieldName: 'w_palette',
      label: '预设色板',
    },
  ];
}

/** 三个可折叠分组（子字段没有物理列，高级组整组不渲染） */
function buildSchema(isChild: boolean) {
  const groups: any[] = [
    {
      type: 'group',
      name: 'basic',
      title: '基础',
      collapsible: true,
      defaultCollapsed: false,
      wrapperClass: 'grid-cols-2',
      children: basicFields(isChild),
    },
    {
      type: 'group',
      name: 'validate',
      title: '校验与选项',
      collapsible: true,
      defaultCollapsed: false,
      wrapperClass: 'grid-cols-2',
      children: validateFields(),
    },
  ];
  if (!isChild) {
    groups.push({
      type: 'group',
      name: 'advanced',
      title: '高级（关联 / 聚合 / 公式 / 条件显隐）',
      collapsible: true,
      defaultCollapsed: true,
      wrapperClass: 'grid-cols-2',
      children: advancedFields(),
    });
  }
  groups.push({
    type: 'group',
    name: 'widget',
    title: '控件外观',
    collapsible: true,
    defaultCollapsed: true,
    wrapperClass: 'grid-cols-2',
    children: widgetFields(),
  });
  return groups;
}

const [Form, formApi] = useVbenForm({
  schema: buildSchema(false),
  showDefaultActions: false,
});

/**
 * 类型变化：同步联动源 + 清掉不属于新类型的控件配置残留。
 *
 * 此前 dependencies 里混用了两种取值方式——
 * 一部分写 `{ show: () => fieldType.value === 'image' }`（闭包读 ref），
 * 一部分写 `{ show: (values) => values.fieldType === 'input' }`（读表单值）。
 * 而 fieldType.value 只在选择字段类型时没有更新（新建时它永远是 'input'），
 * 于是「选了 image 却不出单图形态、选了 rollup 却不出聚合配置」，
 * 只有编辑既有字段（onMounted 里赋过值）才表现正常。
 */
function onFieldTypeChange(v: any) {
  const t = String(v ?? 'input');
  fieldType.value = t;
  // 清残留：类型 A 配过的 w_* 不该被类型 B 带进库（dependencies 只隐藏不清值）
  const allowed = new Set(W_KEYS[t] ?? []);
  const patch: Record<string, any> = {};
  for (const k of ALL_W_KEYS) {
    if (allowed.has(k)) continue;
    patch[k] = BOOL_W_KEYS.has(k) ? false : '';
  }
  formApi.setValues(patch);
  // 时间格式的候选随 date / datetime 切换
  formApi.updateSchema([
    {
      componentProps: {
        options:
          t === 'datetime'
            ? [
                { label: '到分 (YYYY-MM-DD HH:mm)', value: 'YYYY-MM-DD HH:mm' },
                { label: '到秒 (YYYY-MM-DD HH:mm:ss)', value: 'YYYY-MM-DD HH:mm:ss' },
              ]
            : [
                { label: '日 (YYYY-MM-DD)', value: 'YYYY-MM-DD' },
                { label: '月 (YYYY-MM)', value: 'YYYY-MM' },
                { label: '年 (YYYY)', value: 'YYYY' },
              ],
      },
      fieldName: 'w_format',
    },
    {
      componentProps: {
        options:
          t === 'checkbox'
            ? [
                { label: '多选框 (checkbox)', value: 'checkbox' },
                { label: '标签组 (button)', value: 'button' },
              ]
            : [
                { label: '下拉 (select)', value: 'select' },
                { label: '横排单选 (row)', value: 'row' },
                { label: '竖排单选 (column)', value: 'column' },
                { label: '标签组 (button)', value: 'button' },
              ],
      },
      fieldName: 'w_layout',
    },
  ]);
}

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

/** W：把 w_* 表单键收敛为 widget_conf JSON（与后端 FieldWidgetConfUtil 白名单同源） */
function serializeWidgetConf(values: Record<string, any>): string {
  const conf: Record<string, any> = {};
  const put = (key: string, v: any) => {
    if (v !== undefined && v !== null && v !== '') conf[key] = v;
  };
  switch (String(values.fieldType)) {
    case 'image': {
      put('shape', values.w_shape);
      put('maxSize', values.w_maxSize);
      break;
    }
    case 'images':
    case 'files': {
      put('maxCount', values.w_maxCount);
      put('sortMode', values.w_sortMode);
      break;
    }
    case 'date':
    case 'datetime': {
      put('format', values.w_format);
      break;
    }
    case 'textarea': {
      put('rows', values.w_rows);
      put('autoSize', values.w_autoSize ? true : undefined);
      put('showCount', values.w_showCount ? true : undefined);
      break;
    }
    case 'input': {
      put('addonBefore', values.w_addonBefore);
      put('addonAfter', values.w_addonAfter);
      break;
    }
    case 'number': {
      put('addonAfter', values.w_addonAfter);
      break;
    }
    case 'decimal': {
      put('addonAfter', values.w_addonAfter);
      put('step', values.w_step);
      break;
    }
    case 'slug': {
      put('prefix', values.w_prefix);
      break;
    }
    case 'select':
    case 'radio':
    case 'checkbox': {
      put('layout', values.w_layout);
      break;
    }
    case 'switch': {
      put('checkedText', values.w_checkedText);
      put('unCheckedText', values.w_unCheckedText);
      break;
    }
    case 'rating': {
      put('count', values.w_count);
      put('allowHalf', values.w_allowHalf ? true : undefined);
      break;
    }
    case 'color': {
      const palette = String(values.w_palette ?? '').trim();
      if (palette) conf.palette = palette;
      break;
    }
    default:
      break;
  }
  return Object.keys(conf).length ? JSON.stringify(conf) : '';
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
    // category 的 relateModel 留空合法（= 本模型分类树），无需校验
    if (values.fieldType === 'rollup' && !values.rollupSource) {
      message.warning('聚合来源必须选择本模型的一个 relate 字段');
      return;
    }
    // 字典绑定（若依式）：绑字典必须选类型；未绑定一律传空串让后端清除
    if (
      OPTION_TYPES.includes(String(values.fieldType)) &&
      String(values.dictSource) === 'dict' &&
      !values.dictType
    ) {
      message.warning('请选择要绑定的字典类型');
      return;
    }
    modalApi.lock();
    const payload: Record<string, any> = { ...values };
    payload.visibleWhen = serializeVisibleWhen(values);
    payload.rollupExpr = serializeRollupExpr(values);
    payload.widgetConf = serializeWidgetConf(values);
    payload.dictType =
      String(values.dictSource) === 'dict' ? String(values.dictType ?? '') : '';
    // w_* 中间键不入库
    for (const key of Object.keys(payload)) {
      if (key.startsWith('w_')) delete payload[key];
    }
    delete payload.visField;
    delete payload.visOp;
    delete payload.visValue;
    delete payload.rollupSource;
    delete payload.rollupFunc;
    delete payload.rollupColumn;
    delete payload.dictSource;
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
  await loadDictTypeOptions();
  // 子字段不建物理列，高级组整组不渲染（schema 在打开时按子字段模式重建一次）
  if (parentFieldId.value > 0) {
    formApi.setState({ schema: buildSchema(true) });
  }
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
    {
      componentProps: { options: dictTypeOptions.value },
      fieldName: 'dictType',
    },
  ]);
  if (editing.value) {
    const t = String(editing.value.fieldType ?? 'input');
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
    // W 控件配置回填
    let wConf: any = {};
    try {
      wConf = editing.value.widgetConf ? JSON.parse(editing.value.widgetConf) : {};
    } catch {
      wConf = {};
    }
    formApi.setValues({
      fieldLabel: editing.value.fieldLabel,
      fieldName: editing.value.fieldName,
      fieldType: t,
      relateModel: editing.value.relateModel,
      tabName: editing.value.tabName || '基础信息',
      dictSource: editing.value.dictType ? 'dict' : 'options',
      dictType: editing.value.dictType || undefined,
      isFilter: editing.value.isFilter === 1,
      isForm: editing.value.isForm === 0 ? false : true,
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
      formula: editing.value.formula,
      visField: vis.field,
      visOp: vis.op ?? 'eq',
      visValue: vis.value,
      rollupSource: rollup.source,
      rollupFunc: rollup.func ?? 'COUNT',
      rollupColumn: rollup.column,
      w_shape: wConf.shape,
      w_maxSize: wConf.maxSize,
      w_maxCount: wConf.maxCount,
      w_sortMode: wConf.sortMode,
      w_format: wConf.format,
      w_rows: wConf.rows,
      w_autoSize: wConf.autoSize === true,
      w_showCount: wConf.showCount === true,
      w_addonBefore: wConf.addonBefore,
      w_addonAfter: wConf.addonAfter,
      w_step: wConf.step,
      w_prefix: wConf.prefix,
      w_layout: wConf.layout,
      w_checkedText: wConf.checkedText,
      w_unCheckedText: wConf.unCheckedText,
      w_count: wConf.count,
      w_allowHalf: wConf.allowHalf === true,
      w_palette: wConf.palette ? JSON.stringify(wConf.palette) : undefined,
    });
    // 回填后再同步一次类型联动（选项候选 + 残留清理）
    onFieldTypeChange(t);
    formApi.updateSchema([
      { componentProps: { disabled: true }, fieldName: 'fieldName' },
      { componentProps: { disabled: true }, fieldName: 'fieldType' },
    ]);
  } else {
    modalApi.setState({
      title: parentFieldId.value > 0 ? `为「${parentLabel.value}」添加子字段` : '新增字段',
    });
    formApi.setValues({ fieldType: 'input', isList: true, sort: 0 });
    onFieldTypeChange('input');
  }
});
</script>

<template>
  <Modal>
    <div
      v-if="parentFieldId > 0"
      class="mb-2 rounded bg-blue-50 px-3 py-1.5 text-xs text-blue-600"
    >
      子字段模式：子字段存于「{{ parentLabel }}」的 JSON 结构内，不创建独立数据表列
    </div>
    <Form />
  </Modal>
</template>
