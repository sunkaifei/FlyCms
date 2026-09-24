<script lang="ts" setup>
import type { DebugChainCandidate } from '#/api/core/template';

import { computed, onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Button, Input, InputNumber, Select, Tag } from 'ant-design-vue';

import { getDebugChainApi } from '#/api/core/template';

/**
 * P8 候选链调试（§8.2）：给定页面上下文，展示完整模板候选链与命中项。
 * 让"这条页面为什么用这个模板 / 为什么没吃到那个模板"一眼看穿，
 * 改名、建文件前先在这里模拟一次，不用去前台盲试。
 */
const [Modal, modalApi] = useEditDrawer({
  class: 'w-[55%]',
  title: '候选链调试',
});

const PAGE_TYPES = [
  { label: '列表页 LIST', value: 'LIST' },
  { label: '详情页 DETAIL', value: 'DETAIL' },
  { label: '首页 INDEX', value: 'INDEX' },
  { label: '单页栏目 CHANNEL_PAGE', value: 'CHANNEL_PAGE' },
  { label: '搜索页 SEARCH', value: 'SEARCH' },
  { label: '标签页 TAG', value: 'TAG' },
  { label: '错误页 ERROR', value: 'ERROR' },
];

const pageType = ref('LIST');
const model = ref('');
const channel = ref('');
const contentId = ref('');
const shortUrl = ref('');
const errorCode = ref<number>(404);

const loading = ref(false);
const skin = ref('');
const chain = ref<DebugChainCandidate[]>([]);
const queried = ref(false);

/** 真正命中的节点 = 链上第一个存在的候选（与后端"命中即停"一致） */
const hitIndex = computed(() => chain.value.findIndex((c) => c.exists));

/** 按页面类型决定要展示哪些输入项 */
const needModel = computed(() => ['LIST', 'DETAIL'].includes(pageType.value));
const needChannel = computed(() =>
  ['LIST', 'DETAIL', 'CHANNEL_PAGE'].includes(pageType.value),
);
const needShortUrl = computed(() =>
  ['DETAIL', 'TAG'].includes(pageType.value),
);

async function query() {
  loading.value = true;
  try {
    const res = await getDebugChainApi({
      channel: channel.value.trim() || undefined,
      contentId: contentId.value.trim() || undefined,
      errorCode: pageType.value === 'ERROR' ? errorCode.value : undefined,
      model: model.value.trim() || undefined,
      pageType: pageType.value,
      shortUrl: shortUrl.value.trim() || undefined,
    });
    skin.value = res.skin ?? '';
    chain.value = res.chain ?? [];
    queried.value = true;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  const data = modalApi.getData() as
    | { model?: string; pageType?: string }
    | undefined;
  if (data?.model) {
    model.value = data.model;
  }
  if (data?.pageType) {
    pageType.value = data.pageType;
  }
  query();
});
</script>

<template>
  <Modal>
    <div class="mb-3 flex flex-wrap items-end gap-3">
      <div>
        <div class="mb-1 text-sm">页面类型</div>
        <Select v-model:value="pageType" class="w-48" :options="PAGE_TYPES" />
      </div>
      <div v-if="needModel">
        <div class="mb-1 text-sm">模型 code</div>
        <Input v-model:value="model" class="w-40" placeholder="如 articles" />
      </div>
      <div v-if="needChannel">
        <div class="mb-1 text-sm">栏目目录</div>
        <Input v-model:value="channel" class="w-40" placeholder="如 news" />
      </div>
      <div v-if="pageType === 'DETAIL'">
        <div class="mb-1 text-sm">内容 ID</div>
        <Input v-model:value="contentId" class="w-32" placeholder="42" />
      </div>
      <div v-if="needShortUrl">
        <div class="mb-1 text-sm">
          {{ pageType === 'TAG' ? '标签名' : '短网址' }}
        </div>
        <Input v-model:value="shortUrl" class="w-40" placeholder="demo" />
      </div>
      <div v-if="pageType === 'ERROR'">
        <div class="mb-1 text-sm">错误码</div>
        <InputNumber v-model:value="errorCode" class="w-28" :min="400" />
      </div>
      <Button :loading="loading" type="primary" @click="query">查询</Button>
      <span v-if="skin" class="text-xs text-gray-400">当前主题：{{ skin }}</span>
    </div>

    <div v-if="queried" class="space-y-1">
      <div
        v-for="(c, i) in chain"
        :key="`${c.file}-${i}`"
        class="flex items-center gap-2 rounded px-2 py-1.5"
        :class="
          i === hitIndex
            ? 'bg-green-50 ring-1 ring-green-300'
            : c.exists
              ? 'bg-gray-50'
              : ''
        "
      >
        <span class="w-6 text-center text-xs text-gray-400">{{ i + 1 }}</span>
        <code class="min-w-[220px] text-xs">{{ c.file }}.html</code>
        <Tag v-if="c.source" color="default">{{ c.source }}</Tag>
        <Tag v-if="i === hitIndex" color="green">命中</Tag>
        <Tag v-else-if="c.exists" color="orange">存在但被更具体者抢先</Tag>
        <Tag v-else color="red">不存在</Tag>
        <span v-if="c.theme" class="ml-auto text-xs text-gray-400">
          主题: {{ c.theme }}
        </span>
      </div>
      <div v-if="hitIndex < 0" class="mt-2 text-sm text-red-500">
        链上没有任何命中——请确认主题目录下存在候选文件（否则前台会走 404）。
      </div>
      <div class="mt-2 text-xs text-gray-400">
        规则：DB 指派 → 最具体层级 → 通用名 → index.html 兜底，命中即停（§5）。
      </div>
    </div>
  </Modal>
</template>
