/* 全局 API 与登录态封装 */
const Auth = {
  token() { return localStorage.getItem('gm_token') || ''; },
  role() { return localStorage.getItem('gm_role') || ''; },
  name() { return localStorage.getItem('gm_name') || ''; },
  avatar() { return localStorage.getItem('gm_avatar') || ''; },
  id() { return localStorage.getItem('gm_id') || ''; },
  isLogin() { return !!this.token(); },
  save(data) {
    localStorage.setItem('gm_token', data.token || '');
    localStorage.setItem('gm_role', data.role || '');
    localStorage.setItem('gm_name', data.name || '');
    localStorage.setItem('gm_avatar', data.avatar || '');
    localStorage.setItem('gm_id', data.accountId || '');
  },
  clear() {
    ['gm_token', 'gm_role', 'gm_name', 'gm_avatar', 'gm_id'].forEach(k => localStorage.removeItem(k));
  },
  logout() {
    this.clear();
    location.href = '/index.html';
  },
  /** 需要登录时跳转登录页；指定角色不匹配则回首页 */
  require(role) {
    const here = location.pathname + location.search;
    if (!this.isLogin()) {
      location.href = '/login.html?redirect=' + encodeURIComponent(here);
      return false;
    }
    if (role && this.role() !== role) {
      location.href = '/index.html';
      return false;
    }
    return true;
  }
};

const Api = {
  async request(path, opts = {}) {
    const method = opts.method || 'GET';
    const headers = {};
    const body = opts.body;
    if (body !== undefined && !(body instanceof FormData)) {
      headers['Content-Type'] = 'application/json; charset=utf-8';
    }
    if (Auth.token() && opts.auth !== false) {
      headers['Authorization'] = 'Bearer ' + Auth.token();
    }
    let res;
    try {
      res = await fetch(path, {
        method,
        headers,
        body: body instanceof FormData ? body : (body === undefined ? undefined : JSON.stringify(body))
      });
    } catch (e) {
      throw new Error('网络异常，请确认后端已启动');
    }
    const json = await res.json().catch(() => ({ code: 500, message: '服务器返回异常' }));
    if (json.code === 401) {
      Auth.clear();
      if (!location.pathname.endsWith('login.html')) {
        location.href = '/login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
      }
      throw new Error(json.message || '请先登录');
    }
    if (json.code !== 200) {
      throw new Error(json.message || '请求失败');
    }
    return json.data;
  },
  get(path, auth = true) { return this.request(path, { method: 'GET', auth }); },
  post(path, body, auth = true) { return this.request(path, { method: 'POST', body, auth }); },
  put(path, body, auth = true) { return this.request(path, { method: 'PUT', body, auth }); },
  del(path, auth = true) { return this.request(path, { method: 'DELETE', auth }); },
  async upload(file, type = 'common') {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('type', type);
    return this.request('/api/file/upload', { method: 'POST', body: fd });
  }
};

/** 简单的 query 参数读取 */
function qs(name, def = '') {
  return new URLSearchParams(location.search).get(name) || def;
}
