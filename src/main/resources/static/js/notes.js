/* 探店笔记：公开列表 + 发布 */
const NotesPage = {
  page: 1,
  merchants: [],

  init() {
    Nav.render('notes');
    document.getElementById('btnWrite').onclick = () => this.writeModal();
    this.load(1);
    this.loadMerchants();
  },

  async loadMerchants() {
    try {
      const data = await Api.get('/api/merchant/page?page=1&size=100', false);
      this.merchants = data.records;
    } catch (e) { /* ignore */ }
  },

  async load(page) {
    this.page = page;
    const box = document.getElementById('list');
    box.innerHTML = '<div class="skeleton" style="height:220px"></div>'.repeat(4);
    try {
      // 带 token 请求，公开接口也会返回当前用户的点赞状态
      const data = await Api.get(`/api/note/page?page=${page}&size=8`);
      if (!data.records.length) {
        box.innerHTML = UI.empty('还没有公开的探店笔记');
      } else {
        box.innerHTML = data.records.map(n => {
          const cover = n.coverImage || (n.imageList || [])[0];
          return `
            <article class="card" style="overflow:hidden;cursor:pointer" data-id="${n.id}">
              <div style="height:170px;background:linear-gradient(135deg,#ffe8cc,#ffc078);display:grid;place-items:center;font-size:40px;color:#fff">
                ${cover ? `<img src="${UI.esc(cover)}" style="width:100%;height:100%;object-fit:cover" alt="">` : '📝'}
              </div>
              <div class="pad">
                <div class="row">
                  <span class="chip brand">${UI.esc(n.merchantName || '探店')}</span>
                  <span class="spacer"></span>
                  <span class="small muted">${UI.date(n.createTime)}</span>
                </div>
                <h3 style="margin:10px 0 6px;font-size:17px">${UI.esc(n.title)}</h3>
                <p class="small muted" style="line-height:1.7;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden">${UI.esc(n.content || '')}</p>
                <div class="row small muted" style="margin-top:12px">
                  <span class="avatar">${UI.esc((n.userName || '客').slice(0, 1))}</span>
                  <span>${UI.esc(n.userName || '匿名用户')}</span>
                  <span class="spacer"></span>
                  <span>👀 ${n.viewCount || 0}</span>
                  <button class="like-btn ${n.liked ? 'on' : ''}" data-like="${n.id}" title="点赞">
                    ♥ <span>${n.likeCount || 0}</span>
                  </button>
                </div>
              </div>
            </article>`;
        }).join('');
        box.querySelectorAll('[data-id]').forEach(card => {
          card.onclick = () => this.openDetail(card.dataset.id);
        });
        this.bindLikes(box);
      }
      UI.pager(document.getElementById('pager'), data.page, data.pages, p => this.load(p));
    } catch (e) {
      box.innerHTML = UI.empty(e.message);
    }
  },

  async openDetail(id) {
    try {
      const n = await Api.get('/api/note/detail/' + id);
      UI.modal(n.title, `
        <div class="row small muted" style="margin-bottom:12px">
          <span class="avatar">${UI.esc((n.userName || '客').slice(0, 1))}</span>
          <span>${UI.esc(n.userName || '匿名用户')}</span>
          <span>·</span><span>${UI.esc(n.merchantName || '')}</span>
          <span class="spacer"></span><span>${UI.date(n.createTime)}</span>
        </div>
        <div style="line-height:1.9;white-space:pre-wrap">${UI.esc(n.content || '')}</div>
        <div class="review-imgs" style="margin-top:14px">
          ${(n.imageList || []).map(u => `<img src="${UI.esc(u)}" style="width:120px;height:120px" alt="">`).join('')}
        </div>
        <div class="row small muted" style="margin-top:14px">
          <span>浏览 ${n.viewCount || 0}</span>
          <span class="spacer"></span>
          <button class="like-btn ${n.liked ? 'on' : ''}" data-like="${n.id}" title="点赞">
            ♥ <span>${n.likeCount || 0}</span>
          </button>
        </div>`, 640);
      this.bindLikes(document.querySelector('.overlay'));
    } catch (e) { UI.toast(e.message, 'err'); }
  },

  bindLikes(scope) {
    if (!scope) return;
    scope.querySelectorAll('[data-like]').forEach(btn => {
      btn.onclick = async (e) => {
        e.stopPropagation();
        e.preventDefault();
        if (!Auth.isLogin()) {
          location.href = '/login.html?redirect=' + encodeURIComponent('/notes.html');
          return;
        }
        if (Auth.role() !== 'USER') { UI.toast('只有普通用户可以点赞', 'warn'); return; }
        btn.disabled = true;
        try {
          const liked = btn.classList.contains('on');
          const data = liked
            ? await Api.del('/api/note/' + btn.dataset.like + '/like')
            : await Api.post('/api/note/' + btn.dataset.like + '/like');
          btn.classList.toggle('on', data.liked);
          const span = btn.querySelector('span');
          if (span) span.textContent = data.count;
        } catch (err) {
          UI.toast(err.message, 'err');
        } finally {
          btn.disabled = false;
        }
      };
    });
  },

  writeModal() {
    if (!Auth.isLogin()) { location.href = '/login.html?redirect=' + encodeURIComponent('/notes.html'); return; }
    if (Auth.role() !== 'USER') { UI.toast('只有普通用户可以发布探店笔记', 'warn'); return; }
    const options = this.merchants.map(m => `<option value="${m.id}">${UI.esc(m.name)}（${UI.esc(m.category || '')}）</option>`).join('');
    UI.modal('写探店笔记', `
      <div class="field"><label>绑定商家</label>
        <select class="select" id="nMerchant"><option value="">不绑定商家</option>${options}</select></div>
      <div class="field"><label>标题 <span class="req">*</span></label><input class="input" id="nTitle" placeholder="例如：春熙路这家川菜值得排队"></div>
      <div class="field"><label>正文 <span class="req">*</span></label><textarea class="textarea" id="nContent" style="min-height:140px" placeholder="菜品、环境、价格、服务…"></textarea></div>
      <div class="field"><label>上传图片（可选，最多 1 张）</label><input class="input" type="file" id="nImg" accept="image/*"></div>
      <button class="btn btn-primary btn-block" id="nSubmit">提交（审核通过后公开）</button>`, 560);

    document.getElementById('nSubmit').onclick = async () => {
      const title = document.getElementById('nTitle').value.trim();
      const content = document.getElementById('nContent').value.trim();
      if (!title || !content) return UI.toast('标题和正文不能为空', 'warn');
      const images = [];
      const file = document.getElementById('nImg').files[0];
      try {
        if (file) { const up = await Api.upload(file, 'note'); images.push(up.url); }
        await Api.post('/api/note', {
          merchantId: document.getElementById('nMerchant').value ? Number(document.getElementById('nMerchant').value) : null,
          title, content, images, coverImage: images[0] || null
        });
        UI.toast('笔记已提交，等待平台审核');
        document.querySelector('.overlay')?.remove();
        this.load(1);
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  }
};

document.addEventListener('DOMContentLoaded', () => NotesPage.init());
