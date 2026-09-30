import type { Component } from 'vue';

import type {
  DrawerApiOptions,
  DrawerProps,
  ExtendedDrawerApi,
  InferDrawerData,
} from './drawer';

import {
  defineComponent,
  h,
  inject,
  markRaw,
  nextTick,
  onBeforeUnmount,
  provide,
  ref,
  shallowReactive,
} from 'vue';

import { usePreferences } from '@vben-core/preferences';
import { useSelector } from '@vben-core/shared/store';

import {
  DrawerApi,
  drawerPendingPayloadAlive,
  setPendingDrawerPayload,
} from './drawer-api';

// open 重建重放期间置位，重建后新实例的 open 直接走原路径，避免递归重建
let replayingDrawerOpen = false;
// 最近一次构造的 child DrawerApi（重建后渲染的是它，重放打开也必须落在它身上）
let latestDrawerApi: DrawerApi | undefined;
import VbenDrawer from './drawer.vue';

const USER_DRAWER_INJECT_KEY = Symbol('VBEN_DRAWER_INJECT');

declare const DRAWER_DATA_NOT_PROVIDED: unique symbol;

type DrawerDataNotProvided = {
  readonly [DRAWER_DATA_NOT_PROVIDED]: true;
};

type ResolvedDrawerData<
  TData,
  TConnectedComponent extends Component,
> = TData extends DrawerDataNotProvided
  ? InferDrawerData<TConnectedComponent>
  : TData;

interface DrawerInjectData<TData> {
  consumed?: boolean;
  extendApi?: (api: ExtendedDrawerApi<TData>) => void;
  options?: DrawerApiOptions;
  reCreateDrawer?: () => Promise<void>;
}

const { globalEscapeShortcutKey } = usePreferences();

/**
 * 默认配置
 */
const DEFAULT_DRAWER_PROPS: Partial<DrawerProps> = {};

export function setDefaultDrawerProps(props: Partial<DrawerProps>) {
  Object.assign(DEFAULT_DRAWER_PROPS, props);
}

export function useVbenDrawer<
  TData = DrawerDataNotProvided,
  TConnectedComponent extends Component = Component,
