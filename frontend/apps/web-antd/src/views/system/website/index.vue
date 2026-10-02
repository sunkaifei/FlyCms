<script lang="ts" setup>
import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Input,
  message,
  Select,
  Switch,
  Textarea,
} from 'ant-design-vue';

import {
  getWebsiteConfigApi,
  saveWebsiteConfigApi,
  testEmailApi,
} from '#/api/core/website';

defineOptions({ name: 'SystemWebsite' });

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const saving = ref(false);
const config = ref<Record<string, string>>({});
const skins = ref<string[]>([]);
const testEmail = ref('');
const testing = ref(false);

/** SMTP 加密方式（fly_smtp_ssl：1 SSL / 2 STARTTLS / 0 不加密） */
const SSL_OPTIONS = [
  { label: 'SSL（465，QQ/163 常用）', value: '1' },
  { label: 'STARTTLS（587）', value: '2' },
  { label: '不加密（25）', value: '0' },
];

/** 表单分组展示（帝国/Dede 的"系统设置"式分区） */
const GROUPS: { items: { key: string; label: string; placeholder?: string }[]; title: string }[] = [
  {
    items: [
      { key: 'fly_title', label: '网站名称', placeholder: '' },
      { key: 'fly_url', label: '网站地址', placeholder: 'https://...' },
      { key: 'fly_logo', label: 'Logo 地址', placeholder: '/logo.png（可选）' },
    ],
    title: '基本信息',
  },
  {
    items: [
      { key: 'fly_seo_title', label: 'SEO 标题' },
      { key: 'fly_seo_keywords', label: 'SEO 关键词' },
      { key: 'fly_seo_description', label: 'SEO 描述' },
    ],
    title: 'SEO 设置',
  },
  {
    items: [
      { key: 'master', label: '站长' },
      { key: 'qq', label: 'QQ' },
      { key: 'email', label: '邮箱' },
      { key: 'mobile', label: '手机' },
      { key: 'phone', label: '电话' },
      { key: 'address', label: '地址' },
    ],
    title: '联系方式',
  },
  {
    items: [
      {
        key: 'fly_ai_base_url',
        label: 'AI 服务地址',
        placeholder: 'OpenAI 兼容，如 https://api.deepseek.com/v1',
      },
      { key: 'fly_ai_api_key', label: 'AI API Key', placeholder: '留空 = AI 功能停用' },
      { key: 'fly_ai_model', label: '对话模型', placeholder: '如 deepseek-chat' },
      {
        key: 'fly_ai_embed_model',
        label: '向量模型（语义搜索）',
        placeholder: '如 text-embedding-v3；留空 = 语义搜索停用',
      },
      { key: 'fly_mcp_token', label: 'MCP 接入令牌', placeholder: '留空 = /mcp 端点关闭' },
    ],
    title: 'AI 与自动化',
  },
  {
    items: [
      { key: 'fly_wm_enabled', label: '水印开关（1 开 / 0 关）', placeholder: '1 = 上传图片自动加水印' },
      {
        key: 'fly_wm_type',
        label: '水印类型（text 文字 / image 图片）',
        placeholder: 'text',
      },
      { key: 'fly_wm_text', label: '水印文字', placeholder: '如 @ 我的网站' },
      { key: 'fly_wm_font_size', label: '文字字号（12–72）', placeholder: '16' },
      { key: 'fly_wm_font_color', label: '文字颜色', placeholder: '#FFFFFF' },
      {
        key: 'fly_wm_image',
        label: '水印图片路径（type=image 用）',
        placeholder: '/upload/...（附件库里的水印图）',
      },
      {
        key: 'fly_wm_position',
        label: '位置（1 左上 2 上中 3 右上 4 左中 5 居中 6 右中 7 左下 8 下中 9 右下 10 随机）',
        placeholder: '9',
      },
      { key: 'fly_wm_opacity', label: '透明度 %（10–100）', placeholder: '60' },
      { key: 'fly_wm_margin', label: '边距 px（0–100）', placeholder: '10' },
      { key: 'fly_wm_min_width', label: '加印最小图宽（小于跳过）', placeholder: '300' },
      { key: 'fly_wm_quality', label: 'JPG 压缩质量（50–100）', placeholder: '85' },
      { key: 'fly_img_domain', label: '图片本地化域名（Q3，空=当前域名）', placeholder: 'img.example.com' },
    ],
    title: '图片上传与水印',
  },
  {
    items: [
      { key: 'fly_pay_gateway', label: '支付网关', placeholder: 'mock = 内置模拟（真实接入需填密钥）' },
      { key: 'fly_pay_merchant_id', label: '商户号', placeholder: '支付网关商户 ID' },
      { key: 'fly_pay_api_key', label: '支付密钥', placeholder: '网关签名密钥' },
    ],
    title: '支付设置',
  },
];

