<script lang="ts" setup>
import { onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Input, message } from 'ant-design-vue';

import { createTemplateApi } from '#/api/core/website';

/**
 * 新建模板文件弹窗：相对当前皮肤的路径（如 downloads/list.html）
 */
const file = ref('');
let onSaved: (() => void) | undefined;

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const path = file.value.trim();
    if (!path || !path.endsWith('.html')) {
      message.warning('请填写以 .html 结尾的相对路径');
      return;
    }
    await createTemplateApi(path);
    message.success('模板已创建');
    onSaved?.();
    modalApi.close();
  },
  title: '新建模板',
});

onMounted(() => {
  const data = modalApi.getData() as { onSaved?: () => void } | undefined;
  onSaved = data?.onSaved;
  file.value = '';
});
</script>

<template>
  <Modal>
    <div class="mb-1 text-sm">模板路径（相对当前主题目录）</div>
    <Input v-model:value="file" placeholder="如：downloads/list.html 或 article/new_page.html" />
    <div class="mt-2 text-xs text-gray-400">
      仅允许 .html 文件；子目录不存在时自动创建
    </div>
  </Modal>
</template>
