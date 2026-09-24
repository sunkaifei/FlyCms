<script lang="ts" setup>
import type { ChannelModelOption, ChannelRow } from '#/api/core/channel';

import { computed, onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Input, InputNumber, message, Select, Switch, Textarea } from 'ant-design-vue';

import {
  checkChannelDirApi,
  getChannelModelsApi,
  saveChannelApi,
} from '#/api/core/channel';

import RichEditor from '../modeldata/richtext-editor.vue';

/**
 * 新增/编辑栏目。
 *
 * 栏目是「URL 归属层」而非模型的附属品：modelId=0（不绑定模型）是一等公民，
 * 单页/外链/聚合栏目都靠它 —— 这是阶段 C 相对旧 CMS「栏目必须与模型绑死」的关键变化。
 * channelDir 全站唯一且决定前台 URL，改名会撞车，因此输入时做实时预检。
 */
const TYPE_OPTIONS = [
  { label: '列表栏目（绑定模型）', value: 0 },
  { label: '单页栏目（自定义富文本）', value: 1 },
  { label: '外链栏目（跳转到站外）', value: 2 },
  { label: '聚合栏目（跨模型混排）', value: 3 },
];

const RESERVED_HINT =
  '以下前缀为系统保留，不可占用：ac / qc / sc / topics / search / ucenter / p{数字} / index / login / register';

const editing = ref<null | ChannelRow>(null);
let onSaved: (() => void) | undefined;

const models = ref<ChannelModelOption[]>([]);
const id = ref<string>('');
const fatherId = ref<number>(0);
const parentName = ref<string>('');
const channelName = ref('');
const channelDir = ref('');
const modelId = ref<number>(0);
const channelType = ref(0);
const outUrl = ref('');
const pageContent = ref('');
const listTemplate = ref('');
const detailTemplate = ref('');
const seoTitle = ref('');
const seoKeywords = ref('');
const seoDescription = ref('');
const pageSize = ref(20);
const sort = ref(0);
const status = ref(true);

const dirChecking = ref(false);
const dirMsg = ref<{ ok: boolean; text: string } | null>(null);

const isSinglePage = computed(() => channelType.value === 1);
const isOutLink = computed(() => channelType.value === 2);
const needModel = computed(() => channelType.value === 0);

let dirTimer: ReturnType<typeof setTimeout> | undefined;

/** 目录名实时预检：撞 given 系统路由/其他栏目时立刻给出原因，不用等保存报错 */
function onDirInput() {
  dirMsg.value = null;
  const dir = channelDir.value.trim();
  if (!dir) {
    return;
  }
  if (dirTimer) {
    clearTimeout(dirTimer);
  }
  dirTimer = setTimeout(async () => {
    dirChecking.value = true;
    try {
      const res = await checkChannelDirApi(dir, id.value || undefined);
      dirMsg.value = { ok: res.code === 200, text: res.msg };
    } catch {
      dirMsg.value = null;
    } finally {
      dirChecking.value = false;
    }
  }, 300);
}

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    if (!channelName.value.trim()) {
      message.warning('请填写栏目名称');
      return;
    }
    if (!channelDir.value.trim()) {
      message.warning('请填写目录名（决定前台 URL）');
      return;
    }
    if (isOutLink.value && !outUrl.value.trim()) {
      message.warning('外链栏目必须填写目标地址');
      return;
    }
    if (needModel.value && !modelId.value) {
      message.warning('列表栏目必须绑定模型');
      return;
    }
    modalApi.lock();
    try {
      await saveChannelApi({
        channelDir: channelDir.value.trim(),
        channelName: channelName.value.trim(),
        channelType: channelType.value,
        detailTemplate: detailTemplate.value.trim(),
        fatherId: fatherId.value,
        id: id.value || undefined,
        listTemplate: listTemplate.value.trim(),
        modelId: modelId.value,
        outUrl: outUrl.value.trim(),
        pageContent: isSinglePage.value ? pageContent.value : '',
        pageSize: pageSize.value,
        seoDescription: seoDescription.value.trim(),
        seoKeywords: seoKeywords.value.trim(),
        seoTitle: seoTitle.value.trim(),
        sort: sort.value,
        status: status.value ? 1 : 0,
      });
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '新增栏目',
});

