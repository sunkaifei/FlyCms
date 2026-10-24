<script lang="ts" setup>
import type { MenuNode } from '#/api/core/menu-manage';

import { computed, onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Input, InputNumber, message, Select, Switch } from 'ant-design-vue';

import { saveMenuApi, updateMenuApi } from '#/api/core/menu-manage';
import FieldItem from '../_shared/field-item.vue';

/**
 * 新增/编辑菜单节点弹窗（M 目录 / C 菜单 / F 按钮）。
 * 表单用原生受控组件（动态 schema 的类型联动复杂，这里用简单响应式布局）。
 */
const TYPE_OPTIONS = [
  { label: '目录（M）', value: 'M' },
  { label: '菜单（C）', value: 'C' },
  { label: '按钮/接口（F）', value: 'F' },
];

const nodes = ref<MenuNode[]>([]);
const form = ref<{
  actionKey: string;
  component: string;
  icon: string;
  menuName: string;
  menuType: string;
  parentId: number;
  path: string;
  remark: string;
  sort: number;
  visible: boolean;
}>({
  actionKey: '',
  component: '',
  icon: '',
  menuName: '',
  menuType: 'M',
  parentId: 0,
  path: '',
  remark: '',
  sort: 0,
  visible: true,
});

const editing = ref<null | MenuNode>(null);
let onSaved: (() => void) | undefined;

const parentOptions = computed(() => {
  const opts: { label: string; value: number }[] = [
    { label: '根目录', value: 0 },
  ];
  const walk = (list: MenuNode[], prefix: string) => {
    for (const n of list) {
      if (n.menuType === 'F') continue;
      const label = prefix + (n.menuName || n.id);
      opts.push({ label, value: n.id });
      const kids = nodes.value.filter((x) => x.parentId === n.id);
      walk(kids, label + ' / ');
    }
  };
  walk(
    nodes.value.filter((n) => !n.parentId || n.parentId === 0),
    '',
  );
  return opts;
});

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    if (!form.value.menuName.trim()) {
      message.warning('菜单名称不能为空');
      return;
    }
    if (form.value.menuType === 'C' && !form.value.actionKey.trim()) {
      message.warning('菜单节点必须填写权限标识');
      return;
    }
    if (form.value.menuType !== 'F' && !form.value.path.trim()) {
      message.warning('目录/菜单必须填写路由路径');
      return;
    }
    modalApi.lock();
    try {
      const payload: Record<string, unknown> = { ...form.value };
      if (editing.value) {
        await updateMenuApi({ ...payload, id: editing.value.id });
      } else {
        await saveMenuApi(payload);
      }
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '菜单节点',
});

onMounted(() => {
  const data = modalApi.getData() as
    | { onSaved?: () => void; parentId?: number; record?: MenuNode }
    | undefined;
  onSaved = data?.onSaved;
  form.value.parentId = data?.parentId ?? 0;
  if (data?.record) {
    editing.value = data.record;
    form.value = {
      actionKey: data.record.actionKey || '',
      component: data.record.component || '',
      icon: data.record.icon || '',
      menuName: data.record.menuName || '',
      menuType: data.record.menuType,
      parentId: data.record.parentId,
      path: data.record.path || '',
      remark: data.record.remark || '',
      sort: data.record.sort,
      visible: data.record.visible === 1,
    };
  }
});
</script>

<template>
  <Modal>
    <div class="grid grid-cols-2 gap-x-4 gap-y-3">
      <FieldItem full label="节点类型" tips="创建后不可改">
        <Select
          v-model:value="form.menuType"
          :options="TYPE_OPTIONS"
          :disabled="!!editing"
          class="w-full"
        />
      </FieldItem>
      <FieldItem label="上级节点">
        <Select
          v-model:value="form.parentId"
          :options="parentOptions"
          class="w-full"
        />
      </FieldItem>
      <FieldItem label="显示名">
        <Input v-model:value="form.menuName" placeholder="如：管理员管理" />
      </FieldItem>
      <FieldItem
        v-if="form.menuType !== 'M'"
        full
        label="权限标识 actionKey"
        :tips="
          form.menuType === 'F'
            ? '前端权限码（控制按钮显隐）'
            : '授权锚点，通常为列表接口'
        "
      >
        <Input v-model:value="form.actionKey" placeholder="/api/system/xxx/list" />
      </FieldItem>
      <FieldItem v-if="form.menuType !== 'F'" label="路由路径">
        <Input v-model:value="form.path" placeholder="/system/xxx" />
      </FieldItem>
      <FieldItem
        v-if="form.menuType === 'C'"
        label="组件路径"
        tips="相对 src/views"
      >
        <Input v-model:value="form.component" placeholder="/system/xxx/list" />
      </FieldItem>
      <FieldItem label="图标">
        <Input v-model:value="form.icon" placeholder="lucide:xxx（可选）" />
      </FieldItem>
      <FieldItem label="排序">
        <InputNumber v-model:value="form.sort" class="w-full" />
      </FieldItem>
      <div class="col-span-2 flex items-center gap-2">
        <Switch v-model:checked="form.visible" size="small" />
        <span class="text-sm">
          在导航中显示（隐藏时仅注册路由，用于详情等非菜单页）
        </span>
      </div>
    </div>
  </Modal>
</template>