async function load() {
  loading.value = true;
  try {
    const res = await getWebsiteConfigApi();
    config.value = res.config ?? {};
    skins.value = res.skins ?? [];
  } finally {
    loading.value = false;
  }
}

async function save() {
  saving.value = true;
  try {
    await saveWebsiteConfigApi(config.value);
    message.success('站点设置已保存');
  } finally {
    saving.value = false;
  }
}

async function sendTestEmail() {
  if (!testEmail.value || !testEmail.value.includes('@')) {
    message.warning('请填写正确的测试收件邮箱');
    return;
  }
  testing.value = true;
  try {
    await testEmailApi(testEmail.value);
    message.success(`测试邮件已发送，请到 ${testEmail.value} 收件箱（含垃圾邮件）查收`);
  } catch (error: any) {
    // requestClient 拦截器已弹出 message 时这里无需重复提示
    if (!error?.responded) {
      message.error(error?.message || '发送失败，请检查 SMTP 参数');
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
      <div
        v-for="group in GROUPS"
        :key="group.title"
        class="bg-card mb-4 rounded-md border p-4"
      >
        <div class="mb-3 font-medium">{{ group.title }}</div>
        <div class="grid grid-cols-1 gap-x-6 gap-y-3 md:grid-cols-2">
          <div v-for="item in group.items" :key="item.key">
            <div class="mb-1 text-sm text-gray-500">{{ item.label }}</div>
            <Input
              v-model:value="config[item.key]"
              :disabled="loading || !hasAccessByCodes(['/api/system/website/save'])"
              :placeholder="item.placeholder || ''"
            />
          </div>
        </div>
      </div>

      <div class="bg-card mb-4 rounded-md border p-4">
        <div class="mb-3 font-medium">SEO 与收录</div>
        <div class="grid grid-cols-1 gap-y-3">
          <div>
            <div class="mb-1 text-sm text-gray-500">robots.txt 内容（即时生效，访问 /robots.txt）</div>
            <Textarea v-model:value="config.fly_robots" :rows="4" class="font-mono" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="flex items-center gap-2">
              <Switch
                v-model:checked="config.fly_sitemap_status"
                checked-value="1"
                un-checked-value="0"
              />
              <span class="text-sm">开启 sitemap.xml</span>
            </div>
            <div>
              <div class="mb-1 text-sm text-gray-500">每模型收录条数</div>
              <Input v-model:value="config.fly_sitemap_limit" />
            </div>
          </div>
        </div>
      </div>

      <div class="bg-card mb-4 rounded-md border p-4">
        <div class="mb-3 font-medium">主题与状态</div>
        <div class="grid grid-cols-1 gap-x-6 gap-y-3 md:grid-cols-2">
          <div>
            <div class="mb-1 text-sm text-gray-500">PC 主题（views/templates/pc_theme 下的目录）</div>
            <Select
              v-model:value="config.pc_theme"
              :disabled="loading || !hasAccessByCodes(['/api/system/website/save'])"
              :options="skins.map((s) => ({ label: s, value: s }))"
              class="w-full"
            />
          </div>
          <div>
            <div class="mb-1 text-sm text-gray-500">移动端主题</div>
            <Select
              v-model:value="config.m_theme"
              :disabled="loading || !hasAccessByCodes(['/api/system/website/save'])"
              :options="
                (skins.length ? skins : ['defalut']).map((s) => ({ label: s, value: s }))
              "
              class="w-full"
            />
          </div>
          <div class="flex items-center gap-2">
            <Switch
              v-model:checked="config.fly_status"
              checked-value="1"
              :disabled="!hasAccessByCodes(['/api/system/website/save'])"
              un-checked-value="0"
            />
            <span class="text-sm">网站开放访问（关闭后前台提示维护）</span>
          </div>
        </div>
      </div>

      <div class="bg-card mb-4 rounded-md border p-4">
        <div class="mb-1 font-medium">邮件服务（第三方 SMTP）</div>
        <div class="mb-3 text-xs text-gray-400">
          用户注册邮箱验证、绑定邮箱、找回密码、表单提交通知等均使用此处配置；QQ/163
          邮箱需在邮箱后台开启 SMTP 并使用「授权码」而非登录密码。
        </div>
        <div class="grid grid-cols-1 gap-x-6 gap-y-3 md:grid-cols-2">
          <div>
            <div class="mb-1 text-sm text-gray-500">SMTP 服务器</div>
            <Input
              v-model:value="config.fly_smtp_server"
              :disabled="loading || !hasAccessByCodes(['/api/system/website/save'])"
              placeholder="如 smtp.qq.com"
            />
          </div>
          <div>
            <div class="mb-1 text-sm text-gray-500">端口</div>
            <Input
              v-model:value="config.fly_smtp_port"
              :disabled="loading || !hasAccessByCodes(['/api/system/website/save'])"
              placeholder="SSL 465 / STARTTLS 587 / 不加密 25"
            />
          </div>
          <div>
            <div class="mb-1 text-sm text-gray-500">加密方式</div>
            <Select
              v-model:value="config.fly_smtp_ssl"
              :disabled="loading || !hasAccessByCodes(['/api/system/website/save'])"
              :options="SSL_OPTIONS"
              class="w-full"
            />
          </div>
          <div>
            <div class="mb-1 text-sm text-gray-500">发件邮箱账号</div>
            <Input
              v-model:value="config.fly_smtp_usermail"
              :disabled="loading || !hasAccessByCodes(['/api/system/website/save'])"
              placeholder="如 noreply@example.com"
            />
          </div>
          <div>
            <div class="mb-1 text-sm text-gray-500">
              授权码 / 密码（保存后以 ****** 显示，留空或保持掩码则不修改）
            </div>
            <Input.Password
              v-model:value="config.fly_smtp_password"
              :disabled="loading || !hasAccessByCodes(['/api/system/website/save'])"
              autocomplete="new-password"
              placeholder="第三方邮箱授权码"
            />
          </div>
          <div>
            <div class="mb-1 text-sm text-gray-500">发件人昵称（可选）</div>
            <Input
              v-model:value="config.fly_smtp_fromname"
              :disabled="loading || !hasAccessByCodes(['/api/system/website/save'])"
              placeholder="收件人看到的发件人名称"
            />
          </div>
        </div>
        <div
          v-if="hasAccessByCodes(['/api/system/website/testEmail'])"
          class="mt-3 flex items-center gap-2"
        >
          <Input
            v-model:value="testEmail"
            class="max-w-[300px]"
            placeholder="填写你的邮箱，发送测试邮件验证配置"
          />
          <Button
            :disabled="loading"
            :loading="testing"
            @click="sendTestEmail"
          >
            发送测试邮件
          </Button>
        </div>
      </div>

      <Button
        v-if="hasAccessByCodes(['/api/system/website/save'])"
        :loading="saving"
        type="primary"
        @click="save"
      >
        保存设置
      </Button>
    </div>
  </Page>
</template>
