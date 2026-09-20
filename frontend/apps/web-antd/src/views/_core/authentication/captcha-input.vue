<script lang="ts" setup>
import { computed, ref } from 'vue';

import { $t } from '@vben/locales';

import { Input } from 'ant-design-vue';

/**
 * 图形验证码输入框（对接后端 GET /captcha/default，码存 session key=kaptcha）。
 * 绑定契约遵循 vben 表单 adapter：baseModelPropName 为 value（v-model:value）。
 * 父组件递增 captchaKey 即可强制刷新图片（后端校验失败不销毁旧码，提交后必须刷新）。
 */
const props = defineProps<{
  captchaKey: number;
  value?: string;
}>();

const emit = defineEmits<{
  (e: 'update:value', value: string): void;
}>();

const placeholder = $t('authentication.verifyRequiredTip');
const localKey = ref(Date.now());
const src = computed(
  () => `/captcha/default?t=${props.captchaKey || localKey.value}`,
);

function refresh() {
  localKey.value = Date.now();
}
</script>

<template>
  <div class="flex w-full items-center gap-2">
    <Input
      :maxlength="4"
      :placeholder="placeholder"
      :value="props.value"
      @update:value="(v: string) => emit('update:value', v)"
    />
    <img
      :src="src"
      alt="验证码"
      class="h-[32px] min-w-[92px] cursor-pointer rounded"
      title="点击刷新"
      @click="refresh"
    />
  </div>
</template>
