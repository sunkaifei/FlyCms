<script lang="ts" setup>
import type { LinkRow } from '#/api/core/links';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Input, InputNumber, message, Select } from 'ant-design-vue';

import { saveLinkApi } from '#/api/core/links';

/** 新增/编辑友链弹窗 */
const TYPE_OPTIONS = [
  { label: '文字链接', value: 0 },
  { label: 'LOGO 链接', value: 1 },
];

const editing = ref<null | LinkRow>(null);
let onSaved: (() => void) | undefined;

const linkName = ref('');
const linkUrl = ref('');
const linkLogo = ref('');
const type = ref(0);
const sort = ref(0);
const isShow = ref(true);

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    if (!linkName.value.trim()) {
      message.warning('请填写网站名称');
      return;
    }
    if (!linkUrl.value.trim()) {
      message.warning('请填写网站网址');
      return;
    }
    modalApi.lock();
    try {
      await saveLinkApi({
        id: editing.value?.id,
        isShow: isShow.value ? 1 : 0,
        linkLogo: linkLogo.value,
        linkName: linkName.value,
        linkUrl: linkUrl.value,
        sort: sort.value,
        type: type.value,
      });
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '友情链接',
});

onMounted(() => {
  const data = modalApi.getData() as
    | { onSaved?: () => void; record?: LinkRow }
    | undefined;
  onSaved = data?.onSaved;
  if (data?.record) {
    editing.value = data.record;
    linkName.value = data.record.linkName;
    linkUrl.value = data.record.linkUrl;
    linkLogo.value = data.record.linkLogo || '';
    type.value = data.record.type ?? 0;
    sort.value = data.record.sort ?? 0;
    isShow.value = data.record.isShow === 1;
    modalApi.setState({ title: '编辑友情链接' });
  } else {
    modalApi.setState({ title: '新增友情链接' });
  }
});
</script>

<template>
  <Modal>
    <div class="grid grid-cols-2 gap-x-4 gap-y-3">
      <div>
        <div class="mb-1 text-sm">网站名称</div>
        <Input v-model:value="linkName" placeholder="如：开源之家" />
      </div>
      <div>
        <div class="mb-1 text-sm">网站网址</div>
        <Input v-model:value="linkUrl" placeholder="https://" />
      </div>
      <div>
        <div class="mb-1 text-sm">链接类型</div>
        <Select v-model:value="type" :options="TYPE_OPTIONS" class="w-full" />
      </div>
      <div>
        <div class="mb-1 text-sm">排序</div>
        <InputNumber v-model:value="sort" class="w-full" />
      </div>
      <div class="col-span-2">
        <div class="mb-1 text-sm">LOGO 地址（LOGO 链接必填）</div>
        <Input v-model:value="linkLogo" placeholder="/upload/xxx.png" />
      </div>
      <div class="flex items-end gap-2 pb-1">
        <input id="link-show" v-model="isShow" type="checkbox" />
        <label class="text-sm" for="link-show">前台显示</label>
      </div>
    </div>
  </Modal>
</template>
