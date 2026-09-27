<script lang="ts" setup>
import type { TagManualRow } from '#/api/core/template';

import { computed, onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Button, Input, message } from 'ant-design-vue';

import { getTagManualApi } from '#/api/core/template';

/**
 * D6 组件面板 + D7 在线标签手册。
 *
 * 手册数据来源于后端 TagManualService（参数名从标签源码逐个核对，不手写），
 * 因此新增标签只要在后端补条目，这里自然出现 —— 不会出现"文档比代码新/旧"的偏差。
 * 点「插入」把示例片段写回编辑器光标处，既是文档也是输入工具。
 */
let onInsert: ((snippet: string) => void) | undefined;

const [Modal, modalApi] = useEditDrawer({
  class: 'w-[70%]',
  title: '在线标签手册',
});

const manual = ref<Record<string, TagManualRow[]>>({});
const scopeOptions = ref<Record<string, string>>({});
const scope = ref<string>('');
const keyword = ref('');
const activeGroup = ref<string>('');

const groups = computed(() => Object.keys(manual.value));

/** 由文件名推定默认作用域（§9.2）：list-* → 列表页，detail-* → 内容页 */
function guessScope(file?: string): string {
  const f = (file ?? '').toLowerCase();
  if (f.startsWith('list-')) return 'list';
  if (f.startsWith('detail-')) return 'detail';
  return '';
}

async function loadManual() {
  const res = await getTagManualApi(scope.value || undefined);
  manual.value = res?.groups ?? {};
  scopeOptions.value = res?.scopeOptions ?? {};
}

const filtered = computed<Record<string, TagManualRow[]>>(() => {
  const kw = keyword.value.trim().toLowerCase();
  const out: Record<string, TagManualRow[]> = {};
  for (const [group, rows] of Object.entries(manual.value)) {
    const hit = rows.filter(
      (r) =>
        !kw ||
        r.name.toLowerCase().includes(kw) ||
        r.label.toLowerCase().includes(kw) ||
        r.usage.toLowerCase().includes(kw),
    );
    if (hit.length) {
      out[group] = hit;
    }
  }
  return out;
});

const visibleRows = computed<TagManualRow[]>(() => {
  if (activeGroup.value) {
    return filtered.value[activeGroup.value] ?? [];
  }
  return Object.values(filtered.value).flat();
});

function doInsert(row: TagManualRow) {
  onInsert?.(row.snippet);
  message.success(`已插入 ${row.name}`);
}

/** 作用域切换后重新拉取（服务端过滤，保证 global 始终保留） */
async function onScopeChange(value: string) {
  scope.value = value;
  activeGroup.value = '';
  await loadManual();
}

onMounted(async () => {
  const data = modalApi.getData() as
    | { file?: string; onInsert?: (snippet: string) => void }
    | undefined;
  onInsert = data?.onInsert;
  scope.value = guessScope(data?.file);
  await loadManual();
});
</script>

<template>
  <Modal>
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Input
        v-model:value="keyword"
        allow-clear
        class="w-56"
        placeholder="搜索标签名/说明"
      />
      <select
        class="rounded border px-2 py-1 text-sm"
        :value="scope"
        @change="onScopeChange(($event.target as HTMLSelectElement).value)"
      >
        <option value="">全部作用域</option>
        <option v-for="(label, code) in scopeOptions" :key="code" :value="code">
          {{ label }}
        </option>
      </select>
      <div class="flex flex-wrap gap-1">
        <Button
          :type="activeGroup === '' ? 'primary' : 'default'"
          size="small"
          @click="activeGroup = ''"
        >
          全部
        </Button>
        <Button
          v-for="g in groups"
          :key="g"
          :type="activeGroup === g ? 'primary' : 'default'"
          size="small"
          @click="activeGroup = g"
        >
          {{ g }}
        </Button>
      </div>
      <span class="ml-auto text-xs text-gray-400">
        共 {{ visibleRows.length }} 个标签
      </span>
    </div>

    <div
      v-for="row in visibleRows"
      :key="row.name"
      class="mb-3 rounded border p-3"
    >
      <div class="mb-1 flex items-center gap-2">
        <code class="text-sm font-semibold">&lt;@{{ row.name }}&gt;</code>
        <span class="text-xs text-gray-500">{{ row.label }} · {{ row.group }}</span>
        <span
          class="rounded bg-blue-50 px-1.5 py-0.5 text-[11px] text-blue-600"
          title="该标签可使用的模板作用域"
        >
          {{ row.scopeLabel }}
        </span>
        <Button class="ml-auto" size="small" @click="() => doInsert(row)">
          插入
        </Button>
      </div>
      <div class="mb-2 text-xs text-gray-500">{{ row.usage }}</div>
      <div class="mb-2 flex flex-wrap gap-1">
        <span
          v-for="p in row.params"
          :key="p.name"
          :class="
            p.required
              ? 'rounded bg-red-50 px-1.5 py-0.5 text-[11px] text-red-600'
              : 'rounded bg-gray-100 px-1.5 py-0.5 text-[11px] text-gray-600'
          "
          :title="p.desc"
        >
          {{ p.name }}{{ p.required ? '*' : '' }}
        </span>
        <span v-if="!row.params?.length" class="text-xs text-gray-400">
          无参数
        </span>
      </div>
      <pre
        class="overflow-auto rounded bg-[#0d1117] p-2 text-[11px] leading-5 text-gray-100"
      ><code>{{ row.snippet }}</code></pre>
      <div class="mt-1 text-xs text-gray-400">输出：{{ row.output }}</div>
    </div>

    <div v-if="!visibleRows.length" class="py-8 text-center text-gray-400">
      没有匹配的标签
    </div>
  </Modal>
</template>
