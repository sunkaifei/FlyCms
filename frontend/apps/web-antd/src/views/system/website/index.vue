<script lang="ts" setup>
import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, Input, message, Select, Switch } from 'ant-design-vue';

import { getWebsiteConfigApi, saveWebsiteConfigApi } from '#/api/core/website';

defineOptions({ name: 'SystemWebsite' });

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const saving = ref(false);
const config = ref<Record<string, string>>({});
const skins = ref<string[]>([]);

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
