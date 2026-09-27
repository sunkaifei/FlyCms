<script lang="ts" setup>
/**
 * 主题市场（规划 §8.1 / P5）：把"换模板"变成"点按钮"。
 *
 * 卡片墙展示全部皮肤（缩略图/版本/作者/使用中状态），
 * 支持：启用（预检→切换→失败自动回滚）、预览（?__skin 仅自己可见）、
 * 创建子主题（声明式 parent，不复制文件）、回滚、以及模板派生（把通用模板另存为栏目/模型专属模板）。
 */
import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, message, Modal, Tag } from 'ant-design-vue';

import {
  checkThemeApi,
  createChildThemeApi,
  deleteSkinApi,
  deriveTemplateApi,
  downloadBlob,
  enableThemeApi,
  exportSkinApi,
  getDeriveTargetsApi,
  getThemeListApi,
  importSkinApi,
  previewThemeApi,
  rollbackThemeApi,
} from '#/api/core/template';
import type { ThemeInfo } from '#/api/core/template';

defineOptions({ name: 'SystemTheme' });

const { hasAccessByCodes } = useAccess();
const canManage = computed(() => hasAccessByCodes(['/api/system/theme/enable']));
const canCreateChild = computed(() =>
  hasAccessByCodes(['/api/system/theme/createChild']),
);
/** 导入/导出/删除走皮肤管理接口（/api/system/skin/*），权限节点各自独立 */
const canImport = computed(() => hasAccessByCodes(['/api/system/skin/import']));
const canExport = computed(() => hasAccessByCodes(['/api/system/skin/export']));
const canDelete = computed(() => hasAccessByCodes(['/api/system/skin/delete']));

const loading = ref(false);
const themes = ref<ThemeInfo[]>([]);
const current = ref('');

/** P2-1 主题包导入 */
const importInput = ref<HTMLInputElement>();
const importing = ref(false);

// 派生弹窗
const deriveVisible = ref(false);
const deriveSkin = ref('');
const deriveFile = ref('list.html');
const deriveTarget = ref('');
const deriveSlots = ref<{ target: string; label: string; exists: boolean }[]>([]);
const deriving = ref(false);

async function load() {
  loading.value = true;
  try {
    const res = await getThemeListApi();
    themes.value = res.themes || [];
    current.value = res.current || '';
  } catch (e) {
    message.error('加载主题列表失败');
  } finally {
    loading.value = false;
  }
}

async function handleEnable(code: string) {
  const check = await checkThemeApi(code);
  if (check.code !== 0 && !check.data?.ok) {
    Modal.warning({
      title: '兼容性预检未通过',
      content: (check.data?.blocks || []).join('\n') || check.msg,
    });
    return;
  }
  const warnings = check.data?.warnings || [];
  Modal.confirm({
    title: `启用主题「${code}」？`,
    content: warnings.length
      ? '警告：' + warnings.join('；') + '。仍要启用吗？'
      : '启用后访客将立即看到该主题，可随时回滚。',
    onOk: async () => {
      const r = await enableThemeApi(code);
      if (r.code === 0) {
        message.success('已启用：' + code);
        await load();
      } else {
        message.error(r.msg || '启用失败');
      }
    },
  });
}

async function handlePreview(code: string) {
  const r = await previewThemeApi(code);
  const url = r.data?.url;
  if (url) {
    window.open(url, '_blank');
  } else {
    message.error(r.msg || '获取预览地址失败');
  }
}

async function handleRollback() {
  const r = await rollbackThemeApi();
  if (r.code === 0) {
    message.success('已回滚到上一主题');
    await load();
  } else {
    message.error(r.msg || '回滚失败');
  }
}

async function handleCreateChild(parent: string) {
  Modal.confirm({
    title: `基于「${parent}」创建子主题`,
    content: '子主题只放要改的文件，其余自动回退父主题。请输入子主题目录名（字母/数字/下划线/中划线）：',
    onOk: async () => {
      const child = (window.prompt('子主题目录名：') || '').trim();
      if (!child) {
        return;
      }
      const r = await createChildThemeApi(child, parent, child);
      if (r.code === 0) {
        message.success('子主题已创建，去模板部件页编辑要覆盖的文件即可');
        await load();
      } else {
        message.error(r.msg || '创建失败');
      }
    },
  });
}

/** P2-1 导出主题包（zip） */
async function handleExport(code: string) {
  try {
    const blob = await exportSkinApi(code);
    downloadBlob(blob, `${code}.zip`);
    message.success(`已开始下载 ${code}.zip`);
  } catch {
    message.error('导出失败');
  }
}

/** P2-1 触发文件选择 → 导入主题包 */
function handleImportClick() {
  importInput.value?.click();
}

async function onImportPicked(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }
  Modal.confirm({
    content: `导入「${file.name}」？同名主题的文件会被覆盖（overwrite=1）。`,
    onOk: async () => {
      importing.value = true;
      try {
        const r = await importSkinApi(file, true);
        if (r.code === 0) {
          message.success('导入成功');
          await load();
        } else {
          message.error(r.msg || '导入失败');
        }
      } finally {
        importing.value = false;
      }
    },
    title: '导入主题包',
  });
}

