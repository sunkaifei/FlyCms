# -*- coding: utf-8 -*-
"""触发一次全库备份（POST /api/system/tools/db/backup），并把产物复制到 sql/ 快照位。

用法: python tools/trigger_backup.py
前提: 后端运行在 127.0.0.1:80（flycms/admin123）。
"""
import json
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from e2e_template_check import REPO, api, login  # noqa: E402


def main():
    cookie, err = login()
    if not cookie:
        print(f"登录失败: {err}")
        return 1
    st, j = api(cookie, "POST", "/api/system/tools/db/backup")
    data = j.get("data") or {}
    if j.get("code") != 0 or not data.get("name"):
        print(f"备份失败 (HTTP {st}): {j}")
        return 1
    print(f"备份完成: {data['name']}  {data.get('sizeBytes', 0)} bytes")
    src = os.path.join(REPO, "backend", "backup", data["name"])
    dst = os.path.join(REPO, "sql", data["name"])
    if os.path.exists(dst):
        os.remove(dst)
    shutil.copy2(src, dst)
    print(f"快照已更新: {dst}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
