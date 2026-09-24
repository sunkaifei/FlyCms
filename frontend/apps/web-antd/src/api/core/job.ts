import { requestClient } from '#/api/request';

/**
 * 定时任务管理 API（fly_job + fly_job_log，Quartz 底座）
 */

export interface JobRow {
  beanName: string;
  createTime?: string;
  cronExpression: string;
  id: string;
  methodName: string;
  params?: string;
  remark?: string;
  status: string; // 1=启用 0=暂停
}

export interface JobLogRow {
  beanName?: string;
  createTime?: string;
  cronExpression?: string;
  errorMsg?: string;
  jobId?: string;
  methodName?: string;
  params?: string;
  status?: string; // 0=成功 1=失败（ScheduleJob 写入约定）
  times?: number;
}

export async function getJobListApi(params: { p?: number; rows?: number }) {
  return requestClient.get<{ count: number; list: JobRow[] }>('/system/job/list', {
    params,
  });
}

export async function getJobLogListApi(params: { p?: number; rows?: number }) {
  return requestClient.get<{ count: number; list: JobLogRow[] }>(
    '/system/job/logList',
    { params },
  );
}

function buildForm(data: Record<string, null | string | undefined>) {
  const form = new URLSearchParams();
  for (const [k, v] of Object.entries(data)) {
    if (v !== undefined && v !== null) form.append(k, v);
  }
  return form;
}

const FORM_HEADERS = {
  'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
};

export async function saveJobApi(data: {
  beanName: string;
  cronExpression: string;
  methodName: string;
  params?: string;
  remark?: string;
  status?: string;
}) {
  return requestClient.post<void>('/system/job/save', buildForm(data), {
    headers: FORM_HEADERS,
  });
}

export async function updateJobApi(data: {
  beanName: string;
  cronExpression: string;
  id: string;
  methodName: string;
  params?: string;
  remark?: string;
}) {
  return requestClient.post<void>('/system/job/update', buildForm(data), {
    headers: FORM_HEADERS,
  });
}

export async function updateJobStatusApi(id: string, status: string) {
  return requestClient.post<string>(
    '/system/job/status',
    buildForm({ id, status }),
    { headers: FORM_HEADERS },
  );
}

export async function runJobApi(id: string) {
  return requestClient.post<string>('/system/job/run', buildForm({ id }), {
    headers: FORM_HEADERS,
  });
}

export async function deleteJobApi(id: string) {
  return requestClient.post<void>('/system/job/delete', buildForm({ id }), {
    headers: FORM_HEADERS,
  });
}
