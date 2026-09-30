import type { Component } from 'vue';

import type {
  ExtendedModalApi,
  InferModalData,
  ModalApiOptions,
  ModalProps,
} from './modal';

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
  ModalApi,
  modalPendingPayloadAlive,
  setPendingModalPayload,
} from './modal-api';

// open 重建重放期间置位，重建后新实例的 open 直接走原路径，避免递归重建
let replayingModalOpen = false;
// 最近一次构造的 child ModalApi（重建后渲染的是它，重放打开也必须落在它身上）
let latestModalApi: ModalApi | undefined;
import VbenModal from './modal.vue';

const USER_MODAL_INJECT_KEY = Symbol('VBEN_MODAL_INJECT');

declare const MODAL_DATA_NOT_PROVIDED: unique symbol;

type ModalDataNotProvided = {
  readonly [MODAL_DATA_NOT_PROVIDED]: true;
};

type ResolvedModalData<
  TData,
  TConnectedComponent extends Component,
> = TData extends ModalDataNotProvided
  ? InferModalData<TConnectedComponent>
  : TData;

interface ModalInjectData<TData> {
  consumed?: boolean;
  extendApi?: (api: ExtendedModalApi<TData>) => void;
  options?: ModalApiOptions;
  reCreateModal?: () => Promise<void>;
}

const { globalEscapeShortcutKey } = usePreferences();
/**
 * 默认配置
 */
const DEFAULT_MODAL_PROPS: Partial<ModalProps> = {};

export function setDefaultModalProps(props: Partial<ModalProps>) {
  Object.assign(DEFAULT_MODAL_PROPS, props);
}

export function useVbenModal<
  TData = ModalDataNotProvided,
  TConnectedComponent extends Component = Component,
>(options: ModalApiOptions<TConnectedComponent> = {}) {
  type TResolvedData = ResolvedModalData<TData, TConnectedComponent>;

  // Modal一般会抽离出来，所以如果有传入 connectedComponent，则表示为外部调用，与内部组件进行连接
  // 外部的Modal通过provide/inject传递api

  const defaultOptions = {
    closeOnPressEscape: globalEscapeShortcutKey.value, // 全局Esc快捷键配置
    ...options,
  };
  const { connectedComponent } = options;
  if (connectedComponent) {
    const extendedApi = shallowReactive({}) as ExtendedModalApi<TResolvedData>;
    const isModalReady = ref(true);
    const Modal = defineComponent(
      (props: ModalProps, { attrs, slots }) => {
        function rebindApi(api: ExtendedModalApi<TResolvedData>) {
          Object.setPrototypeOf(extendedApi, markRaw(api));
        }

        provide(USER_MODAL_INJECT_KEY, {
          extendApi: rebindApi,
          consumed: false,
          options: defaultOptions,
          async reCreateModal() {
            isModalReady.value = false;
            await nextTick();
            isModalReady.value = true;
          },
        });
        checkProps(extendedApi, {
          ...props,
          ...attrs,
          ...slots,
        });
        return () =>
          h(
            isModalReady.value ? connectedComponent : 'div',
            {
              ...props,
              ...attrs,
            },
            slots,
          );
      },
      // eslint-disable-next-line vue/one-component-per-file
      {
        name: 'VbenParentModal',
        inheritAttrs: false,
      },
    );

    return [Modal, extendedApi] as const;
  }

  const injectData = inject<ModalInjectData<TResolvedData>>(
    USER_MODAL_INJECT_KEY,
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
    ...DEFAULT_MODAL_PROPS,
    ...effectiveOptions,
    ...defaultOptions,
  } as ModalApiOptions;

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
      injectData.reCreateModal?.();
    }
  };
  const api = new ModalApi<TResolvedData>(mergedOptions);
  latestModalApi = api;

  // destroyOnClose = 每次打开都是全新实例（与 use-drawer 同款兜底，注释详见彼处）。
  // bindMethods 已把 open 绑定为实例自有方法，父侧 setData().open() 链式调用最终
  // 落到 child 的这个 open 上，因此包装必须挂在 child 实例侧。
  if (mergedOptions.destroyOnClose && injectData.consumed !== undefined) {
    const stockOpen = api.open.bind(api);
    api.open = () => {
      if (replayingModalOpen) {
        stockOpen();
        return;
      }
      replayingModalOpen = true;
      setPendingModalPayload(api.sharedData.payload);
      void injectData.reCreateModal?.().then(async () => {
        for (let i = 0; i < 10 && modalPendingPayloadAlive(); i += 1) {
          await nextTick();
        }
        // 重建后渲染的是最新实例；replaying 仍置位，其包装 open 直接走原路径
        latestModalApi?.open();
        replayingModalOpen = false;
      });
    };
  }
  const extendedApi = api as ExtendedModalApi<TResolvedData>;

  extendedApi.useStore = (selector) => {
    return useSelector(api.store, selector);
  };

  const Modal = defineComponent(
    (props: ModalProps, { attrs, slots }) => {
      return () =>
        h(
          VbenModal,
          {
            ...props,
            ...attrs,
            modalApi: extendedApi,
          },
          slots,
        );
    },
    // eslint-disable-next-line vue/one-component-per-file
    {
      name: 'VbenModal',
      inheritAttrs: false,
    },
  );
  injectData.extendApi?.(extendedApi);

  return [Modal, extendedApi] as const;
}

export function createVbenModal<TData = unknown>() {
  return function useTypedVbenModal<
    TConnectedComponent extends Component = Component,
  >(options: ModalApiOptions<TConnectedComponent> = {}) {
    return useVbenModal<TData, TConnectedComponent>(options);
  };
}

async function checkProps<TData>(
  api: ExtendedModalApi<TData>,
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
      // connectedComponent存在时，不要传入Modal的props，会造成复杂度提升，如果你需要修改Modal的props，请使用 useModal 或者api
      console.warn(
        `[Vben Modal]: When 'connectedComponent' exists, do not set props or slots '${attr}', which will increase complexity. If you need to modify the props of Modal, please use useVbenModal or api.`,
      );
    }
  }
}
