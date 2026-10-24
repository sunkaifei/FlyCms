/**
 * 富文本 HTML 白名单清洗（零依赖）。
 *
 * <p>用途：后台要把<b>用户提交的</b>正文渲染成富文本（审核页看稿、预览）时，
 * 必须先清洗再进 v-html。投稿内容未经服务端 jsoup 清洗，直接 v-html 等于把
 * 存储型 XSS 送进管理员会话——管理员会话能改全站配置，危害远大于前台。
 *
 * <p>安全性依赖：{@code <template>.innerHTML} 是惰性文档，赋值不会执行
 * script、不会触发 img onerror、不会加载 iframe。因此在模板里先删后取，
 * 取出的字符串已不含脚本载体。
 */

/** 允许的标签（编辑器产物 + 排版标签） */
const ALLOWED_TAGS = new Set([
  'p', 'br', 'hr', 'div', 'span', 'section', 'article',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'strong', 'b', 'em', 'i', 'u', 's', 'del', 'ins', 'sub', 'sup', 'mark', 'small',
  'ul', 'ol', 'li', 'dl', 'dt', 'dd',
  'blockquote', 'pre', 'code', 'figure', 'figcaption',
  'table', 'thead', 'tbody', 'tfoot', 'tr', 'th', 'td', 'colgroup', 'col',
  'a', 'img', 'video', 'audio', 'source', 'iframe_free_placeholder',
]);

/** 允许保留的属性（其余一律丢弃，含全部 on* 事件与 style 里的 url()） */
const ALLOWED_ATTRS: Record<string, Set<string>> = {
  a: new Set(['href', 'title', 'target', 'rel']),
  img: new Set(['src', 'alt', 'title', 'width', 'height', 'loading']),
  video: new Set(['src', 'controls', 'width', 'height', 'poster']),
  audio: new Set(['src', 'controls']),
  source: new Set(['src', 'type']),
  td: new Set(['colspan', 'rowspan']),
  th: new Set(['colspan', 'rowspan']),
  col: new Set(['span']),
  '*': new Set(['class', 'dir', 'lang']),
};

/** 危险 URL 协议（javascript:/data: 等一律拒） */
function isSafeUrl(v: string): boolean {
  const s = v.trim().toLowerCase();
  if (s.startsWith('#') || s.startsWith('/') || s.startsWith('./') || s.startsWith('../')) {
    return true;
  }
  return /^(https?:|mailto:|tel:)/.test(s);
}

/**
 * 清洗 HTML 片段。
 *
 * @param html 原始 HTML（可空）
 * @returns 只含白名单标签/属性的安全 HTML；输入为空或清洗失败返回空串
 */
export function sanitizeHtml(html?: string | null): string {
  if (!html) return '';
  // iframe 只允许可信视频站（白名单域名），其余整块移除
  const tpl = document.createElement('template');
  tpl.innerHTML = String(html);

  const walk = (root: ParentNode) => {
    for (const node of Array.from(root.children)) {
      const el = node as Element;
      const tag = el.tagName.toLowerCase();
      walk(el);

      if (tag === 'iframe') {
        const src = (el.getAttribute('src') ?? '').trim();
        if (!src || !/^https:\/\/(?:www\.)?(?:youtube\.com|player\.bilibili\.com|player\.youku\.com)\//.test(src)) {
          el.remove();
          continue;
        }
        // iframe 只留 src/allowfullscreen，强制 sandbox 由渲染侧加
        for (const attr of Array.from(el.attributes)) {
          if (!['src', 'allowfullscreen', 'width', 'height', 'frameborder'].includes(attr.name)) {
            el.removeAttribute(attr.name);
          }
        }
        continue;
      }

      if (!ALLOWED_TAGS.has(tag)) {
        // 不在白名单：去掉标签但保留文字（script/style 连文字一起丢）
        if (tag === 'script' || tag === 'style' || tag === 'noscript') {
          el.remove();
        } else {
          el.replaceWith(...Array.from(el.childNodes));
        }
        continue;
      }

      const allowed = ALLOWED_ATTRS[tag] ?? new Set<string>();
      for (const attr of Array.from(el.attributes)) {
        const name = attr.name.toLowerCase();
        const keep =
          (ALLOWED_ATTRS['*']!.has(name) || allowed.has(name)) && !name.startsWith('on');
        if (!keep) {
          el.removeAttribute(attr.name);
          continue;
        }
        // href/src 过协议白名单（javascript: 型 XSS）
        if ((name === 'href' || name === 'src') && !isSafeUrl(attr.value)) {
          el.removeAttribute(attr.name);
        }
      }
      // 外链强制不共享 opener
      if (tag === 'a' && el.getAttribute('target')) {
        el.setAttribute('rel', 'noopener noreferrer');
      }
    }
  };

  try {
    walk(tpl.content as unknown as ParentNode);
    return tpl.innerHTML;
  } catch {
    return '';
  }
}
