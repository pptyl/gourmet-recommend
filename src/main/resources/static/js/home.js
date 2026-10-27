/* 首页：推荐 / 榜单 / 商家筛选列表 */
const Home = {
  state: { page: 1, size: 8, area: '', category: '', minScore: '', sort: 'score', keyword: '' },

  init() {
    Nav.render('home');
    this.bind();
    this.loadRecommend();
    this.loadRank();
    this.loadMerchants();
    this.loadFilters();
    if (Auth.isLogin() && Auth.role() === 'USER') {
      const link = document.getElementById('tasteLink');
      if (link) { link.href = '/profile.html'; link.textContent = '去设置口味偏好'; }
    }
  },

  bind() {
    document.getElementById('searchForm').onsubmit = e => {
      e.preventDefault();
      // 搜索按全局处理：清掉地区/菜系/评分筛选，避免出现“筛选残留导致 0 条”
      this.state.area = '';
      this.state.category = '';
      this.state.minScore = '';
      document.getElementById('fArea').value = '';
      document.getElementById('fCategory').value = '';
      document.getElementById('fScore').value = '';
      this.state.keyword = document.getElementById('kw').value.trim();
      this.state.page = 1;
      this.loadMerchants();
    };

    const onChange = id => {
      document.getElementById(id).onchange = () => this.applyFilters();
    };
    ['fArea', 'fCategory', 'fScore', 'fSort'].forEach(onChange);

    document.getElementById('btnFilter').onclick = () => this.applyFilters();
    document.getElementById('btnReset').onclick = () => {
      this.state = { page: 1, size: 8, area: '', category: '', minScore: '', sort: 'score', keyword: '' };
      document.getElementById('kw').value = '';
      document.getElementById('fArea').value = '';
      document.getElementById('fCategory').value = '';
      document.getElementById('fScore').value = '';
      document.getElementById('fSort').value = 'score';
      this.loadMerchants();
    };
  },

  applyFilters() {
    this.state.area = document.getElementById('fArea').value;
    this.state.category = document.getElementById('fCategory').value;
    this.state.minScore = document.getElementById('fScore').value;
    this.state.sort = document.getElementById('fSort').value;
    this.state.page = 1;
    this.loadMerchants();
  },

  async loadFilters() {
    try {
      const data = await Api.get('/api/merchant/page?page=1&size=100', false);
      const areas = [...new Set(data.records.map(m => m.area).filter(Boolean))];
      const cats = [...new Set(data.records.map(m => m.category).filter(Boolean))];
      document.getElementById('fArea').innerHTML = '<option value="">全部地区</option>' +
        areas.map(a => `<option value="${UI.esc(a)}">${UI.esc(a)}</option>`).join('');
      document.getElementById('fCategory').innerHTML = '<option value="">全部菜系</option>' +
        cats.map(c => `<option value="${UI.esc(c)}">${UI.esc(c)}</option>`).join('');
      document.getElementById('quickTags').innerHTML = cats.slice(0, 6)
        .map(c => `<span data-cat="${UI.esc(c)}" style="cursor:pointer">${UI.esc(c)}</span>`).join('');
      document.querySelectorAll('#quickTags span').forEach(tag => {
        tag.onclick = () => {
          this.state.category = tag.dataset.cat;
          this.state.keyword = '';
          this.state.page = 1;
          document.getElementById('kw').value = '';
          document.getElementById('fCategory').value = tag.dataset.cat;
          this.loadMerchants();
          document.getElementById('list').scrollIntoView({ behavior: 'smooth' });
        };
      });
    } catch (e) { /* 筛选条件失败不阻塞页面 */ }
  },

  async loadRecommend() {
    const box = document.getElementById('recList');
    box.innerHTML = '<div class="skeleton" style="height:230px"></div>'.repeat(3);
    try {
      const list = await Api.get('/api/recommend/home?limit=6');
      document.getElementById('recSub').textContent = Auth.isLogin()
        ? '根据口味偏好与浏览、收藏、评价行为智能匹配'
        : '登录后可根据口味偏好个性化推荐，当前展示热门商家';
      if (!list.length) { box.innerHTML = UI.empty('暂无推荐'); return; }
      box.innerHTML = list.map(m => this.card(m, true)).join('');
      this.bindCards(box);
    } catch (e) {
      box.innerHTML = UI.empty(e.message);
    }
  },

  async loadRank() {
    const box = document.getElementById('rankList');
    box.innerHTML = '<div class="skeleton" style="height:180px"></div>'.repeat(4);
    try {
      const list = await Api.get('/api/recommend/hot?limit=4', false);
      box.innerHTML = list.map((m, i) => `
        <a class="m-card" href="/merchant.html?id=${m.id}">
          ${UI.cover(m.coverImage, m.name)}
          <div class="m-body">
            <div class="m-title"><h3>${UI.esc(m.name)}</h3></div>
            <div class="m-meta">${UI.rating(m.score)} <span>·</span> <span>${UI.esc(m.category || '')}</span></div>
            <div class="m-meta"><span>热度 ${Math.round(m.heat || 0)}</span><span>·</span><span>${UI.money(m.avgPrice)}/人</span></div>
          </div>
        </a>`).join('');
    } catch (e) {
      box.innerHTML = UI.empty(e.message);
    }
  },

  async loadMerchants() {
    const box = document.getElementById('list');
    box.innerHTML = '<div class="skeleton" style="height:230px"></div>'.repeat(4);
    const s = this.state;
    const params = new URLSearchParams({ page: s.page, size: s.size, sort: s.sort });
    if (s.area) params.set('area', s.area);
    if (s.category) params.set('category', s.category);
    if (s.minScore) params.set('minScore', s.minScore);
    if (s.keyword) params.set('keyword', s.keyword);
    try {
      const data = await Api.get('/api/merchant/page?' + params.toString(), false);
      document.getElementById('listSub').textContent = `共 ${data.total} 家`;
      if (!data.records.length) { box.innerHTML = UI.empty('没有符合条件的商家'); }
      else {
        box.innerHTML = data.records.map(m => this.card(m, false)).join('');
        this.bindCards(box);
      }
      UI.pager(document.getElementById('pager'), data.page, data.pages, p => {
        this.state.page = p;
        this.loadMerchants();
        document.getElementById('list').scrollIntoView({ behavior: 'smooth' });
      });
    } catch (e) {
      box.innerHTML = UI.empty(e.message);
    }
  },

  card(m, showReason) {
    const reason = showReason && m.recommendReason
      ? `<div class="m-reason">${UI.esc(m.recommendReason)}</div>` : '';
    return `
      <a class="m-card" href="/merchant.html?id=${m.id}">
        ${UI.cover(m.coverImage, m.name)}
        <div class="m-body">
          <div class="m-title">
            <h3>${UI.esc(m.name)}</h3>
            ${m.favorite ? '<span class="chip brand">已收藏</span>' : ''}
          </div>
          <div class="m-meta">
            ${UI.rating(m.score)}
            <span>·</span><span>${UI.esc(m.category || '')}</span>
            <span>·</span><span>${UI.esc(m.area || '')}</span>
          </div>
          <div class="m-meta">
            <span>${UI.money(m.avgPrice)}/人</span>
            <span>·</span><span>收藏 ${m.favoriteCount || 0}</span>
            <span>·</span><span>评价 ${m.reviewCount || 0}</span>
          </div>
          ${reason}
        </div>
      </a>`;
  },

  bindCards(container) {
    container.querySelectorAll('.m-card').forEach(card => {
      card.onclick = e => {
        if (e.target.closest('button')) return;
        location.href = card.getAttribute('href');
      };
    });
  }
};

document.addEventListener('DOMContentLoaded', () => Home.init());
