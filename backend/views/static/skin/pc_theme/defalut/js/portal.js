/* ============================================================
   FlyCms 门户主题「墨章」portal.js  v2.1
   ------------------------------------------------------------
   纯渐进增强：本文件加载失败 / 被禁用时，页面功能与内容完整可用。
   无外部依赖、无全局污染（IIFE）、无 eval。
   能力清单
     1. 顶栏滚动投影（.topbar.is-stuck）
     2. 返回顶部按钮（.to-top，滚动 > 600px 出现）
     3. 文章阅读进度条（.read-progress，仅详情页存在时启用）
     4. 破图兜底：img 加载失败 → 换成 CSS 封面占位（与 <@cover> 同款）
     5. 分享：复制链接按钮（[data-copy]）
     6. 站内锚点平滑滚动（尊重 prefers-reduced-motion）
     7. 表单防重复提交（提交后按钮置灰，避免双击）
     8. 图片懒加载兜底（给正文图补 loading=lazy）
     9. 验证码图片点击刷新（[data-captcha]）
    10. 表单异步提交（form[data-ajax]）：POST → JSON(DataVo) → 成功跳转 / 失败就地提示
    11. 「获取验证码」按钮（button[data-post]）：请求下发短信/邮件码 + 按钮倒计时
    12. 分段切换（[data-switch]）：radio 切换同名面板显隐
    13. 关注按钮（[data-follow] → POST /ucenter/user/follow，成功后原地变「已关注」）
    14. 头像上传（input[type=file][data-avatar] → base64 → POST /ucenter/avatar.json）
    15. 省市区联动（[data-area-province] → /areas/area_child → [data-area-city]）
   注：10/11 依赖后端返回 DataVo（{code,message,url}）；无 JS 时表单退化为原生 POST。
   ============================================================ */
