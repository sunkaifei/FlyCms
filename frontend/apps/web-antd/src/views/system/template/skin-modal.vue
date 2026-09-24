<script lang="ts" setup>
import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Button, Input, message, Popconfirm, Select, Space } from 'ant-design-vue';

import {
  createSkinApi,
  deleteSkinApi,
  downloadBlob,
  exportSkinApi,
  getSkinListApi,
  importSkinApi,
} from '#/api/core/template';
import { saveWebsiteConfigApi } from '#/api/core/website';

/**
 * D2/D3/D4/D5 皮肤管理：新建（可从现有皮肤复制）、删除、启用、导出包、导入包。
 *
 * 「启用」本质是写网站配置 pc_theme —— 模板路径始终按该配置解析，所以切换即时生效；
 * 删除当前使用中的皮肤会被后端拒绝，避免出现"前台无模板可用"的空窗。
 */
let onChanged: (() => void) | undefined;

const [Modal, modalApi] = useEditDrawer({
  class: 'w-[55%]',
  title: '皮肤管理',
});

const skins = ref<string[]>([]);
const current = ref('');
const loading = ref(false);

const newSkin = ref('');
const copyFrom = ref('');
const creating = ref(false);

const importFile = ref<File | null>(null);
const overwrite = ref(false);
const importing = ref(false);

async function load() {
  loading.value = true;
  try {
    const res = await getSkinListApi();
    skins.value = res.skins ?? [];
    current.value = res.current ?? '';
  } finally {
    loading.value = false;
  }
}

async function onCreate() {
  const name = newSkin.value.trim();
  if (!/^[a-zA-Z][\w\-]{0,49}$/.test(name)) {
    message.warning('皮肤名只能由字母开头，含字母/数字/下划线/中划线，最长 50 位');
    return;
  }
  creating.value = true;
  try {
    await createSkinApi(name, copyFrom.value || undefined);
    message.success('皮肤已创建');
    newSkin.value = '';
    copyFrom.value = '';
    await load();
    onChanged?.();
  } finally {
    creating.value = false;
  }
}

/** 启用 = 写 pc_theme 配置 */
async function onEnable(name: string) {
  await saveWebsiteConfigApi({ pc_theme: name });
  message.success(`已启用皮肤「${name}」，前台模板即时切换`);
  current.value = name;
  onChanged?.();
}

async function onDelete(name: string) {
  await deleteSkinApi(name);
  message.success('皮肤已删除');
  await load();
  onChanged?.();
}

async function onExport(name: string) {
  const blob = await exportSkinApi(name);
  downloadBlob(blob, `${name}.zip`);
}

function pickFile(e: Event) {
  const target = e.target as HTMLInputElement;
  importFile.value = target.files?.[0] ?? null;
}

async function onImport() {
  if (!importFile.value) {
    message.warning('请先选择皮肤包（zip）');
    return;
  }
  importing.value = true;
  try {
    const res = await importSkinApi(importFile.value, overwrite.value);
    const code = (res as any)?.code;
    if (code !== undefined && code !== 200) {
      message.error(((res as any)?.msg as string) || '导入失败');
      return;
    }
    message.success('皮肤包已导入');
    importFile.value = null;
    overwrite.value = false;
    await load();
    onChanged?.();
  } finally {
    importing.value = false;
  }
}

onMounted(() => {
  const data = modalApi.getData() as { onChanged?: () => void } | undefined;
  onChanged = data?.onChanged;
  load();
});
</script>

<template>
  <Modal>
    <div v-if="loading" class="py-6 text-center text-gray-400">加载中…</div>
    <div v-else>
      <div class="mb-3 text-sm font-medium">当前皮肤</div>
      <div class="mb-4">
        <span class="mr-2 font-mono text-sm">{{ current || '未设置' }}</span>
        <span class="text-xs text-gray-400">
          模板路径按此配置解析：views/templates/pc_theme/{{ current }}
        </span>
      </div>

      <div class="mb-3 text-sm font-medium">皮肤列表</div>
      <div class="mb-4">
        <div
          v-for="s in skins"
          :key="s"
          class="mb-2 flex items-center gap-2 rounded border px-3 py-2"
        >
          <span class="w-40 font-mono text-sm">{{ s }}</span>
          <span
            v-if="s === current"
            class="rounded bg-green-100 px-2 py-0.5 text-xs text-green-700"
          >
            使用中
          </span>
          <Space class="ml-auto">
            <Button
              v-if="s !== current"
              size="small"
              type="link"
              @click="() => onEnable(s)"
            >
              启用
            </Button>
            <Button size="small" type="link" @click="() => onExport(s)">
              导出包
            </Button>
            <Popconfirm
              :title="`删除皮肤「${s}」？其模板文件会一并移除`"
              ok-text="删除"
              @confirm="() => onDelete(s)"
            >
              <Button danger size="small" type="link">删除</Button>
            </Popconfirm>
          </Space>
        </div>
        <div v-if="!skins.length" class="text-sm text-gray-400">暂无皮肤</div>
      </div>

      <div class="mb-3 text-sm font-medium">新建皮肤</div>
      <div class="mb-4 flex items-end gap-2">
        <div>
          <div class="mb-1 text-xs text-gray-500">皮肤目录名</div>
          <Input v-model:value="newSkin" class="w-52" placeholder="如 blue_v2" />
        </div>
        <div>
          <div class="mb-1 text-xs text-gray-500">从现有皮肤复制（可选）</div>
          <Select
            v-model:value="copyFrom"
            :options="[
              { label: '空白皮肤', value: '' },
              ...skins.map((s) => ({ label: s, value: s })),
            ]"
            class="w-52"
          />
        </div>
        <Button :loading="creating" type="primary" @click="onCreate">创建</Button>
      </div>

      <div class="mb-3 text-sm font-medium">导入皮肤包</div>
      <div class="flex items-center gap-2">
        <input accept=".zip" type="file" @change="pickFile" />
        <label class="flex items-center gap-1 text-xs">
          <input v-model="overwrite" type="checkbox" />
          覆盖同名模板
        </label>
        <Button :loading="importing" @click="onImport">导入</Button>
      </div>
      <div class="mt-2 text-xs text-gray-400">
        上传包需为 zip，解压目标限定在当前皮肤目录内（后端做 zip-slip 防御 + 后缀白名单 + 50MB 上限）
      </div>
    </div>
  </Modal>
</template>
