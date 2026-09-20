import type { RouteRecordRaw } from 'vue-router';

/**
 * FlyCms 系统管理菜单。
 * 路由不设 authority：角色组是数据驱动的（角色组名可随时增改），
 * 页面级控制交给按钮权限码（accessCodes = 后端 action_key）+ 后端 403 兜底，
 * 见 docs/frontend-access-guide.md §2.3。
 */
const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:settings',
      order: 0,
      title: '系统管理',
    },
    name: 'System',
    path: '/system',
    children: [
      {
        name: 'SystemAdmin',
        path: 'admin',
        component: () => import('#/views/system/admin/list.vue'),
        meta: {
          icon: 'lucide:user-cog',
          title: '管理员管理',
        },
      },
      {
        name: 'SystemGroup',
        path: 'group',
        component: () => import('#/views/system/group/list.vue'),
        meta: {
          icon: 'lucide:users',
          title: '角色组管理',
        },
      },
      {
        name: 'SystemPermission',
        path: 'permission',
        component: () => import('#/views/system/permission/list.vue'),
        meta: {
          icon: 'lucide:shield-check',
          title: '权限管理',
        },
      },
    ],
  },
];

export default routes;
