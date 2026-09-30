<script lang="ts" setup>
import { ref, watch } from 'vue';

import { Input, message, Modal, Pagination, TabPane, Tabs, Upload } from 'ant-design-vue';

import { getImagesPageApi, type ImageRow, uploadImageApi } from '#/api/core/images';

/**
 * 附件选择共用弹层（W 批次字段控件层）：
 * Tab1 直接上传（multipart → /api/system/images/upload，落 fly_images 孤儿态）
 * Tab2 附件库分页选择
 * emit picked([{id, url, name}])——单选模式选完即关。
 */
const props = defineProps<{
  /** 大小上限 MB（0=不限制，走后端 2MB 硬限） */
  maxSize?: number;
  multiple?: boolean;
  open: boolean;
}>();

const emit = defineEmits<{
  (e: 'picked', list: { id: string; name?: string; url?: string }[]): void;
  (e: 'update:open', v: boolean): void;
}>();

const tab = ref('upload');
const page = ref(1);
const keyword = ref('');
const rows = ref<ImageRow[]>([]);
const total = ref(0);
const selected = ref<Map<string, { id: string; name?: string; url?: string }>>(
  new Map(),
);

watch(
  () => props.open,
  (v) => {
    if (v) {
      tab.value = 'upload';
      selected.value = new Map();
      load();
    }
  },
);

async function load() {
  try {
    const res = await getImagesPageApi({
      keyword: keyword.value || undefined,
      p: page.value,
      rows: 20,
    });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } catch {
    rows.value = [];
  }
}

async function customRequest(options: any) {
  const file: globalThis.File = options.file;
  if (props.maxSize && file.size > props.maxSize * 1024 * 1024) {
    message.warning(`「${file.name}」超过大小上限 ${props.maxSize}MB`);
    return;
  }
  try {
    const res = await uploadImageApi(file);
    const item = { id: res.id, name: res.imgName, url: res.imgUrl };
    if (props.multiple) {
      selected.value.set(res.id, item);
      message.success(`「${file.name}」已上传（点「确定」采用）`);
    } else {
      emit('picked', [item]);
      emit('update:open', false);
    }
  } catch {
    message.error(`「${file.name}」上传失败`);
  }
}

function togglePick(row: ImageRow) {
  if (props.multiple) {
    if (selected.value.has(row.id)) {
      selected.value.delete(row.id);
    } else {
      selected.value.set(row.id, {
        id: row.id,
        name: row.imgName,
        url: row.imgUrl,
      });
    }
  } else {
    emit('picked', [{ id: row.id, name: row.imgName, url: row.imgUrl }]);
    emit('update:open', false);
  }
}

function onConfirm() {
  if (selected.value.size === 0) {
    message.warning('请先选择或上传附件');
    return;
  }
  emit('picked', [...selected.value.values()]);
  emit('update:open', false);
}
</script>

<template>
  <Modal
    :footer="multiple ? undefined : null"
    :open="open"
    :title="multiple ? '添加附件（可多选）' : '选择附件'"
    width="760px"
    @cancel="emit('update:open', false)"
    @ok="onConfirm"
  >
    <Tabs v-model:active-key="tab">
      <TabPane key="upload" tab="上传">
        <Upload.Dragger
          :custom-request="customRequest"
          :multiple="true"
          :show-upload-list="false"
          accept=".jpg,.png,.gif,.bmp,.webp"
        >
          <p class="px-6 py-6 text-sm text-gray-500">
            点击或拖拽图片到此处上传（jpg/png/gif/bmp/webp，单张{{
              maxSize ? `≤ ${maxSize}MB` : '≤ 2MB'
            }}）{{ multiple ? '，可批量' : '' }}
          </p>
        </Upload.Dragger>
        <div
          v-if="multiple && selected.size > 0"
          class="mt-2 text-xs text-gray-500"
        >
          已选 {{ selected.size }} 项（含本次上传），点「确定」采用
        </div>
      </TabPane>
      <TabPane key="library" tab="附件库">
        <div class="mb-2 flex items-center gap-2">
          <Input
            v-model:value="keyword"
            allow-clear
            class="w-56"
            placeholder="文件名/路径"
            @press-enter="
              () => {
                page = 1;
                load();
              }
            "
          />
          <Pagination
            v-model:current="page"
            :page-size="20"
            :show-size-changer="false"
            :total="total"
            size="small"
            @change="load"
          />
        </div>
        <div class="flex flex-wrap gap-2">
          <div
            v-for="row in rows"
            :key="row.id"
            :class="{
              'border-blue-500 ring-1 ring-blue-400': selected.has(row.id),
            }"
            class="cursor-pointer rounded border p-1 hover:border-blue-400"
            @click="togglePick(row)"
          >
            <img
              v-if="row.imgUrl"
              :alt="row.imgName"
              :src="row.imgUrl"
              class="h-20 w-20 rounded object-cover"
            />
            <div
              v-else
              class="flex h-20 w-20 items-center justify-center rounded bg-gray-100 text-xs"
            >
              {{ row.imgName || row.id }}
            </div>
            <div class="w-20 truncate text-center text-xs text-gray-500">
              {{ row.imgName || row.id }}
            </div>
          </div>
          <div v-if="rows.length === 0" class="p-4 text-gray-400">
            附件库为空
          </div>
        </div>
      </TabPane>
    </Tabs>
  </Modal>
</template>
