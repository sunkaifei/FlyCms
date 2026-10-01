import { requestClient } from '#/api/request';

/**
 * 站点预设 API（P 批次零代码建站向导）
 */

export interface PresetCard {
  applied: boolean;
  description?: string;
  icon?: string;
  modelCount: number;
  name: string;
  code: string;
  templateCount: number;
  version: number;
}

export interface PresetDetail {
  categoryCount: number;
  channelCount: number;
  fieldCount: number;
  guideCount: number;
  modelCount: number;
  preset: { code: string; description?: string; name: string };
  sampleCount: number;
  settingsCount: number;
  templateCount: number;
}

export async function getPresetListApi() {
  return requestClient.get<PresetCard[]>('/system/preset/list');
}

export async function getPresetDetailApi(code: string) {
  return requestClient.get<PresetDetail>('/system/preset/detail', {
    params: { code },
  });
}

const FORM_HEADERS = {
  'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
};

function formBody(data: Record<string, unknown>) {
  const form = new URLSearchParams();
  for (const [k, v] of Object.entries(data)) {
    if (v !== undefined && v !== null) form.append(k, String(v));
  }
  return form.toString();
}

export async function applyPresetApi(code: string, options?: {
  overwriteTemplates?: boolean;
  withSampleContent?: boolean;
}) {
  return requestClient.post<{ data: string[]; message: string }>(
    '/system/preset/apply',
    formBody({
      code,
      overwriteTemplates: options?.overwriteTemplates ? '1' : '0',
      withSampleContent: options?.withSampleContent ? '1' : '0',
    }),
    { headers: FORM_HEADERS },
  );
}

export async function clearPresetSampleApi(code: string) {
  return requestClient.post<void>(
    '/system/preset/clearSample',
    formBody({ code }),
    { headers: FORM_HEADERS },
  );
}