/** P2-1 删除主题：有子主题时后端会拒绝（避免子主题失去父级） */
function handleDelete(code: string, name: string) {
  Modal.confirm({
    content: `删除主题「${name || code}」（目录 ${code}）？该操作会删掉整个主题目录，不可撤销。`,
    okButtonProps: { danger: true },
    okText: '删除',
    onOk: async () => {
      const r = await deleteSkinApi(code);
      if (r.code === 0) {
        message.success('已删除');
        await load();
      } else {
        message.error(r.msg || '删除失败');
      }
    },
    title: '确认删除主题',
  });
}

async function openDerive(code: string) {
  deriveSkin.value = code;
  deriveFile.value = 'list.html';
  deriveTarget.value = '';
  deriving.value = false;
  try {
    const res = await getDeriveTargetsApi(code);
    deriveSlots.value = (res.slots || []).map((s) => ({
      exists: s.exists,
      label: s.label,
      target: s.target,
    }));
  } catch {
    deriveSlots.value = [];
  }
  deriveVisible.value = true;
}

async function doDerive() {
  if (!deriveTarget.value) {
    message.warning('请选择派生的目标模板名');
    return;
  }
  deriving.value = true;
  try {
    const r = await deriveTemplateApi(
      deriveFile.value,
      deriveTarget.value,
      deriveSkin.value,
      true,
    );
    if (r.code === 0) {
      message.success(`已派生 ${deriveTarget.value}，去模板编辑器修改它即可让该栏目/模型单独换版式`);
      deriveVisible.value = false;
    } else {
      message.error(r.msg || '派生失败');
    }
  } finally {
    deriving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <Page title="主题市场" description="换主题 = 点一下；改模板 = 选一下；加页面 = 建一个文件">
    <template #extra>
      <div class="flex items-center gap-2">
        <input
          ref="importInput"
          accept=".zip"
          class="hidden"
          type="file"
          @change="onImportPicked"
        />
        <Button
          v-if="canImport"
          :loading="importing"
          @click="handleImportClick"
        >
          导入主题包
        </Button>
        <Button v-if="canManage" danger @click="handleRollback">回滚上一主题</Button>
      </div>
    </template>

    <div v-if="loading" class="p-4 text-center text-gray-400">加载中…</div>

    <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <div
        v-for="t in themes"
        :key="t.code"
        class="rounded-lg border border-gray-200 p-4 transition hover:shadow-md"
      >
        <div class="mb-3 h-32 overflow-hidden rounded bg-gray-50">
          <img
            v-if="t.thumbnail"
            :alt="t.name"
            class="h-full w-full object-cover"
            :src="t.thumbnail"
            @error="(e: Event) => ((e.target as HTMLImageElement).style.display = 'none')"
          />
          <div
            v-else
            class="flex h-full items-center justify-center text-xs text-gray-400"
          >
            无缩略图
          </div>
        </div>

        <div class="mb-2 flex items-center justify-between">
          <span class="text-base font-semibold">{{ t.name }}</span>
          <Tag v-if="t.code === current" color="green">使用中</Tag>
          <Tag v-else color="default">{{ t.code }}</Tag>
        </div>

        <div class="mb-2 flex flex-wrap gap-1">
          <Tag v-if="t.version">v{{ t.version }}</Tag>
          <Tag v-if="t.author">{{ t.author }}</Tag>
          <Tag v-if="t.parentCode" color="blue">子主题·{{ t.parentCode }}</Tag>
        </div>

        <p class="mb-3 min-h-[40px] text-sm text-gray-500">
          {{ t.description || '（未提供描述）' }}
        </p>

        <div class="flex flex-wrap gap-2">
          <Button
            v-if="canManage && t.code !== current"
            type="primary"
            size="small"
            @click="handleEnable(t.code)"
          >
            启用
          </Button>
          <Button size="small" @click="handlePreview(t.code)">预览</Button>
          <Button size="small" @click="openDerive(t.code)">派生模板</Button>
          <Button
            v-if="canExport"
            size="small"
            @click="handleExport(t.code)"
          >
            导出
          </Button>
          <Button
            v-if="canCreateChild"
            size="small"
            @click="handleCreateChild(t.code)"
          >
            创建子主题
          </Button>
          <Button
            v-if="canDelete && t.code !== current"
            danger
            size="small"
            @click="handleDelete(t.code, t.name)"
          >
            删除
          </Button>
        </div>
      </div>
    </div>

    <Modal
      v-model:open="deriveVisible"
      title="模板派生（另存为层级槽位）"
      ok-text="派生"
      :confirm-loading="deriving"
      @ok="doDerive"
    >
      <p class="mb-2 text-sm text-gray-500">
        从「{{ deriveSkin }}」的通用模板另存为更具体的模板，即可让某栏目/模型单独换版式（配合模板层级）。
      </p>
      <div class="mb-2">
        <span class="mr-2">源模板：</span>
        <Tag>list.html</Tag>
      </div>
      <div class="mb-2">
        <span class="mr-2">目标槽位：</span>
        <a-select
          v-model:value="deriveTarget"
          class="w-full"
          placeholder="选择或输入目标模板名，如 list-news.html"
          show-search
          :options="
            deriveSlots.map((s) => ({
              label: s.label + (s.exists ? '（已存在）' : ''),
              value: s.target,
            }))
          "
        />
      </div>
      <p class="text-xs text-gray-400">
        也可直接输入任意层级名（list-{channel}.html / detail-{model}.html 等），命名规则见开发文档 §5。
      </p>
    </Modal>
  </Page>
</template>
