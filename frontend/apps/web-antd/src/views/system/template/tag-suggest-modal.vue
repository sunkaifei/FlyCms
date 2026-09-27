<script lang="ts" setup>
import type { TagSuggestResult } from '#/api/core/template';

import { computed, onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Alert, Button, message, Skeleton, Tag } from 'ant-design-vue';

import { tagSuggestApi } from '#/api/core/template';

/**
 * P8 智能标签建议（规划 §8.2）。
 *
 * 与「标签手册」的分工：手册是**全部标签的字典**（我知道名字来找用法），
 * 这里是**当前文件的反向索引**（我打开某个模板，它告诉我该用哪些标签、能拉哪些字段）。
 *
 * 模型从哪里来？按文件名推定（list-news.html → news，articles/detail.html → articles），
 * 推不出来就不硬编：只给该页面类型的通用骨架，并把「未识别到模型」如实显示出来。
 * 骨架里的模型 code 与筛选字段都是后端填好的，点了就能跑，不用手改占位符。
 */
let onInsert: ((snippet: string) => void) | undefined;
let file = '';
let pageType = '';

const [Modal, modalApi] = useEditDrawer({
  class: 'w-[62%]',
  title: '智能标签建议',
});

const loading = ref(false);
const data = ref<TagSuggestResult | null>(null);

const fields = computed(() => data.value?.fields ?? []);
const suggestions = computed(() => data.value?.suggestions ?? []);

function doInsert(item: { code: string; title: string }) {
  onInsert?.(item.code);
  message.success(`已插入「${item.title}」骨架`);
}

async function load() {
  loading.value = true;
  try {
    data.value = await tagSuggestApi(file, pageType);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  const d = modalApi.getData() as
    | { file?: string; onInsert?: (s: string) => void; pageType?: string }
    | undefined;
  file = d?.file ?? '';
  pageType = d?.pageType ?? '';
  onInsert = d?.onInsert;
  modalApi.setState({ title: `智能标签建议 · ${file}` });
  load();
});
</script>

<template>
  <Modal>
    <Skeleton v-if="loading" active :paragraph="{ rows: 6 }" />

    <template v-else-if="data">
      <Alert
        v-if="data.detected"
        :message="`识别到模型：${data.modelName || data.model}（${data.model}）· 页面类型 ${data.pageType}`"
        class="mb-3"
        show-icon
        type="success"
      />
      <Alert
        v-else
        :message="`未从文件名识别出模型，以下为该页面类型（${data.pageType}）的通用骨架`"
        class="mb-3"
        description="文件名遵循 list-模型code.html / detail-模型code.html / 模型code/list.html 时才能自动带出字段。"
        show-icon
        type="warning"
      />

      <!-- 该模型可用的字段（列表列 / 筛选 / 搜索） -->
      <div v-if="fields.length" class="mb-4">
        <div class="mb-2 text-sm font-medium">
          该模型字段
          <span class="ml-1 text-xs font-normal text-gray-400">
            红色 = 可筛选（isFilter）
          </span>
        </div>
        <div class="flex flex-wrap gap-1">
          <Tag
            v-for="f in fields"
            :key="f.name"
            :color="f.isFilter ? 'red' : 'default'"
            :title="`${f.label} · 类型 ${f.type}${f.isSearch ? ' · 可搜索' : ''}`"
          >
            {{ f.name }}
            <span class="opacity-60">· {{ f.label }}</span>
          </Tag>
        </div>
      </div>
      <div v-else class="mb-4 text-xs text-gray-400">
        未识别到模型，无字段清单 —— 不影响下方通用骨架的使用。
      </div>

      <!-- 可复制的标签骨架 -->
      <div
        v-for="(s, i) in suggestions"
        :key="i"
        class="mb-3 rounded border p-3"
      >
        <div class="mb-1 flex items-center gap-2">
          <span class="text-sm font-semibold">{{ s.title }}</span>
          <Button class="ml-auto" size="small" type="primary" @click="() => doInsert(s)">
            插入到编辑器
          </Button>
        </div>
        <div class="mb-2 text-xs text-gray-500">{{ s.note }}</div>
        <pre
          class="max-h-64 overflow-auto rounded bg-[#0d1117] p-2 text-[11px] leading-5 text-gray-100"
        ><code>{{ s.code }}</code></pre>
      </div>

      <div v-if="!suggestions.length" class="py-8 text-center text-gray-400">
        暂无建议
      </div>
    </template>
  </Modal>
</template>
