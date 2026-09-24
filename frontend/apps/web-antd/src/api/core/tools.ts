import { requestClient } from '#/api/request';

/**
 * 系统工具 API：数据库备份（纯 Java 导出，见后端 DbBackupService）
 */

export interface DbBackupFile {
  backupTime: number;
  name: string;
  sizeBytes: number;
}

export async function getDbBackupListApi() {
  return requestClient.get<DbBackupFile[]>('/system/tools/db/list');
}

export async function createDbBackupApi() {
  const form = new URLSearchParams();
  return requestClient.post<DbBackupFile>('/system/tools/db/backup', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

export async function deleteDbBackupApi(name: string) {
  const form = new URLSearchParams();
  form.append('name', name);
  return requestClient.post<void>('/system/tools/db/delete', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

/** 下载走同源 cookie 直链（window.open），服务端校验 session + 权限 */
export function dbBackupDownloadUrl(name: string) {
  return `/api/system/tools/db/download?name=${encodeURIComponent(name)}`;
}
