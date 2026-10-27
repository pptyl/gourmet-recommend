/* 领券中心：全平台优惠券集中领取 */
const CouponCenter = {
  page: 1,
  state: '',

  init() {
    Nav.render('coupons');
    document.querySelectorAll('#stateTabs button').forEach(btn => {
      btn.onclick = () => {
        document.querySelectorAll('#stateTabs button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.state = btn.dataset.state;
        this.load(1);
      };
    });
    this.load(1);
  },

  async load(page) {
    this.page = page;
    const box = document.getElementById('list');
    box.innerHTML = '<div class="skeleton" style="height:150px"></div>'.repeat(4);
    const params = new URLSearchParams({ page, size: 9 });
    if (this.state !== '') params.set('state', this.state);
    try {
      const data = await Api.get('/api/coupon/center?' + params.toString());
      if (!data.records.length) {
        box.innerHTML = UI.empty(this.state === '0' ? '暂时没有即将开始的券'
          : this.state === '2' ? '暂时没有已结束的券' : '暂时没有可领取的优惠券');
      } else {
        box.innerHTML = data.records.map(c => this.card(c)).join('');
        this.bind(box);
      }
      UI.pager(document.getElementById('pager'), data.page, data.pages, p => this.load(p));
    } catch (e) {
      box.innerHTML = UI.empty(e.message);
    }
  },

  card(c) {
    const disabled = c.seckillStatus !== 1 || c.received;
    const btnText = c.received ? '已领取'
      : c.seckillStatus === 1 ? '立即领取'
        : c.seckillStatus === 0 ? '即将开始'
          : c.seckillStatus === 3 ? '已抢光' : '已结束';
    const tip = c.seckillStatus === 0 && c.countdownSeconds
      ? `距开抢 ${this.fmt(c.countdownSeconds)}`
      : `有效期至 ${UI.date(c.validEndTime)}`;
    return `
      <div class="coupon-card coupon-big">
        <div class="coupon-side">
          <div class="coupon-amount">${UI.money(c.discountAmount)}</div>
          <div class="small muted">满 ${Number(c.thresholdAmount)} 可用</div>
        </div>
        <div class="coupon-main">
          <div class="row">
            <strong>${UI.esc(c.title)}</strong>
            ${UI.couponStatus(c)}
            ${c.received ? '<span class="chip ok">已领取</span>' : ''}
          </div>
          <a class="small" style="color:var(--brand)" href="/merchant.html?id=${c.merchantId}">
            ${UI.esc(c.merchantName || '商家')} →
          </a>
          <div class="small muted" style="margin-top:6px">${UI.esc(c.description || '')}</div>
          <div class="small muted">剩余 ${c.remainStock}/${c.totalStock} 张 · ${tip}</div>
        </div>
        <div class="coupon-action">
          <button class="btn ${disabled ? 'btn-ghost' : 'btn-primary'}" data-claim="${c.id}" ${disabled ? 'disabled' : ''}>
            ${btnText}
          </button>
        </div>
      </div>`;
  },

  bind(scope) {
    scope.querySelectorAll('[data-claim]').forEach(btn => {
      btn.onclick = async () => {
        if (!Auth.isLogin()) {
          location.href = '/login.html?redirect=' + encodeURIComponent('/coupons.html');
          return;
        }
        if (Auth.role() !== 'USER') { UI.toast('只有普通用户可以领取优惠券', 'warn'); return; }
        btn.disabled = true;
        try {
          const res = await Api.post('/api/coupon/' + btn.dataset.claim + '/seckill');
          btn.textContent = res.queued ? '处理中…' : '已领取';
          btn.className = 'btn btn-ghost';
          UI.toast(res.message || '抢券成功，已放入我的券包');
          if (res.queued) {
            // 异步落库：稍后刷新一次，券包即可看到
            setTimeout(() => this.load(this.page), 1200);
          }
        } catch (e) {
          UI.toast(e.message, 'err');
          btn.disabled = false;
        }
      };
    });
  },

  fmt(sec) {
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    return h > 0 ? `${h} 小时 ${m} 分` : `${Math.max(1, m)} 分钟`;
  }
};

document.addEventListener('DOMContentLoaded', () => CouponCenter.init());