(function () {
  'use strict';

  var doc = document;
  var root = doc.documentElement;
  var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function ready(fn) {
    if (doc.readyState === 'loading') doc.addEventListener('DOMContentLoaded', fn);
    else fn();
  }

  /* ---------- 1/2/3. 滚动相关 ---------- */
  function initScroll() {
    var topbar = doc.querySelector('.topbar');
    var toTop = doc.querySelector('.to-top');
    var bar = doc.querySelector('.read-progress');
    var body = doc.querySelector('.art-body');
    var ticking = false;

    function update() {
      var y = window.pageYOffset || root.scrollTop;

      if (topbar) topbar.classList[y > 8 ? 'add' : 'remove']('is-stuck');
      if (toTop) toTop.classList[y > 600 ? 'add' : 'remove']('show');

      if (bar && body) {
        var start = body.getBoundingClientRect().top + y;
        var total = Math.max(1, body.offsetHeight - window.innerHeight * 0.5);
        var p = (y - start + window.innerHeight * 0.5) / total;
        bar.style.width = Math.min(100, Math.max(0, p * 100)).toFixed(2) + '%';
      }
      ticking = false;
    }

    window.addEventListener('scroll', function () {
      if (ticking) return;
      ticking = true;
      window.requestAnimationFrame(update);
    }, { passive: true });

    window.addEventListener('resize', update, { passive: true });
    update();

    if (toTop) {
      toTop.addEventListener('click', function () {
        window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' });
      });
    }
  }

  /* ---------- 4. 破图兜底 ---------- */
  function phFor(img) {
    var span = doc.createElement('span');
    span.className = 'ph';
    span.setAttribute('data-label', img.getAttribute('data-ph') || '');
    var w = img.getAttribute('width') || img.clientWidth || 0;
    var h = img.getAttribute('height') || img.clientHeight || 0;
    if (!h) {
      // 无显式尺寸时按父容器比例撑开，避免塌陷
      var rect = img.getBoundingClientRect();
      w = rect.width || w;
      h = rect.height || Math.round((rect.width || 240) * 0.625);
    }
    if (w) span.style.width = w + 'px';
    if (h) span.style.minHeight = h + 'px';
    if (img.className) span.className += ' ' + img.className;
    img.parentNode.replaceChild(span, img);
  }

  function initImageFallback() {
    var imgs = doc.querySelectorAll('img:not([data-nofallback])');
    Array.prototype.forEach.call(imgs, function (img) {
      // 内联 data:URI 与已失败过的跳过
      if (img.getAttribute('src') && img.getAttribute('src').indexOf('data:') === 0) return;
      img.setAttribute('data-nofallback', 'pending');
      img.removeAttribute('data-nofallback');

      function onErr() {
        img.removeEventListener('error', onErr);
        phFor(img);
      }
      if (img.complete && img.naturalWidth === 0 && img.getAttribute('src')) {
        onErr();
      } else {
        img.addEventListener('error', onErr);
      }
      // 正文图补懒加载
      if (!img.hasAttribute('loading')) img.setAttribute('loading', 'lazy');
      if (!img.hasAttribute('decoding')) img.setAttribute('decoding', 'async');
    });
  }

  /* ---------- 5. 复制链接 ---------- */
  function initCopy() {
    doc.addEventListener('click', function (e) {
      var el = e.target.closest && e.target.closest('[data-copy]');
      if (!el) return;
      e.preventDefault();
      var text = el.getAttribute('data-copy') || window.location.href;
      var done = function () {
        var old = el.getAttribute('data-label');
        var label = el.querySelector('.tx') || el;
        var prev = label.textContent;
        label.textContent = '已复制';
        el.classList.add('is-done');
        window.setTimeout(function () {
          label.textContent = prev;
          el.classList.remove('is-done');
        }, 1600);
        if (old) el.setAttribute('data-label', old);
      };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(done, function () {});
      } else {
        var ta = doc.createElement('textarea');
        ta.value = text;
        ta.style.cssText = 'position:fixed;left:-9999px';
        doc.body.appendChild(ta);
        ta.select();
        try { doc.execCommand('copy'); done(); } catch (err) {}
        doc.body.removeChild(ta);
      }
    });
  }

  /* ---------- 5b. 分享（微博等），URL 一律取当前页面 ---------- */
  function initShare() {
    doc.addEventListener('click', function (e) {
      var el = e.target.closest && e.target.closest('[data-share]');
      if (!el) return;
      e.preventDefault();
      var url = encodeURIComponent(window.location.href);
      var title = encodeURIComponent(doc.title || '');
      var to = el.getAttribute('data-share');
      var map = {
        weibo: 'https://service.weibo.com/share/share.php?url=' + url + '&title=' + title,
        qq: 'https://connect.qq.com/widget/shareqq/index.html?url=' + url + '&title=' + title
      };
      if (map[to]) window.open(map[to], '_blank', 'noopener,width=660,height=520');
    });
  }

  /* ---------- 6. 锚点平滑滚动 ---------- */
  function initAnchors() {
    if (reduceMotion) return;
    doc.addEventListener('click', function (e) {
      var a = e.target.closest && e.target.closest('a[href^="#"]');
      if (!a) return;
      var id = a.getAttribute('href');
      if (!id || id === '#' || id === '#!' || a.hasAttribute('data-no-smooth')) return;
      var t = doc.querySelector(id);
      if (!t) return;
      e.preventDefault();
      var top = t.getBoundingClientRect().top + (window.pageYOffset || root.scrollTop) - 78;
      window.scrollTo({ top: top, behavior: 'smooth' });
      if (history.replaceState) history.replaceState(null, '', id);
    });
  }

  /* ---------- 7. 表单防重复提交 ---------- */
  function initForms() {
    doc.addEventListener('submit', function (e) {
      var form = e.target;
      if (!form || form.tagName !== 'FORM') return;
      if (form.hasAttribute('data-keep-alive')) return;
      var btns = form.querySelectorAll('button[type=submit],input[type=submit]');
      window.setTimeout(function () {
        Array.prototype.forEach.call(btns, function (b) {
          b.disabled = true;
          b.classList.add('is-disabled');
        });
      }, 0);
      // 8 秒后恢复，避免提交失败后用户被卡住
      window.setTimeout(function () {
        Array.prototype.forEach.call(btns, function (b) {
          b.disabled = false;
          b.classList.remove('is-disabled');
        });
      }, 8000);
    });
  }

  /* ---------- 栅格/表格溢出提示：给宽表套滚动容器 ---------- */
  function initTableScroll() {
    Array.prototype.forEach.call(doc.querySelectorAll('table.table, table.tbl'), function (tb) {
      var p = tb.parentNode;
      if (!p || p.classList.contains('table-scroll')) return;
      if (tb.scrollWidth > p.clientWidth + 4) {
        var box = doc.createElement('div');
        box.className = 'table-scroll';
        box.style.cssText = 'overflow-x:auto;-webkit-overflow-scrolling:touch';
        p.insertBefore(box, tb);
        box.appendChild(tb);
      }
    });
  }

  /* ---------- 9. 验证码刷新 ---------- */
  function refreshCaptcha(img) {
    var base = img.getAttribute('data-src') || img.getAttribute('src') || '/captcha/default';
    img.setAttribute('data-src', base.split('?')[0]);
    img.setAttribute('src', base.split('?')[0] + '?_t=' + Date.now());
  }

  function initCaptcha() {
    doc.addEventListener('click', function (e) {
      var img = e.target.closest && e.target.closest('img[data-captcha]');
      if (!img) return;
      e.preventDefault();
      refreshCaptcha(img);
    });
  }

  /* ---------- 10/11 共用：提示区 ---------- */
  function msgBox(scope) {
    var box = scope.querySelector('[data-msg]');
    if (!box) {
      box = doc.createElement('div');
      box.className = 'alert alert-danger';
      box.setAttribute('data-msg', '');
      box.style.display = 'none';
      scope.insertBefore(box, scope.firstChild);
    }
    return box;
  }

  function showMsg(scope, text, kind) {
    var box = msgBox(scope);
    box.className = 'alert alert-' + (kind || 'danger');
    box.textContent = text;
    box.style.display = text ? '' : 'none';
  }

  function post(url, body) {
    return fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
        'X-Requested-With': 'XMLHttpRequest'
      },
      body: body
    }).then(function (r) { return r.text(); }).then(function (t) {
      try { return JSON.parse(t); } catch (err) { return { code: -1, message: t ? t.slice(0, 120) : '服务无响应' }; }
    });
  }

  function formBody(form) {
    var parts = [];
    var fd = new FormData(form);
    // FormData 迭代（老浏览器逐个遍历元素兜底）
    if (fd.forEach) {
      fd.forEach(function (v, k) { parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(v)); });
    } else {
      Array.prototype.forEach.call(form.elements, function (el) {
        if (el.name && !el.disabled) parts.push(encodeURIComponent(el.name) + '=' + encodeURIComponent(el.value));
      });
    }
    return parts.join('&');
  }

  /* ---------- 10. 表单异步提交 ---------- */
  function initAjaxForms() {
    doc.addEventListener('submit', function (e) {
      var form = e.target;
      if (!form || form.tagName !== 'FORM' || !form.hasAttribute('data-ajax')) return;
      e.preventDefault();
      var submitBtns = form.querySelectorAll('button[type=submit],input[type=submit]');
      Array.prototype.forEach.call(submitBtns, function (b) { b.disabled = true; });
      showMsg(form, '', 'info');
      post(form.getAttribute('action'), formBody(form)).then(function (j) {
        if (j && j.code === 0) {
          var to = j.url || form.getAttribute('data-ok-url') || '';
          if (to) { window.location.href = to; return; }
          showMsg(form, j.message || '操作成功', 'ok');
        } else {
          showMsg(form, (j && j.message) || '提交失败，请稍后重试', 'danger');
        }
      }).catch(function () {
        showMsg(form, '网络异常，请稍后重试', 'danger');
      }).then(function () {
        Array.prototype.forEach.call(submitBtns, function (b) { b.disabled = false; });
        // 失败后刷新验证码，避免复用已失效的码
        var img = form.querySelector('img[data-captcha]');
        if (img) refreshCaptcha(img);
      });
    });
  }

  /* ---------- 11. 获取验证码（短信 / 邮件） + 倒计时 ---------- */
  function initSendCode() {
    doc.addEventListener('click', function (e) {
      var btn = e.target.closest && e.target.closest('button[data-post]');
      if (!btn || btn.disabled) return;
      e.preventDefault();
      var form = btn.closest('form') || doc;
      var scope = btn.closest('.dash-main') || btn.closest('.auth-card') || btn.closest('form') || doc.body;
      var url = btn.getAttribute('data-post');
      var names = (btn.getAttribute('data-fields') || '').split(',');
      var parts = [];
      for (var i = 0; i < names.length; i++) {
        var spec = names[i].trim();
        if (!spec) continue;
        // 支持「来源名>提交名」重映射（前端字段名与接口参数名不一致时用）
        var gt = spec.indexOf('>');
        var src = gt < 0 ? spec : spec.slice(0, gt);
        var dst = gt < 0 ? spec : spec.slice(gt + 1);
        var el = form.querySelector('[name="' + src + '"]');
        parts.push(encodeURIComponent(dst) + '=' + encodeURIComponent(el ? el.value : ''));
      }
      var wait = parseInt(btn.getAttribute('data-count') || '60', 10);
      btn.disabled = true;
      btn.textContent = '发送中…';
      post(url, parts.join('&')).then(function (j) {
        if (j && j.code === 0) {
          showMsg(scope, j.message || '验证码已发送，请注意查收', 'ok');
          var left = wait;
          btn.textContent = left + ' 秒后重发';
          var timer = window.setInterval(function () {
            left -= 1;
            if (left <= 0) {
              window.clearInterval(timer);
              btn.disabled = false;
              btn.textContent = btn.getAttribute('data-label') || '获取验证码';
            } else {
              btn.textContent = left + ' 秒后重发';
            }
          }, 1000);
        } else {
          showMsg(scope, (j && j.message) || '发送失败，请稍后重试', 'danger');
          btn.disabled = false;
          btn.textContent = btn.getAttribute('data-label') || '获取验证码';
        }
      }).catch(function () {
        showMsg(scope, '网络异常，请稍后重试', 'danger');
        btn.disabled = false;
        btn.textContent = btn.getAttribute('data-label') || '获取验证码';
      });
    });
  }

  /* ---------- 12. 分段切换（注册方式等） ---------- */
  function initSwitch() {
    Array.prototype.forEach.call(doc.querySelectorAll('[data-switch]'), function (group) {
      var panes = doc.querySelectorAll('[data-pane]');
      function sync() {
        var on = group.querySelector('input:checked');
        var val = on ? on.value : '';
        Array.prototype.forEach.call(panes, function (p) {
          p.hidden = p.getAttribute('data-pane') !== val;
        });
      }
      group.addEventListener('change', sync);
      sync();
    });
  }

  /* ---------- 13. 关注按钮（[data-follow] → POST /ucenter/user/follow） ---------- */
  function initFollow() {
    doc.addEventListener('click', function (e) {
      var btn = e.target.closest && e.target.closest('button[data-follow]');
      if (!btn || btn.disabled) return;
      e.preventDefault();
      var uid = btn.getAttribute('data-follow');
      btn.disabled = true;
      btn.textContent = '…';
      post('/ucenter/user/follow', 'id=' + encodeURIComponent(uid)).then(function (j) {
        if (j && j.code === 0) {
          var span = doc.createElement('span');
          span.className = 'fbtn';
          span.innerHTML = '<span class="tag tag-ok">已关注</span>';
          btn.parentNode.replaceChild(span, btn);
        } else {
          btn.disabled = false;
          btn.textContent = '关注';
          if (j && /登陆|登录/.test(j.message || '')) window.location.href = '/login';
          else if (j && j.message) window.alert(j.message);
        }
      }).catch(function () {
        btn.disabled = false;
        btn.textContent = '关注';
      });
    });
  }

  /* ---------- 14. 头像上传（[data-avatar] file → base64 → POST /ucenter/avatar.json） ---------- */
  function initAvatar() {
    Array.prototype.forEach.call(doc.querySelectorAll('input[type=file][data-avatar]'), function (input) {
      input.addEventListener('change', function () {
        var f = input.files && input.files[0];
        if (!f) return;
        var scope = input.closest('.dash-main') || input.closest('form') || doc.body;
        if (f.size > 2 * 1024 * 1024) { showMsg(scope, '图片不要超过 2 MB', 'danger'); input.value = ''; return; }
        if (!/^image\//.test(f.type)) { showMsg(scope, '请选择图片文件', 'danger'); input.value = ''; return; }
        var reader = new FileReader();
        reader.onload = function () {
          showMsg(scope, '头像上传中…', 'info');
          post(input.getAttribute('data-avatar') || '/ucenter/avatar.json',
            'avatar=' + encodeURIComponent(String(reader.result))).then(function (j) {
            if (j && j.code === 0) {
              showMsg(scope, '头像已更新', 'ok');
              window.setTimeout(function () { window.location.reload(); }, 600);
            } else {
              showMsg(scope, (j && j.message) || '头像上传失败', 'danger');
            }
          }).catch(function () { showMsg(scope, '网络异常，头像上传失败', 'danger'); });
        };
        reader.readAsDataURL(f);
      });
    });
  }

  /* ---------- 15. 省市区联动（[data-area-province] → [data-area-city]） ---------- */
  function initAreaCascade() {
    Array.prototype.forEach.call(doc.querySelectorAll('[data-area-province]'), function (sel) {
      var city = doc.querySelector(sel.getAttribute('data-area-city-target') || '[data-area-city]');
      if (!city) return;
      sel.addEventListener('change', function () {
        var pid = sel.value;
        city.innerHTML = '<option value="0">加载中…</option>';
        fetch('/areas/area_child?parentId=' + encodeURIComponent(pid), { credentials: 'same-origin' })
          .then(function (r) { return r.json(); })
          .then(function (list) {
            var html = '<option value="0">请选择地区</option>';
            (list || []).forEach(function (a) {
              html += '<option value="' + a.areaId + '">' + a.areaName + '</option>';
            });
            city.innerHTML = html;
          })
          .catch(function () { city.innerHTML = '<option value="0">请选择地区</option>'; });
      });
    });
  }

  ready(function () {
    initScroll();
    initImageFallback();
    initCopy();
    initShare();
    initAnchors();
    initForms();
    initTableScroll();
    initCaptcha();
    initAjaxForms();
    initSendCode();
    initSwitch();
    initFollow();
    initAvatar();
    initAreaCascade();
  });
})();
