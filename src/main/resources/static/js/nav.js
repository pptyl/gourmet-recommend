/* 顶部导航渲染（按角色显示不同入口 + 未读消息角标） */
const Nav = {
  render(active = '') {
    const el = document.getElementById('nav');
    if (!el) return;
    const role = Auth.role();

    let links = `
      <a href="/index.html" class="${active === 'home' ? 'active' : ''}">首页</a>
      <a href="/coupons.html" class="${active === 'coupons' ? 'active' : ''}">领券中心</a>
      <a href="/notes.html" class="${active === 'notes' ? 'active' : ''}">探店笔记</a>
      <a href="/index.html#rank" class="${active === 'rank' ? 'active' : ''}">榜单</a>`;
    if (role === 'USER') links += `<a href="/profile.html" class="${active === 'profile' ? 'active' : ''}">个人中心</a>`;
    if (role === 'MERCHANT') links += `<a href="/console.html" class="${active === 'console' ? 'active' : ''}">商家后台</a>`;
    if (role === 'ADMIN') links += `<a href="/admin.html" class="${active === 'admin' ? 'active' : ''}">平台后台</a>`;

    let right;
    if (Auth.isLogin()) {
      const home = role === 'MERCHANT' ? '/console.html' : role === 'ADMIN' ? '/admin.html' : '/profile.html';
      const roleText = { USER: '普通用户', MERCHANT: '商家', ADMIN: '管理员' }[role] || '';
      const avatar = Auth.avatar();
      const inner = avatar
        ? `<img src="${UI.esc(avatar)}" alt="">`
        : UI.esc((Auth.name() || '?').slice(0, 1));
      right = `
        <a class="user-chip avatar-only" href="${home}" title="${UI.esc(Auth.name() || '我')}（${roleText}）">
          <span class="avatar">${inner}</span>
          <span id="nav-unread" class="dot-badge hidden"></span>
        </a>
        <button class="btn btn-ghost btn-sm" onclick="Auth.logout()">退出</button>`;
    } else {
      right = `
        <a class="btn btn-ghost btn-sm" href="/login.html">登录</a>
        <a class="btn btn-primary btn-sm" href="/login.html?tab=register">注册</a>`;
    }

    el.innerHTML = `
      <div class="container nav">
        <a class="logo" href="/index.html">
          <span class="mark">🍜</span><span>寻味 · 美食推荐</span>
        </a>
        <nav class="links">${links}</nav>
        <div class="spacer"></div>
        ${right}
      </div>`;
    this.loadUnread();
    this.syncProfile();
  },

  /** 用服务端资料校正本地缓存（换设备/旧会话也能显示最新头像和昵称） */
  async syncProfile() {
    if (!Auth.isLogin() || Auth.role() !== 'USER') return;
    try {
      const c = await Api.get('/api/user/center');
      const u = c.user || {};
      const avatar = u.avatar || '';
      const name = u.nickname || '';
      const changed = (avatar && avatar !== Auth.avatar()) || (name && name !== Auth.name());
      if (!changed) return;
      if (avatar) localStorage.setItem('gm_avatar', avatar);
      if (name) localStorage.setItem('gm_name', name);
      const box = document.querySelector('.user-chip .avatar');
      if (box) {
        if (avatar) box.innerHTML = `<img src="${UI.esc(avatar)}" alt="">`;
        else box.textContent = (name || '?').slice(0, 1);
      }
    } catch (e) { /* 忽略资料同步失败 */ }
  },

  async loadUnread() {
    if (!Auth.isLogin() || Auth.role() === 'ADMIN') return;
    try {
      const n = await Api.get('/api/message/unread-count');
      const badge = document.getElementById('nav-unread');
      if (badge && n > 0) {
        badge.textContent = n > 99 ? '99+' : n;
        badge.classList.remove('hidden');
      }
    } catch (e) { /* 忽略角标失败 */ }
  }
};
