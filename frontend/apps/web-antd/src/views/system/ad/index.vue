<script lang="ts" setup>
import type { AdPositionRow, AdRow } from '#/api/core/ad';
import type { ImageRow } from '#/api/core/images';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { useEditDrawer } from '#/utils/edit-drawer';

import {
  Alert,
  Button,
  DatePicker,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Pagination,
  RadioGroup,
  Table,
  Tag,
  Textarea,
} from 'ant-design-vue';

import {
  deleteAdApi,
  deleteAdPositionApi,
  getAdListApi,
  getAdPositionListApi,
  getAdStatApi,
  saveAdApi,
  saveAdPositionApi,
  updateAdApi,
  updateAdPositionApi,
} from '#/api/core/ad';
import { getImagesPageApi } from '#/api/core/images';

defineOptions({ name: 'SystemAd' });

const { hasAccessByCodes } = useAccess();
const canPosSave = hasAccessByCodes(['/api/system/ad/position/save']);
const canPosUpdate = hasAccessByCodes(['/api/system/ad/position/update']);
const canPosDelete = hasAccessByCodes(['/api/system/ad/position/delete']);
const canAdList = hasAccessByCodes(['/api/system/ad/ad/list']);
const canAdSave = hasAccessByCodes(['/api/system/ad/ad/save']);
const canAdUpdate = hasAccessByCodes(['/api/system/ad/ad/update']);
const canAdDelete = hasAccessByCodes(['/api/system/ad/ad/delete']);

/** 点击率（views=0 显示 -） */
function ctr(row: AdRow): string {
  const views = Number(row.countView ?? 0);
  const clicks = Number(row.countClick ?? 0);
  return views > 0 ? `${((clicks / views) * 100).toFixed(2)}%` : '-';
}

const TYPE_TEXT: Record<string, string> = {
  code: '代码',
  image: '图片',
  text: '文字',
};

// ---------------- 广告位列表 ----------------
const rows = ref<AdPositionRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const keyword = ref('');

