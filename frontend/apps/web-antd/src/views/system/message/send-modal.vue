<script lang="ts" setup>
import { onMounted } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Input, message, Textarea } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { sendMessageApi } from '#/api/core/message';

/**
 * 发送站内信弹窗（系统信息：is_admin=1，收件人按用户名解析）
 */
let onSaved: (() => void) | undefined;

const [Form, formApi] = useVbenForm({
  schema: [
    {
      component: 'Input',
      componentProps: { placeholder: '收件人登录用户名' },
      fieldName: 'toUsername',
      label: '收件人',
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'subject',
      label: '标题',
      rules: 'required',
    },
    {
      component: 'Textarea',
      componentProps: { rows: 5, placeholder: '信息内容' },
      fieldName: 'message',
      label: '内容',
      rules: 'required',
    },
  ],
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    modalApi.lock();
    try {
      await sendMessageApi({
        message: String(values.message ?? ''),
        subject: String(values.subject ?? ''),
        toUsername: String(values.toUsername ?? ''),
      });
      message.success('站内信已发送');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '发送站内信',
});

onMounted(() => {
  const data = modalApi.getData() as { onSaved?: () => void } | undefined;
  onSaved = data?.onSaved;
});
</script>

<template>
  <Modal>
    <Form />
  </Modal>
</template>
