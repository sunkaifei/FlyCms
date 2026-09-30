<script lang="ts" setup>
import type { MenuNode } from '#/api/core/menu-manage';

import { computed, onMounted, ref } from 'vue';

import { Button, Empty, Input, message, Spin, Tag, Tree } from 'ant-design-vue';

import { getMenuListApi } from '#/api/core/menu-manage';
import { assignGroupPermissionsApi, getGroupPermissionIdsApi } from '#/api/core/system';
import { useEditDrawer } from '#/utils/edit-drawer';

/**
 * 角色分配权限（若依式）：级联勾选菜单树（M/C/F）。
 * 提交 = 勾选节点 + 半选父节点 的 permission id 全量保存，后端做增删差量。
 * 超级管理员组（id=1）后端硬编码保护，前端提交时拦截并提示。
 *
 * 工具条：展开/收起全部、全部勾选/取消、关键字过滤（按名称或权限码）、已选计数。
 */
const groupId = ref<string>('');
const groupName = ref('');
const loading = ref(false);
const expandedKeys = ref<string[]>([]);
const checkedKeys = ref<string[]>([]);
const halfCheckedKeys = ref<string[]>([]);
const keyword = ref('');
/** 菜单节点必须是响应式的：treeData computed 依赖它，普通变量赋值不会触发重算 */
const allNodes = ref<MenuNode[]>([]);
let onSaved: (() => void) | undefined;

interface TreeRow {
  actionKey?: string;
  children?: TreeRow[];
  id: string;
  key: string;
  menuName?: string;
  menuType: string;
  /** 上级权限节点 id（MenuNode 原始类型），0=根；查 Map 时转字符串对齐 key */
  parentId?: number;
  title: string;
}

function nodeTitle(n: MenuNode): string {
  return (
    (n.menuName || n.actionKey || String(n.id)) +
    (n.menuType === 'F' && n.actionKey ? `（${n.actionKey}）` : '')
  );
}

/** 全量树（key/parentId 统一字符串，雪花 id 序列化后无精度问题） */
const fullTree = computed<TreeRow[]>(() => {
  const map = new Map<string, TreeRow>();
  for (const n of allNodes.value) {
    map.set(String(n.id), {
      ...n,
      id: String(n.id),
      children: undefined,
      key: String(n.id),
      title: nodeTitle(n),
    });
  }
  const roots: TreeRow[] = [];
  for (const row of map.values()) {
    const parent = row.parentId ? map.get(String(row.parentId)) : undefined;
    if (parent) {
      (parent.children ??= []).push(row);
    } else {
      roots.push(row);
    }
  }
  return roots;
});

/** 关键字过滤：命中名称/权限码的节点保留，祖先链自动保留（大小写不敏感） */
const filteredTree = computed<TreeRow[]>(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return fullTree.value;
  const keep = new Set<string>();
  const walk = (rows: TreeRow[], inherited: boolean): boolean => {
    let anyKept = false;
    for (const row of rows) {
      const selfHit =
        inherited ||
        (row.title || '').toLowerCase().includes(kw) ||
        (row.actionKey || '').toLowerCase().includes(kw);
      const childHit = row.children ? walk(row.children, selfHit) : false;
      if (selfHit || childHit) {
        keep.add(row.id);
        anyKept = true;
      }
    }
    return anyKept;
  };
  walk(fullTree.value, false);
  const prune = (rows: TreeRow[]): TreeRow[] =>
    rows
      .filter((r) => keep.has(r.id))
      .map((r) => ({ ...r, children: r.children ? prune(r.children) : undefined }));
  return prune(fullTree.value);
});

const filterActive = computed(() => keyword.value.trim() !== '');

/** 当前过滤树里展示的全部节点 id（含祖先链） */
const visibleIds = computed(() => {
  const ids: string[] = [];
  const walk = (rows: TreeRow[]) => {
    for (const row of rows) {
      ids.push(row.id);
      if (row.children) walk(row.children);
    }
  };
  walk(filteredTree.value);
  return ids;
});

/**
 * 程序化批量勾选后的级联归一化：antd Tree 只在点击事件里推导父子级联，
 * 代码直接赋值 checkedKeys 不会重算——叶子按集合判定，父节点按子孙全勾/部分勾
 * 推导出 checked / halfChecked，保证展示与提交口径一致。
 */
function normalizeChecked(checkedSet: Set<string>) {
  const checked: string[] = [];
  const half: string[] = [];
  const status = (row: TreeRow): 'checked' | 'half' | 'none' => {
    const kids = row.children ?? [];
    if (kids.length === 0) {
      if (checkedSet.has(row.id)) {
        checked.push(row.id);
        return 'checked';
      }
      return 'none';
    }
    let all = true;
    let any = false;
    for (const kid of kids) {
      const state = status(kid);
      if (state !== 'checked') all = false;
      if (state !== 'none') any = true;
    }
    if (all) {
      checked.push(row.id);
      return 'checked';
    }
    if (any) {
      half.push(row.id);
      return 'half';
    }
    return 'none';
  };
  for (const root of fullTree.value) status(root);
  checkedKeys.value = checked;
  halfCheckedKeys.value = half;
}

