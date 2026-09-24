<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import {
  CheckboxGroup,
  DatePicker,
  Input,
  InputNumber,
  message,
  Select,
  TabPane,
  Tabs,
  Textarea,
} from 'ant-design-vue';

import {
  getDataDetailApi,
  saveDataApi,
  updateDataApi,
  type ModelCategoryRow,
  type ModelFieldRow,
  type ModelRow,
} from '#/api/core/model';

import AttachmentInput from './attachment-input.vue';
import RichtextEditor from './richtext-editor.vue';

/**
 * 动态内容表单（元数据驱动，全部模型共用，无需新增 Vue 页面）。
 * - 选项卡：自定义字段按 tabName 分组（字段管理里配置），组内按 sort 排序；
 *   多个不同 tabName 时自动出现选项卡，全同则单页。
 * - 自定义控件（attachment-input/richtext-editor）走默认 modelValue 绑定（坑 15）。
 * - DatePicker 用 valueFormat 直接绑字符串，后端 SimpleDateFormat 解析。
 */
const TAB_BASE = '基础信息';
const TAB_CONTENT = '详细内容';
const TAB_SEO = 'SEO 设置';

const model = ref<null | ModelRow>(null);
const fields = ref<ModelFieldRow[]>([]);
const categories = ref<ModelCategoryRow[]>([]);
const record = ref<null | Record<string, any>>(null);
const activeTab = ref(TAB_BASE);
const values = reactive<Record<string, any>>({ status: '0' });
let onSaved: (() => void) | undefined;

/** 自定义字段的选项卡名（保持首次出现顺序） */
const customTabs = computed(() => {
  const names: string[] = [];
  for (const f of fields.value) {
    const tab = f.tabName || TAB_BASE;
    if (!names.includes(tab)) names.push(tab);
  }
  return names;
});

function fieldsOfTab(tab: string): ModelFieldRow[] {
  return fields.value.filter((f) => (f.tabName || TAB_BASE) === tab);
}

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

function pick(row: Record<string, any>, name: string) {
  if (row[name] !== undefined && row[name] !== null) return row[name];
  const camel = name
    .split('_')
    .map((p, i) => (i === 0 ? p : p.charAt(0).toUpperCase() + p.slice(1)))
    .join('');
  return row[camel];
}

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    if (!String(values.title ?? '').trim()) {
      message.warning(`${model.value?.titleLabel || '标题'}不能为空`);
      return;
    }
    if (values.status === '4' && !values.publish_time) {
      message.warning('定时发布必须填写发布时间');
      return;
    }
    for (const f of fields.value) {
      const v = values[f.fieldName];
      const blank =
        v === undefined ||
        v === null ||
        v === '' ||
        (Array.isArray(v) && v.length === 0);
      if (f.isRequired === 1 && blank) {
        message.warning(`${f.fieldLabel}不能为空`);
        return;
      }
      if (!blank && f.regex && !String(v).match(new RegExp(f.regex))) {
        message.warning(`${f.fieldLabel}格式不正确`);
        return;
      }
    }
    modalApi.lock();
    try {
      const payload: Record<string, any> = {
        modelId: model.value?.id,
        title: values.title,
        status: values.status ?? '0',
      };
      for (const key of [
        'categoryId',
        'thumbnail',
        'content',
        'keywords',
        'description',
      ]) {
        if (values[key]) payload[key] = values[key];
      }
      for (const f of fields.value) {
        const v = values[f.fieldName];
        if (v === undefined || v === null || v === '') continue;
        payload[f.fieldName] = Array.isArray(v) ? JSON.stringify(v) : String(v);
      }
      if (values.publish_time) payload.publish_time = values.publish_time;
      if (record.value) {
        await updateDataApi({ ...payload, id: record.value.id });
      } else {
        await saveDataApi(payload);
      }
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '内容',
});

onMounted(async () => {
  const data = modalApi.getData() as
    | {
        categories?: ModelCategoryRow[];
        fields?: ModelFieldRow[];
        model?: ModelRow;
        onSaved?: () => void;
        record?: Record<string, any>;
      }
    | undefined;
  onSaved = data?.onSaved;
  if (data?.fields) fields.value = data.fields;
  if (data?.categories) categories.value = data.categories;
  if (data?.model) model.value = data.model;

  if (data?.record) {
    record.value = data.record;
    // 编辑：详情接口给的是动态表原始行
    let row: Record<string, any> = {};
    try {
      row = (await getDataDetailApi(model.value!.id, data.record.id)) ?? {};
    } catch {
      row = data.record; // 列表行兜底
    }
    modalApi.setState({ title: `编辑${model.value?.titleLabel || '内容'}` });
    values.title = pick(row, 'title') ?? '';
    values.content = pick(row, 'content') ?? '';
    values.keywords = pick(row, 'keywords') ?? '';
    values.description = pick(row, 'description') ?? '';
    values.categoryId = pick(row, 'category_id') || undefined;
    values.thumbnail = pick(row, 'thumbnail') || '';
    values.status = String(row.status ?? '0');
    for (const f of fields.value) {
      let v = pick(row, f.fieldName) ?? '';
      if (f.fieldType === 'checkbox') {
        try {
          v = v ? JSON.parse(String(v)) : [];
        } catch {
          v = v ? String(v).split(',') : [];
        }
      } else if (f.fieldType === 'number' || f.fieldType === 'decimal') {
        v = v === '' || v === null ? undefined : Number(v);
      } else if (
        (f.fieldType === 'image' || f.fieldType === 'file') &&
        typeof v === 'object'
      ) {
        v = '';
      }
      values[f.fieldName] = v;
    }
  } else {
    modalApi.setState({ title: `添加${model.value?.titleLabel || '内容'}` });
    for (const f of fields.value) {
      values[f.fieldName] =
        f.fieldType === 'checkbox'
          ? []
          : (f.defaultValue ??
            (f.fieldType === 'number' || f.fieldType === 'decimal'
              ? undefined
              : ''));
    }
  }
});
</script>

