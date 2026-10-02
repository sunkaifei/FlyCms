<script lang="ts" setup>
import type { ModelRow } from '#/api/core/model';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { saveModelApi, updateModelApi } from '#/api/core/model';

import AssignSelect from '../template/assign-select.vue';

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
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '显示', value: 1 },
          { label: '隐藏', value: 0 },
        ],
        optionType: 'button',
      },
      defaultValue: 1,
      fieldName: 'useContent',
      label: '详细内容页签',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '显示', value: 1 },
          { label: '隐藏', value: 0 },
        ],
        optionType: 'button',
      },
      defaultValue: 1,
      fieldName: 'useSeo',
      label: 'SEO设置页签',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '开放', value: 1 },
          { label: '关闭', value: 0 },
        ],
        optionType: 'button',
      },
      defaultValue: 1,
      fieldName: 'enableComment',
      label: '前台评论',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '开放', value: 1 },
          { label: '关闭', value: 0 },
        ],
        optionType: 'button',
      },
      defaultValue: 1,
      fieldName: 'enableSubmit',
      label: '前台投稿',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '开启', value: 1 },
          { label: '关闭', value: 0 },
        ],
        optionType: 'button',
      },
      defaultValue: 0,
      fieldName: 'localizeImages',
      label: '图片本地化（保存时自动抓取编辑器外站图片到本地）',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '后台可发', value: 1 },
          { label: '仅前台生成', value: 0 },
        ],
        optionType: 'button',
      },
      defaultValue: 1,
      fieldName: 'adminCreate',
      label: '后台新增（仅前台生成=隐藏后台新增与内容菜单，保留审核）',
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
      useContent: editing.value.useContent === 0 ? 0 : 1,
      useSeo: editing.value.useSeo === 0 ? 0 : 1,
      enableComment: editing.value.enableComment === 0 ? 0 : 1,
      enableSubmit: editing.value.enableSubmit === 0 ? 0 : 1,
      adminCreate: editing.value.adminCreate === 0 ? 0 : 1,
      localizeImages: editing.value.localizeImages === 1 ? 1 : 0,
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
    formApi.setValues({ sort: 0, useContent: 1, useSeo: 1, enableComment: 1, enableSubmit: 1 });
  }
});
</script>

<template>
  <Modal>
    <Form />

    <!--
      P2-3 模型级模板指派：modelData/form-modal.vue 已做「内容级(CONTENT/DETAIL)」，
      这里补「模型级(MODEL)」——整模列表/详情默认版式。target_id = 模型 code。
      仅编辑态可见：新增时模型尚未落库，指派无意义。
    -->
    <div v-if="editing" class="mt-4 space-y-3 border-t pt-3">
      <div class="text-sm font-medium">模板指派</div>
      <AssignSelect
        page-type="LIST"
        :target-id="editing.code"
        target-type="MODEL"
        slot-prefix="list-"
      />
      <AssignSelect
        page-type="DETAIL"
        :target-id="editing.code"
        target-type="MODEL"
        slot-prefix="detail-"
      />
    </div>
  </Modal>
</template>
