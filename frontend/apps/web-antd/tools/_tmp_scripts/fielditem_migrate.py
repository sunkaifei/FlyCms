"""把后台页面里手写的字段标签统一成 FieldItem 组件。

匹配形态（缩进敏感）:
    <ws><div[ 附加属性]>
    <ws>  <div class="mb-1 text-sm[可选修饰]">[可选红星]标签</div>   ← 标签可跨行
    <ws>  <控件 ... />
    <ws></div>

替换为:
    <ws><FieldItem label="标签" [required] [tips="..."] [原附加属性]>
    <ws>  <控件 ... />
    <ws></FieldItem>

- 红星形态  <span class="text-red-500">*</span> X  →  required + label="X"
- 动态形态  {{ item.label }}                     →  :label="item.label"
- 标签里带「（…）」且整段超过 14 字时拆成 label + tips
- 包装层上的 v-if 等属性原样搬到 FieldItem（条件渲染语义不变）
- 未匹配任何结构就原样跳过（宁可不改，不可改错）
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]  # tools/_tmp_scripts/x.py → frontend/apps/web-antd
BASE = ROOT / "src/views/system"

LABEL_START_RE = re.compile(r'^(\s*)<div class="mb-1 text-sm([^"]*)">\s*$')
LABEL_INLINE_RE = re.compile(r'^(\s*)<div class="mb-1 text-sm([^"]*)">(.*)</div>\s*$')
OPEN_RE = re.compile(r'^(\s*)<div(\s+[^>]*)?>\s*$')
# 收尾必须按「出现次数」数：行内 <div ...>xxx</div> 的闭合也在同一行，
# 只匹配整行 </div> 会把这类行当成未闭合，导致块尾算错（改错结构）。
CLOSE_ANY_RE = re.compile(r'</div\s*>')
SELF_RE = re.compile(r'<div\b[^>]*/>')
DIV_OPEN_RE = re.compile(r'<div\b')
STAR_RE = re.compile(r'^\s*<span class="text-red-500">\*</span>\s*(.+)$')


def split_tips(label: str):
    """长标签把「（…）」提示拆到 tips，主标签保持短而一致。"""
    m = re.search(r'（([^）]*)）\s*$', label)
    if m and len(label) > 14:
        return label[: m.start()].strip(), m.group(1).strip()
    return label, None


def find_block_end(lines, start):
    """从 start 行起按 div 嵌套找配对的收尾，返回其行号；找不到返回 None。"""
    depth = 1
    for j in range(start, len(lines)):
        line = lines[j]
        depth += len(DIV_OPEN_RE.findall(SELF_RE.sub("", line)))
        depth -= len(CLOSE_ANY_RE.findall(line))
        if depth == 0:
            return j
        if depth < 0:
            return None
    return None


def read_label(lines, i):
    """读取第 i 行的标签 div，返回 (raw_text, end_line) ；不是标签则 (None, i)。"""
    m = LABEL_INLINE_RE.match(lines[i])
    if m:
        return m.group(3).strip(), i
    m = LABEL_START_RE.match(lines[i])
    if not m:
        return None, i
    end = find_block_end(lines, i + 1)
    if end is None:
        return None, i
    text = "".join(lines[i + 1 : end]).strip()
    return text, end


def convert(path: Path) -> int:
    # newline='' 保留原始行尾（CRLF/LF），否则 Path.read_text 的 universal newlines
    # 会把 CRLF 归一成 LF，制造出与业务无关的大量 diff 噪音
    with open(path, encoding="utf-8", newline="") as fh:
        lines = fh.readlines()
    eol = "\r\n" if any(l.endswith("\r\n") for l in lines) else "\n"
    out = []
    i = 0
    changed = 0
    while i < len(lines):
        m_open = OPEN_RE.match(lines[i])
        if not m_open or i + 1 >= len(lines):
            out.append(lines[i])
            i += 1
            continue
        raw, lab_end = read_label(lines, i + 1)
        if raw is None:
            out.append(lines[i])
            i += 1
            continue

        ws_open = m_open.group(1)
        wrap_attrs = m_open.group(2) or ""
        # 标签 div 自身的 class 修饰（判断红星）
        lab_line = lines[i + 1]
        m_any = LABEL_INLINE_RE.match(lab_line) or LABEL_START_RE.match(lab_line)
        extra = m_any.group(2) if m_any else ""

        close_at = find_block_end(lines, i + 1)
        if close_at is None:
            out.append(lines[i])
            i += 1
            continue

        dynamic = "{{" in raw
        required = "text-red-500" in extra
        m_star = STAR_RE.match(raw)
        if m_star:
            required = True
            raw = m_star.group(1).strip()

        # 保守边界：标签里含 HTML 标签（<template v-if>…）时不转。
        # 这类标签的语义是多分支文案，塞进 label 属性会产出非法模板；
        # 宁可不改，不可改错。
        if not dynamic and ("<" in raw or ">" in raw):
            out.append(lines[i])
            i += 1
            continue

        if dynamic:
            # 属性表达式里不能再套 {{ }}：:label="x" 而不是 :label="{{ x }}"
            expr = raw.strip()
            if expr.startswith("{{") and expr.endswith("}}"):
                expr = expr[2:-2].strip()
            label_attr, label_val = ":label", expr
        else:
            label_attr = "label"
            label_val, tips = split_tips(raw)
            if tips:
                wrap_attrs = f'{wrap_attrs} tips="{tips}"'

        attrs = [f'{label_attr}="{label_val}"']
        if required:
            attrs.append("required")
        attrs.append(wrap_attrs.strip())

        out.append(f"{ws_open}<FieldItem {' '.join(attrs)}>{eol}")
        for k in range(lab_end + 1, close_at):
            out.append(lines[k])
        out.append(f"{ws_open}</FieldItem>{eol}")
        changed += 1
        i = close_at + 1

    if changed:
        text = "".join(out)
        # 不变量：开合标签必须成对。不成对说明块尾算错（会静默吞掉后半段模板），
        # 这种情况宁可整体不落盘，也不能把坏文件写进去。
        if text.count("<FieldItem") != text.count("</FieldItem>"):
            raise SystemExit(
                f"拒绝写入 {path}：FieldItem 开合不一致 "
                f"({text.count('<FieldItem')} vs {text.count('</FieldItem>')})"
            )
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.writelines(out)
    return changed


def main():
    total = 0
    for p in sorted(BASE.rglob("*.vue")):
        if p.name == "field-item.vue":
            continue
        n = convert(p)
        if n:
            print(f"{p.relative_to(ROOT)}: {n}")
            total += n
    print(f"TOTAL {total}")


if __name__ == "__main__":
    sys.exit(main())
