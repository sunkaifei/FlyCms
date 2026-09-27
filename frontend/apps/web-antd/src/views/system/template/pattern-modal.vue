<script lang="ts" setup>
/**
 * 区块图案库（P3-3 / 规划 §7.1、§8.2 组件面板）
 *
 * 与「模板部件」的区别：部件（parts/*.html）是页头页脚那类被 include 的位置件；
 * 图案（patterns/*.html）是"想加一块内容时抄一段"的素材——只读展示 + 一键插入，
 * 不参与模板解析链。图案内容来自当前主题包，不在后台改（避免两个可写入口）。
 */
import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Button, Empty, Input, message } from 'ant-design-vue';

import { getPatternListApi, readPatternApi } from '#/api/core/template';
import type { PatternRow } from '#/api/core/template';

let onInsert: ((snippet: string) => void) | undefined;

const [Modal, modalApi] = useEditDrawer({
  class: 'w-[72%]',
  title: '区块图案库',
});

const patterns = ref<PatternRow[]>([]);
const keyword = ref('');
const skin = ref('');
const loading = ref(false);

function filtered() {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return patterns.value;
  return patterns.value.filter(
    (p) =>
      p.name.toLowerCase().includes(kw) ||
      p.file.toLowerCase().includes(kw) ||
      (p.desc ?? '').toLowerCase().includes(kw),
  );
}

async function doInsert(row: PatternRow) {
  loading.value = true;
  try {
    const res = await readPatternApi(row.file, skin.value || undefined);
    onInsert?.(res?.content ?? '');
    message.success(`已插入图案：${row.name}`);
  } catch {
    message.error('读取图案失败');
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  const data = modalApi.getData() as
    | { onInsert?: (snippet: string) => void; skin?: string }
    | undefined;
  onInsert = data?.onInsert;
  skin.value = data?.skin ?? '';
  try {
    const res = await getPatternListApi(skin.value || undefined);
    patterns.value = res?.patterns ?? [];
    skin.value = res?.skin ?? skin.value;
  } catch {
    patterns.value = [];
  }
});
</script>

<template>
  <Modal>
    <div class="mb-3 flex items-center gap-2">
      <Input
        v-model:value="keyword"
        allow-clear
        class="w-64"
        placeholder="搜索图案名/文件名/说明"
      />
      <span class="text-xs text-gray-400">主题：{{ skin || '当前主题' }}</span>
      <span class="ml-auto text-xs text-gray-400">
        共 {{ filtered().length }} 个图案
      </span>
    </div>

    <Empty
      v-if="!patterns.length"
      description="当前主题没有 patterns/ 目录。在主题包里建 patterns/*.html 即可出现在这里。"
    />

    <div v-else class="space-y-3">
      <div
        v-for="p in filtered()"
        :key="p.file"
        class="flex items-start gap-3 rounded border p-3"
      >
        <div class="min-w-0 flex-1">
          <div class="flex items-center gap-2">
            <span class="text-sm font-semibold">{{ p.name }}</span>
            <code class="text-xs text-gray-400">patterns/{{ p.file }}</code>
          </div>
          <div class="mt-1 text-xs text-gray-500">
            {{ p.desc || '（无说明；可在文件首行写 <#-- name: xx | desc: yy --> ）' }}
          </div>
        </div>
        <Button
          :loading="loading"
          size="small"
          type="primary"
          @click="doInsert(p)"
        >
          插入
        </Button>
      </div>
    </div>
  </Modal>
</template>
