/* 个人中心：资料、收藏、心愿单、评价、笔记、券包、消息 */
const Profile = {
  user: null,
  couponStatus: 0,

  async init() {
    if (!Auth.require('USER')) return;
    Nav.render('profile');
    document.querySelectorAll('#side button').forEach(btn => {
      btn.onclick = () => {
        document.querySelectorAll('#side button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.show(btn.dataset.tab);
      };
    });
    await this.loadStats();
    // 支持 /profile.html?tab=coupons 直接定位到某个标签
    const want = qs('tab');
    const target = want ? [...document.querySelectorAll('#side button')].find(b => b.dataset.tab === want) : null;
    if (target) target.click();
    else this.show('info');
  },

  async loadStats() {
    try {
      const c = await Api.get('/api/user/center');
      this.user = c.user;
      document.getElementById('greeting').textContent = '你好，' + (c.user.nickname || c.user.phone);
      const s = c.statistic;
      document.getElementById('stats').innerHTML = `
        <div class="stat brand"><div class="k">评价</div><div class="v">${s.reviewCount}</div></div>
        <div class="stat"><div class="k">收藏店铺</div><div class="v">${s.favoriteCount}</div></div>
        <div class="stat"><div class="k">探店笔记</div><div class="v">${s.noteCount}</div></div>
        <div class="stat"><div class="k">可用优惠券</div><div class="v">${s.unusedCouponCount}</div></div>
        <div class="stat"><div class="k">未读消息</div><div class="v">${c.unreadMessageCount}</div></div>`;
    } catch (e) { UI.toast(e.message, 'err'); }
  },

  async show(tab) {
    const panel = document.getElementById('panel');
    if (window.innerWidth <= 860) {
      setTimeout(() => panel.scrollIntoView({ behavior: 'smooth', block: 'start' }), 80);
    }
    panel.innerHTML = '<div class="skeleton" style="height:120px"></div>';
    try {
      if (tab === 'info') return this.renderInfo();
      if (tab === 'favorites') return this.renderFavorites();
      if (tab === 'wishlist') return this.renderWishlist();
      if (tab === 'reviews') return this.renderReviews();
      if (tab === 'notes') return this.renderNotes();
      if (tab === 'coupons') return this.renderCoupons();
      if (tab === 'messages') return this.renderMessages();
    } catch (e) {
      UI.toast(e.message, 'err');
      panel.innerHTML = UI.empty(e.message);
    }
  },

  renderInfo() {
    const u = this.user || {};
    const panel = document.getElementById('panel');
    panel.innerHTML = `
      <h3 style="margin-bottom:14px">基本资料</h3>
      <div class="form-grid">
        <div class="field"><label>手机号（登录账号）</label><input class="input" value="${UI.esc(u.phone || '')}" disabled></div>
        <div class="field"><label>昵称</label><input class="input" id="pNick" value="${UI.esc(u.nickname || '')}"></div>
        <div class="field"><label>联系电话</label><input class="input" id="pPhone" value="${UI.esc(u.contactPhone || '')}"></div>
        <div class="field full"><label>头像</label>
          <div class="row" style="gap:14px;align-items:center">
            <span class="avatar lg">${u.avatar
              ? `<img src="${UI.esc(u.avatar)}" alt="头像">`
              : UI.esc((u.nickname || u.phone || '?').slice(0, 1))}</span>
            <div style="flex:1">
              <input class="input" type="file" id="pAvatarFile" accept="image/*">
              <div class="small muted" style="margin-top:6px">${u.avatar ? '点击可更换头像' : '支持 jpg / png，最大 10MB'}</div>
            </div>
          </div>
        </div>
      </div>
      <button class="btn btn-primary" id="saveProfile">保存资料</button>

      <hr style="border:0;border-top:1px solid var(--line);margin:24px 0">
      <h3 style="margin-bottom:14px">口味偏好 <span class="small muted">（用于个性化推荐）</span></h3>
      <div class="form-grid">
        <div class="field full">
          <label>偏好菜系 <span class="hint">可多选，系统会据此推荐商家</span></label>
          ${UI.cuisineChips(u.tasteCuisine || '')}
        </div>
        <div class="field"><label>口味轻重</label>
          <select class="select" id="tFlavor">
            ${['', '清淡', '微辣', '中辣', '重辣'].map(v => `<option ${u.tasteFlavor === v ? 'selected' : ''} value="${v}">${v || '不设置'}</option>`).join('')}
          </select>
        </div>
        <div class="field"><label>忌口食材</label><input class="input" id="tTaboo" value="${UI.esc(u.taboo || '')}" placeholder="香菜,折耳根"></div>
      </div>
      <button class="btn btn-primary" id="saveTaste">保存偏好</button>

      <hr style="border:0;border-top:1px solid var(--line);margin:24px 0">
      <h3 style="margin-bottom:14px">修改密码</h3>
      <div class="form-grid">
        <div class="field"><label>原密码</label><input class="input" type="password" id="oldPwd"></div>
        <div class="field"><label>新密码</label><input class="input" type="password" id="newPwd" placeholder="6-20 位"></div>
      </div>
      <button class="btn btn-ghost" id="savePwd">修改密码</button>`;

    UI.bindChips(panel);
    const avatarInput = document.getElementById('pAvatarFile');
    avatarInput.onchange = async () => {
      const f = avatarInput.files[0];
      if (!f) return;
      if (f.size > 10 * 1024 * 1024) { UI.toast('图片不能超过 10MB', 'warn'); return; }
      avatarInput.disabled = true;
      try {
        const up = await Api.upload(f, 'avatar');
        await Api.put('/api/user/profile', { avatar: up.url });
        this.user.avatar = up.url;
        localStorage.setItem('gm_avatar', up.url);
        UI.toast('头像已更新');
        Nav.render('profile');
        this.renderInfo();
      } catch (e) {
        UI.toast(e.message, 'err');
        avatarInput.disabled = false;
      }
    };
    document.getElementById('saveProfile').onclick = async () => {
      try {
        const nickname = document.getElementById('pNick').value.trim();
        const contactPhone = document.getElementById('pPhone').value.trim();
        await Api.put('/api/user/profile', { nickname, contactPhone });
        this.user.nickname = nickname;
        this.user.contactPhone = contactPhone;
        localStorage.setItem('gm_name', nickname);
        UI.toast('资料已保存');
        Nav.render('profile');
        await this.loadStats();
        this.renderInfo();
      } catch (e) { UI.toast(e.message, 'err'); }
    };
    document.getElementById('saveTaste').onclick = async () => {
      try {
        await Api.put('/api/user/taste', {
          tasteCuisine: UI.chipsValue(panel),
          tasteFlavor: document.getElementById('tFlavor').value || null,
          taboo: document.getElementById('tTaboo').value.trim()
        });
        UI.toast('口味偏好已保存');
      } catch (e) { UI.toast(e.message, 'err'); }
    };
    document.getElementById('savePwd').onclick = async () => {
      try {
        await Api.put('/api/user/password', {
          oldPassword: document.getElementById('oldPwd').value,
          newPassword: document.getElementById('newPwd').value
        });
        UI.toast('密码已修改，请重新登录');
        setTimeout(() => Auth.logout(), 800);
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  },

  async renderFavorites() {
    const data = await Api.get('/api/favorite/page?page=1&size=50');
    const panel = document.getElementById('panel');
    if (!data.records.length) { panel.innerHTML = UI.empty('还没有收藏店铺，去首页逛逛吧'); return; }
    panel.innerHTML = `<h3 style="margin-bottom:12px">我的收藏 <span class="small muted">${data.total} 家</span></h3>` +
      data.records.map(f => `
        <div class="list-row">
          ${UI.thumb(f.coverImage, f.merchantName)}
          <div style="flex:1;min-width:0">
            <a href="/merchant.html?id=${f.merchantId}"><strong>${UI.esc(f.merchantName)}</strong></a>
            <div class="small muted">${UI.esc(f.category || '')} · ${UI.esc(f.area || '')} · ${UI.money(f.avgPrice)}/人</div>
          </div>
          ${UI.rating(f.score)}
          <button class="btn btn-ghost btn-sm" data-del="${f.merchantId}">取消收藏</button>
        </div>`).join('');
    panel.querySelectorAll('[data-del]').forEach(btn => {
      btn.onclick = async () => {
        try { await Api.del('/api/favorite/' + btn.dataset.del); UI.toast('已取消收藏'); this.renderFavorites(); this.loadStats(); }
        catch (e) { UI.toast(e.message, 'err'); }
      };
    });
  },

  async renderWishlist() {
    const data = await Api.get('/api/wishlist/page?page=1&size=50');
    const panel = document.getElementById('panel');
    if (!data.records.length) { panel.innerHTML = UI.empty('想去清单还是空的'); return; }
    panel.innerHTML = `<h3 style="margin-bottom:12px">想去清单 <span class="small muted">${data.total} 家</span></h3>` +
      data.records.map(w => `
        <div class="list-row">
          ${UI.thumb(w.coverImage, w.merchantName)}
          <div style="flex:1;min-width:0">
            <a href="/merchant.html?id=${w.merchantId}"><strong>${UI.esc(w.merchantName)}</strong></a>
            <div class="small muted">${UI.esc(w.remark || '')} ${w.planDate ? '· 计划 ' + w.planDate : ''}</div>
          </div>
          ${w.score ? UI.rating(w.score) : ''}
          <button class="btn btn-ghost btn-sm" data-del="${w.id}">删除</button>
        </div>`).join('');
    panel.querySelectorAll('[data-del]').forEach(btn => {
      btn.onclick = async () => {
        try { await Api.del('/api/wishlist/' + btn.dataset.del); UI.toast('已删除'); this.renderWishlist(); this.loadStats(); }
        catch (e) { UI.toast(e.message, 'err'); }
      };
    });
  },

  async renderReviews() {
    const data = await Api.get('/api/review/my?page=1&size=50');
    const panel = document.getElementById('panel');
    if (!data.records.length) { panel.innerHTML = UI.empty('还没有发表过评价'); return; }
    panel.innerHTML = `<h3 style="margin-bottom:12px">我的评价 <span class="small muted">${data.total} 条</span></h3>` +
      data.records.map(r => `
        <div class="msg-item">
          <div class="row">
            <strong>${UI.esc(r.merchantName || '商家')}</strong>
            ${UI.rating(r.score)} ${UI.contentStatus(r.status)}
            <span class="spacer"></span>
            <span class="small muted">${UI.date(r.createTime)}</span>
          </div>
          <div style="margin-top:8px;line-height:1.7">${UI.esc(r.content || '')}</div>
          <div class="review-imgs">${(r.imageList || []).map(u => `<img src="${UI.esc(u)}" alt="">`).join('')}</div>
        </div>`).join('');
  },

  async renderNotes() {
    const data = await Api.get('/api/note/my?page=1&size=50');
    const panel = document.getElementById('panel');
    if (!data.records.length) { panel.innerHTML = UI.empty('还没有发布探店笔记') + '<div class="center"><a class="btn btn-primary" href="/notes.html">去写一篇</a></div>'; return; }
    panel.innerHTML = `<h3 style="margin-bottom:12px">我的笔记 <span class="small muted">${data.total} 篇</span></h3>` +
      data.records.map(n => `
        <div class="list-row">
          ${UI.thumb(n.coverImage || (n.imageList || [])[0], n.title)}
          <div style="flex:1;min-width:0">
            <strong>${UI.esc(n.title)}</strong> ${UI.contentStatus(n.status)}
            <div class="small muted">${UI.esc(n.merchantName || '未绑定商家')} · 浏览 ${n.viewCount || 0} · ${UI.date(n.createTime)}</div>
          </div>
          <button class="btn btn-ghost btn-sm" data-del="${n.id}">删除</button>
        </div>`).join('');
    panel.querySelectorAll('[data-del]').forEach(btn => {
      btn.onclick = async () => {
        if (!await UI.confirm('确定删除这篇笔记吗？')) return;
        try { await Api.del('/api/note/' + btn.dataset.del); UI.toast('已删除'); this.renderNotes(); this.loadStats(); }
        catch (e) { UI.toast(e.message, 'err'); }
      };
    });
  },

  async renderCoupons() {
    const statuses = [{ v: 0, t: '未使用' }, { v: 1, t: '已使用' }, { v: 2, t: '已过期' }];
    const panel = document.getElementById('panel');
    // 一次取全部券，再按状态分组：tab 上的数量与实际列表严格一致
    const all = await Api.get('/api/coupon/my');
    const counts = { 0: 0, 1: 0, 2: 0 };
    all.forEach(c => {
      const st = Number(c.status == null ? 0 : c.status);
      counts[st] = (counts[st] || 0) + 1;
    });
    const list = all.filter(c => Number(c.status == null ? 0 : c.status) === this.couponStatus);
    panel.innerHTML = `
      <div class="row" style="margin-bottom:10px">
        <h3>我的券包</h3>
        <span class="spacer"></span>
        <span class="small muted">共 ${all.length} 张</span>
      </div>
      <div class="tabs">
        ${statuses.map(s => `<button data-cs="${s.v}" class="${this.couponStatus === s.v ? 'active' : ''}">${s.t} ${counts[s.v] || 0}</button>`).join('')}
      </div>
      ${list.length ? list.map(c => `
        <div class="coupon-card" style="margin-bottom:12px">
          <div>
            <div class="coupon-amount">${UI.money(c.discountAmount)}<small> 减</small></div>
            <div class="small muted">满 ${Number(c.thresholdAmount)} 可用</div>
          </div>
          <div style="flex:1">
            <div style="font-weight:700">${UI.esc(c.couponTitle || '优惠券')}</div>
            <div class="small muted">${UI.esc(c.merchantName || '')}</div>
            <div class="small muted">有效期至 ${UI.date(c.expireTime)}</div>
          </div>
          <div class="center">
            ${c.status === 0 ? '<span class="chip ok">未使用</span>' : c.status === 1 ? '<span class="chip">已使用</span>' : '<span class="chip danger">已过期</span>'}
            ${c.status === 0 ? `<div class="redeem-box">
              <div class="small muted">到店出示核销码</div>
              <div class="redeem-code" id="rc-${c.id}">${UI.esc(c.redeemCode || '—')}</div>
              <button class="btn btn-ghost btn-sm" data-copy="${UI.esc(c.redeemCode || '')}">复制</button>
            </div>` : ''}
          </div>
        </div>`).join('') : UI.empty('这个分类下没有优惠券')}`;

    panel.querySelectorAll('[data-cs]').forEach(btn => {
      btn.onclick = () => { this.couponStatus = Number(btn.dataset.cs); this.renderCoupons(); };
    });
    panel.querySelectorAll('[data-copy]').forEach(btn => {
      btn.onclick = async () => {
        const code = btn.dataset.copy;
        if (!code) return;
        try { await navigator.clipboard.writeText(code); UI.toast('核销码已复制'); }
        catch { UI.toast('复制失败，请手动长按选择', 'warn'); }
      };
    });
  },

  async renderMessages() {
    const data = await Api.get('/api/message/page?page=1&size=30');
    const panel = document.getElementById('panel');
    panel.innerHTML = `
      <div class="row" style="margin-bottom:12px">
        <h3>消息通知</h3><span class="spacer"></span>
        <button class="btn btn-ghost btn-sm" id="readAll">全部标为已读</button>
      </div>
      ${data.records.length ? data.records.map(m => `
        <div class="msg-item ${m.isRead ? '' : 'unread'}">
          <div class="row">
            <strong>${UI.esc(m.title)}</strong>
            ${m.isRead ? '' : '<span class="chip brand">未读</span>'}
            <span class="spacer"></span>
            <span class="small muted">${UI.date(m.createTime)}</span>
          </div>
          <div class="small muted" style="margin-top:6px;line-height:1.7">${UI.esc(m.content || '')}</div>
          ${m.isRead ? '' : `<div style="margin-top:8px"><button class="btn btn-ghost btn-sm" data-read="${m.id}">标记已读</button></div>`}
        </div>`).join('') : UI.empty('暂无消息')}`;
    const ra = document.getElementById('readAll');
    if (ra) ra.onclick = async () => {
      try { await Api.put('/api/message/read-all'); UI.toast('已全部标为已读'); this.renderMessages(); this.loadStats(); }
      catch (e) { UI.toast(e.message, 'err'); }
    };
    panel.querySelectorAll('[data-read]').forEach(btn => {
      btn.onclick = async () => {
        try { await Api.put('/api/message/' + btn.dataset.read + '/read'); this.renderMessages(); }
        catch (e) { UI.toast(e.message, 'err'); }
      };
    });
  }
};

document.addEventListener('DOMContentLoaded', () => Profile.init());