const isSuperGroup = computed(
  () => Number(groupId.value) === 1 || groupName.value === '超级管理员',
);

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    if (isSuperGroup.value) {
      message.warning('超级管理员组默认拥有全部权限，不能修改');
      return;
    }
    modalApi.lock();
    try {
      const all = [...checkedKeys.value, ...halfCheckedKeys.value];
      await assignGroupPermissionsApi(groupId.value, all as unknown as number[]);
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '分配权限（菜单树）',
});

// /////////////////// 工具条动作 ///////////////////

/** 过滤生效时只展开过滤树里的父节点，否则展开全树父节点 */
function expandAll() {
  const keys: string[] = [];
  const walk = (rows: TreeRow[]) => {
    for (const row of rows) {
      if (row.children?.length) {
        keys.push(row.id);
        walk(row.children);
      }
    }
  };
  walk(filteredTree.value);
  expandedKeys.value = keys;
}

function collapseAll() {
  expandedKeys.value = [];
}

/** 过滤生效时仅勾选可见节点（保留过滤范围外的已有勾选），否则勾选全树 */
function checkAll() {
  if (isSuperGroup.value) return;
  const set = new Set(checkedKeys.value);
  const ids = filterActive.value
    ? visibleIds.value
    : allNodes.value.map((n) => String(n.id));
  for (const id of ids) set.add(id);
  normalizeChecked(set);
}

/** 过滤生效时仅取消可见节点（保留过滤范围外的已有勾选），否则清空 */
function cancelAll() {
  if (isSuperGroup.value) return;
  if (!filterActive.value) {
    checkedKeys.value = [];
    halfCheckedKeys.value = [];
    return;
  }
  const set = new Set(checkedKeys.value);
  for (const id of visibleIds.value) set.delete(id);
  normalizeChecked(set);
}

/** 勾选事件：第一参就是勾选后的键数组；半选键取自 info（修复旧实现误用布尔的问题） */
function onCheck(keys: any, info: any) {
  if (isSuperGroup.value) return;
  checkedKeys.value = (Array.isArray(keys) ? keys : []) as string[];
  halfCheckedKeys.value = (info?.halfCheckedKeys ?? []) as string[];
}

onMounted(async () => {
  const data = modalApi.getData() as
    | undefined
    | { group: { id: number | string; name: string }; onSaved?: () => void };
  onSaved = data?.onSaved;
  groupId.value = String(data?.group?.id ?? '');
  groupName.value = data?.group?.name ?? '';
  // connectedComponent 随页面加载即空挂载（无组数据）：此时不发请求，
  // 等真正打开（重建实例带数据）再加载，避免 permissionIds?id=0 的报错 toast
  if (!groupId.value) return;

  loading.value = true;
  try {
    const [nodes, ids] = await Promise.all([
      getMenuListApi(),
      getGroupPermissionIdsApi(groupId.value),
    ]);
    allNodes.value = nodes ?? [];
    const childIds = new Set(allNodes.value.map((n) => String(n.parentId)));
    expandedKeys.value = allNodes.value
      .filter((n) => n.menuType === 'M' && n.visible === 1)
      .map((n) => String(n.id));
    // 初始只勾叶子（父节点状态由树级联推导）；键与绑定 id 统一字符串
    checkedKeys.value = (ids ?? [])
      .map((id) => String(id))
      .filter((id) => !childIds.has(id) && allNodes.value.some((n) => String(n.id) === id));
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <Modal>
    <Spin :spinning="loading">
      <p class="mb-2 text-sm">
        为「{{ groupName }}」勾选可用的菜单与操作（按钮节点即前端权限码，未勾选则接口 403）：
      </p>
      <div
        v-if="isSuperGroup"
        class="mb-2 rounded bg-blue-50 px-3 py-1.5 text-xs text-blue-600"
      >
        超级管理员组默认拥有全部权限，仅供查看，不能修改。
      </div>
      <div class="mb-2 flex flex-wrap items-center gap-2">
        <Button size="small" @click="expandAll">展开</Button>
        <Button size="small" @click="collapseAll">收缩</Button>
        <Button v-if="!isSuperGroup" size="small" @click="checkAll">
          {{ filterActive ? '勾选可见' : '全选' }}
        </Button>
        <Button v-if="!isSuperGroup" size="small" @click="cancelAll">
          {{ filterActive ? '取消可见' : '取消' }}
        </Button>
        <Input
          v-model:value="keyword"
          allow-clear
          class="!w-52"
          placeholder="按名称 / 权限码过滤"
          size="small"
        />
        <Tag class="ml-auto">
          已选 {{ checkedKeys.length }} 项<template v-if="halfCheckedKeys.length > 0">
（{{ halfCheckedKeys.length }} 项半选）
</template>
        </Tag>
      </div>
      <p v-if="filterActive" class="mb-2 text-xs text-gray-400">
        过滤生效中：批量勾选 / 取消仅作用于当前可见节点，范围外的已有勾选保持不变。
      </p>
      <Empty
        v-if="!loading && filteredTree.length === 0"
        :description="keyword ? '没有匹配的节点' : '暂无菜单节点'"
      />
      <div v-else class="max-h-[60vh] overflow-y-auto">
        <Tree
          v-model:checked-keys="checkedKeys"
          v-model:expanded-keys="expandedKeys"
          :checkable="!isSuperGroup"
          :height="480"
          :selectable="false"
          :tree-data="filteredTree"
          @check="onCheck"
        />
      </div>
    </Spin>
  </Modal>
</template>
