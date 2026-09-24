<script lang="ts" setup>
import type { TemplateFile } from '#/api/core/website';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import { Button, message, Modal, Tabs, Tree } from 'ant-design-vue';

import {
  deleteTemplateApi,
  getTemplateFilesApi,
  readTemplateApi,
  saveTemplateApi,
} from '#/api/core/website';
import { checkTemplateApi, previewTemplateApi } from '#/api/core/template';

import NewFileModal from './new-file-modal.vue';
import SkinModal from './skin-modal.vue';
import TagManualModal from './tag-manual-modal.vue';
import VersionModal from './version-modal.vue';

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

/** 编辑器 DOM：标签手册插入片段时定位光标 */
const editorRef = ref<HTMLTextAreaElement | null>(null);
/** D8 试渲染结果 html，为空则不展示预览区 */
const previewHtml = ref('');
const checking = ref(false);
const previewing = ref(false);
const rightTab = ref<'code' | 'preview'>('code');

const [NewFileModalComp, newFileModalApi] = useEditDrawer({
  connectedComponent: NewFileModal,
  destroyOnClose: true,
});

const [SkinModalComp, skinModalApi] = useEditDrawer({
  connectedComponent: SkinModal,
  destroyOnClose: true,
});

const [VersionModalComp, versionModalApi] = useEditDrawer({
  connectedComponent: VersionModal,
  destroyOnClose: true,
});

const [TagManualModalComp, tagManualModalApi] = useEditDrawer({
  connectedComponent: TagManualModal,
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
    const fileNode: FileRow = { key: f.file, isLeaf: true, title: name };
    if (parts.length === 0) {
      root.push(fileNode);
    } else {
      const dir = ensureDir(parts.join('/'));
      (dir.children ??= []).push(fileNode);
    }
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
  previewHtml.value = '';
  rightTab.value = 'code';
}

/** D8 保存前语法校验：不放过任何会写坏的模板（后端同样会在 save 时校验） */
async function onCheck() {
  if (!currentFile.value) return;
  checking.value = true;
  try {
    const res = await checkTemplateApi(currentFile.value, content.value);
    const ok = Number((res as any)?.code ?? 200) === 200;
    if (ok) {
      message.success('语法检查通过');
    } else {
      message.error(((res as any)?.msg as string) || '语法错误');
    }
  } finally {
    checking.value = false;
  }
}

/**
 * D8 试渲染：用样例数据渲染当前编辑器内容，不落盘、不影响线上。
 * 这是"写完立刻知道长什么样"的关键 —— 不必先保存再刷前台页面。
 */
async function onPreview() {
  if (!currentFile.value) return;
  previewing.value = true;
  try {
    const res = await previewTemplateApi(content.value, currentFile.value);
    const data = (res as any)?.data;
    if (data?.html) {
      previewHtml.value = data.html as string;
      rightTab.value = 'preview';
    } else {
      message.error(((res as any)?.msg as string) || '渲染失败');
    }
  } finally {
    previewing.value = false;
  }
}

async function onSave() {
  if (!currentFile.value) return;
  saving.value = true;
  try {
    await saveTemplateApi(currentFile.value, content.value);
    message.success('模板已保存并生成版本快照，前台立即生效');
    dirty.value = false;
  } finally {
    saving.value = false;
  }
}

/** 标签手册插入片段到光标处（无选区则追加到末尾） */
function insertSnippet(snippet: string) {
  const el = editorRef.value;
  if (!el) return;
  const start = el.selectionStart ?? content.value.length;
  const end = el.selectionEnd ?? start;
  content.value =
    content.value.slice(0, start) + snippet + content.value.slice(end);
  dirty.value = true;
  rightTab.value = 'code';
}

function openSkins() {
  skinModalApi.setData({ onChanged: () => { load(); } }).open();
}

function openVersions() {
  if (!currentFile.value) {
    message.warning('请先选择一个模板文件');
    return;
  }
  versionModalApi
    .setData({ file: currentFile.value, onRestored: () => doOpen(currentFile.value) })
    .open();
}

function openTagManual() {
  tagManualModalApi.setData({ onInsert: insertSnippet }).open();
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
          @select="(_keys: any, info: any) => onOpenFile(info?.node?.key, info?.node?.isLeaf)"
        >
          <template #title="{ title }">
            <span class="font-mono text-xs">{{ title }}</span>
          </template>
        </Tree>
      </div>

      <!-- 右：在线编辑器 + 试渲染（帝国CMS 式编辑区 + 即时预览） -->
      <div class="bg-card flex min-h-[70vh] flex-1 flex-col rounded-md border p-3">
        <div class="mb-2 flex flex-wrap items-center gap-2">
          <span class="font-mono text-sm">
            {{ currentFile || '选择左侧模板文件开始编辑' }}
          </span>
          <span v-if="dirty" class="text-xs text-orange-500">未保存</span>
          <div class="ml-auto flex flex-wrap gap-2">
            <Button size="small" @click="openSkins">皮肤管理</Button>
            <Button size="small" @click="openTagManual">标签手册</Button>
            <Button size="small" @click="openVersions">版本历史</Button>
            <Button
              v-if="currentFile"
              :loading="checking"
              size="small"
              @click="onCheck"
            >
              语法检查
            </Button>
            <Button
              v-if="currentFile"
              :loading="previewing"
              size="small"
              @click="onPreview"
            >
              试渲染
            </Button>
            <Button
              v-if="currentFile && hasAccessByCodes(['/api/system/template/delete'])"
              danger
              size="small"
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

        <Tabs v-model:active-key="rightTab" class="flex-1" size="small">
          <template #tabBarExtraContent>
            <span class="text-xs text-gray-400">
              保存链路：语法校验 → 版本快照 → 落盘 → 失效缓存
            </span>
          </template>
          <Tabs.TabPane key="code" tab="模板源码">
            <textarea
              ref="editorRef"
              v-model="content"
              class="min-h-[55vh] w-full resize-none rounded border bg-[#0d1117] p-3 font-mono text-xs leading-5 text-gray-100 outline-none"
              :disabled="!currentFile || !canEdit"
              spellcheck="false"
              wrap="off"
              @input="dirty = true"
            ></textarea>
            <div class="mt-1 text-xs text-gray-400">
              FreeMarker 尖括号语法（&lt;@ListModel model="xxx"&gt;…&lt;/@ListModel&gt;），保存后前台立即生效；
              写坏的模板会被语法校验拦住，永不上线
            </div>
          </Tabs.TabPane>
          <Tabs.TabPane key="preview" tab="试渲染效果">
            <div v-if="!previewHtml" class="py-10 text-center text-sm text-gray-400">
              点击上方「试渲染」用样例数据预览当前编辑器内容（不落盘、不影响线上）
            </div>
            <iframe
              v-else
              :srcdoc="previewHtml"
              class="border"
              sandbox="allow-same-origin"
              title="模板试渲染"
              width="100%"
              height="600"
            ></iframe>
          </Tabs.TabPane>
        </Tabs>
      </div>
    </div>
    <NewFileModalComp />
    <SkinModalComp />
    <VersionModalComp />
    <TagManualModalComp />
  </Page>
</template>
