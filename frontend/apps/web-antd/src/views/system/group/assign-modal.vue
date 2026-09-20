<script lang="ts" setup>
import type { GroupRow, PermissionRow } from '#/api/core/system';

import { computed, onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Checkbox, message } from 'ant-design-vue';

import {
  assignGroupPermissionsApi,
  getAllPermissionsApi,
  getGroupPermissionIdsApi,
} from '#/api/core/system';

/**
 * 角色组分配权限弹窗：全量勾选提交，后端做增删差量。
 * 超级管理员组（id=1）后端硬编码保护，前端直接禁用提交。
 */
const groupId = ref(0);
const groupName = ref('');
const permissions = ref<PermissionRow[]>([]);
const checkedIds = ref<number[]>([]);
const loading = ref(false);
let onSaved: (() => void) | undefined;

const isSuperGroup = computed(() => groupId.value === 1);

/** 按后端 controller 字段分组展示（与老后台分配页一致） */
const grouped = computed(() => {
  const map = new Map<string, PermissionRow[]>();
  for (const p of permissions.value) {
    const key = p.controller || '其他';
    const list = map.get(key) ?? [];
    list.push(p);
    map.set(key, list);
  }
  return [...map.entries()];
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    if (isSuperGroup.value) {
      message.warning('超级管理员组权限不能修改');
      return;
    }
    modalApi.lock();
    try {
      await assignGroupPermissionsApi(groupId.value, checkedIds.value);
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '分配权限',
});

onMounted(async () => {
  const data = modalApi.getData() as
    | { group: GroupRow; onSaved?: () => void }
    | undefined;
  onSaved = data?.onSaved;
  groupId.value = data?.group?.id ?? 0;
  groupName.value = data?.group?.name ?? '';

  loading.value = true;
  try {
    const [all, checked] = await Promise.all([
      getAllPermissionsApi(),
      getGroupPermissionIdsApi(groupId.value),
    ]);
    permissions.value = all ?? [];
    checkedIds.value = checked ?? [];
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <Modal>
    <div class="max-h-[60vh] overflow-y-auto">
      <p class="mb-3 text-sm">
        为「{{ groupName }}」配置可访问的操作权限（未勾选的操作后端会返回 403）：
      </p>
      <a-empty v-if="!loading && permissions.length === 0" description="暂无权限节点，请先在权限管理页执行「同步权限」" />
      <div
        v-for="[controller, list] in grouped"
        :key="controller"
        class="mb-4"
      >
        <div class="mb-2 text-sm font-semibold">{{ controller }}</div>
        <Checkbox.Group v-model:value="checkedIds" class="flex flex-wrap gap-y-2">
          <Checkbox
            v-for="p in list"
            :key="p.id"
            :disabled="isSuperGroup"
            :value="p.id"
          >
            {{ p.actionKey }}
          </Checkbox>
        </Checkbox.Group>
      </div>
    </div>
  </Modal>
</template>