onMounted(async () => {
  const data = modalApi.getData() as
    | {
        onSaved?: () => void;
        parentId?: number;
        parentName?: string;
        record?: ChannelRow;
      }
    | undefined;
  onSaved = data?.onSaved;
  fatherId.value = data?.parentId ?? 0;
  parentName.value = data?.parentName ?? '';

  try {
    models.value = (await getChannelModelsApi()) ?? [];
  } catch {
    models.value = [{ code: '', id: 0, modelName: '（不绑定模型）' }];
  }

  if (data?.record) {
    const r = data.record;
    editing.value = r;
    id.value = r.id;
    channelName.value = r.channelName;
    channelDir.value = r.channelDir;
    modelId.value = r.modelId;
    channelType.value = r.channelType;
    outUrl.value = r.outUrl || '';
    pageContent.value = r.pageContent || '';
    listTemplate.value = r.listTemplate || '';
    detailTemplate.value = r.detailTemplate || '';
    seoTitle.value = r.seoTitle || '';
    seoKeywords.value = r.seoKeywords || '';
    seoDescription.value = r.seoDescription || '';
    pageSize.value = r.pageSize || 20;
    sort.value = r.sort;
    status.value = r.status === 1;
    fatherId.value = r.fatherId;
    modalApi.setState({ title: `编辑栏目「${r.channelName}」` });
  } else {
    modalApi.setState({
      title: parentName.value ? `在「${parentName.value}」下新增栏目` : '新增根栏目',
    });
  }
});
</script>

<template>
  <Modal>
    <div class="grid grid-cols-2 gap-x-4 gap-y-3">
      <div>
        <div class="mb-1 text-sm">栏目名称</div>
        <Input v-model:value="channelName" placeholder="如：新房楼盘" />
      </div>
      <div>
        <div class="mb-1 text-sm">
          目录名（URL 片段）
          <code class="ml-1 text-xs text-gray-400">/{'{'}dir{'}'}/</code>
        </div>
        <Input
          v-model:value="channelDir"
          placeholder="loupan"
          :disabled="!!editing"
          @input="onDirInput"
        />
        <div class="mt-1 text-xs">
          <span v-if="dirChecking" class="text-gray-400">检查中…</span>
          <span v-else-if="dirMsg" :class="dirMsg.ok ? 'text-green-600' : 'text-red-600'">
            {{ dirMsg.text }}
          </span>
          <span v-else class="text-gray-400">
            保存后不可修改（改动会导致存量 URL 失效）
          </span>
        </div>
      </div>

      <div>
        <div class="mb-1 text-sm">栏目类型</div>
        <Select v-model:value="channelType" :options="TYPE_OPTIONS" class="w-full" />
      </div>
      <div>
        <div class="mb-1 text-sm">绑定模型</div>
        <Select
          v-model:value="modelId"
          :options="
            models.map((m) => ({ label: m.modelName, value: m.id }))
          "
          class="w-full"
        />
        <div class="mt-1 text-xs text-gray-400">
          列表/聚合栏目选模型；单页与外链栏目选「不绑定模型」
        </div>
      </div>

      <div v-if="isOutLink" class="col-span-2">
        <div class="mb-1 text-sm">外链地址</div>
        <Input v-model:value="outUrl" placeholder="https://example.com/xf" />
      </div>

      <div class="col-span-2">
        <div class="mb-1 text-sm">每页条数</div>
        <InputNumber v-model:value="pageSize" :min="1" :max="200" class="w-40" />
      </div>

      <div v-if="isSinglePage" class="col-span-2">
        <div class="mb-1 text-sm">单页内容</div>
        <RichEditor v-model="pageContent" />
      </div>

      <div class="col-span-2 mt-2 text-sm font-medium">SEO（留空则继承全站配置）</div>
      <div class="col-span-2">
        <div class="mb-1 text-sm">SEO 标题</div>
        <Input v-model:value="seoTitle" placeholder="留空=栏目名 + 站名" />
      </div>
      <div class="col-span-2">
        <div class="mb-1 text-sm">SEO 关键词</div>
        <Textarea v-model:value="seoKeywords" :rows="2" />
      </div>
      <div class="col-span-2">
        <div class="mb-1 text-sm">SEO 描述</div>
        <Textarea v-model:value="seoDescription" :rows="2" />
      </div>

      <div class="col-span-2 mt-1 text-sm font-medium">模板覆盖（可选）</div>
      <div>
        <div class="mb-1 text-sm">列表模板</div>
        <Input v-model:value="listTemplate" placeholder="留空=模型默认，如 channel/list.html" />
      </div>
      <div>
        <div class="mb-1 text-sm">详情模板</div>
        <Input v-model:value="detailTemplate" placeholder="留空=模型默认" />
      </div>

      <div>
        <div class="mb-1 text-sm">排序</div>
        <InputNumber v-model:value="sort" class="w-full" />
      </div>
      <div class="flex items-center gap-2 pb-1">
        <Switch v-model:checked="status" size="small" />
        <span class="text-sm">显示</span>
      </div>

      <div class="col-span-2 mt-1 rounded bg-gray-50 p-2 text-xs text-gray-500">
        {{ RESERVED_HINT }}
      </div>
    </div>
  </Modal>
</template>