>(options: DrawerApiOptions<TConnectedComponent> = {}) {
  type TResolvedData = ResolvedDrawerData<TData, TConnectedComponent>;

  // Drawer一般会抽离出来，所以如果有传入 connectedComponent，则表示为外部调用，与内部组件进行连接
  // 外部的Drawer通过provide/inject传递api

  const defaultOptions = {
    closeOnPressEscape: globalEscapeShortcutKey.value, // 全局Esc快捷键配置
    ...options,
  };
  const { connectedComponent } = options;
  if (connectedComponent) {
    const extendedApi = shallowReactive({}) as ExtendedDrawerApi<TResolvedData>;
    const isDrawerReady = ref(true);
    const Drawer = defineComponent(
      (props: DrawerProps, { attrs, slots }) => {
        function rebindApi(api: ExtendedDrawerApi<TResolvedData>) {
          Object.setPrototypeOf(extendedApi, markRaw(api));
        }

        provide(USER_DRAWER_INJECT_KEY, {
          extendApi: rebindApi,
          consumed: false,
          options: defaultOptions,
          async reCreateDrawer() {
            isDrawerReady.value = false;
            await nextTick();
            isDrawerReady.value = true;
          },
        });
        checkProps(extendedApi, {
          ...props,
          ...attrs,
          ...slots,
        });
        return () =>
          h(
            isDrawerReady.value ? connectedComponent : 'div',
            { ...props, ...attrs },
            slots,
          );
      },
      // eslint-disable-next-line vue/one-component-per-file
      {
        name: 'VbenParentDrawer',
        inheritAttrs: false,
      },
    );

    return [Drawer, extendedApi] as const;
  }

  const injectData = inject<DrawerInjectData<TResolvedData>>(
    USER_DRAWER_INJECT_KEY,
    {},
  );
  const isConsumed = injectData.consumed;
  const effectiveOptions = isConsumed ? {} : injectData.options;
  if (!isConsumed && injectData.consumed !== undefined) {
    injectData.consumed = true;
  }
  onBeforeUnmount(() => {
    if (!isConsumed && injectData.consumed !== undefined) {
      injectData.consumed = false;
    }
  });

  const mergedOptions = {
    ...DEFAULT_DRAWER_PROPS,
    ...effectiveOptions,
    ...defaultOptions,
  } as DrawerApiOptions;

  mergedOptions.onOpenChange = (isOpen: boolean) => {
    options.onOpenChange?.(isOpen);
    if (!isConsumed) {
      injectData.options?.onOpenChange?.(isOpen);
    }
  };

  const onClosed = mergedOptions.onClosed;
  mergedOptions.onClosed = () => {
    onClosed?.();
    if (mergedOptions.destroyOnClose && !isConsumed) {
      if (injectData.consumed !== undefined) {
        injectData.consumed = false;
      }
      injectData.reCreateDrawer?.();
    }
  };
  const api = new DrawerApi<TResolvedData>(mergedOptions);
  latestDrawerApi = api;

  // destroyOnClose = 每次打开都是全新实例。connected 组件随页面加载即挂载，其
  // onMounted 里的 getData 只能读到上一次的数据（首次为空）——官方约定 getData
  // 配合 onOpenChange 使用，这里在框架层兜底：open 时先重建 child 实例（重建期间
  // 把本次 payload 经暂存槽交给新 DrawerApi 构造器），存量「onMounted 读
  // getData」的页面写法无需任何改动即可读到本次数据。
  // 注意 bindMethods 已把 open 绑定为实例自有方法，父侧 setData().open() 链式
  // 调用最终落到 child 的这个 open 上，因此包装必须挂在 child 实例侧。
  if (mergedOptions.destroyOnClose && injectData.consumed !== undefined) {
    const stockOpen = api.open.bind(api);
    api.open = () => {
      if (replayingDrawerOpen) {
        stockOpen();
        return;
      }
      replayingDrawerOpen = true;
      setPendingDrawerPayload(api.sharedData.payload);
      void injectData.reCreateDrawer?.().then(async () => {
        // onClosed 的 stock 重建与本次重建可能叠加，轮询到消费完暂存槽为止
        for (let i = 0; i < 10 && drawerPendingPayloadAlive(); i += 1) {
          await nextTick();
        }
        // 重建后渲染的是最新实例；replaying 仍置位，其包装 open 直接走原路径
        latestDrawerApi?.open();
        replayingDrawerOpen = false;
      });
    };
  }
  const extendedApi = api as ExtendedDrawerApi<TResolvedData>;

  extendedApi.useStore = (selector) => {
    return useSelector(api.store, selector);
  };

  const Drawer = defineComponent(
    (props: DrawerProps, { attrs, slots }) => {
      return () =>
        h(VbenDrawer, { ...props, ...attrs, drawerApi: extendedApi }, slots);
    },
    // eslint-disable-next-line vue/one-component-per-file
    {
      name: 'VbenDrawer',
      inheritAttrs: false,
    },
  );
  injectData.extendApi?.(extendedApi);
  return [Drawer, extendedApi] as const;
}

export function createVbenDrawer<TData = unknown>() {
  return function useTypedVbenDrawer<
    TConnectedComponent extends Component = Component,
  >(options: DrawerApiOptions<TConnectedComponent> = {}) {
    return useVbenDrawer<TData, TConnectedComponent>(options);
  };
}

async function checkProps<TData>(
  api: ExtendedDrawerApi<TData>,
  attrs: Record<string, any>,
) {
  if (!attrs || Object.keys(attrs).length === 0) {
    return;
  }
  await nextTick();

  const state = api?.store?.state;

  if (!state) {
    return;
  }

  const stateKeys = new Set(Object.keys(state));

  for (const attr of Object.keys(attrs)) {
    if (stateKeys.has(attr) && !['class'].includes(attr)) {
      // connectedComponent存在时，不要传入Drawer的props，会造成复杂度提升，如果你需要修改Drawer的props，请使用 useVbenDrawer 或者api
      console.warn(
        `[Vben Drawer]: When 'connectedComponent' exists, do not set props or slots '${attr}', which will increase complexity. If you need to modify the props of Drawer, please use useVbenDrawer or api.`,
      );
    }
  }
}