<template>
  <Modal>
    <Tabs v-model:active-key="activeTab">
      <TabPane :tab="TAB_BASE" key="base">
        <div class="grid grid-cols-2 gap-x-5 gap-y-3">
          <div class="col-span-2">
            <div class="mb-1 text-sm">
              <span class="text-red-500">*</span>
              {{ model?.titleLabel || '标题' }}
            </div>
            <Input v-model:value="values.title" :maxlength="250" />
          </div>
          <div v-if="categories.length > 0">
            <div class="mb-1 text-sm">分类</div>
            <Select
              v-model:value="values.categoryId"
              allow-clear
              class="w-full"
              :options="categories.map((c) => ({ label: c.name, value: c.id }))"
              placeholder="选择分类"
            />
          </div>
          <div>
            <div class="mb-1 text-sm">状态</div>
            <Select
              v-model:value="values.status"
              class="w-full"
              :options="[
                { label: '待审核', value: '0' },
                { label: '发布', value: '1' },
                { label: '未通过', value: '2' },
                { label: '定时发布', value: '4' },
              ]"
            />
          </div>
          <div class="col-span-2">
            <div class="mb-1 text-sm">封面图</div>
            <AttachmentInput v-model:model-value="values.thumbnail" />
          </div>
          <div class="col-span-2" v-if="values.status === '4'">
            <div class="mb-1 text-sm">
              <span class="text-red-500">*</span>
              定时发布时间（到点自动发布）
            </div>
            <Input v-model:value="values.publish_time" placeholder="yyyy-MM-dd HH:mm:ss" />
          </div>
        </div>
      </TabPane>

      <TabPane :tab="TAB_CONTENT" key="content">
        <div class="mb-1 text-xs text-gray-400">
          正文（富文本，编辑器图片上传走附件通道）
        </div>
        <RichtextEditor v-model:model-value="values.content" />
        <div class="mt-3">
          <Button v-if="!values.content" @click="() => (values.content = ' ')">
            初始化编辑器
          </Button>
        </div>
      </TabPane>

      <TabPane
        v-for="tab in customTabs"
        :key="`c-${tab}`"
        :tab="tab"
      >
        <div class="grid grid-cols-2 gap-x-5 gap-y-3">
          <div
            v-for="f in fieldsOfTab(tab)"
            :key="f.id"
            :class="f.fieldType === 'textarea' || f.fieldType === 'images' || f.fieldType === 'files' ? 'col-span-2' : ''"
          >
            <div class="mb-1 text-sm">
              <span v-if="f.isRequired === 1" class="text-red-500">*</span>
              {{ f.fieldLabel }}
              <span class="ml-1 text-xs text-gray-400">{{ f.fieldName }}</span>
            </div>
            <InputNumber
              v-if="f.fieldType === 'number'"
              v-model:value="values[f.fieldName]"
              class="w-full"
              :placeholder="f.placeholder || f.fieldLabel"
            />
            <InputNumber
              v-else-if="f.fieldType === 'decimal'"
              v-model:value="values[f.fieldName]"
              class="w-full"
              :placeholder="f.placeholder || f.fieldLabel"
              :step="0.01"
            />
            <Select
              v-else-if="f.fieldType === 'select' || f.fieldType === 'radio'"
              v-model:value="values[f.fieldName]"
              allow-clear
              class="w-full"
              :options="parseOptions(f.options)"
              :placeholder="f.placeholder || f.fieldLabel"
            />
            <CheckboxGroup
              v-else-if="f.fieldType === 'checkbox'"
              v-model:value="values[f.fieldName]"
              :options="parseOptions(f.options)"
            />
            <DatePicker
              v-else-if="f.fieldType === 'date'"
              v-model:value="values[f.fieldName]"
              class="w-full"
              value-format="YYYY-MM-DD"
            />
            <DatePicker
              v-else-if="f.fieldType === 'datetime'"
              v-model:value="values[f.fieldName]"
              show-time
              class="w-full"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
            <AttachmentInput
              v-else-if="f.fieldType === 'image' || f.fieldType === 'file'"
              v-model:model-value="values[f.fieldName]"
            />
            <AttachmentInput
              v-else-if="f.fieldType === 'images' || f.fieldType === 'files'"
              v-model:model-value="values[f.fieldName]"
              multiple
            />
            <Textarea
              v-else-if="f.fieldType === 'textarea'"
              v-model:value="values[f.fieldName]"
              :placeholder="f.placeholder || f.fieldLabel"
              :rows="3"
            />
            <Input
              v-else
              v-model:value="values[f.fieldName]"
              :maxlength="f.maxlength || undefined"
              :placeholder="f.placeholder || f.fieldLabel"
            />
            <div v-if="f.tips" class="mt-0.5 text-xs text-gray-400">
              {{ f.tips }}
            </div>
          </div>
        </div>
      </TabPane>

      <TabPane :tab="TAB_SEO" key="seo">
        <div class="grid grid-cols-1 gap-y-3">
          <div>
            <div class="mb-1 text-sm">关键词（SEO）</div>
            <Input v-model:value="values.keywords" placeholder="英文逗号分隔" />
          </div>
          <div>
            <div class="mb-1 text-sm">描述（SEO）</div>
            <Textarea v-model:value="values.description" :rows="3" />
          </div>
        </div>
      </TabPane>
    </Tabs>
  </Modal>
</template>
