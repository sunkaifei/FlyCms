# WordPress 主题 → FlyCms 皮肤 转换器

把 WordPress 主题转成 FlyCms 皮肤：PHP 模板 → FreeMarker 模板，WP 函数 → `<@fly_xxx>` 标签。

```bash
python convert.py <WP主题目录> -o <输出皮肤目录> --skin <皮肤名>
```

示例：

```bash
python convert.py ./twentytwentyfour -o ./skins/tt4 --skin tt4
```

---

## 一、先说清楚：能转多少

**这是有损转换，不是一键迁移。** 诚实的预期：

| 层次 | 能转吗 | 说明 |
|---|---|---|
| **HTML 结构** | ✅ 100% | 原样保留，一个标签不动 |
| **CSS / JS / 图片** | ✅ 100% | 原样复制 |
| **The Loop 循环** | ✅ 好 | `while(have_posts())` → `<@fly_page_model>` + `<#list>` |
| **常用内容函数** | ✅ 好 | `the_title()` `the_content()` `the_permalink()` 等 |
| **页头/页脚/侧栏** | ✅ 好 | `get_header()` → `<@fly_part name="header"/>` |
| **资源路径** | ✅ 好 | `get_template_directory_uri()` → `${skinPath}` |
| **主题元信息** | ✅ 好 | `style.css` 头部 → `theme.json` |
| **菜单** | 🟡 部分 | `wp_nav_menu()` 结构差异大，通常要手工重写 |
| **小部件 / 侧栏挂件** | 🟡 部分 | 转成 TODO，改用碎片 `<@fly_block>` |
| **自定义字段** | 🟡 部分 | `get_post_meta()` 需在 FlyCms 建同名字段 |
| **评论** | ❌ 不转 | FlyCms 评论体系与 WP 不同 |
| **WP 插件函数** | ❌ 不转 | `the_views()` 等，标记 TODO |
| **functions.php** | ❌ 不转 | 钩子/小部件注册，FlyCms 无对应机制 |
| **区块主题（FSE）** | ❌ 不转 | `templates/*.html` 块标记需人工重写 |

**经验值**：一个典型的传统（classic）PHP 主题，能自动转换到 **70%~85%**，
剩下的是"语义对齐"工作——主要集中在菜单、挂件、自定义字段三块。

---

## 二、转换映射表

### 文件映射

| WordPress | FlyCms |
|---|---|
| `index.php` / `front-page.php` / `home.php` | `index.html` |
| `single.php` | `detail.html` |
| `page.php` | `page.html` |
| `archive.php` / `category.php` | `list.html` |
| `tag.php` | `tag.html` |
| `search.php` | `search.html` |
| `404.php` | `404.html` |
| `header.php` | `parts/header.html` |
| `footer.php` | `parts/footer.html` |
| `sidebar.php` | `parts/sidebar.html` |
| `style.css` | `css/style.css`（头部另生成 `theme.json`） |
| `css/` `js/` `images/` `fonts/` | 原样复制 |
| `functions.php` `comments.php` | 跳过，写进 TODO 报告 |

### 函数映射

**列表循环体内**（`<#list dataList as item>` 中）：

| WordPress | FlyCms |
|---|---|
| `the_title()` | `${(item.title)!''}` |
| `the_permalink()` | `/${model.code}/${item.shortUrl}.html` |
| `the_content()` | `${(item.content)!''}` |
| `the_excerpt()` | `${(item.description)!''}` |
| `the_ID()` | `${item.id!''}` |
| `the_author()` | `${(item.author)!''}` |
| `the_time('Y-m-d')` | `${(item.createTime?substring(0,10))!''}` |
| `the_post_thumbnail()` | `<img src="${(item.image)!''}">` |

**详情页**（`<@fly_info_model>` 中）：同上，但 `item` 换成 `info`。

**全局**：

| WordPress | FlyCms |
|---|---|
| `get_header()` | `<@fly_part name="header"/>` |
| `get_footer()` | `<@fly_part name="footer"/>` |
| `get_sidebar()` | `<@fly_part name="sidebar"/>` |
| `get_template_directory_uri()` | `${skinPath}`（自动注入 `<#assign skinPath=...>`） |
| `get_stylesheet_uri()` | `${skinPath}/css/style.css` |
| `home_url()` / `site_url()` | `/` |
| `bloginfo('name')` | `${web_name!''}` |
| `bloginfo('description')` | `${seo_description!''}` |
| `bloginfo('charset')` | `UTF-8` |
| `wp_head()` / `wp_footer()` | 注释掉（FlyCms 无钩子机制） |
| `language_attributes()` | `lang="zh"` |
| `body_class()` | 删除 |

