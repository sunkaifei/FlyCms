import type { Recordable } from '@vben/types';

import { useVbenDrawer } from '@vben/common-ui';

/**
 * 编辑/发布页统一抽屉（所有发布和编辑页面共用）：
 * - 默认宽度 75%，头部"最大化"按钮可在 75% 与全屏间切换；
 * - 点击遮罩不关闭（防误触丢失编辑内容）。
 * 用法与 useVbenDrawer 完全一致，直接替换即可。
 */
export function useEditDrawer(options: Recordable<any> = {}) {
  return useVbenDrawer({
    class: 'w-[75%]',
    closeOnClickModal: false,
    fullscreenButton: true,
    ...options,
  });
}
