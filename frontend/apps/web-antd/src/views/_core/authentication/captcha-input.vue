<script lang="ts" setup>
import { computed, ref } from 'vue';

import { $t } from '@vben/locales';

import { Input } from 'ant-design-vue';

/**
 * 图形验证码输入框（对接后端 GET /captcha/default，码存 session key=kaptcha）。
 * 绑定契约：作为未注册的自定义组件进 vben 表单时走默认 modelValue（form-field 的
 * resolveModelPropName 对非字符串组件回退 DEFAULT_MODEL_PROP_NAME='modelValue'），
 * 并在 schema 字段上显式声明 modelPropName: 'modelValue' 双保险。
 * 父组件递增 captchaKey 即可强制刷新图片（后端校验失败不销毁旧码，提交后必须刷新）。
 */
const props = defineProps<{
  captchaKey: number;
  modelValue?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
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
      :value="props.modelValue"
      @update:value="(v: string) => emit('update:modelValue', v)"
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