**条件标签**（`is_home()` `is_single()` `is_category()` 等）：
FlyCms 用**模板层级**区分页面类型（首页用 `index.html`、详情用 `detail.html`），
不需要条件判断，转换时保留内容并加 TODO 注释。

---

## 三、转换后必做的人工步骤

### 步骤 1：放对两个目录（最容易踩的坑）

FlyCms 的**模板**和**静态资源**是分开的两个目录：

```
backend/views/templates/pc_theme/{skin}/     ← 模板 .html（转换产物）
backend/views/static/assets/skin/pc_theme/{skin}/  ← css/js/images（资源）
```

转换产物里的 `css/` `js/` `images/` 要**单独搬到第二个目录**：

```bash
# 示例：contoso 皮肤
cp -r ./skins/contoso/css    backend/views/static/assets/skin/pc_theme/contoso/
cp -r ./skins/contoso/js     backend/views/static/assets/skin/pc_theme/contoso/
cp -r ./skins/contoso/images backend/views/static/assets/skin/pc_theme/contoso/
```

`${skinPath}` 已自动指向 `/assets/skin/pc_theme/{skin}`，搬对了就能直接加载。

### 步骤 2：模型字段对齐（决定能不能跑通）

转换器假设你的模型有这些字段。**缺哪个就补哪个**，否则对应变量渲染为空：

| 模板变量 | 需要的模型字段 | 说明 |
|---|---|---|
| `item.title` / `info.title` | `title` | 固定列，必有 |
| `item.content` / `info.content` | `content` | 正文，通常是 `editor` 类型 |
| `item.shortUrl` | `short_url` | 固定列，必有 |
| `item.createTime` | `create_time` | 固定列，必有 |
| `item.description` | `description` | 摘要 |
| `item.author` | `author` | 作者 |
| `item.image` | `image` | 缩略图（`image` 类型） |
| `item.countView` | `count_view` | 浏览数（替代 WP 的 `the_views()`） |

> 后台「自定义模型 → 字段管理」里加。字段类型参考 `FieldTypeEnum`。

### 步骤 3：处理 TODO 报告

打开 `CONVERT-REPORT.md`，逐条处理。最常见的三类：

| TODO 类型 | 处理方式 |
|---|---|
| `wp_nav_menu()` | 改用 `<@fly_channel_tree>` 遍历 `channelTree` 输出导航 |
| `dynamic_sidebar()` / 挂件 | 改用碎片 `<@fly_block key="xxx"/>`，后台碎片管理里配内容 |
| `get_post_meta()` / 插件函数 | 建同名模型字段；插件功能若必须，考虑用标签或碎片重写 |

### 步骤 4：启用与验证

1. 后台「模板中心 → 主题」导入或直接放目录后刷新；
2. 切到该主题，访问首页/列表页/详情页各一次；
3. 开模板调试条确认命中的模板名（见开发方案 §8.2）；
4. 看浏览器控制台，404 的资源说明步骤 1 的路径没搬对。

---

## 四、已知限制

1. **不处理 FSE 区块主题**（`templates/*.html` 带 `<!-- wp:xxx -->` 块标记的）。
   块标记语义与 FlyCms 标签差异过大，转换价值低于重写。
2. **不处理 `WP_Query` 自定义查询**。复杂查询建议改用
   `<@fly_list_model>` + 筛选参数，或 `<@fly_hot_model>`。
3. **不保证 PHP 短标签 `<?=`** —— 转换器只认 `<?php`。
   若主题用了 `<?=`，先用 `sed` 批量替换成 `<?php echo` 再转。
4. **多循环嵌套**（循环里套循环）只处理最外层，内层会标 TODO。
5. **不转换多语言函数**（`__()` `_e()`），直接输出原文。

---

## 五、扩展映射表

新增函数映射改 `convert.py` 顶部三张表即可：

| 表 | 作用 |
|---|---|
| `LOOP_MAP` | 列表循环体内的函数（用 `item`） |
| `SINGLE_MAP` | 详情页的函数（用 `info`） |
| `GLOBAL_MAP` | 全局函数（任何模板） |

用 `_php('函数名')` 辅助构造正则，**必须匹配整个 `<?php ... ?>` 标签**，
否则残留会被兜底逻辑再包一层 TODO（这是踩过的坑）：

```python
(_php('the_subtitle'), "${(item.subtitle)!''}"),          # 无参
(_php('the_time', "'Y-m-d'"), "${(item.createTime?substring(0,10))!''}"),  # 带参
```
