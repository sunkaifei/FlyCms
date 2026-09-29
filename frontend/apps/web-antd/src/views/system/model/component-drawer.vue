<script lang="ts" setup>
/**
 * G15 字段组库抽屉：列出全部字段组（复制式 Component），
 * 「应用到本模型」= 新建 group 字段 + 复制子字段（同名跳过）；支持删除。
 * 另存字段组在 field.vue 的行操作里发起（pickup）。
 */
import { onMounted, ref } from 'vue';

import { Button, Drawer, message, Modal } from 'ant-design-vue';

import {
  applyComponentApi,
  deleteComponentApi,
  getComponentListApi,
} from '#/api/core/model';

const props = defineProps<{
  modelId: string;
}>();

const emit = defineEmits<(e: 'applied') => void>();

const open = ref(true);
const loading = ref(false);
const components = ref<any[]>([]);

async function load() {
  loading.value = true;
  try {
    components.value = (await getComponentListApi()) ?? [];
  } finally {
    loading.value = false;
  }
}

function apply(c: any) {
  Modal.confirm({
    content: `把字段组「${c.name}」（${c.fieldCount} 个子字段）应用到当前模型？将新建 group 字段 ${c.code} 并复制子字段；与本模型同名的子字段会跳过（复制式，不做联动更新）。`,
    okText: '应用',
    onOk: async () => {
      const res: any = await applyComponentApi(props.modelId, c.id);
      message.success(res?.message ?? '已应用', 5);
      emit('applied');
    },
    title: '应用字段组',
  });
}

function onDelete(c: any) {
  Modal.confirm({
    content: `删除字段组「${c.name}」？已应用过的模型不受影响（复制式）。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteComponentApi(c.id);
      message.success('已删除');
      await load();
    },
    title: '删除确认',
  });
}

onMounted(load);
</script>

<template>
  <Drawer v-model:open="open" title="字段组库（复制式 Component）" :width="520">
    <div class="mb-3 text-xs text-gray-400">
      字段组 = 一组可复用的子字段定义。应用时会在当前模型新建 group 字段并复制子字段，
      之后各自独立演进；需要同步就重新应用一份。
    </div>
    <div v-if="components.length === 0 && !loading" class="py-8 text-center text-gray-400">
      库里还没有字段组——在字段列表对 group/repeater 字段点「存为字段组」即可入库
    </div>
    <div v-for="c in components" :key="c.id" class="mb-3 rounded border p-3">
      <div class="flex items-center justify-between">
        <div>
          <span class="font-medium">{{ c.name }}</span>
          <span class="ml-2 text-xs text-gray-400">{{ c.code }} · {{ c.fieldCount }} 子字段</span>
        </div>
        <div class="shrink-0">
          <Button class="mr-1" size="small" type="primary" @click="apply(c)">应用到本模型</Button>
          <Button danger size="small" type="link" @click="onDelete(c)">删除</Button>
        </div>
      </div>
      <div v-if="c.remark" class="mt-1 text-xs text-gray-500">{{ c.remark }}</div>
    </div>
    <template #footer>
      <Button @click="open = false">关闭</Button>
    </template>
  </Drawer>
</template>