const columns = [
  { title: '广告位名称', key: 'name' },
  { title: '调用标识', dataIndex: 'adKey', key: 'adKey' },
  { title: '建议尺寸', key: 'size', width: 110 },
  { title: '广告数', dataIndex: 'adCount', key: 'adCount', width: 80 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 250 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getAdPositionListApi({
      keyword: keyword.value || undefined,
      p: page.value,
      rows: 20,
    });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

// ---------------- 广告位新增/编辑 ----------------
const posOpen = ref(false);
const posSaving = ref(false);
const posForm = reactive({
  id: '',
  name: '',
  adKey: '',
  description: '',
  width: undefined as number | undefined,
  height: undefined as number | undefined,
  sort: 0,
  status: 1,
});

function openPosAdd() {
  Object.assign(posForm, {
    id: '',
    name: '',
    adKey: '',
    description: '',
    width: undefined,
    height: undefined,
    sort: 0,
    status: 1,
  });
  posOpen.value = true;
}

function openPosEdit(row: AdPositionRow) {
  Object.assign(posForm, {
    id: String(row.id),
    name: row.name,
    adKey: row.adKey,
    description: row.description || '',
    width: row.width,
    height: row.height,
    sort: row.sort,
    status: row.status,
  });
  posOpen.value = true;
}

async function onPosSave() {
  if (!posForm.name.trim()) {
    message.warning('广告位名称不能为空');
    return;
  }
  if (!posForm.id && !posForm.adKey.trim()) {
    message.warning('调用标识不能为空');
    return;
  }
  posSaving.value = true;
  try {
    if (posForm.id) {
      await updateAdPositionApi({ ...posForm, id: posForm.id });
      message.success('已更新（调用标识创建后不可改）');
    } else {
      await saveAdPositionApi({ ...posForm });
      message.success('已添加');
    }
    posOpen.value = false;
    load();
  } finally {
    posSaving.value = false;
  }
}

function onPosDelete(row: AdPositionRow) {
  Modal.confirm({
    content: `删除广告位「${row.name}」？位下还有广告时将拒绝删除。`,
    onOk: async () => {
      await deleteAdPositionApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

// ---------------- 广告管理（位下广告列表，右侧抽屉 75% + 可最大化） ----------------
const [AdDrawer, adDrawerApi] = useEditDrawer({
  footer: false,
});
const adLoading = ref(false);
const adRows = ref<AdRow[]>([]);
const adTotal = ref(0);
const adPage = ref(1);
const currentPosition = ref<null | AdPositionRow>(null);

const adColumns = [
  { title: '广告名称', key: 'name' },
  { title: '类型', key: 'adType', width: 70 },
  { title: '跳转/内容', key: 'target' },
  { title: '权重', dataIndex: 'weight', key: 'weight', width: 70 },
  { title: '投放时间', key: 'time', width: 200 },
  { title: '展示/点击', key: 'count', width: 100 },
  { title: '点击率', key: 'ctr', width: 80 },
  { title: '状态', key: 'status', width: 70 },
  { title: '操作', key: 'action', width: 170 },
];

async function openAds(row: AdPositionRow) {
  currentPosition.value = row;
  adPage.value = 1;
  adDrawerApi
    .setState({ title: `广告管理 — ${row.name}（${row.adKey}）` })
    .open();
  await loadAds();
}

async function loadAds() {
  if (!currentPosition.value) return;
  adLoading.value = true;
  try {
    const res = await getAdListApi({
      positionId: String(currentPosition.value.id),
      p: adPage.value,
      rows: 50,
    });
    adRows.value = res.list ?? [];
    adTotal.value = res.count ?? 0;
  } finally {
    adLoading.value = false;
  }
}

function fmtTime(row: AdRow) {
  const start = row.startTime ? row.startTime.slice(0, 10) : '立即';
  const end = row.endTime ? row.endTime.slice(0, 10) : '永久';
  return `${start} ~ ${end}`;
}

// ---------------- 广告新增/编辑 ----------------
const adEditOpen = ref(false);
const adSaving = ref(false);
const adForm = reactive({
  id: '',
  name: '',
  adType: 'image',
  imageUrl: '',
  url: '',
  textContent: '',
  htmlCode: '',
  weight: 1,
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  status: 1,
  remark: '',
  sort: 0,
});

function openAdAdd() {
  Object.assign(adForm, {
    id: '',
    name: '',
    adType: 'image',
    imageUrl: '',
    url: '',
    textContent: '',
    htmlCode: '',
    weight: 1,
    startTime: undefined,
    endTime: undefined,
    status: 1,
    remark: '',
    sort: 0,
  });
  adEditOpen.value = true;
}

function openAdEdit(row: AdRow) {
  Object.assign(adForm, {
    id: String(row.id),
    name: row.name,
    adType: row.adType,
    imageUrl: row.imageUrl || '',
    url: row.url || '',
    textContent: row.textContent || '',
    htmlCode: row.htmlCode || '',
    weight: row.weight,
    startTime: row.startTime,
    endTime: row.endTime,
    status: row.status,
    remark: row.remark || '',
    sort: row.sort,
  });
  adEditOpen.value = true;
}

async function onAdSave() {
  if (!currentPosition.value) return;
  if (!adForm.name.trim()) {
    message.warning('广告名称不能为空');
    return;
  }
  if (adForm.adType === 'image' && !adForm.imageUrl.trim()) {
    message.warning('图片广告必须填写图片地址');
    return;
  }
  if (adForm.adType === 'text' && !adForm.textContent.trim()) {
    message.warning('文字广告必须填写文字内容');
    return;
  }
  if (adForm.adType === 'code' && !adForm.htmlCode.trim()) {
    message.warning('代码广告必须填写代码');
    return;
  }
  adSaving.value = true;
  try {
    if (adForm.id) {
      await updateAdApi({ ...adForm, id: adForm.id });
      message.success('已更新');
    } else {
      await saveAdApi({
        ...adForm,
        positionId: String(currentPosition.value.id),
      });
      message.success('已添加');
    }
    adEditOpen.value = false;
    loadAds();
    load();
  } finally {
    adSaving.value = false;
  }
}

function onAdDelete(row: AdRow) {
  Modal.confirm({
    content: `删除广告「${row.name}」？`,
    onOk: async () => {
      await deleteAdApi(String(row.id));
      message.success('已删除');
      loadAds();
      load();
    },
    title: '删除确认',
  });
}

// ---------------- 单广告统计（按日明细 + 点击率） ----------------
const statOpen = ref(false);
const statLoading = ref(false);
const statRows = ref<{ clicks: number; statDate: string; views: number }[]>([]);
const statAd = ref<null | AdRow>(null);
const statDays = ref<number>(30);

function openStat(row: AdRow) {
  statAd.value = row;
  statOpen.value = true;
  loadStat();
}

async function loadStat() {
  if (!statAd.value) return;
  statLoading.value = true;
  try {
    statRows.value =
      (await getAdStatApi({ adId: String(statAd.value.id), days: statDays.value })) ?? [];
  } finally {
    statLoading.value = false;
  }
}

const statColumns = [
  { title: '日期', dataIndex: 'statDate', key: 'statDate' },
  { title: '展现', dataIndex: 'views', key: 'views', width: 100 },
  { title: '点击', dataIndex: 'clicks', key: 'clicks', width: 100 },
  { title: '点击率', key: 'ctr2', width: 100 },
];

// ---------------- 获取广告代码（联盟式 JS / 模板标签） ----------------
const codeOpen = ref(false);
const codeRow = ref<null | AdPositionRow>(null);
const codeOrigin = ref(window.location.origin);

function openCode(row: AdPositionRow) {
  codeRow.value = row;
  codeOpen.value = true;
}

/** JS 调用代码：贴到任意模板页/站外页面（同百度联盟/谷歌联盟取码模式） */
const jsCode = computed(
  () => `<script src="${codeOrigin.value}/ad/js/${codeRow.value?.adKey ?? ''}"><\/script>`,
);

/** 站内 FreeMarker 模板标签：主题内服务端直出（无 JS 依赖） */
const ftlCode = computed(() => `<@fly_ad key="${codeRow.value?.adKey ?? ''}"/>`);

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text);
    message.success('已复制到剪贴板');
  } catch {
    message.warning('复制失败，请手动选择复制');
  }
}

// ---------------- 附件库选图 ----------------
const pickerOpen = ref(false);
const pickerLoading = ref(false);
const pickerRows = ref<ImageRow[]>([]);
const pickerTotal = ref(0);
const pickerPage = ref(1);

function openPicker() {
  pickerOpen.value = true;
  pickerPage.value = 1;
  loadPicker();
}

async function loadPicker() {
  pickerLoading.value = true;
  try {
    const res = await getImagesPageApi({ p: pickerPage.value, rows: 12 });
    pickerRows.value = res.list ?? [];
    pickerTotal.value = res.count ?? 0;
  } finally {
    pickerLoading.value = false;
  }
}

function pickImage(row: ImageRow) {
  adForm.imageUrl = row.imgUrl;
  pickerOpen.value = false;
}

onMounted(load);
</script>

<template>
  <Page
    title="广告管理"
    description="广告位 → 广告 两级结构：先建广告位（如「首页轮播」），再往里投放图片/文字/代码广告。"
  >
    <Alert class="mb-3" show-icon type="info">
      <template #message>
        三步上线广告：① 新建广告位（确定投放位置 + 调用标识）→ ② 点「广告管理」往位里投放广告
        （图片 / 文字 / 代码均可）→ ③ 点「代码」复制调用码，粘贴到模板页面即生效。
      </template>
      <template #description>
        展现与点击自动计数（广告列表可见点击率，单条广告可看按天统计）；同一广告位可投多条，
        按权重降序轮换输出；停用广告位或单条广告可随时下线，配置保留。
      </template>
    </Alert>
    <div class="mb-3 flex items-center gap-2">
      <Button v-if="canPosSave" type="primary" @click="openPosAdd">
        新增广告位
      </Button>
      <Input
        v-model:value="keyword"
        allow-clear
        class="w-56"
        placeholder="名称 / 调用标识"
        @press-enter="
          () => {
            page = 1;
            load();
          }
        "
      />
      <Button @click="load">刷新</Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div class="font-medium">{{ (record as AdPositionRow).name }}</div>
          <div class="text-xs text-gray-400">
            {{ (record as AdPositionRow).description }}
          </div>
        </template>
        <template v-else-if="column.key === 'adKey'">
          <code class="rounded bg-gray-100 px-1.5 py-0.5 text-xs">
            {{ (record as AdPositionRow).adKey }}
          </code>
        </template>
        <template v-else-if="column.key === 'size'">
          {{
            (record as AdPositionRow).width
              ? `${(record as AdPositionRow).width}×${(record as AdPositionRow).height ?? '*'}`
              : '-'
          }}
        </template>
        <template v-else-if="column.key === 'status'">
          <Tag :color="(record as AdPositionRow).status === 1 ? 'green' : 'default'">
            {{ (record as AdPositionRow).status === 1 ? '启用' : '停用' }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="canAdList"
            size="small"
            type="link"
            @click="openAds(record as AdPositionRow)"
          >
            广告管理
          </Button>
          <Button
            size="small"
            type="link"
            @click="openCode(record as AdPositionRow)"
          >
            代码
          </Button>
          <Button
            v-if="canPosUpdate"
            size="small"
            type="link"
            @click="openPosEdit(record as AdPositionRow)"
          >
            编辑
          </Button>
          <Button
            v-if="canPosDelete"
            danger
            size="small"
            type="link"
            @click="onPosDelete(record as AdPositionRow)"
          >
            删除
          </Button>
        </template>
      </template>
    </Table>

    <div class="mt-3 flex justify-end">
      <Pagination
        v-model:current="page"
        :page-size="20"
        :show-size-changer="false"
        :total="total"
        size="small"
        @change="load"
      />
    </div>

    <!-- 广告位新增/编辑 -->
    <Modal
      v-model:open="posOpen"
      :confirm-loading="posSaving"
      :title="posForm.id ? '编辑广告位' : '新增广告位'"
      @ok="onPosSave"
    >
      <Form layout="vertical" class="pt-2">
        <FormItem
          extra="给自己看的投放位置名，如：首页轮播图、文章页侧栏"
          label="广告位名称"
          required
        >
          <Input v-model:value="posForm.name" placeholder="如：首页轮播" />
        </FormItem>
        <FormItem
          :extra="posForm.id
            ? '调用标识创建后不可改（模板与 JS 调用码里用的就是它）'
            : '模板与 JS 调用用的英文标识：小写字母开头，如 home_carousel；创建后不可改'"
          label="调用标识"
          :required="!posForm.id"
        >
          <Input
            v-model:value="posForm.adKey"
            :disabled="!!posForm.id"
            placeholder="小写字母开头，如 home_carousel"
          />
        </FormItem>
        <FormItem
          extra="投放位置说明，方便其他管理员理解这里展示在网站的哪个位置"
          label="描述"
        >
          <Input v-model:value="posForm.description" placeholder="如：网站首页顶部通栏" />
        </FormItem>
        <FormItem
          extra="仅作投放素材的尺寸参考（如 1200×300），不强制约束"
          label="建议尺寸(px)"
        >
          <div class="flex items-center gap-2">
            <InputNumber
              v-model:value="posForm.width"
              class="w-28"
              :min="1"
              placeholder="宽"
            />
            ×
            <InputNumber
              v-model:value="posForm.height"
              class="w-28"
              :min="1"
              placeholder="高"
            />
          </div>
        </FormItem>
        <FormItem extra="数字小的排前面（只影响广告位列表的显示顺序）" label="排序">
          <InputNumber v-model:value="posForm.sort" class="w-28" />
        </FormItem>
        <FormItem extra="停用后前台该广告位的所有广告都不再输出（配置保留）" label="状态">
          <Tag :color="posForm.status === 1 ? 'green' : 'default'">
            {{ posForm.status === 1 ? '启用' : '停用' }}
          </Tag>
          <Button
            size="small"
            @click="posForm.status = posForm.status === 1 ? 0 : 1"
          >
            {{ posForm.status === 1 ? '点击停用' : '点击启用' }}
          </Button>
        </FormItem>
      </Form>
    </Modal>

    <!-- 广告管理（位下广告列表） -->
    <AdDrawer>
    >
      <div class="mb-3 flex items-center gap-2 pt-2">
        <Button v-if="canAdSave" size="small" type="primary" @click="openAdAdd">
          新增广告
        </Button>
        <Button size="small" @click="loadAds">刷新</Button>
        <span class="text-xs text-gray-400">
          展示与点击自动计数（每条可点「统计」看按天明细）；多条广告按权重降序输出；
          调用码在广告位列表的「代码」按钮里取
        </span>
      </div>
      <Table
        :columns="adColumns"
        :data-source="adRows"
        :loading="adLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            {{ (record as AdRow).name }}
          </template>
          <template v-else-if="column.key === 'adType'">
            <Tag>{{ TYPE_TEXT[(record as AdRow).adType] ?? (record as AdRow).adType }}</Tag>
          </template>
          <template v-else-if="column.key === 'target'">
            <img
              v-if="(record as AdRow).adType === 'image' && (record as AdRow).imageUrl"
              :src="(record as AdRow).imageUrl"
              class="mr-2 inline max-h-10 rounded align-middle"
            />
            <span class="text-xs">
              {{
                (record as AdRow).adType === 'text'
                  ? (record as AdRow).textContent
                  : (record as AdRow).url || '-'
              }}
            </span>
          </template>
          <template v-else-if="column.key === 'time'">
            {{ fmtTime(record as AdRow) }}
          </template>
          <template v-else-if="column.key === 'count'">
            {{ (record as AdRow).countView ?? 0 }} /
            {{ (record as AdRow).countClick ?? 0 }}
          </template>
          <template v-else-if="column.key === 'ctr'">
            {{ ctr(record as AdRow) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="(record as AdRow).status === 1 ? 'green' : 'default'">
              {{ (record as AdRow).status === 1 ? '投放中' : '停用' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Button
              v-if="hasAccessByCodes(['/api/system/ad/stat/list'])"
              size="small"
              type="link"
              @click="openStat(record as AdRow)"
            >
              统计
            </Button>
            <Button
              v-if="canAdUpdate"
              size="small"
              type="link"
              @click="openAdEdit(record as AdRow)"
            >
              编辑
            </Button>
            <Button
              v-if="canAdDelete"
              danger
              size="small"
              type="link"
              @click="onAdDelete(record as AdRow)"
            >
              删除
            </Button>
          </template>
        </template>
      </Table>
      <div class="mt-3 flex justify-end">
        <Pagination
          v-model:current="adPage"
          :page-size="50"
          :show-size-changer="false"
          :total="adTotal"
          size="small"
          @change="loadAds"
        />
      </div>

      <!-- 广告新增/编辑 -->
      <Modal
        v-model:open="adEditOpen"
        :confirm-loading="adSaving"
        :title="adForm.id ? '编辑广告' : '新增广告'"
        width="640px"
        @ok="onAdSave"
      >
        <Form layout="vertical" class="pt-2">
          <FormItem
            extra="给自己看的名称，如：618 促销横幅、联系客服按钮"
            label="广告名称"
            required
          >
            <Input v-model:value="adForm.name" placeholder="内部标识，如：618 促销横幅" />
          </FormItem>
          <FormItem
            extra="图片=一张图点击跳转；文字=纯文字链接；代码=第三方联盟代码（百度联盟/谷歌联盟给的代码直接贴进「代码」框，原样输出）"
            label="广告类型"
          >
            <RadioGroup
              v-model:value="adForm.adType"
              :options="[
                { label: '图片广告', value: 'image' },
                { label: '文字广告', value: 'text' },
                { label: '代码广告', value: 'code' },
              ]"
            />
          </FormItem>
          <FormItem
            v-if="adForm.adType === 'image'"
            extra="从附件库选择或直接粘贴图片地址；尺寸建议参考广告位的建议尺寸"
            label="图片"
            required
          >
            <div class="flex items-center gap-2">
              <Input
                v-model:value="adForm.imageUrl"
                class="flex-1"
                placeholder="图片地址，可从附件库选择或直接粘贴"
              />
              <Button size="small" @click="openPicker">从附件库选择</Button>
            </div>
            <img
              v-if="adForm.imageUrl"
              :src="adForm.imageUrl"
              class="mt-2 max-h-24 rounded border"
            />
          </FormItem>
          <FormItem v-if="adForm.adType === 'text'" label="文字内容" required>
            <Input v-model:value="adForm.textContent" placeholder="展示的文字" />
          </FormItem>
          <FormItem v-if="adForm.adType === 'code'" label="代码" required>
            <Textarea
              v-model:value="adForm.htmlCode"
              :rows="5"
              placeholder="原样输出的 HTML/JS 代码（如第三方联盟代码）"
            />
          </FormItem>
          <FormItem
            extra="点击广告后跳转的目标地址，自动带点击统计；留空=仅展示不跳转（代码广告自身带链接时不用填）"
            label="跳转链接"
          >
            <Input v-model:value="adForm.url" placeholder="https://..." />
          </FormItem>
          <FormItem
            extra="都不填 = 立即上线且永不下线；填起止时间可实现定时上下线（如促销活动到期自动下线）"
            label="投放时间窗"
          >
            <div class="flex items-center gap-2">
              <DatePicker
                v-model:value="adForm.startTime"
                show-time
                class="flex-1"
                placeholder="开始（空=立即）"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
              <span>~</span>
              <DatePicker
                v-model:value="adForm.endTime"
                show-time
                class="flex-1"
                placeholder="结束（空=永久）"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </div>
          </FormItem>
          <div class="grid grid-cols-3 gap-3">
            <FormItem
              extra="同一广告位有多条广告同时投放时，权重大的排前面"
              label="权重"
            >
              <InputNumber v-model:value="adForm.weight" class="w-full" :min="0" />
            </FormItem>
            <FormItem extra="权重相同时按排序、再按时间倒序" label="排序">
              <InputNumber v-model:value="adForm.sort" class="w-full" />
            </FormItem>
            <FormItem extra="停用=暂不下线（配置保留），投放中=前台可见" label="状态">
              <Tag :color="adForm.status === 1 ? 'green' : 'default'">
                {{ adForm.status === 1 ? '投放中' : '停用' }}
              </Tag>
              <Button
                size="small"
                @click="adForm.status = adForm.status === 1 ? 0 : 1"
              >
                {{ adForm.status === 1 ? '点击停用' : '点击投放' }}
              </Button>
            </FormItem>
          </div>
          <FormItem label="备注">
            <Input v-model:value="adForm.remark" />
          </FormItem>
        </Form>
      </Modal>

      <!-- 单广告统计 -->
      <Modal
        v-model:open="statOpen"
        :footer="null"
        :title="`统计 — ${statAd?.name ?? ''}`"
        width="560px"
      >
        <div class="mb-3 flex items-center gap-3 pt-2">
          <span class="text-sm">
            合计：展现 {{ statAd?.countView ?? 0 }} / 点击
            {{ statAd?.countClick ?? 0 }} /
            点击率 {{ ctr(statAd as AdRow) }}
          </span>
          <Button
            :class="statDays === 7 ? '' : 'text-gray-400'"
            size="small"
            :type="statDays === 7 ? 'primary' : 'default'"
            @click="
              () => {
                statDays = 7;
                loadStat();
              }
            "
          >
            近 7 天
          </Button>
          <Button
            :class="statDays === 30 ? '' : 'text-gray-400'"
            size="small"
            :type="statDays === 30 ? 'primary' : 'default'"
            @click="
              () => {
                statDays = 30;
                loadStat();
              }
            "
          >
            近 30 天
          </Button>
        </div>
        <Table
          :columns="statColumns"
          :data-source="statRows"
          :loading="statLoading"
          :pagination="false"
          row-key="statDate"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'ctr2'">
              {{
                (record as any).views > 0
                  ? `${(((record as any).clicks / (record as any).views) * 100).toFixed(2)}%`
                  : '-'
              }}
            </template>
          </template>
        </Table>
        <div
          v-if="statRows.length === 0 && !statLoading"
          class="py-6 text-center text-gray-400"
        >
          暂无数据（广告被展现/点击后这里会出按天明细）
        </div>
      </Modal>


      <!-- 附件库选图 -->
      <Modal
        v-model:open="pickerOpen"
        :footer="null"
        title="从附件库选择图片"
        width="720px"
      >
        <div class="grid grid-cols-4 gap-3 pt-2">
          <div
            v-for="img in pickerRows"
            :key="img.id"
            class="cursor-pointer rounded border p-1 text-center hover:border-blue-400"
            @click="pickImage(img)"
          >
            <img
              :src="img.imgUrl"
              class="h-20 w-full rounded object-cover"
              loading="lazy"
            />
            <div class="truncate text-xs text-gray-500">
              {{ img.imgName || img.imgUrl }}
            </div>
          </div>
        </div>
        <div v-if="pickerRows.length === 0 && !pickerLoading" class="py-8 text-center text-gray-400">
          附件库还没有图片，请先在「附件库」上传
        </div>
        <div class="mt-3 flex justify-end">
          <Pagination
            v-model:current="pickerPage"
            :page-size="12"
            :show-size-changer="false"
            :total="pickerTotal"
            size="small"
            @change="loadPicker"
          />
        </div>
      </Modal>
    </AdDrawer>
    <!-- 获取广告代码 -->
    <Modal
      v-model:open="codeOpen"
      :footer="null"
      :title="`获取广告代码 — ${codeRow?.name ?? ''}`"
      width="640px"
    >
      <div class="space-y-5 pt-2">
        <div>
          <div class="mb-1 text-sm font-medium">JS 调用代码（推荐）</div>
          <div class="mb-2 text-xs text-gray-400">
            用法：复制下面代码 → 粘贴到模板文件的任意位置（如主题目录的 index.html，
            想显示在哪一行就贴在哪一行）→ 保存刷新页面即出广告。站外页面也能投
            （同百度联盟/谷歌联盟取码模式，域名请改成正式站点域名）。
            后台改广告即时生效，无需动模板。
          </div>
          <div class="flex items-start gap-2">
            <Textarea
              :value="jsCode"
              :rows="2"
              readonly
              class="flex-1 font-mono text-xs"
              @focus="(e: any) => e.target?.select?.()"
            />
            <Button size="small" type="primary" @click="copyText(jsCode)">
              复制
            </Button>
          </div>
        </div>
        <div>
          <div class="mb-1 text-sm font-medium">站内模板标签</div>
          <div class="mb-2 text-xs text-gray-400">
            站内主题模板专用（FreeMarker 文件里粘贴，服务端直出，无 JS 依赖、利于 SEO）。
            需要自定义样式/轮播时用
            &lt;@fly_ad key="..."&gt;…&lt;#list adList as ad&gt;…&lt;/#list&gt;…&lt;/@fly_ad&gt;
            循环，变量 adList / adPosition。
          </div>
          <div class="flex items-start gap-2">
            <Textarea
              :value="ftlCode"
              :rows="1"
              readonly
              class="flex-1 font-mono text-xs"
              @focus="(e: any) => e.target?.select?.()"
            />
            <Button size="small" type="primary" @click="copyText(ftlCode)">
              复制
            </Button>
          </div>
        </div>
        <div class="rounded bg-blue-50 px-3 py-2 text-xs text-blue-600">
          展示/点击自动计数：JS 与标签输出的链接均指向 /ad/click/{"{id}"}，
          点击计数后 302 跳转到广告链接。
        </div>
      </div>
    </Modal>
  </Page>
</template>
