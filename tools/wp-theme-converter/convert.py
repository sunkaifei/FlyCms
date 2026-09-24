#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
WordPress 主题 → FlyCms 皮肤 转换器

把 WP 主题目录转成 FlyCms 皮肤目录：PHP 模板 → FreeMarker 模板（.html）。

设计原则：
  1. 保守转换：能确定映射的才转，不能确定的原样保留并写进 TODO 报告，绝不静默丢弃；
  2. 无损兜底：任何未识别的 PHP 块都转成 HTML 注释留在原处，人工可查；
  3. 产出可交付：即使只转了 70%，剩下的 30% 有明确的 TODO 清单指路。

用法：
    python convert.py <wp主题目录> [-o 输出皮肤目录] [--skin 皮肤名]

示例：
    python convert.py ./twentytwentyfour -o ../skins/tt4 --skin tt4
"""

import argparse
import json
import os
import re
import shutil
import sys
from datetime import datetime

# ///////////////////// 文件映射 /////////////////////

# WP 模板文件 → FlyCms 皮肤内相对路径
FILE_MAP = {
    'index.php':      'index.html',
    'front-page.php': 'index.html',
    'home.php':       'index.html',
    'single.php':     'detail.html',
    'page.php':       'page.html',
    'archive.php':    'list.html',
    'category.php':   'list.html',
    'tag.php':        'tag.html',
    'search.php':     'search.html',
    '404.php':        '404.html',
    'header.php':     'parts/header.html',
    'footer.php':     'parts/footer.html',
    'sidebar.php':    'parts/sidebar.html',
}

# 明确不转换的文件（语义无法映射，写进报告）
SKIP_FILES = {
    'functions.php':  '主题功能注册（钩子/小部件/菜单），FlyCms 无对应机制，需按需求用标签或碎片重写',
    'comments.php':   '评论模板，FlyCms 评论体系与 WP 不同（走 fly_comment + 文章评论标签）',
    'comments-template.php': '同上',
    'searchform.php': '搜索表单，用 <@fly_ 搜索标签或纯 HTML 表单替代',
    'sidebar-*.php':  '额外小部件区，用碎片 <@fly_block> 替代',
    'template-parts/*.php': '块主题的部件，已按 parts/ 处理主要项',
}

def _php(call, arg=r''):
    """构造匹配"整个 PHP 标签"的正则：<?php [echo] call([arg])[;] ?>"""
    return (r'<\?php\s*(?:echo\s+)?' + call + r'\s*\(\s*'
            + (re.escape(arg) + r'\s*' if arg else r'[^)]*\s*')
            + r'\)\s*;?\s*\?>')


# WP → FlyCms 函数/变量映射（列表循环体内，用 item）
# 注意：必须匹配"整个 PHP 标签"，否则会残留 <?php 被兜底逻辑再包一层 TODO
LOOP_MAP = [
    (_php('the_time', "'Y-m-d'"),      "${(item.createTime?substring(0,10))!''}"),
    (_php('the_time', '"Y-m-d"'),      "${(item.createTime?substring(0,10))!''}"),
    (_php('the_time', "'Y'"),          "${(item.createTime?substring(0,4))!''}"),
    (_php('the_time'),                 "${(item.createTime)!''}"),
    (_php('the_date'),                 "${(item.createTime?substring(0,10))!''}"),
    (_php('the_title'),                "${(item.title)!''}"),
    (_php('get_the_title'),            "${(item.title)!''}"),
    (_php('the_permalink'),            "/${model.code}/${item.shortUrl}.html"),
    (_php('get_permalink'),            "/${model.code}/${item.shortUrl}.html"),
    (_php('the_content'),              "${(item.content)!''}"),
    (_php('the_excerpt'),              "${(item.description)!''}"),
    (_php('the_ID'),                   "${item.id!''}"),
    (_php('get_the_ID'),               "${item.id!''}"),
    (_php('the_author'),               "${(item.author)!''}"),
    (_php('the_post_thumbnail_url'),   "${(item.image)!''}"),
    (_php('the_post_thumbnail'),       "<img src=\"${(item.image)!''}\" alt=\"${(item.title)!''}\">"),
]

# 单条内容上下文（single/page 页，用 info 而不是 item）
SINGLE_MAP = [
    (_php('the_time', "'Y-m-d'"),      "${(info.createTime?substring(0,10))!''}"),
    (_php('the_time'),                 "${(info.createTime)!''}"),
    (_php('the_title'),                "${(info.title)!''}"),
    (_php('get_the_title'),            "${(info.title)!''}"),
    (_php('the_permalink'),            "/${model.code}/${info.shortUrl}.html"),
    (_php('the_content'),              "${(info.content)!''}"),
    (_php('the_excerpt'),              "${(info.description)!''}"),
    (_php('the_ID'),                   "${info.id!''}"),
    (_php('the_author'),               "${(info.author)!''}"),
    (_php('the_post_thumbnail'),       "<img src=\"${(info.image)!''}\" alt=\"${(info.title)!''}\">"),
]

# 全局函数（循环体外，任何模板都适用）
GLOBAL_MAP = [
    (r'<\?php\s*(?:echo\s+)?get_header\s*\(\s*\)\s*;?\s*\?>', '<@fly_part name="header"/>'),
    (r'<\?php\s*(?:echo\s+)?get_footer\s*\(\s*\)\s*;?\s*\?>', '<@fly_part name="footer"/>'),
    (r'<\?php\s*(?:echo\s+)?get_sidebar\s*\(\s*\)\s*;?\s*\?>', '<@fly_part name="sidebar"/>'),
    (r'<\?php\s*wp_head\s*\(\s*\)\s*;?\s*\?>',   '<!-- WP: wp_head() 钩子，FlyCms 无需（样式/脚本直接在模板引入） -->'),
    (r'<\?php\s*wp_footer\s*\(\s*\)\s*;?\s*\?>', '<!-- WP: wp_footer() 钩子，FlyCms 无需 -->'),
    (r'<\?php\s*body_class\s*\(\s*\)\s*;?\s*\?>', ''),
    (r'<\?php\s*wp_body_open\s*\(\s*\)\s*;?\s*\?>', ''),
    (r'<\?php\s*language_attributes\s*\(\s*\)\s*;?\s*\?>', 'lang="zh"'),
    (r'<\?php\s*(?:echo\s+)?get_template_directory_uri\s*\(\s*\)\s*;?\s*\?>', '${skinPath}'),
    (r'<\?php\s*(?:echo\s+)?get_stylesheet_directory_uri\s*\(\s*\)\s*;?\s*\?>', '${skinPath}'),
    (r'<\?php\s*(?:echo\s+)?get_stylesheet_uri\s*\(\s*\)\s*;?\s*\?>', '${skinPath}/css/style.css'),
    (r'<\?php\s*(?:echo\s+)?home_url\s*\(\s*\)\s*;?\s*\?>', '/'),
    (r'<\?php\s*(?:echo\s+)?site_url\s*\(\s*\)\s*;?\s*\?>', '/'),
    (r'<\?php\s*bloginfo\s*\(\s*[\'"]name[\'"]\s*\)\s*;?\s*\?>', '${web_name!\'\'}'),
    (r'<\?php\s*(?:echo\s+)?get_bloginfo\s*\(\s*[\'"]name[\'"]\s*\)\s*;?\s*\?>', '${web_name!\'\'}'),
    (r'<\?php\s*bloginfo\s*\(\s*[\'"]description[\'"]\s*\)\s*;?\s*\?>', '${seo_description!\'\'}'),
    (r'<\?php\s*bloginfo\s*\(\s*[\'"]url[\'"]\s*\)\s*;?\s*\?>', '/'),
    (r'<\?php\s*bloginfo\s*\(\s*[\'"]charset[\'"]\s*\)\s*;?\s*\?>', 'UTF-8'),
    (r'<\?php\s*(?:echo\s+)?esc_url\s*\(\s*home_url\s*\(\s*\)\s*\)\s*;?\s*\?>', '/'),
]

# WP 条件标签 → 说明（FlyCms 用模板层级区分页面类型，不需要条件判断）
CONDITIONAL_NOTE = {
    'is_home': 'FlyCms 首页直接用 index.html，无需条件判断',
    'is_front_page': '同上',
    'is_single': 'FlyCms 详情页用 detail.html，无需条件判断',
    'is_page': 'FlyCms 单页栏目用 page.html，无需条件判断',
    'is_category': 'FlyCms 分类列表用 list.html / list-{channel}.html，无需条件判断',
    'is_archive': '同上',
    'is_search': 'FlyCms 搜索页用 search.html',
    'is_404': 'FlyCms 错误页用 404.html',
    'is_tag': 'FlyCms 标签页用 tag.html',
}

TODO = []          # 待人工处理清单
COPIED = []        # 已复制的静态资源


def log_todo(fname, kind, detail, line_no=None):
    loc = fname + ((':' + str(line_no)) if line_no else '')
    TODO.append({'file': loc, 'kind': kind, 'detail': detail})


# ///////////////////// 转换核心 /////////////////////

def convert_loop(src, fname):
    """把 WP 的 The Loop 转成 FreeMarker 列表标签。

    WP 典型写法：
        <?php if (have_posts()) : while (have_posts()) : the_post(); ?>
            ... the_title() ...
        <?php endwhile; endif; ?>
    或：
        <?php while (have_posts()) : the_post(); ?> ... <?php endwhile; ?>
    """
    # 归一化：把 <?php ... ?> 的多种变体统一
    loop_open = re.compile(
        r'<\?php\s*(?:if\s*\(\s*have_posts\s*\(\s*\)\s*\)\s*:\s*)?'
        r'while\s*\(\s*have_posts\s*\(\s*\)\s*\)\s*:\s*the_post\s*\(\s*\)\s*;?\s*\?>',
        re.I)
    loop_close = re.compile(
        r'<\?php\s*(?:endwhile\s*;?\s*)?(?:endif\s*;?\s*)?\s*\?>',
        re.I)

    # 找到循环体
    m = loop_open.search(src)
    if not m:
        return src, False

    start = m.end()
    # 从 start 开始找第一个 endwhile
    close = re.compile(r'<\?php\s*endwhile', re.I).search(src, start)
    if not close:
        return src, False
    end = close.start()

    body = src[start:end]
    # 清理结尾可能残留的 endif
    body = re.sub(r'<\?php\s*endif\s*;?\s*\?>\s*$', '', body, flags=re.I)
    body = re.sub(r'<\?php\s*endwhile\s*;?\s*\?>\s*$', '', body, flags=re.I)

    # 循环体内部：PHP 标签替换为 FlyCms 变量
    for pattern, repl in LOOP_MAP:
        body = re.sub(pattern, repl, body)
    # 残留 PHP：<?php echo $x; ?> 之类
    body = _strip_residual_php(body, fname, in_loop=True)

    # 组装：用 page_model 提供数据 + 分页条
    out = (
        '<@fly_page_model model="${model.code}" p="${p!1}" rows="10">\n'
        '<#if dataList?? && dataList?size gt 0>\n'
        '<#list dataList as item>\n'
        + body.strip() + '\n'
        '</#list>\n'
        '<#else>\n'
        '<p>暂无内容</p>\n'
        '</#if>\n'
        '<#if pageHtml?? && pageHtml != \'\'>${pageHtml}</#if>\n'
        '</@fly_page_model>'
    )
    # 替换原始区间
    # endwhile 之后可能还有 endif 的 ?>，一并吃掉
    tail_end = end
    tail = re.compile(r'<\?php\s*endif\s*;?\s*?>', re.I).search(src, end)
    if tail and tail.start() - end < 40:
        tail_end = tail.end()
    else:
        tail_end = src.find('?>', end)
        tail_end = tail_end + 2 if tail_end != -1 else end

    src = src[:m.start()] + out + src[tail_end:]
    return src, True


def convert_single(src, fname):
    """单条内容页（single/page）：包一层 info_model 并把 the_xxx() 映射到 info."""
    has_single_tag = re.search(r'\bthe_title\s*\(|\bthe_content\s*\(', src)
    if not has_single_tag:
        return src, False
    for pattern, repl in SINGLE_MAP:
        src = re.sub(pattern, repl, src)
    return src, True


def _strip_residual_php(src, fname, in_loop=False):
    """把无法识别的 PHP 块转成 HTML 注释，保留现场供人工处理。"""
    def repl(m):
        code = m.group(0)
        inner = code.strip()
        # 空块直接删
        if re.fullmatch(r'<\?php\s*\?>', inner or ''):
            return ''
        note = 'WP_PHP' if not in_loop else 'WP_PHP_IN_LOOP'
        log_todo(fname, '未识别PHP块', inner[:200])
        return '<!-- TODO[%s]: %s -->' % (note, inner.replace('-->', '--\\>')[:200])
    return re.sub(r'<\?php.*?\?>', repl, src, flags=re.S)


def apply_global_map(src, fname, skin):
    """全局函数替换（get_header / bloginfo / 资源路径等）。"""
    # skinPath 变量注入：模板里用 ${skinPath} 指代当前皮肤资源根
    for pattern, repl in GLOBAL_MAP:
        src = re.sub(pattern, repl, src, flags=re.I)

    # 条件标签：if ( is_home() ) { ... } → 直接保留内容并注释
    for fn, note in CONDITIONAL_NOTE.items():
        p = re.compile(r'<\?php\s*if\s*\(\s*' + fn + r'\s*\(\s*\)\s*\)\s*:\s*\?>', re.I)
        if p.search(src):
            log_todo(fname, '条件标签', '%s() —— %s' % (fn, note))
            src = p.sub('<!-- TODO: ' + note + ' -->', src)

    # 残留 PHP
    src = _strip_residual_php(src, fname)

    # 注入 skinPath（FreeMarker assign）
    if '${skinPath}' in src:
        src = '<#assign skinPath = "/assets/skin/pc_theme/' + skin + '">\n' + src
    return src


def convert_file(src_path, dst_path, fname_out, skin, page_type):
    """转换单个模板文件。"""
    with open(src_path, 'r', encoding='utf-8', errors='replace') as f:
        src = f.read()

    converted_loop = False
    if page_type == 'list':
        src, converted_loop = convert_loop(src, fname_out)
        if not converted_loop:
            # 没有 The Loop，但有 the_xxx()，尝试按单条处理
            src, _ = convert_single(src, fname_out)
    elif page_type == 'detail':
        src, _ = convert_single(src, fname_out)
        src, converted_loop = convert_loop(src, fname_out)

    src = apply_global_map(src, fname_out, skin)

    # 详情页/单页：若用了 info 变量，包一层 info_model
    if '${(info.' in src or '${info.' in src:
        if '<@fly_info_model' not in src:
            src = ('<@fly_info_model model="${model.code}" shortUrl="${shortUrl!}">\n'
                   + src + '\n</@fly_info_model>')

    os.makedirs(os.path.dirname(dst_path), exist_ok=True)
    with open(dst_path, 'w', encoding='utf-8') as f:
        f.write(src)
    return src


# ///////////////////// theme.json 生成 /////////////////////

def parse_style_header(style_path):
    """从 WP style.css 头部注释提取主题元信息。"""
    if not os.path.isfile(style_path):
        return {}
    with open(style_path, 'r', encoding='utf-8', errors='replace') as f:
        head = f.read(2000)
    meta = {}
    for key in ['Theme Name', 'Theme URI', 'Author', 'Author URI',
                'Description', 'Version', 'License', 'Text Domain', 'Tags']:
        m = re.search(r'^\s*\*?\s*' + re.escape(key) + r'\s*:\s*(.+)$', head, re.M | re.I)
        if m:
            meta[key] = m.group(1).strip()
    return meta


def build_theme_json(meta, skin, files):
    return {
        'name': meta.get('Theme Name', skin),
        'code': skin,
        'version': meta.get('Version', '1.0.0'),
        'author': meta.get('Author', 'converted-from-wordpress'),
        'parent': '',
        'thumbnail': 'screenshot.png' if os.path.exists('screenshot.png') else '',
        'description': meta.get('Description', '由 WordPress 主题自动转换'),
        'engine': 'freemarker',
        'origin': {
            'source': 'wordpress',
            'themeName': meta.get('Theme Name', skin),
            'themeUri': meta.get('Theme URI', ''),
            'convertedAt': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        },
        'supports': {
            'templates': sorted([f for f in files if f.endswith('.html')]),
            'parts': sorted([os.path.basename(f)[:-5]
                             for f in files if f.startswith('parts/')]),
            'device': ['pc'],
        },
        'settings': {
            'layout': {'contentSize': '1200px', 'wideSize': '1400px'},
        },
        'customTemplates': [],
    }


# ///////////////////// 主流程 /////////////////////

PAGE_TYPE = {
    # WP 的 index.php 就是文章列表（The Loop），必须按 list 处理
    'index.html': 'list', 'list.html': 'list', 'tag.html': 'list',
    'detail.html': 'detail', 'page.html': 'detail',
    'search.html': 'list', '404.html': 'index',
    'parts/header.html': 'part', 'parts/footer.html': 'part', 'parts/sidebar.html': 'part',
}


def main():
    ap = argparse.ArgumentParser(description='WordPress 主题 → FlyCms 皮肤转换器')
    ap.add_argument('src', help='WP 主题目录')
    ap.add_argument('-o', '--out', required=True, help='输出皮肤目录')
    ap.add_argument('--skin', default=None, help='皮肤名（默认取目录名）')
    args = ap.parse_args()

    src_dir = os.path.abspath(args.src)
    if not os.path.isdir(src_dir):
        print('错误：源目录不存在 ' + src_dir)
        sys.exit(1)
    skin = args.skin or os.path.basename(src_dir.rstrip('/\\'))
    out_dir = os.path.abspath(args.out)

    if os.path.exists(out_dir):
        print('输出目录已存在，将覆盖：' + out_dir)
    os.makedirs(out_dir, exist_ok=True)

    produced = []

    # 1) 转换模板文件
    for wp_file, fly_file in FILE_MAP.items():
        p = os.path.join(src_dir, wp_file)
        if not os.path.isfile(p):
            continue
        # 多个源文件映射到同一目标时，只取第一个（优先级按 FILE_MAP 声明顺序）
        dst = os.path.join(out_dir, fly_file)
        if os.path.exists(dst) and fly_file in produced:
            log_todo(wp_file, '重复映射', '%s 已被前面的文件生成，已跳过' % fly_file)
            continue
        convert_file(p, dst, fly_file, skin, PAGE_TYPE.get(fly_file, 'index'))
        produced.append(fly_file)
        print('  ✓ %-18s → %s' % (wp_file, fly_file))

    # 2) 复制静态资源（css/js/images/fonts 等）
    for res in ['css', 'js', 'images', 'img', 'assets', 'fonts', 'inc']:
        rp = os.path.join(src_dir, res)
        if os.path.isdir(rp):
            dst = os.path.join(out_dir, res)
            shutil.copytree(rp, dst, dirs_exist_ok=True)
            COPIED.append(res)
            print('  ✓ %-18s → %s/（原样复制）' % (res, res))

    # style.css → assets 内，同时保留一份供 theme.json 参考
    style_src = os.path.join(src_dir, 'style.css')
    if os.path.isfile(style_src):
        os.makedirs(os.path.join(out_dir, 'css'), exist_ok=True)
        shutil.copy(style_src, os.path.join(out_dir, 'css', 'style.css'))
        print('  ✓ %-18s → css/style.css' % 'style.css')

    # 截图
    shot = os.path.join(src_dir, 'screenshot.png')
    if os.path.isfile(shot):
        shutil.copy(shot, os.path.join(out_dir, 'screenshot.png'))
        print('  ✓ %-18s → screenshot.png' % 'screenshot.png')

    # 3) 记录跳过的文件
    for wp_file, reason in SKIP_FILES.items():
        if '*' in wp_file:
            continue
        if os.path.isfile(os.path.join(src_dir, wp_file)):
            log_todo(wp_file, '跳过不转换', reason)

    # 4) 生成 theme.json
    meta = parse_style_header(style_src)
    tj = build_theme_json(meta, skin, produced)
    with open(os.path.join(out_dir, 'theme.json'), 'w', encoding='utf-8') as f:
        json.dump(tj, f, ensure_ascii=False, indent=2)
    print('  ✓ %-18s → theme.json' % 'theme.json')

    # 5) 生成转换报告
    report = {
        'skin': skin,
        'source': src_dir,
        'output': out_dir,
        'convertedAt': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        'convertedFiles': produced,
        'copiedResources': COPIED,
        'todoCount': len(TODO),
        'todo': TODO,
    }
    with open(os.path.join(out_dir, 'CONVERT-REPORT.json'), 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    # 6) 人类可读报告
    lines = ['# 转换报告：%s' % skin, '']
    lines.append('- 源主题：%s' % src_dir)
    lines.append('- 输出皮肤：%s' % out_dir)
    lines.append('- 转换文件：%s' % ', '.join(produced) or '（无）')
    lines.append('- 复制资源：%s' % (', '.join(COPIED) or '（无）'))
    lines.append('- 待处理项：**%d** 条' % len(TODO))
    lines.append('')
    if TODO:
        lines.append('## 待人工处理')
        lines.append('')
        lines.append('| 文件 | 类型 | 说明 |')
        lines.append('|---|---|---|')
        for t in TODO[:200]:
            lines.append('| %s | %s | %s |' % (t['file'], t['kind'], t['detail'].replace('|', '\\|')))
    with open(os.path.join(out_dir, 'CONVERT-REPORT.md'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))

    print('')
    print('转换完成：%s' % out_dir)
    print('  模板 %d 个，资源 %d 组，待人工处理 %d 条' % (len(produced), len(COPIED), len(TODO)))
    print('  详见 CONVERT-REPORT.md / CONVERT-REPORT.json')
    if not produced:
        print('')
        print('⚠ 未找到任何可转换的模板文件，请确认目录是 WordPress 主题根目录。')


if __name__ == '__main__':
    main()
