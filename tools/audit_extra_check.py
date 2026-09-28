# -*- coding: utf-8 -*-
"""模板引擎「缺陷修复回归脚本」（2026-09-28 P4 批次后翻转为正向断言）。

与 tools/e2e_template_check.py 的分工：
- e2e_template_check.py：验收已知能力的正向用例（应全 PASS）。
- 本脚本：复核验收中暴露过的真实缺陷（D1~D6）的**修复仍然有效**。
  断言方向为正向 ——「缺陷修复后行为正确」→ PASS。修复若回退，此处 FAIL。

覆盖 5 组：菜单节点装配 / 站点级指派其余 pageType / 主题包导出导入一致性 /
标签手册骨架可用性 / 试渲染错误检测一致性。

用法：
    C:/Users/kaife/.workbuddy/binaries/python/envs/default/Scripts/python.exe tools/audit_extra_check.py
退出码非 0 = 有修复回退或新缺陷。
"""
import io
import os
import re
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from e2e_template_check import login, api, raw, front  # noqa: E402

ok, fail = [], []


def check(name, cond, detail=""):
    (ok if cond else fail).append(name)
    print(f"  [{'PASS' if cond else 'FAIL'}] {name} {detail}")


def walk_routes(nodes, out):
    for n in nodes or []:
        out.append(n.get("path") or "")
        walk_routes(n.get("children"), out)


def multipart(field, filename, payload, ct="application/zip"):
    b = "----flycmsaudit"
    body = (
        f'--{b}\r\nContent-Disposition: form-data; name="{field}"; '
        f'filename="{filename}"\r\nContent-Type: {ct}\r\n\r\n'
    ).encode("utf-8") + payload + f"\r\n--{b}--\r\n".encode("utf-8")
    return body, {"Content-Type": f"multipart/form-data; boundary={b}"}


