<script lang="ts" setup>
import type { PresetCard } from '#/api/core/preset';

import { onMounted, ref, reactive } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Alert,
  Button,
  Card,
  Checkbox,
  Modal,
  Tag,
  message,
} from 'ant-design-vue';

import {
  applyPresetApi,
  getPresetDetailApi,
  getPresetListApi,
  type PresetDetail,
} from '#/api/core/preset';

defineOptions({ name: 'SystemPreset' });

const { hasAccessByCodes } = useAccess();
const canApply = hasAccessByCodes(['/api/system/preset/apply']);

const cards = ref<PresetCard[]>([]);
const loading = ref(false);

// 向导状态
const wizardOpen = ref(false);
const step = ref(1);
const applying = ref(false);
const current = ref<PresetCard | null>(null);
const detail = ref<PresetDetail | null>(null);
const optOverwrite = ref(false);
const optSample = ref(false);
const report = ref<string[]>([]);

async function load() {
  loading.value = true;
  try {
    cards.value = (await getPresetListApi()) ?? [];
  } finally {
    loading.value = false;
  }
}

async function openWizard(card: PresetCard) {
  current.value = card;
  step.value = 1;
  report.value = [];
  const res = await getPresetDetailApi(card.code);
  detail.value = res;
  wizardOpen.value = true;
}

async function confirmApply() {
  if (!current.value) return;
  applying.value = true;
  try {
    const j = await applyPresetApi(current.value.code, {
      overwriteTemplates: optOverwrite.value,
      withSampleContent: optSample.value,
    });
    report.value = [
      ...(j.data ?? []),
      '',
      j.message ?? '应用完成',
    ];
    step.value = 2;
    load();
    message.success('预设已应用');
  } finally {
    applying.value = false;
  }
}

const typeLabel = reactive<Record<string, string>>({});
function initTypeLabel() {
  // 预设类型的中文说明（与官方预设包对应）
  cards.value.forEach((c) => {
    const map: Record<string, string> = {
      qa: '问答网站',
      blog: '个人博客',
      photos: '图片站',
      corp: '企业官网',
      classifieds: '分类信息',
      mall: '商城',
      blank: '空白站',
    };
    typeLabel[c.code] = map[c.code] ?? c.name;
  });
}

onMounted(async () => {
  await load();
  initTypeLabel();
});
</script>

<template>
  <Page
    title="建站向导"
    description="选一个站点类型，一键生成完整站点（模型/字段/分类/栏目/导航/页面）。生成后可再应用其他类型做增量扩展；已应用的类型重复应用不会破坏现有数据。"
  >
    <Alert
      class="mb-4"
      message="首次建站？选一张卡片点「使用此类型建站」，一分钟得到一个能跑的完整站点。"
      show-icon
      type="info"
    />

    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
      <Card
        v-for="card in cards"
        :key="card.code"
        :loading="loading"
        class="shadow-sm"
      >
        <template #title>
          <div class="flex items-center gap-2">
            <span>{{ card.name }}</span>
            <Tag v-if="card.applied" color="green">已应用</Tag>
          </div>
        </template>
        <p class="min-h-[48px] text-sm text-gray-500">
          {{ card.description }}
        </p>
        <p class="mb-3 text-xs text-gray-400">
          含 {{ card.modelCount }} 个数据模型 · {{ card.templateCount }} 个页面模板
        </p>
        <Button
          v-if="canApply"
          block
          type="primary"
          @click="openWizard(card)"
        >
          {{ card.applied ? '重新应用 / 增量修复' : '使用此类型建站' }}
        </Button>
      </Card>
    </div>

    <Modal
      v-model:open="wizardOpen"
      :confirm-loading="applying"
      :ok-text="step === 1 ? '开始应用' : '完成'"
      :title="`建站向导 — ${current?.name ?? ''}`"
      :width="640"
      @cancel="wizardOpen = false"
      @ok="
        () => {
          if (step === 1) confirmApply();
          else wizardOpen = false;
        }
      "
    >
      <template v-if="step === 1 && detail">
        <Alert
          class="mb-3"
          message="应用是幂等的：已存在的对象会跳过，不会破坏现有数据。"
          show-icon
          type="info"
        />
        <ul class="mb-3 text-sm">
          <li>数据模型：{{ detail.modelCount }} 个</li>
          <li>字段：{{ detail.fieldCount }} 个 / 分类：{{ detail.categoryCount }} 个</li>
          <li>栏目：{{ detail.channelCount }} 个 / 导航：{{ detail.guideCount }} 项</li>
          <li>页面模板：{{ detail.templateCount }} 个</li>
          <li v-if="detail.sampleCount">示例内容：{{ detail.sampleCount }} 条（可选）</li>
        </ul>
        <div class="flex flex-col gap-2">
          <Checkbox v-model:checked="optOverwrite">
            覆盖已存在的页面模板（默认跳过以保护你的修改）
          </Checkbox>
          <Checkbox v-model:checked="optSample">
            插入示例内容（便于快速看到站点效果，可一键清空）
          </Checkbox>
        </div>
      </template>
      <template v-else-if="step === 2">
        <Alert
          class="mb-3"
          message="应用完成！以下是执行报告。"
          show-icon
          type="success"
        />
        <div class="max-h-[280px] overflow-auto rounded bg-gray-50 p-3 text-xs">
          <div v-for="(line, i) in report" :key="i">{{ line }}</div>
        </div>
        <p class="mt-2 text-xs text-gray-500">
          现在可以：访问前台查看站点；进「模型管理」调整字段；进「模板管理」微调页面。
        </p>
      </template>
    </Modal>
  </Page>
</template>
