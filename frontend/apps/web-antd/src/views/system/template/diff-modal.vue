<script lang="ts" setup>
import type { TemplateDiffResult, TemplateVersionRow } from '#/api/core/template';

import { computed, onMounted, ref, watch } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Alert, Button, Select, Spin, Tag } from 'ant-design-vue';

import {
  diffTemplateApi,
  getTemplateVersionsApi,
  restoreTemplateApi,
} from '#/api/core/template';

/**
 * P8 版本差异对比（规划 §8.2「差异对比」）。
 *
 * 「版本历史」解决"回到过去"，这里解决"过去改了什么" —— 回滚前先看清楚差异，
 * 避免把有用的改动一起退掉。右侧默认「当前磁盘内容」，左侧选任意历史版本。
 * 行级 LCS diff 由后端算好（type: same/add/del），前端只负责上色，不做二次解析。
 */
let file = '';
let onRestored: (() => void) | undefined;

const [Modal, modalApi] = useEditDrawer({
  class: 'w-[78%]',
  title: '版本差异对比',
});

const versions = ref<TemplateVersionRow[]>([]);
const leftVersion = ref<number>(0);
const rightVersion = ref<number>(0);
const loading = ref(false);
const restoring = ref(false);
const result = ref<TemplateDiffResult | null>(null);

const versionOptions = computed(() => [
  { label: '（空 / 新增）', value: 0 },
  ...versions.value.map((v) => ({
    label: `v${v.version} · ${v.createTime ?? ''}`,
    value: v.version,
  })),
]);

const rightOptions = computed(() => [
  { label: '当前磁盘内容（未落库）', value: 0 },
  ...versions.value.map((v) => ({ label: `v${v.version}`, value: v.version })),
]);

async function loadVersions() {
  const res = await getTemplateVersionsApi(file);
  versions.value = res.versions ?? [];
  // 默认「次新版本 → 最新版本」= 最近一次保存改了什么。
  // 不能用「最新版本 vs 磁盘」：快照先于落盘写入，保存后两者内容恒等，diff 必然全同。
  leftVersion.value = versions.value[1]?.version ?? versions.value[0]?.version ?? 0;
  rightVersion.value = versions.value[0]?.version ?? 0;
}

async function loadDiff() {
  loading.value = true;
  try {
    result.value = await diffTemplateApi(
      file,
      leftVersion.value,
      rightVersion.value,
    );
  } finally {
    loading.value = false;
  }
}

async function onRestoreLeft() {
  if (leftVersion.value <= 0) return;
  restoring.value = true;
  try {
    await restoreTemplateApi(file, leftVersion.value);
    modalApi.close();
    onRestored?.();
  } finally {
    restoring.value = false;
  }
}

onMounted(async () => {
  const d = modalApi.getData() as
    | { file?: string; onRestored?: () => void }
    | undefined;
  file = d?.file ?? '';
  onRestored = d?.onRestored;
  modalApi.setState({ title: `版本差异对比 · ${file}` });
  await loadVersions();
  await loadDiff();
});

watch([leftVersion, rightVersion], loadDiff);
</script>

<template>
  <Modal>
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <span class="text-sm">左（旧）</span>
      <Select
        v-model:value="leftVersion"
        class="w-56"
        :options="versionOptions"
      />
      <span class="text-sm">右（新）</span>
      <Select
        v-model:value="rightVersion"
        class="w-56"
        :options="rightOptions"
      />
      <Button :loading="loading" size="small" @click="loadDiff">重新对比</Button>
      <Button
        v-if="leftVersion > 0"
        :loading="restoring"
        class="ml-auto"
        danger
        size="small"
        @click="onRestoreLeft"
      >
        回滚到左侧版本
      </Button>
    </div>

    <Alert
      v-if="result?.error"
      :message="result.error"
      class="mb-3"
      show-icon
      type="error"
    />

    <template v-else-if="result">
      <div class="mb-2 flex items-center gap-2 text-xs text-gray-400">
        <Tag color="green">+{{ result.added }}</Tag>
        <Tag color="red">-{{ result.removed }}</Tag>
        <span>
          {{ result.leftLabel }}（{{ result.leftCount }} 行） →
          {{ result.rightLabel }}（{{ result.rightCount }} 行）
        </span>
      </div>

      <Spin :spinning="loading">
        <div
          class="max-h-[58vh] overflow-auto rounded border bg-[#0d1117] font-mono text-[11px] leading-5"
        >
          <div
            v-for="(l, i) in result.lines"
            :key="i"
            class="flex"
            :class="
              l.type === 'add'
                ? 'bg-green-900/40'
                : l.type === 'del'
                  ? 'bg-red-900/40'
                  : ''
            "
          >
            <span
              class="w-12 shrink-0 select-none border-r border-gray-700 px-2 text-right text-gray-500"
            >
              {{ l.oldNo ?? '' }}
            </span>
            <span
              class="w-12 shrink-0 select-none border-r border-gray-700 px-2 text-right text-gray-500"
            >
              {{ l.newNo ?? '' }}
            </span>
            <span
              class="w-6 shrink-0 select-none text-center"
              :class="
                l.type === 'add'
                  ? 'text-green-400'
                  : l.type === 'del'
                    ? 'text-red-400'
                    : 'text-gray-600'
              "
            >
              {{ l.type === 'add' ? '+' : l.type === 'del' ? '-' : ' ' }}
            </span>
            <span
              class="whitespace-pre px-1"
              :class="l.type === 'add' ? 'text-green-200' : 'text-gray-100'"
              >{{ l.text }}</span
            >
          </div>
        </div>
      </Spin>
    </template>

    <div v-else class="py-8 text-center text-gray-400">暂无版本可对比</div>
  </Modal>
</template>
