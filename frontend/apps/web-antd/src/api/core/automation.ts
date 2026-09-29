import { requestClient } from '#/api/request';

/**
 * G17 自动化规则 API（/api/system/automation/**，DataVo 契约已由拦截器剥壳）
 */

export interface AutomationRuleRow {
  actions: string;
  conditions: string | null;
  createTime?: string;
  event: string;
  id: string;
  modelCode: string | null;
  ruleName: string;
  status: number;
}

export async function getAutomationListApi() {
  return requestClient.get<AutomationRuleRow[]>('/system/automation/list');
}

function postForm<T>(url: string, data: Record<string, unknown>) {
  const form = new URLSearchParams();
  for (const [k, v] of Object.entries(data)) {
    if (v !== undefined && v !== null && v !== '') {
      form.append(k, String(v));
    }
  }
  return requestClient.post<T>(url, form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

export async function saveAutomationRuleApi(data: Record<string, unknown>) {
  return postForm<void>('/system/automation/save', data);
}

export async function deleteAutomationRuleApi(id: string) {
  return postForm<void>('/system/automation/del', { id });
}
