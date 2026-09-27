<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Input,
  message,
  Select,
} from 'ant-design-vue';

import {
  getSmsConfigApi,
  saveSmsConfigApi,
  testSmsApi,
} from '#/api/core/sms';
import type { SmsConfig, SmsProviderConfig } from '#/api/core/sms';

defineOptions({ name: 'SystemSms' });

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const saving = ref(false);
const data = ref<SmsConfig | null>(null);
const testPhone = ref('');
const testing = ref(false);

const canSave = computed(() => hasAccessByCodes(['/api/system/sms/save']));
const disabled = computed(() => loading.value || !canSave.value);

const FIELD_LABELS: Record<keyof SmsProviderConfig, string> = {
  ak: '访问密钥 ID',
  sk: '访问密钥 Secret',
  sign: '短信签名',
  account: '应用 / 通道标识',
  tpl_reg: '注册验证码模板',
  tpl_safe: '绑定手机验证码模板',
  tpl_reset: '找回密码验证码模板',
};

const FIELD_PLACEHOLDERS: Record<keyof SmsProviderConfig, string> = {
  ak: '阿里AccessKeyID / 腾讯SecretID / 华为APP_Key / 百度INVOKE_ID / 火山AccessKeyID',
  sk: '对应的 Secret（保存后以 ****** 显示）',
  sign: '已审核通过的短信签名',
  account: '腾讯SmsSdkAppId / 华为通道号 / 火山SmsAccount（阿里、百度可空）',
  tpl_reg: '注册验证码模板 ID',
  tpl_safe: '绑定手机验证码模板 ID',
  tpl_reset: '找回密码验证码模板 ID',
};

const current = computed<Record<string, any>>(
  () => data.value?.config?.[data.value?.provider ?? ''] as any,
);

/** 每个厂商字段的三场景模板说明 */
function fieldLabel(field: keyof SmsProviderConfig) {
  return FIELD_LABELS[field];
}

async function load() {
  loading.value = true;
  try {
    data.value = await getSmsConfigApi();
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (!data.value) return;
  saving.value = true;
  try {
    const params: Record<string, string> = { fly_sms_provider: data.value.provider };
    for (const [key, cfg] of Object.entries(data.value.config)) {
      for (const [field, value] of Object.entries(cfg)) {
        params[`fly_sms_${key}_${field}`] = value ?? '';
      }
    }
    await saveSmsConfigApi(params);
    message.success('短信设置已保存');
    await load();
  } finally {
    saving.value = false;
  }
}

async function sendTest() {
  if (!testPhone.value || !/^1[3-9]\d{9}$/.test(testPhone.value)) {
    message.warning('请填写正确的 11 位手机号');
    return;
  }
  testing.value = true;
  try {
    await testSmsApi(testPhone.value);
    message.success('测试短信已提交，请查收手机短信');
  } catch (error: any) {
    if (!error?.responded) {
      message.error(error?.message || '发送失败，请检查短信参数');
    }
  } finally {
    testing.value = false;
  }
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="max-w-[860px]">
      <div class="bg-card mb-4 rounded-md border p-4">
        <div class="mb-1 font-medium">短信服务</div>
        <div class="mb-3 text-xs text-gray-400">
          用户注册、绑定手机、找回密码的短信验证码均使用此处配置。短信签名与模板需先在各云厂商控制台
          <span class="font-medium">审核通过</span>；模板内容需包含变量 code。密钥 Secret
          保存后以 ****** 显示，留空或保持掩码则不修改。
        </div>
        <div class="grid grid-cols-1 gap-x-6 gap-y-3 md:grid-cols-2">
          <div>
            <div class="mb-1 text-sm text-gray-500">当前短信厂商</div>
            <Select
              v-if="data"
              v-model:value="data.provider"
              :disabled="disabled"
              :options="
                data.providers.map((p) => ({ label: p.label, value: p.key }))
              "
              class="w-full"
            />
          </div>
          <div
            v-if="hasAccessByCodes(['/api/system/sms/test'])"
            class="flex items-end gap-2"
          >
            <div class="flex-1">
              <div class="mb-1 text-sm text-gray-500">
                测试手机号（用注册验证码模板发送）
              </div>
              <Input
                v-model:value="testPhone"
                :disabled="loading"
                placeholder="11 位手机号"
              />
            </div>
            <Button :disabled="loading" :loading="testing" @click="sendTest">
              发送测试短信
            </Button>
          </div>
        </div>
      </div>

      <div
        v-if="current"
        class="bg-card mb-4 rounded-md border p-4"
      >
        <div class="mb-3 font-medium">
          {{ data?.providers.find((p) => p.key === data?.provider)?.label }}
          配置
        </div>
        <div class="grid grid-cols-1 gap-x-6 gap-y-3 md:grid-cols-2">
          <div v-for="(field, _) in Object.keys(current)" :key="field">
            <div class="mb-1 text-sm text-gray-500">
              {{ fieldLabel(field as keyof SmsProviderConfig) }}
            </div>
            <Input
              v-model:value="current[field]"
              :disabled="disabled"
              :placeholder="FIELD_PLACEHOLDERS[field as keyof SmsProviderConfig]"
            />
          </div>
        </div>
      </div>

      <Button
        v-if="canSave"
        :loading="saving"
        type="primary"
        @click="save"
      >
        保存设置
      </Button>
    </div>
  </Page>
</template>
