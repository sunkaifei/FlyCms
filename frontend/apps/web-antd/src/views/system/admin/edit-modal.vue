<script lang="ts" setup>
import type { AdminRow, GroupRow } from '#/api/core/system';

import { onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  getGroupListApi,
  saveAdminApi,
  updateAdminApi,
} from '#/api/core/system';

/**
 * 新增/编辑管理员弹窗。
 * connectedComponent + destroyOnClose：每次打开重新挂载，onMounted 里读 getData() 初始化。
 */
const editingId = ref<null | number>(null);
let onSaved: (() => void) | undefined;

const [Form, formApi] = useVbenForm({
  schema: [
    {
      component: 'Input',
      fieldName: 'adminName',
      label: '账号',
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'nickName',
      label: '昵称',
    },
    {
      component: 'Select',
      componentProps: {
        options: [],
        placeholder: '请选择角色组',
      },
      fieldName: 'roleId',
      label: '角色组',
      rules: 'selectRequired',
    },
    {
      component: 'InputPassword',
      componentProps: {
        placeholder: '6-30位字符',
      },
      fieldName: 'password',
      label: '密码',
    },
    {
      component: 'InputPassword',
      componentProps: {
        placeholder: '再次输入密码',
      },
      fieldName: 'repassword',
      label: '确认密码',
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
      if (editingId.value) {
        await updateAdminApi({ ...values, id: editingId.value });
      } else {
        await saveAdminApi(values);
      }
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '管理员',
});

onMounted(async () => {
  const data = modalApi.getData() as
    | { onSaved?: () => void; record?: AdminRow }
    | undefined;
  onSaved = data?.onSaved;
  editingId.value = data?.record?.id ?? null;

  const groups = (await getGroupListApi()) ?? [];
  formApi.updateSchema([
    {
      componentProps: {
        options: groups.map((g: GroupRow) => ({ label: g.name, value: g.id })),
      },
      fieldName: 'roleId',
    },
  ]);

  if (data?.record) {
    modalApi.setState({ title: '编辑管理员' });
    formApi.setValues({
      adminName: data.record.adminName,
      nickName: data.record.nickName,
      roleId: data.record.roleId,
    });
    formApi.updateSchema([
      {
        componentProps: { placeholder: '留空则不修改密码' },
        fieldName: 'password',
      },
    ]);
  } else {
    modalApi.setState({ title: '新增管理员' });
  }
});
</script>

<template>
  <Modal>
    <Form />
  </Modal>
</template>
