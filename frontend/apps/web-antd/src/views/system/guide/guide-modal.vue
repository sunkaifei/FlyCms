<script lang="ts" setup>
import type { GuideRow } from '#/api/core/guide';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Input, InputNumber, message } from 'ant-design-vue';

import { saveGuideApi } from '#/api/core/guide';

/** 新增/编辑导航弹窗 */
const editing = ref<null | GuideRow>(null);
let onSaved: (() => void) | undefined;

const name = ref('');
const link = ref('');
const sort = ref(0);
const status = ref(true);

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    if (!name.value.trim()) {
      message.warning('请填写导航名称');
      return;
    }
    if (!link.value.trim()) {
      message.warning('请填写链接地址');
      return;
    }
    modalApi.lock();
    try {
      await saveGuideApi({
        id: editing.value?.id,
        link: link.value,
        name: name.value,
        sort: sort.value,
        status: status.value ? 1 : 0,
      });
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '导航',
});

onMounted(() => {
  const data = modalApi.getData() as
    | { onSaved?: () => void; record?: GuideRow }
    | undefined;
  onSaved = data?.onSaved;
  if (data?.record) {
    editing.value = data.record;
    name.value = data.record.name;
    link.value = data.record.link;
    sort.value = data.record.sort ?? 0;
    status.value = data.record.status === 1;
    modalApi.setState({ title: '编辑导航' });
  } else {
    modalApi.setState({ title: '新增导航' });
  }
});
</script>

<template>
  <Modal>
    <div class="grid grid-cols-2 gap-x-4 gap-y-3">
      <div>
        <div class="mb-1 text-sm">导航名称</div>
        <Input v-model:value="name" placeholder="如：产品官网" />
      </div>
      <div>
        <div class="mb-1 text-sm">链接地址</div>
        <Input v-model:value="link" placeholder="/about 或 https://" />
      </div>
      <div>
        <div class="mb-1 text-sm">排序（越小越靠前）</div>
        <InputNumber v-model:value="sort" class="w-full" />
      </div>
      <div class="flex items-end gap-2 pb-1">
        <input id="guide-status" v-model="status" type="checkbox" />
        <label class="text-sm" for="guide-status">前台显示</label>
      </div>
    </div>
  </Modal>
</template>
