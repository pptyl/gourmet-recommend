/* 通用 UI 工具：转义、提示、弹窗、格式化、分页 */
const UI = {
  esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  },

  toast(msg, type = 'ok') {
    let box = document.getElementById('toasts');
    if (!box) {
      box = document.createElement('div');
      box.id = 'toasts';
      document.body.appendChild(box);
    }
    const el = document.createElement('div');
    el.className = 'toast toast-' + type;
    el.textContent = msg;
    box.appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; el.style.transform = 'translateY(6px)'; }, 2400);
    setTimeout(() => el.remove(), 2800);
  },

  /** 确认框，返回 Promise<boolean> */
  confirm(text, okText = '确定') {
    return new Promise(resolve => {
      const overlay = document.createElement('div');
      overlay.className = 'overlay';
      overlay.innerHTML = `
        <div class="modal" style="width:min(400px,100%)">
          <div class="modal-head"><h3>请确认</h3></div>
          <div class="modal-body" style="font-size:14px;line-height:1.7">${this.esc(text)}</div>
          <div class="modal-foot">
            <button class="btn btn-ghost" data-no>取消</button>
            <button class="btn btn-primary" data-yes>${this.esc(okText)}</button>
          </div>
        </div>`;
      document.body.appendChild(overlay);
      const close = v => { overlay.remove(); resolve(v); };
      overlay.querySelector('[data-no]').onclick = () => close(false);
      overlay.querySelector('[data-yes]').onclick = () => close(true);
      overlay.onclick = e => { if (e.target === overlay) close(false); };
    });
  },

  /** 通用弹窗：body 为 HTML 字符串，返回关闭函数 */
  modal(title, bodyHtml, width = 560) {
    const overlay = document.createElement('div');
    overlay.className = 'overlay';
    overlay.innerHTML = `
      <div class="modal" style="width:min(${width}px,100%)">
        <div class="modal-head">
          <h3>${this.esc(title)}</h3>
          <div class="spacer"></div>
          <button class="btn btn-ghost btn-sm" data-close>关闭</button>
        </div>
        <div class="modal-body">${bodyHtml}</div>
      </div>`;
    document.body.appendChild(overlay);
    const close = () => overlay.remove();
    overlay.querySelector('[data-close]').onclick = close;
    overlay.onclick = e => { if (e.target === overlay) close(); };
    return close;
  },

  money(v) {
    const n = Number(v || 0);
    return '¥' + (Number.isInteger(n) ? n : n.toFixed(2));
  },

  date(s) {
    if (!s) return '-';
    return String(s).replace('T', ' ').slice(0, 16);
  },

  stars(score) {
    const n = Math.round(Number(score || 0));
    let html = '';
    for (let i = 1; i <= 5; i++) html += `<span class="star">${i <= n ? '★' : '☆'}</span>`;
    return html;
  },

  rating(score) {
    const v = Number(score || 0);
    return `<span class="rating">${this.stars(v)} ${v > 0 ? v.toFixed(1) : '暂无'}</span>`;
  },

  /** 封面：有图用图，无图用渐变 + 店名首字 */
  cover(image, name, className = 'm-cover') {
    if (image) {
      return `<div class="${className}"><img src="${this.esc(image)}" alt="${this.esc(name)}" onerror="this.remove()"></div>`;
    }
    const ch = (name || '食').slice(0, 1);
    return `<div class="${className}">${this.esc(ch)}</div>`;
  },

  thumb(image, name) {
    if (image) return `<div class="thumb"><img src="${this.esc(image)}" alt=""></div>`;
    return `<div class="thumb">${this.esc((name || '食').slice(0, 1))}</div>`;
  },

  empty(text = '暂无数据') {
    return `<div class="empty"><div class="big">🍽️</div>${this.esc(text)}</div>`;
  },

  /** 分页控件 */
  pager(el, page, pages, onChange) {
    if (!el) return;
    if (!pages || pages <= 1) { el.innerHTML = ''; return; }
    const cur = Number(page);
    let html = `<button ${cur <= 1 ? 'disabled' : ''} data-p="${cur - 1}">上一页</button>`;
    const start = Math.max(1, cur - 2);
    const end = Math.min(pages, start + 4);
    for (let i = start; i <= end; i++) {
      html += `<button class="${i === cur ? 'active' : ''}" data-p="${i}">${i}</button>`;
    }
    html += `<button ${cur >= pages ? 'disabled' : ''} data-p="${cur + 1}">下一页</button>`;
    el.innerHTML = html;
    el.querySelectorAll('button[data-p]').forEach(btn => {
      btn.onclick = () => onChange(Number(btn.dataset.p));
    });
  },

  /** 审核状态 → 标签 */
  contentStatus(status) {
    if (status === 0) return '<span class="chip warn">待审核</span>';
    if (status === 1) return '<span class="chip ok">已通过</span>';
    return '<span class="chip danger">已下架</span>';
  },

  couponStatus(c) {
    if (c.seckillStatus === 0) return '<span class="chip warn">未开始</span>';
    if (c.seckillStatus === 1) return '<span class="chip ok">抢购中</span>';
    if (c.seckillStatus === 2) return '<span class="chip">已结束</span>';
    if (c.seckillStatus === 3) return '<span class="chip danger">已抢光</span>';
    return '<span class="chip">已下架</span>';
  },

  coverImageInput(inputEl, previewEl, type = 'common') {
    inputEl.onchange = async () => {
      const file = inputEl.files[0];
      if (!file) return;
      try {
        const data = await Api.upload(file, type);
        previewEl.value = data.url;
        UI.toast('图片已上传');
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  },

  /* ---------------- 表单校验辅助 ---------------- */

  /** 预设菜系，与后端校验规则保持一致 */
  CUISINES: ['川菜', '湘菜', '粤菜', '江浙菜', '京菜', '鲁菜', '徽菜', '闽菜', '苏菜', '浙菜',
    '西北菜', '东北菜', '云贵菜', '日料', '韩餐', '泰餐', '西餐', '火锅', '烧烤', '面食', '小吃', '甜品', '饮品'],

  /** 菜系下拉框 */
  cuisineSelect(selected, id = '') {
    const list = this.CUISINES.map(c =>
      `<option value="${c}" ${selected === c ? 'selected' : ''}>${c}</option>`).join('');
    return `<select class="select" ${id ? `id="${id}"` : ''}>
      <option value="">请选择菜系</option>${list}<option value="其他">其他（请在地址中说明）</option>
    </select>`;
  },

  /** 菜系多选胶囊（用于口味偏好 / 注册） */
  cuisineChips(selectedStr) {
    const set = new Set(String(selectedStr || '').split(/[,，]/).map(s => s.trim()).filter(Boolean));
    return `<div class="pick" data-chips>${this.CUISINES.map(c =>
      `<button type="button" data-v="${c}" class="${set.has(c) ? 'on' : ''}">${c}</button>`).join('')}</div>`;
  },

  bindChips(scope) {
    scope.querySelectorAll('[data-chips] button').forEach(btn => {
      btn.onclick = () => btn.classList.toggle('on');
    });
  },

  chipsValue(scope) {
    return [...scope.querySelectorAll('[data-chips] button.on')].map(b => b.dataset.v).join(',');
  },

  /**
   * 校验单个输入：ok=false 时标红并显示说明
   * 依赖字段结构：.field > input + .err
   */
  check(input, ok, msg) {
    if (!input) return ok;
    input.classList.toggle('invalid', !ok);
    const field = input.closest('.field');
    const err = field ? field.querySelector('.err') : null;
    if (err) {
      err.textContent = ok ? '' : msg;
      err.classList.toggle('show', !ok);
    }
    return ok;
  },

  clearChecks(scope) {
    scope.querySelectorAll('.invalid').forEach(el => el.classList.remove('invalid'));
    scope.querySelectorAll('.err').forEach(el => { el.textContent = ''; el.classList.remove('show'); });
  },

  /** 依次校验，返回是否全部通过 */
  validAll(fn) {
    let ok = true;
    fn((input, cond, msg) => { if (!this.check(input, cond, msg)) ok = false; });
    return ok;
  }
};
