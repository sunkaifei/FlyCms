<script lang="ts" setup>
import type { FormRow } from '#/api/core/form';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Input, InputNumber, message, Textarea } from 'ant-design-vue';

import { saveFormApi } from '#/api/core/form';

/** 新增/编辑表单弹窗。formCode 创建后锁定（模板调用依赖）。 */
const editing = ref<null | FormRow>(null);
let onSaved: (() => void) | undefined;

const formCode = ref('');
const formName = ref('');
const audit = ref(false);
const submitLimit = ref(1);
const needCaptcha = ref(true);
const successTip = ref('提交成功，我们会尽快与您联系');
const notifyEmail = ref('');
const status = ref(true);

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    if (!formName.value.trim()) {
      message.warning('请填写表单名称');
      return;
    }
    if (!editing.value && !/^[a-zA-Z][a-zA-Z0-9_]*$/.test(formCode.value)) {
      message.warning('调用码只能为字母/数字/下划线，且以字母开头');
      return;
    }
    modalApi.lock();
    try {
      await saveFormApi({
        audit: audit.value ? 1 : 0,
        formCode: formCode.value,
        formName: formName.value,
        id: editing.value?.id,
        needCaptcha: needCaptcha.value ? 1 : 0,
        notifyEmail: notifyEmail.value,
        status: status.value ? 1 : 0,
        submitLimit: submitLimit.value,
        successTip: successTip.value,
      });
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '表单',
});

onMounted(() => {
  const data = modalApi.getData() as
    | { onSaved?: () => void; record?: FormRow }
    | undefined;
  onSaved = data?.onSaved;
  if (data?.record) {
    editing.value = data.record;
    formCode.value = data.record.formCode;
    formName.value = data.record.formName;
    audit.value = data.record.audit === 1;
    submitLimit.value = data.record.submitLimit ?? 1;
    needCaptcha.value = data.record.needCaptcha === 1;
    successTip.value = data.record.successTip || '';
    notifyEmail.value = data.record.notifyEmail || '';
    status.value = data.record.status === 1;
    modalApi.setState({ title: '编辑表单' });
  } else {
    modalApi.setState({ title: '新增表单' });
  }
});
</script>

<template>
  <Modal>
    <div class="grid grid-cols-2 gap-x-4 gap-y-3">
      <div>
        <div class="mb-1 text-sm">表单名称</div>
        <Input v-model:value="formName" placeholder="如：在线留言" />
      </div>
      <div>
        <div class="mb-1 text-sm">调用码（英文）</div>
        <Input
          v-model:value="formCode"
          :disabled="!!editing"
          placeholder="feedback"
        />
      </div>
      <div>
        <div class="mb-1 text-sm">每日提交上限（同一 IP/用户，0=不限）</div>
        <InputNumber v-model:value="submitLimit" :min="0" class="w-full" />
      </div>
      <div>
        <div class="mb-1 text-sm">新提交通知邮箱（留空不通知）</div>
        <Input v-model:value="notifyEmail" placeholder="admin@example.com" />
      </div>
      <div class="col-span-2">
        <div class="mb-1 text-sm">提交成功提示语</div>
        <Textarea v-model:value="successTip" :rows="2" />
      </div>
      <div class="flex items-end gap-4 pb-1">
        <label class="flex items-center gap-1 text-sm">
          <input v-model="needCaptcha" type="checkbox" />
          需要验证码
        </label>
        <label class="flex items-center gap-1 text-sm">
          <input v-model="audit" type="checkbox" />
          提交后需审核
        </label>
        <label class="flex items-center gap-1 text-sm">
          <input v-model="status" type="checkbox" />
          启用
        </label>
      </div>
    </div>
    <div class="mt-3 text-xs text-gray-400">
      前台模板调用：<code>&lt;@fly_form code="调用码"&gt;&lt;/@fly_form&gt;</code>
      ；提交接口：<code>POST /api/form/submit/调用码</code>
    </div>
  </Modal>
</template>
