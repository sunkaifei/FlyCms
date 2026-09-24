<script lang="ts" setup>
import type { GroupRow } from '#/api/core/system';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { saveGroupApi } from '#/api/core/system';

/**
 * 新增/编辑角色组弹窗
 */
const editingId = ref<null | number>(null);
let onSaved: (() => void) | undefined;

const [Form, formApi] = useVbenForm({
  schema: [
    {
      component: 'Input',
      componentProps: { placeholder: '角色组名称' },
      fieldName: 'name',
      label: '角色组名',
      rules: 'required',
    },
  ],
  showDefaultActions: false,
});

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    modalApi.lock();
    try {
      await saveGroupApi({
        id: editingId.value ?? undefined,
        name: values.name,
      });
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '角色组',
});

onMounted(() => {
  const data = modalApi.getData() as
    | { onSaved?: () => void; record?: GroupRow }
    | undefined;
  onSaved = data?.onSaved;
  editingId.value = data?.record?.id ?? null;
  if (data?.record) {
    modalApi.setState({ title: '编辑角色组' });
    formApi.setValues({ name: data.record.name });
  } else {
    modalApi.setState({ title: '新增角色组' });
  }
});
</script>

<template>
  <Modal>
    <Form />
  </Modal>
</template>
