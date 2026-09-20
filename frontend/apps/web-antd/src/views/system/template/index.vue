<script lang="ts" setup>
import type { TemplateFile } from '#/api/core/website';

import { computed, onMounted, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, message, Modal, Tree } from 'ant-design-vue';

import {
  deleteTemplateApi,
  getTemplateFilesApi,
  readTemplateApi,
  saveTemplateApi,
} from '#/api/core/website';

import NewFileModal from './new-file-modal.vue';

defineOptions({ name: 'SystemTemplate' });

const { hasAccessByCodes } = useAccess();
const canEdit = computed(() => hasAccessByCodes(['/api/system/template/save']));

interface FileRow {
  key: string;
  title: string;
  isLeaf?: boolean;
  children?: FileRow[];
}

const skin = ref('');
const fileList = ref<TemplateFile[]>([]);
const loading = ref(false);
const saving = ref(false);
const dirty = ref(false);
const currentFile = ref('');
const content = ref('');

const [NewFileModalComp, newFileModalApi] = useVbenModal({
  connectedComponent: NewFileModal,
  destroyOnClose: true,
});

/** 目录树（由扁平文件列表推导） */
const fileTree = computed<FileRow[]>(() => {
  const root: FileRow[] = [];
  const dirMap = new Map<string, FileRow>();
  const ensureDir = (path: string): FileRow => {
    if (!path) return { key: '', title: '/' };
    const existing = dirMap.get(path);
    if (existing) return existing;
    const parts = path.split('/');
    const name = parts.pop() as string;
    const parentPath = parts.join('/');
    const parent = ensureDir(parentPath);
    const dir: FileRow = { key: `dir:${path}`, title: `${name}/` };
    dirMap.set(path, dir);
    (parent.children ??= []).push(dir);
    if (!parentPath) root.push(dir);
    return dir;
  };
  for (const f of fileList.value) {
    const parts = f.file.split('/');
    const name = parts.pop() as string;
    const dir = ensureDir(parts.join('/'));
    (dir.children ??= []).push({ key: f.file, isLeaf: true, title: name });
  }
  return root;
});

async function load() {
  loading.value = true;
  try {
    const res = await getTemplateFilesApi();
    skin.value = res.skin ?? '';
    fileList.value = res.files ?? [];
  } finally {
    loading.value = false;
  }
}

async function onOpenFile(key: string, isLeaf?: boolean) {
  if (isLeaf === false || !key.endsWith('.html')) return;
  if (dirty.value) {
    Modal.confirm({
      content: `「${currentFile.value}」有未保存的修改，放弃修改并打开新文件？`,
      onOk: () => doOpen(key),
      title: '未保存提醒',
    });
    return;
  }
  await doOpen(key);
}

async function doOpen(key: string) {
  const res = await readTemplateApi(key);
  currentFile.value = res.file;
  content.value = res.content;
  dirty.value = false;
}

async function onSave() {
  if (!currentFile.value) return;
  saving.value = true;
  try {
    await saveTemplateApi(currentFile.value, content.value);
    message.success('模板已保存，前台立即生效');
    dirty.value = false;
  } finally {
    saving.value = false;
  }
}

function onDelete() {
  if (!currentFile.value) return;
  Modal.confirm({
    content: `删除模板「${currentFile.value}」？`,
    onOk: async () => {
      await deleteTemplateApi(currentFile.value);
      message.success('已删除');
      currentFile.value = '';
      content.value = '';
      dirty.value = false;
      load();
    },
    title: '删除确认',
  });
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="flex gap-3">
      <!-- 左：文件树 -->
      <div class="bg-card w-[280px] shrink-0 rounded-md border p-3">
        <div class="mb-2 flex items-center justify-between">
          <span class="text-sm font-medium">
            模板文件
            <span class="ml-1 text-xs text-gray-400">主题: {{ skin }}</span>
          </span>
          <div class="flex gap-1">
            <Button
              v-if="hasAccessByCodes(['/api/system/template/create'])"
              size="small"
              @click="() => newFileModalApi.setData({ onSaved: load }).open()"
            >
              新建
            </Button>
            <Button size="small" @click="load">刷新</Button>
          </div>
        </div>
        <Tree
          :load-data="undefined"
          :tree-data="fileTree"
          block-node
          default-expand-all
          :selected-keys="currentFile ? [currentFile] : []"
          size="small"
          @select="({ node }: any) => onOpenFile(node.key, node.isLeaf)"
        >
          <template #title="{ title }">
            <span class="font-mono text-xs">{{ title }}</span>
          </template>
        </Tree>
      </div>

      <!-- 右：在线编辑器（帝国CMS 式 textarea 编辑区） -->
      <div class="bg-card flex min-h-[70vh] flex-1 flex-col rounded-md border p-3">
        <div class="mb-2 flex items-center gap-2">
          <span class="font-mono text-sm">
            {{ currentFile || '选择左侧模板文件开始编辑' }}
          </span>
          <span v-if="dirty" class="text-xs text-orange-500">未保存</span>
          <div class="ml-auto flex gap-2">
            <Button
              v-if="currentFile && hasAccessByCodes(['/api/system/template/delete'])"
              danger
              @click="onDelete"
            >
              删除
            </Button>
            <Button
              v-if="currentFile && canEdit"
              :loading="saving"
              type="primary"
              @click="onSave"
            >
              保存模板
            </Button>
          </div>
        </div>
        <textarea
          v-model="content"
          class="min-h-[60vh] flex-1 resize-none rounded border bg-[#0d1117] p-3 font-mono text-xs leading-5 text-gray-100 outline-none"
          :disabled="!currentFile || !canEdit"
          spellcheck="false"
          wrap="off"
          @input="dirty = true"
        ></textarea>
        <div class="mt-1 text-xs text-gray-400">
          FreeMarker 尖括号语法（&lt;@ListModel model="xxx"&gt;…&lt;/@ListModel&gt;），保存后前台立即生效
        </div>
      </div>
    </div>
    <NewFileModalComp />
  </Page>
</template>