def main():
    cookie, err = login()
    if err:
        print("LOGIN_FAIL:", err)
        sys.exit(2)
    print("LOGIN_OK")

    print("\n=== ① 菜单节点装配（新增页面是否真的进了路由树）===")
    st, j = api(cookie, "GET", "/api/menu/all")
    data = j.get("data")
    nodes = data if isinstance(data, list) else (data or {}).get("menus") or (data or {}).get("routes") or []
    paths = []
    walk_routes(nodes, paths)
    check("菜单含 /system/area（布局管理可达）", "/system/area" in paths, f"共 {len(paths)} 个 path")

    print("\n=== ② 站点级指派 SITE/site 的其余 pageType（e2e 只测了 TAG）===")
    cases = [
        ("INDEX", "/", "search.html", "index.html"),
        ("SEARCH", "/search?q=a", "index.html", "search.html"),
        ("ERROR", "/404", "index.html", "404.html"),
    ]
    for pt, url, tpl, back in cases:
        api(cookie, "POST", "/api/system/template/assign",
            f"targetType=SITE&targetId=site&pageType={pt}&template={tpl}")
        _, _, t1 = front(url)
        check(f"SITE/{pt} 指派生效", t1 == tpl, f"tpl={t1}")
        api(cookie, "POST", "/api/system/template/unassign",
            f"targetType=SITE&targetId=site&pageType={pt}")
        _, _, t2 = front(url)
        check(f"SITE/{pt} 取消复原", t2 == back, f"tpl={t2}")

    print("\n=== ③ 主题包「导出 → 导入」一致性（P4-1/P4-2 修复回归）===")
    st, blob, _ = raw("GET", "/api/system/skin/export?skin=defalut", cookies=cookie)
    check("导出返回真实 zip", st == 200 and blob[:2] == b"PK", f"HTTP={st} bytes={len(blob)}")
    names = zipfile.ZipFile(io.BytesIO(blob)).namelist()
    check("导出包首条目是根级 manifest.json", names[0] == "manifest.json", f"first={names[0]}")
    body, hdr = multipart("file", "defalut.zip", blob)
    st, resp2, _ = raw("POST", "/api/system/skin/import?overwrite=1", body=body, headers=hdr, cookies=cookie)
    txt = resp2.decode("utf-8", "replace")
    check("自导出包可原样导回（P4-1：manifest name 推导皮肤名）",
          '"code":0' in txt and "已导入" in txt, f"{txt[:120]}")

    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as zf:
        zf.writestr("pc_theme/d3skin/theme.json", '{"name":"d3"}')
        zf.writestr("pc_theme/d3skin/index.html", "<html/>")
    body, hdr = multipart("file", "auditskin.zip", buf.getvalue())
    st, resp3, _ = raw("POST", "/api/system/skin/import", body=body, headers=hdr, cookies=cookie)
    t3 = resp3.decode("utf-8", "replace")
    check("带 pc_theme/ 前缀的包被保留名拒绝（P4-2）",
          "系统保留名" in t3 and '"code":-1' in t3, f"{t3[:140]}")

    print("\n=== ④ 标签手册骨架可用性（P3-4/P4-3/P4-4/P4-6 修复回归）===")
    from urllib.parse import urlencode
    st, j = api(cookie, "GET", "/api/system/tags/manual")
    groups = (j.get("data") or {}).get("groups") or {}
    items = [(it.get("name"), it.get("snippet")) for v in groups.values() for it in v]
    check("手册标签数 = 55", len(items) == 55, f"{len(items)}")

    hard = []      # 与上下文无关的硬缺陷
    ctx = []       # 仅因示例上下文不足
    for name, snip in items:
        st, j2 = api(cookie, "POST", "/api/system/template/preview",
                     urlencode({"content": snip or "", "file": "list-articles.html", "model": "articles"}))
        html = (j2.get("data") or {}).get("html") or ""
        blobtxt = (j2.get("message") or "") + " " + html
        if j2.get("code") == 0 and "FreeMarker template error" not in html:
            continue
        if ("compare" in blobtxt or "Cannot invoke" in blobtxt
                or "Expected a hash" in blobtxt):
            hard.append(name)
        elif re.search(r"==> (l\.|a\.|t\.)", blobtxt):
            hard.append(name + "(字段名)")
        elif "null or missing" in blobtxt:
            ctx.append(name)
        else:
            hard.append(name + "(其他)")
    print("   硬缺陷:", sorted(hard))
    print("   仅上下文不足:", sorted(ctx))
    check("55 个骨架硬缺陷归零（P4-3 字段名/FTL 写法 + P4-4 Avatar 判空）",
          not hard, f"hard={sorted(hard)}")
    check("仅上下文不足的骨架不超过 6 个（P4-6 已补 channel/categoryList/userinfo 等 mock）",
          len(ctx) <= 6, f"ctx={sorted(ctx)}")

    print("\n=== ⑤ 试渲染错误检测一致性（P4-5 修复回归）===")
    # 用例 A：错误在模板顶层 → 渲染抛异常 → preview 返回失败
    st, jA = api(cookie, "POST", "/api/system/template/preview",
                 urlencode({"content": "<#if (someBool!true) == 1>x</#if>", "file": "t.html"}))
    # 用例 B：错误在**标签体**内 → 曾被标签降级 catch 吞掉误判成功 → 现靠输出流 grep 判失败
    st, jB = api(cookie, "POST", "/api/system/template/preview",
                 urlencode({"content": '<@fly_useractivation userId="${(userId)!0}">'
                                       "<#if (status!0) == 1>已激活</#if></@fly_useractivation>",
                           "file": "t.html"}))
    htmlB = (jB.get("data") or {}).get("html") or ""
    print(f"   顶层错误 code={jA.get('code')} / 标签体错误 code={jB.get('code')}")
    check("两类错误的 code 一致（都为失败）",
          jA.get("code") != 0 and jB.get("code") != 0,
          f"A={jA.get('code')} B={jB.get('code')}")
    check("标签体报错不再作为「操作成功」返回",
          not (jB.get("code") == 0 and "FreeMarker template error" in htmlB),
          f"code={jB.get('code')}")

    print(f"\n复核结论：PASS {len(ok)}  FAIL {len(fail)}")
    if fail:
        print("以下项「预期缺陷已消失」，请更新文档结论：", fail)
        sys.exit(1)


if __name__ == "__main__":
    main()
