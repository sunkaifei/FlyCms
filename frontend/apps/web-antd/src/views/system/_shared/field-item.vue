<script lang="ts" setup>
/**
 * 后台表单统一字段容器（2026-10-23）。
 *
 * <p>背景：此前每个表单页面手写
 * {@code <div><div class="mb-1 text-sm">* 标签</div><控件/></div>}，
 * 全后台 17 个文件 103 处。手写模式的固有缺陷：
 *   · 标签是 div 不是 label → 点击标签不聚焦控件，无障碍缺失；
 *   · 必填红星靠人肉加，漏加是必然；
 *   · 没有错误态容器 → 校验只能弹 toast，用户找不到错在哪一项；
 *   · 间距写死 mb-1，与页面其他区块对不齐。
 *
 * 本组件把「标签 + 必填标记 + 提示 + 错误」收敛为一个位置，
 * 控件通过默认插槽传入，任何页面引用即自动对齐。
 */
defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    /** 字段标签 */
    label?: string;
    /** 字段名（技术名，灰色小字附在标签后，方便排查模板变量） */
    name?: string;
    required?: boolean;
    /** 常规提示（灰色） */
    tips?: string;
    /** 错误信息（红色，出现时替换 tips） */
    error?: string;
    /** 占满栅格两列（长文本/图组/富文本用） */
    full?: boolean;
  }>(),
  { full: false },
);
</script>

<template>
  <div :class="props.full ? 'col-span-2' : ''">
    <label class="mb-1 block text-sm">
      <span v-if="props.required" class="mr-0.5 text-red-500">*</span>
      {{ props.label }}
      <span v-if="props.name" class="ml-1 text-xs text-gray-400">
        {{ props.name }}
      </span>
    </label>
    <slot />
    <div v-if="props.error" class="mt-0.5 text-xs text-red-500">
      {{ props.error }}
    </div>
    <div v-else-if="props.tips" class="mt-0.5 text-xs text-gray-400">
      {{ props.tips }}
    </div>
  </div>
</template>
