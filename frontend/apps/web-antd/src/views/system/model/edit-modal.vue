<script lang="ts" setup>
import type { ModelRow } from '#/api/core/model';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { saveModelApi, updateModelApi } from '#/api/core/model';

/**
 * 新增/编辑模型弹窗。code 创建后锁定（前台路由与动态表依赖）。
 */
const editing = ref<null | ModelRow>(null);
let onSaved: (() => void) | undefined;

const [Form, formApi] = useVbenForm({
  schema: [
    {
      component: 'Input',
      componentProps: { placeholder: '如：下载模型' },
      fieldName: 'name',
      label: '模型名称',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '小写字母开头的字母/数字/下划线，如 downloads' },
      fieldName: 'code',
      label: '模型标识',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '默认：标题' },
      fieldName: 'titleLabel',
      label: '标题字段名',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '后台菜单图标（可选）' },
      fieldName: 'icon',
      label: '图标',
    },
    {
      component: 'Textarea',
      componentProps: { rows: 2 },
      fieldName: 'description',
      label: '描述',
    },
    {
      component: 'InputNumber',
      fieldName: 'sort',
      label: '排序',
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
      if (editing.value) {
        await updateModelApi({ ...values, id: editing.value.id });
      } else {
        await saveModelApi(values);
        message.success('模型已创建，数据表与模板已生成');
      }
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '模型',
});

onMounted(() => {
  const data = modalApi.getData() as
    | { onSaved?: () => void; record?: ModelRow }
    | undefined;
  onSaved = data?.onSaved;
  editing.value = data?.record ?? null;
  if (editing.value) {
    modalApi.setState({ title: '编辑模型' });
    formApi.setValues({
      code: editing.value.code,
      description: editing.value.description,
      icon: editing.value.icon,
      name: editing.value.name,
      sort: editing.value.sort,
      titleLabel: editing.value.titleLabel,
    });
    // code 锁定
    formApi.updateSchema([
      {
        componentProps: { disabled: true },
        fieldName: 'code',
      },
    ]);
  } else {
    modalApi.setState({ title: '新增模型' });
    formApi.setValues({ sort: 0 });
  }
});
</script>

<template>
  <Modal>
    <Form />
  </Modal>
</template>
