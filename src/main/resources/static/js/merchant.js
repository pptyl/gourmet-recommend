/* 商家详情：菜品 / 评价 / 优惠券 / 相似推荐 */
const MerchantPage = {
  id: null,
  merchant: null,
  reviewPage: 1,
  reviewImages: [],
  score: 5,

  async init() {
    Nav.render('');
    this.id = qs('id');
    if (!this.id) { this.error('缺少商家 ID'); return; }
    try {
      await this.load();
    } catch (e) {
      this.error(e.message);
    }
  },

  error(msg) {
    document.getElementById('main').innerHTML =
      `<div class="card pad" style="margin-top:22px">${UI.empty(msg)}<div class="center"><a class="btn btn-primary" href="/index.html">返回首页</a></div></div>`;
  },

  async load() {
    const [m, dishes, coupons, similar] = await Promise.all([
      Api.get('/api/merchant/' + this.id + '/detail'),
      Api.get('/api/merchant/' + this.id + '/dishes', false),
      Api.get('/api/coupon/merchant/' + this.id, false),
      Api.get('/api/recommend/similar/' + this.id + '?limit=4', false).catch(() => [])
    ]);
    this.merchant = m;
    document.title = m.name + ' · 寻味';
    document.getElementById('main').innerHTML = this.tpl(m, dishes, coupons, similar);
    this.bind(m);
    this.loadReviews(1);
  },

  tpl(m, dishes, coupons, similar) {
    const album = (m.album || '').split(',').filter(Boolean);
    const albumHtml = album.length
      ? album.map(a => `<img src="${UI.esc(a)}" alt="">`).join('')
      : '<div class="ph"></div><div class="ph"></div><div class="ph"></div>';

    const couponHtml = coupons.length ? coupons.map(c => `
      <div class="coupon-card">
        <div>
          <div class="coupon-amount">${UI.money(c.discountAmount)}<small> 减</small></div>
          <div class="small muted">满 ${Number(c.thresholdAmount)} 可用</div>
        </div>
        <div style="flex:1">
          <div style="font-weight:700">${UI.esc(c.title)}</div>
          <div class="small muted">剩余 ${c.remainStock}/${c.totalStock} 张 · ${UI.esc(c.description || '')}</div>
          <div class="small muted">有效期至 ${UI.date(c.validEndTime)}</div>
        </div>
        <div class="center">
          ${UI.couponStatus(c)}
          <div style="margin-top:8px">
            <button class="btn btn-primary btn-sm" data-seckill="${c.id}"
              ${c.seckillStatus === 1 ? '' : 'disabled'}>
              ${c.seckillStatus === 1 ? '立即领取' : (c.seckillStatus === 3 ? '已抢光' : '暂不可领')}
            </button>
          </div>
        </div>
      </div>`).join('') : '<div class="muted small">该店铺暂时没有可领取的优惠券</div>';

    const dishHtml = dishes.length ? dishes.map(d => `
      <div class="dish-card">
        <div class="dish-thumb">${d.image ? `<img src="${UI.esc(d.image)}" alt="">` : '🍲'}</div>
        <div style="flex:1;min-width:0">
          <div class="row"><strong>${UI.esc(d.name)}</strong><span class="spacer"></span><span class="price">${UI.money(d.price)}</span></div>
          <div class="small muted" style="margin-top:4px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden">${UI.esc(d.intro || '暂无介绍')}</div>
          <div class="small muted" style="margin-top:6px">已售 ${d.sales || 0}</div>
        </div>
      </div>`).join('') : UI.empty('还没有上架菜品');

    const similarHtml = (similar || []).length ? similar.map(x => `
      <a class="m-card" href="/merchant.html?id=${x.id}">
        ${UI.cover(x.coverImage, x.name)}
        <div class="m-body">
          <div class="m-title"><h3>${UI.esc(x.name)}</h3></div>
          <div class="m-meta">${UI.rating(x.score)}<span>·</span><span>${UI.esc(x.category || '')}</span></div>
          <div class="m-reason">${UI.esc(x.recommendReason || '相似商家')}</div>
        </div>
      </a>`).join('') : UI.empty('暂无相似商家');

    return `
      <div class="detail-cover" style="margin-top:22px">
        ${m.coverImage ? `<img src="${UI.esc(m.coverImage)}" alt="">` : UI.esc((m.name || '食').slice(0, 1))}
      </div>
      <div class="detail-head">
        <div class="detail-title">
          <div class="row wrap" style="gap:10px">
            <h1>${UI.esc(m.name)}</h1>
            <span class="chip brand">${UI.esc(m.category || '美食')}</span>
            <span class="chip">${UI.esc(m.area || '')}</span>
            ${m.status === 0 ? '<span class="chip danger">停业整顿</span>' : '<span class="chip ok">营业中</span>'}
          </div>
          <div class="row wrap" style="margin-top:10px;gap:14px">
            ${UI.rating(m.score)}
            <span class="muted small">${m.scoreCount || 0} 人评分 · ${m.reviewCount || 0} 条评价 · 收藏 ${m.favoriteCount || 0}</span>
            <span class="price">${UI.money(m.avgPrice)}<small>/人</small></span>
          </div>
          <div class="m-meta" style="margin-top:10px">
            <span>📍 ${UI.esc(m.address || '')}</span>
            <span>🕒 ${UI.esc(m.businessHours || '营业时间未填写')}</span>
            <span>📞 ${UI.esc(m.phone || '')}</span>
          </div>
          <p class="muted" style="margin-top:12px;line-height:1.7">${UI.esc(m.intro || '这家店还没有填写简介。')}</p>
          <div class="album">${albumHtml}</div>
        </div>
        <div style="width:220px;flex:none">
          <div class="card pad">
            <div class="row" style="margin-bottom:10px">
              <button class="btn ${m.favorite ? 'btn-soft' : 'btn-primary'} btn-block" id="btnFav">
                ${m.favorite ? '已收藏' : '收藏店铺'}
              </button>
            </div>
            <button class="btn btn-ghost btn-block" id="btnWish" style="margin-bottom:10px">加入想去清单</button>
            <a class="btn btn-ghost btn-block" href="#dishes">查看菜品</a>
            <div class="small muted" style="margin-top:12px">浏览 ${m.viewCount || 0} 次</div>
          </div>
        </div>
      </div>

      <section class="section">
        <div class="section-head"><h2>优惠券</h2><span class="sub">限时抢购 · 每人限领一张</span></div>
        <div class="grid two">${couponHtml}</div>
      </section>

      <section class="section" id="dishes">
        <div class="section-head"><h2>招牌菜品</h2><span class="sub">共 ${dishes.length} 道在售</span></div>
        <div class="grid two">${dishHtml}</div>
      </section>

      <section class="section">
        <div class="section-head">
          <h2>用户评价</h2>
          <span class="sub">仅展示审核通过的图文评价</span>
          <div class="spacer"></div>
          <button class="btn btn-primary btn-sm" id="btnReview">写评价</button>
        </div>
        <div class="card pad" id="reviewList"></div>
        <div class="pager" id="reviewPager"></div>
      </section>

      <section class="section">
        <div class="section-head"><h2>相似商家</h2><span class="sub">同菜系 · 人均相近</span></div>
        <div class="grid four">${similarHtml}</div>
      </section>`;
  },

  bind(m) {
    const fav = document.getElementById('btnFav');
    fav.onclick = async () => {
      if (!Auth.require('USER')) return;
      try {
        if (m.favorite) { await Api.del('/api/favorite/' + m.id); m.favorite = false; }
        else { await Api.post('/api/favorite/' + m.id); m.favorite = true; }
        fav.textContent = m.favorite ? '已收藏' : '收藏店铺';
        fav.className = 'btn ' + (m.favorite ? 'btn-soft' : 'btn-primary') + ' btn-block';
        UI.toast(m.favorite ? '已收藏' : '已取消收藏');
      } catch (e) { UI.toast(e.message, 'err'); }
    };

    document.getElementById('btnWish').onclick = () => {
      if (!Auth.require('USER')) return;
      UI.modal('加入想去清单', `
        <div class="field"><label>备注</label><input class="input" id="wishRemark" placeholder="例如：周末约朋友去吃"></div>
        <div class="field"><label>计划打卡日期</label><input class="input" type="date" id="wishDate"></div>
        <button class="btn btn-primary btn-block" id="wishOk">确定加入</button>`, 420);
      const btn = document.getElementById('wishOk');
      if (btn) btn.onclick = async () => {
        try {
          await Api.post('/api/wishlist', {
            merchantId: Number(this.id),
            remark: document.getElementById('wishRemark').value,
            planDate: document.getElementById('wishDate').value || null
          });
          UI.toast('已加入想去清单');
          document.querySelector('.overlay')?.remove();
        } catch (e) { UI.toast(e.message, 'err'); }
      };
    };

    document.querySelectorAll('[data-seckill]').forEach(btn => {
      btn.onclick = async () => {
        if (!Auth.require('USER')) return;
        btn.disabled = true;
        try {
          const res = await Api.post('/api/coupon/' + btn.dataset.seckill + '/seckill');
          UI.toast(res.message || '抢券成功');
          btn.textContent = res.queued ? '处理中…' : '已领取';
          const c = await Api.get('/api/coupon/merchant/' + this.id, false);
          document.querySelectorAll('.grid.two')[0].innerHTML = c.map(x => this.couponTpl(x)).join('');
          this.bindCoupons();
          if (res.queued) {
            setTimeout(() => this.load(), 1200);
          }
        } catch (e) { UI.toast(e.message, 'err'); btn.disabled = false; }
      };
    });

    document.getElementById('btnReview').onclick = () => this.reviewModal();
  },

  couponTpl(c) {
    return `
      <div class="coupon-card">
        <div>
          <div class="coupon-amount">${UI.money(c.discountAmount)}<small> 减</small></div>
          <div class="small muted">满 ${Number(c.thresholdAmount)} 可用</div>
        </div>
        <div style="flex:1">
          <div style="font-weight:700">${UI.esc(c.title)}</div>
          <div class="small muted">剩余 ${c.remainStock}/${c.totalStock} 张</div>
          <div class="small muted">有效期至 ${UI.date(c.validEndTime)}</div>
        </div>
        <div class="center">${UI.couponStatus(c)}
          <div style="margin-top:8px"><button class="btn btn-primary btn-sm" data-seckill="${c.id}"
            ${c.seckillStatus === 1 ? '' : 'disabled'}>${c.seckillStatus === 1 ? '立即领取' : '暂不可领'}</button></div>
        </div>
      </div>`;
  },

  bindCoupons() {
    document.querySelectorAll('[data-seckill]').forEach(btn => {
      btn.onclick = async () => {
        if (!Auth.require('USER')) return;
        btn.disabled = true;
        try {
          const res = await Api.post('/api/coupon/' + btn.dataset.seckill + '/seckill');
          UI.toast(res.message || '抢券成功');
          btn.textContent = res.queued ? '处理中…' : '已领取';
          if (res.queued) {
            setTimeout(() => this.load(), 1200);
          } else {
            await this.load();
          }
        } catch (e) { UI.toast(e.message, 'err'); btn.disabled = false; }
      };
    });
  },

  reviewModal() {
    if (!Auth.require('USER')) return;
    this.score = 5;
    UI.modal('写评价', `
      <div class="field">
        <label>评分</label>
        <div class="stars-input" id="starPick">${[1, 2, 3, 4, 5].map(i => `<span data-v="${i}">★</span>`).join('')}</div>
      </div>
      <div class="field"><label>评价内容 <span class="req">*</span></label>
        <textarea class="textarea" id="rvContent" placeholder="说说菜品、环境和服务体验…"></textarea></div>
      <div class="field"><label>上传实拍图（可选）</label>
        <input class="input" type="file" id="rvImg" accept="image/*"></div>
      <button class="btn btn-primary btn-block" id="rvOk">提交评价</button>
      <p class="small muted center" style="margin-top:10px">评价需平台审核通过后公开展示</p>`, 480);

    const pick = document.getElementById('starPick');
    const paint = () => pick.querySelectorAll('span').forEach(s => {
      s.style.color = Number(s.dataset.v) <= this.score ? '#f79009' : '#d6d3d1';
    });
    paint();
    pick.querySelectorAll('span').forEach(s => s.onclick = () => { this.score = Number(s.dataset.v); paint(); });

    document.getElementById('rvOk').onclick = async () => {
      const content = document.getElementById('rvContent').value.trim();
      if (!content) { UI.toast('请填写评价内容', 'warn'); return; }
      const images = [];
      const file = document.getElementById('rvImg').files[0];
      try {
        if (file) { const up = await Api.upload(file, 'review'); images.push(up.url); }
        await Api.post('/api/review', {
          merchantId: Number(this.id), score: this.score, content, images
        });
        UI.toast('评价已提交，等待审核');
        document.querySelector('.overlay')?.remove();
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  },

  async loadReviews(page) {
    this.reviewPage = page;
    const box = document.getElementById('reviewList');
    box.innerHTML = '<div class="skeleton" style="height:80px;margin-bottom:10px"></div>'.repeat(2);
    try {
      const data = await Api.get(`/api/review/merchant/${this.id}?page=${page}&size=5`);
      if (!data.records.length) {
        box.innerHTML = UI.empty('还没有评价，来当第一个探店的人吧');
      } else {
        box.innerHTML = data.records.map(r => `
          <div class="review-item">
            <div class="avatar lg">${UI.esc((r.userName || '客').slice(0, 1))}</div>
            <div style="flex:1;min-width:0">
              <div class="row">
                <strong>${UI.esc(r.userName || '匿名用户')}</strong>
                ${UI.rating(r.score)}
                <span class="spacer"></span>
                <button class="like-btn ${r.liked ? 'on' : ''}" data-review-like="${r.id}" title="点赞">
                  ♥ <span>${r.likeCount || 0}</span>
                </button>
                <span class="small muted">${UI.date(r.createTime)}</span>
              </div>
              <div style="margin-top:6px;line-height:1.7">${UI.esc(r.content || '')}</div>
              <div class="review-imgs">
                ${(r.imageList || []).map(u => `<img src="${UI.esc(u)}" alt="">`).join('')}
              </div>
            </div>
          </div>`).join('');
        this.bindReviewLikes(box);
      }
      UI.pager(document.getElementById('reviewPager'), data.page, data.pages, p => this.loadReviews(p));
    } catch (e) {
      box.innerHTML = UI.empty(e.message);
    }
  },

  bindReviewLikes(scope) {
    if (!scope) return;
    scope.querySelectorAll('[data-review-like]').forEach(btn => {
      btn.onclick = async (e) => {
        e.stopPropagation();
        if (!Auth.isLogin()) {
          location.href = '/login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
          return;
        }
        if (Auth.role() !== 'USER') { UI.toast('只有普通用户可以点赞', 'warn'); return; }
        btn.disabled = true;
        try {
          const liked = btn.classList.contains('on');
          const data = liked
            ? await Api.del('/api/review/' + btn.dataset.reviewLike + '/like')
            : await Api.post('/api/review/' + btn.dataset.reviewLike + '/like');
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
  }
};

document.addEventListener('DOMContentLoaded', () => MerchantPage.init());
