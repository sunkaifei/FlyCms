<script lang="ts" setup>
/**
 * 模板指派下拉（P2-3 / 规划 §8.3，对应 WP 后台 Page Template 下拉）
 *
 * 三处指派共用同一个 DB 覆盖层（fly_template_assign）：
 *   - CONTENT：单篇内容 → detail-*.html（modeldata/form-modal.vue）
 *   - CHANNEL：栏目 → list-*.html / page-*.html（本组件，targetId = 栏目目录名）
 *   - MODEL  ：模型 → list-*.html / detail-*.html（本组件，targetId = 模型 code）
 *
 * target_id 口径（P2-2 已定）：CHANNEL 用**目录名**而非数字 ID——
 * 前台路由就是 /{channelDir}，且栏目重建后目录名不变、数字 ID 会漂移。
 */
import { computed, ref, watch } from 'vue';

import { useAccess } from '@vben/access';

import { message, Select } from 'ant-design-vue';

import {
  assignTemplateApi,
  getAssignListApi,
  getDeriveTargetsApi,
  unassignTemplateApi,
} from '#/api/core/template';

const props = withDefaults(
  defineProps<{
    /** 页面类型：LIST / DETAIL / CHANNEL_PAGE / INDEX / SEARCH / TAG / ERROR */
    pageType: string;
    /** 槽位文件前缀，如 list- / detail- / page-；空串表示不过滤 */
    slotPrefix?: string;
    /** 指派目标 id：栏目目录名 / 模型 code */
    targetId: string;
    /** 目标类型：CHANNEL / MODEL / CONTENT */
    targetType: string;
  }>(),
  { slotPrefix: '' },
);

const { hasAccessByCodes } = useAccess();
const canAssign = computed(() =>
  hasAccessByCodes(['/api/system/template/assign']),
);

const value = ref<undefined | string>();
const options = ref<{ label: string; value: string }[]>([]);
const loading = ref(false);

async function load() {
  if (!props.targetId) {
    value.value = undefined;
    options.value = [];
    return;
  }
  loading.value = true;
  try {
    const [assignRes, slotRes] = await Promise.all([
      getAssignListApi(props.targetType),
      getDeriveTargetsApi(),
    ]);
    const mine = (assignRes.list ?? []).find(
      (r) =>
        r.pageType === props.pageType &&
        r.targetId === String(props.targetId),
    );
    value.value = mine?.template;

    const seen = new Set<string>();
    const list: { label: string; value: string }[] = [];
    for (const s of slotRes.slots ?? []) {
      if (props.slotPrefix && !s.target.startsWith(props.slotPrefix)) continue;
      if (seen.has(s.target)) continue;
      seen.add(s.target);
      list.push({ label: s.label, value: s.target });
    }
    options.value = list;
  } catch {
    options.value = [];
  } finally {
    loading.value = false;
  }
}

async function onChange(v: unknown) {
  if (!props.targetId) return;
  const tpl = typeof v === 'string' && v ? v : undefined;
  try {
    if (tpl) {
      await assignTemplateApi(
        props.targetType,
        String(props.targetId),
        props.pageType,
        tpl,
      );
      message.success(`已指派：该${labelOf()}使用 ${tpl}`);
    } else {
      await unassignTemplateApi(
        props.targetType,
        String(props.targetId),
        props.pageType,
      );
      message.success('已取消指派，恢复层级默认模板');
    }
  } catch {
    message.error('指派失败，请重试');
  }
}

function labelOf() {
  if (props.targetType === 'CHANNEL') return '栏目';
  if (props.targetType === 'MODEL') return '模型';
  return '内容';
}

watch(() => [props.targetId, props.targetType, props.pageType], load, {
  immediate: true,
});

defineExpose({ reload: load });
</script>

<template>
  <div>
    <div class="mb-1 text-sm">模板指派</div>
    <Select
      v-model:value="value"
      allow-clear
      class="w-full"
      :disabled="!canAssign || !targetId"
      :loading="loading"
      :options="options"
      placeholder="不选 = 按模板层级自动匹配"
      show-search
      @change="onChange"
    />
    <div class="mt-1 text-xs text-gray-400">
      指派后立即生效，优先于模板层级（{{ pageType }}）。列表为空时先在「主题市场 → 派生模板」生成对应槽位。
    </div>
  </div>
</template>
