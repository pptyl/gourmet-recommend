/* 商家后台：店铺资料 / 菜品 / 优惠券 / 评价 / 消息 */
const Console = {
  shop: null,

  async init() {
    if (!Auth.require('MERCHANT')) return;
    Nav.render('console');
    document.querySelectorAll('#side button').forEach(btn => {
      btn.onclick = () => {
        document.querySelectorAll('#side button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.show(btn.dataset.tab);
      };
    });
    await this.loadHead();
    this.show('shop');
  },

  async loadHead() {
    try {
      this.shop = await Api.get('/api/merchant/profile');
      document.getElementById('shopName').textContent = this.shop.name;
      document.getElementById('shopMeta').textContent =
        `${this.shop.category || ''} · ${this.shop.area || ''} · 审核${this.shop.auditStatus === 1 ? '已通过' : '未通过'}`;
      const s = await Api.get('/api/statistic/merchant');
      document.getElementById('stats').innerHTML = `
        <div class="stat brand"><div class="k">综合评分</div><div class="v">${Number(s.score || 0).toFixed(1)}</div></div>
        <div class="stat"><div class="k">评价数</div><div class="v">${s.reviewCount}</div></div>
        <div class="stat"><div class="k">收藏量</div><div class="v">${s.favoriteCount}</div></div>
        <div class="stat"><div class="k">浏览量</div><div class="v">${s.viewCount}</div></div>
        <div class="stat"><div class="k">菜品总数</div><div class="v">${s.dishCount}</div></div>
        <div class="stat"><div class="k">领券总数</div><div class="v">${s.couponClaimCount}</div></div>`;
    } catch (e) { UI.toast(e.message, 'err'); }
  },

  async show(tab) {
    const panel = document.getElementById('panel');
    if (window.innerWidth <= 860) {
      setTimeout(() => panel.scrollIntoView({ behavior: 'smooth', block: 'start' }), 80);
    }
    panel.innerHTML = '<div class="skeleton" style="height:120px"></div>';
    try {
      if (tab === 'shop') return this.renderShop();
      if (tab === 'dishes') return this.renderDishes();
      if (tab === 'coupons') return this.renderCoupons();
      if (tab === 'reviews') return this.renderReviews();
      if (tab === 'messages') return this.renderMessages();
    } catch (e) {
      panel.innerHTML = UI.empty(e.message);
      UI.toast(e.message, 'err');
    }
  },

  renderShop() {
    const s = this.shop;
    const panel = document.getElementById('panel');
    panel.innerHTML = `
      <h3 style="margin-bottom:14px">店铺资料</h3>
      <div class="form-grid">
        <div class="field"><label>店铺名称 <span class="req">*</span></label>
          <input class="input" id="sName" maxlength="100" value="${UI.esc(s.name)}"><div class="err"></div></div>
        <div class="field"><label>菜系 <span class="req">*</span></label>
          ${UI.cuisineSelect(s.category || '', 'sCategory')}<div class="err"></div></div>
        <div class="field"><label>地区 <span class="req">*</span></label>
          <input class="input" id="sArea" list="areaList" maxlength="20" value="${UI.esc(s.area || '')}"><div class="err"></div></div>
        <div class="field"><label>营业时间</label>
          <input class="input" id="sHours" placeholder="10:00-22:00" value="${UI.esc(s.businessHours || '')}"><div class="err"></div></div>
        <div class="field full"><label>详细地址 <span class="req">*</span></label>
          <input class="input" id="sAddress" maxlength="255" value="${UI.esc(s.address || '')}"><div class="err"></div></div>
        <div class="field"><label>人均消费</label>
          <input class="input" type="number" id="sPrice" min="0" value="${s.avgPrice || 0}"><div class="err"></div></div>
        <div class="field"><label>封面图</label><input class="input" type="file" id="sCoverFile" accept="image/*">
          <div class="small muted" style="margin-top:6px">${UI.esc(s.coverImage || '暂无封面')}</div></div>
        <div class="field full"><label>相册（图片地址，逗号分隔）</label>
          <input class="input" id="sAlbum" value="${UI.esc(s.album || '')}"></div>
        <div class="field full"><label>店铺简介</label><textarea class="textarea" id="sIntro">${UI.esc(s.intro || '')}</textarea></div>
      </div>
      <datalist id="areaList">
        <option value="成都锦江区"></option><option value="北京朝阳区"></option>
        <option value="上海静安区"></option><option value="广州天河区"></option>
        <option value="杭州西湖区"></option><option value="长沙岳麓区"></option>
      </datalist>
      <button class="btn btn-primary" id="saveShop">保存店铺资料</button>`;

    let cover = s.coverImage || '';
    document.getElementById('sCoverFile').onchange = async (e) => {
      const f = e.target.files[0];
      if (!f) return;
      try { const up = await Api.upload(f, 'merchant'); cover = up.url; UI.toast('封面上传成功，保存后生效'); }
      catch (err) { UI.toast(err.message, 'err'); }
    };
    document.getElementById('saveShop').onclick = async () => {
      const name = document.getElementById('sName');
      const category = document.getElementById('sCategory');
      const area = document.getElementById('sArea');
      const hours = document.getElementById('sHours');
      const address = document.getElementById('sAddress');
      const price = document.getElementById('sPrice');
      const ok = UI.validAll(check => {
        check(name, name.value.trim().length >= 2, '店铺名称至少 2 个字');
        check(category, !!category.value, '请选择菜系');
        check(area, /^[\u4e00-\u9fa5A-Za-z·]{2,20}$/.test(area.value.trim()), '地区填中文名称，2-20 个字，不能是数字');
        check(address, address.value.trim().length >= 5 && /[\u4e00-\u9fa5]/.test(address.value), '地址至少 5 个字，并包含中文门牌');
        check(hours, !hours.value.trim() || /^([01]?\d|2[0-3]):[0-5]\d\s*[-~至]\s*([01]?\d|2[0-3]):[0-5]\d$/.test(hours.value.trim()), '营业时间格式如 10:00-22:00');
        check(price, price.value === '' || Number(price.value) >= 0, '人均消费不能为负');
      });
      if (!ok) { UI.toast('请检查标红的必填项', 'warn'); return; }
      try {
        await Api.put('/api/merchant/profile', {
          name: name.value.trim(),
          category: category.value,
          area: area.value.trim(),
          address: address.value.trim(),
          businessHours: hours.value.trim() || null,
          avgPrice: Number(price.value || 0),
          intro: document.getElementById('sIntro').value.trim(),
          coverImage: cover,
          album: document.getElementById('sAlbum').value.split(',').map(x => x.trim()).filter(Boolean)
        });
        UI.toast('店铺资料已保存');
        await this.loadHead();
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  },

  async renderDishes() {
    const data = await Api.get('/api/dish/mine?page=1&size=100');
    const panel = document.getElementById('panel');
    panel.innerHTML = `
      <div class="row" style="margin-bottom:12px">
        <h3>菜品管理</h3><span class="small muted">${data.total} 道</span>
        <span class="spacer"></span>
        <button class="btn btn-primary btn-sm" id="addDish">新增菜品</button>
      </div>
      ${data.records.length ? `
      <table class="table">
        <thead><tr><th>菜品</th><th>价格</th><th>状态</th><th>销量</th><th style="width:200px">操作</th></tr></thead>
        <tbody>
        ${data.records.map(d => `
          <tr>
            <td><div class="row">${UI.thumb(d.image, d.name)}<span>${UI.esc(d.name)}</span></div></td>
            <td class="price">${UI.money(d.price)}</td>
            <td>${d.status === 1 ? '<span class="chip ok">在售</span>' : '<span class="chip">已下架</span>'}</td>
            <td>${d.sales || 0}</td>
            <td>
              <button class="btn btn-ghost btn-sm" data-edit="${d.id}">编辑</button>
              <button class="btn btn-ghost btn-sm" data-toggle="${d.id}" data-status="${d.status}">${d.status === 1 ? '下架' : '上架'}</button>
              <button class="btn btn-danger btn-sm" data-del="${d.id}">删除</button>
            </td>
          </tr>`).join('')}
        </tbody>
      </table>` : UI.empty('还没有菜品，先添加一道招牌菜吧')}`;

    document.getElementById('addDish').onclick = () => this.dishModal();
    panel.querySelectorAll('[data-edit]').forEach(btn => {
      btn.onclick = () => this.dishModal(data.records.find(d => d.id === Number(btn.dataset.edit)));
    });
    panel.querySelectorAll('[data-toggle]').forEach(btn => {
      btn.onclick = async () => {
        const next = Number(btn.dataset.status) === 1 ? 0 : 1;
        try { await Api.put(`/api/dish/${btn.dataset.toggle}/status?status=${next}`); UI.toast(next ? '已上架' : '已下架'); this.renderDishes(); }
        catch (e) { UI.toast(e.message, 'err'); }
      };
    });
    panel.querySelectorAll('[data-del]').forEach(btn => {
      btn.onclick = async () => {
        if (!await UI.confirm('确定删除这道菜吗？')) return;
        try { await Api.del('/api/dish/' + btn.dataset.del); UI.toast('已删除'); this.renderDishes(); this.loadHead(); }
        catch (e) { UI.toast(e.message, 'err'); }
      };
    });
  },

  dishModal(dish) {
    const edit = !!dish;
    UI.modal(edit ? '编辑菜品' : '新增菜品', `
      <div class="field"><label>菜名 <span class="req">*</span></label><input class="input" id="dName" maxlength="100" value="${edit ? UI.esc(dish.name) : ''}"><div class="err"></div></div>
      <div class="form-grid">
        <div class="field"><label>价格 <span class="req">*</span></label><input class="input" type="number" id="dPrice" min="0.01" step="0.01" value="${edit ? dish.price : ''}"><div class="err"></div></div>
        <div class="field"><label>图片</label><input class="input" type="file" id="dImgFile" accept="image/*"></div>
        <div class="field full"><label>介绍</label><textarea class="textarea" id="dIntro" maxlength="1000">${edit ? UI.esc(dish.intro || '') : ''}</textarea></div>
      </div>
      <button class="btn btn-primary btn-block" id="dSave">保存</button>`, 480);

    let image = edit ? dish.image : '';
    document.getElementById('dImgFile').onchange = async e => {
      const f = e.target.files[0];
      if (!f) return;
      try { const up = await Api.upload(f, 'dish'); image = up.url; UI.toast('图片已上传'); }
      catch (err) { UI.toast(err.message, 'err'); }
    };
    document.getElementById('dSave').onclick = async () => {
      const name = document.getElementById('dName');
      const price = document.getElementById('dPrice');
      const ok = UI.validAll(check => {
        check(name, name.value.trim().length >= 2, '菜名至少 2 个字');
        check(price, Number(price.value) > 0, '价格必须大于 0');
      });
      if (!ok) { UI.toast('请检查标红的必填项', 'warn'); return; }
      const body = {
        name: name.value.trim(),
        price: Number(price.value),
        intro: document.getElementById('dIntro').value.trim(),
        image
      };
      try {
        if (edit) await Api.put('/api/dish/' + dish.id, body);
        else await Api.post('/api/dish', body);
        UI.toast(edit ? '菜品已更新' : '菜品已新增');
        document.querySelector('.overlay')?.remove();
        this.renderDishes();
        this.loadHead();
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  },

  async renderCoupons() {
    const data = await Api.get('/api/coupon/page?page=1&size=100');
    const panel = document.getElementById('panel');
    panel.innerHTML = `
      <div class="row" style="margin-bottom:12px">
        <h3>优惠券</h3><span class="small muted">${data.total} 个活动</span>
        <span class="spacer"></span>
        <button class="btn btn-primary btn-sm" id="addCoupon">发布优惠券</button>
      </div>
      ${data.records.length ? `
      <div class="card" style="padding:16px;margin-bottom:14px">
        <div class="row">
          <h3>核销优惠券</h3>
          <span class="small muted">输入顾客券包中的 12 位核销码，核销后该券立即失效</span>
        </div>
        <div class="row" style="margin-top:10px">
          <input class="input" id="redeemCode" placeholder="请输入核销码，例如 482913746502"
                 style="max-width:300px;font-family:ui-monospace,Consolas,monospace;letter-spacing:1px">
          <button class="btn btn-primary" id="redeemBtn">核销</button>
        </div>
        <div id="redeemResult" class="small" style="margin-top:8px"></div>
      </div>
      <table class="table">
        <thead><tr><th>券</th><th>门槛/减免</th><th>库存</th><th>抢购时间</th><th>状态</th><th style="width:230px">操作</th></tr></thead>
        <tbody>
        ${data.records.map(c => `
          <tr>
            <td><strong>${UI.esc(c.title)}</strong></td>
            <td>满 ${Number(c.thresholdAmount)} 减 ${Number(c.discountAmount)}</td>
            <td>${c.remainStock}/${c.totalStock}</td>
            <td class="small muted">${UI.date(c.grabBeginTime)}<br>~ ${UI.date(c.grabEndTime)}</td>
            <td>${c.status === 1 ? '<span class="chip ok">上架</span>' : '<span class="chip">下架</span>'}</td>
            <td>
              <button class="btn btn-ghost btn-sm" data-toggle="${c.id}" data-status="${c.status}">${c.status === 1 ? '下架' : '上架'}</button>
              <button class="btn btn-ghost btn-sm" data-records="${c.id}">领取记录</button>
              <button class="btn btn-danger btn-sm" data-del="${c.id}">删除</button>
            </td>
          </tr>`).join('')}
        </tbody>
      </table>` : UI.empty('还没有优惠券活动，先发布一张吧')}`;

    document.getElementById('addCoupon').onclick = () => this.couponModal();
    const redeemBtn = document.getElementById('redeemBtn');
    if (redeemBtn) redeemBtn.onclick = () => this.doRedeem();
    panel.querySelectorAll('[data-toggle]').forEach(btn => {
      btn.onclick = async () => {
        const next = Number(btn.dataset.status) === 1 ? 0 : 1;
        try { await Api.put(`/api/coupon/${btn.dataset.toggle}/status?status=${next}`); UI.toast(next ? '已上架' : '已下架'); this.renderCoupons(); }
        catch (e) { UI.toast(e.message, 'err'); }
      };
    });
    panel.querySelectorAll('[data-del]').forEach(btn => {
      btn.onclick = async () => {
        if (!await UI.confirm('确定删除该优惠券吗？')) return;
        try { await Api.del('/api/coupon/' + btn.dataset.del); UI.toast('已删除'); this.renderCoupons(); }
        catch (e) { UI.toast(e.message, 'err'); }
      };
    });
    panel.querySelectorAll('[data-records]').forEach(btn => {
      btn.onclick = () => this.recordsModal(btn.dataset.records);
    });
  },

  async doRedeem() {
    const input = document.getElementById('redeemCode');
    const box = document.getElementById('redeemResult');
    const code = (input.value || '').trim();
    if (!code) { UI.toast('请输入核销码', 'warn'); return; }
    const btn = document.getElementById('redeemBtn');
    btn.disabled = true;
    try {
      const r = await Api.post('/api/coupon/redeem', { code });
      box.innerHTML = `<span class="chip ok">核销成功</span>
        <span style="margin-left:8px">${UI.esc(r.couponTitle || '优惠券')} · 满 ${Number(r.thresholdAmount)} 减 ${Number(r.discountAmount)}</span>
        <span class="muted" style="margin-left:8px">顾客：${UI.esc(r.userNickname || '')} ${UI.esc(r.userPhone || '')}</span>
        <span class="muted" style="margin-left:8px">${UI.date(r.useTime)}</span>`;
      input.value = '';
      UI.toast('核销成功');
    } catch (e) {
      box.innerHTML = `<span class="chip danger">核销失败</span><span style="margin-left:8px">${UI.esc(e.message)}</span>`;
    } finally {
      btn.disabled = false;
    }
  },

  async recordsModal(id) {
    try {
      const data = await Api.get(`/api/coupon/${id}/records?page=1&size=50`);
      UI.modal('领取记录', data.records.length ? `
        <table class="table">
          <thead><tr><th>用户</th><th>手机号</th><th>状态</th><th>领取时间</th></tr></thead>
          <tbody>${data.records.map(r => `
            <tr>
              <td>${UI.esc(r.userNickname || '')}</td>
              <td>${UI.esc(r.userPhone || '')}</td>
              <td>${r.status === 0 ? '<span class="chip ok">未使用</span>' : r.status === 1 ? '<span class="chip">已使用</span>' : '<span class="chip danger">已过期</span>'}</td>
              <td class="small muted">${UI.date(r.receiveTime)}</td>
            </tr>`).join('')}</tbody>
        </table>` : UI.empty('还没有人领取'), 640);
    } catch (e) { UI.toast(e.message, 'err'); }
  },

  couponModal() {
    const now = new Date();
    const begin = new Date(now.getTime() - 3600 * 1000);
    const end = new Date(now.getTime() + 7 * 24 * 3600 * 1000);
    const fmt = d => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    UI.modal('发布优惠券', `
      <div class="field"><label>券名称 <span class="req">*</span></label><input class="input" id="cTitle" maxlength="100" placeholder="满100减30"><div class="err"></div></div>
      <div class="field"><label>使用说明</label><input class="input" id="cDesc" maxlength="500" placeholder="全场通用，不与其他优惠叠加"></div>
      <div class="form-grid">
        <div class="field"><label>满减门槛 <span class="req">*</span></label><input class="input" type="number" id="cThreshold" min="0.01" step="0.01" value="100"><div class="err"></div></div>
        <div class="field"><label>减免金额 <span class="req">*</span></label><input class="input" type="number" id="cDiscount" min="0.01" step="0.01" value="30"><div class="err"></div></div>
        <div class="field"><label>总库存 <span class="req">*</span></label><input class="input" type="number" id="cStock" min="1" step="1" value="100"><div class="err"></div></div>
        <div class="field"><label>每人限领</label><input class="input" type="number" id="cPer" min="1" max="10" step="1" value="1"></div>
        <div class="field"><label>抢购开始 <span class="req">*</span></label><input class="input" type="datetime-local" id="cGrabBegin" value="${fmt(begin)}"><div class="err"></div></div>
        <div class="field"><label>抢购结束 <span class="req">*</span></label><input class="input" type="datetime-local" id="cGrabEnd" value="${fmt(end)}"><div class="err"></div></div>
      </div>
      <button class="btn btn-primary btn-block" id="cSave">发布</button>`, 560);

    const toServer = v => v ? v.replace('T', ' ') + ':00' : null;
    document.getElementById('cSave').onclick = async () => {
      const title = document.getElementById('cTitle');
      const threshold = document.getElementById('cThreshold');
      const discount = document.getElementById('cDiscount');
      const stock = document.getElementById('cStock');
      const beginEl = document.getElementById('cGrabBegin');
      const endEl = document.getElementById('cGrabEnd');
      const t = Number(threshold.value), d = Number(discount.value), st = Number(stock.value);
      const ok = UI.validAll(check => {
        check(title, title.value.trim().length >= 2, '券名称至少 2 个字');
        check(threshold, t > 0, '门槛必须大于 0');
        check(discount, d > 0 && d <= t, '减免金额需大于 0 且不超过门槛');
        check(stock, Number.isInteger(st) && st >= 1, '库存必须是大于 0 的整数');
        check(beginEl, !!beginEl.value, '请选择抢购开始时间');
        check(endEl, !!endEl.value && new Date(endEl.value) > new Date(beginEl.value), '结束时间必须晚于开始时间');
      });
      if (!ok) { UI.toast('请检查标红的必填项', 'warn'); return; }

      const body = {
        title: title.value.trim(),
        description: document.getElementById('cDesc').value.trim(),
        thresholdAmount: t,
        discountAmount: d,
        totalStock: st,
        perLimit: Number(document.getElementById('cPer').value || 1),
        grabBeginTime: toServer(beginEl.value),
        grabEndTime: toServer(endEl.value)
      };
      try {
        await Api.post('/api/coupon', body);
        UI.toast('优惠券已发布');
        document.querySelector('.overlay')?.remove();
        this.renderCoupons();
        this.loadHead();
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  },

  async renderReviews() {
    const id = this.shop ? this.shop.id : Auth.id();
    const data = await Api.get(`/api/merchant/${id}/reviews?page=1&size=20`);
    const panel = document.getElementById('panel');
    panel.innerHTML = `<h3 style="margin-bottom:12px">用户评价 <span class="small muted">${data.total} 条（仅展示审核通过）</span></h3>` +
      (data.records.length ? data.records.map(r => `
        <div class="msg-item">
          <div class="row">
            <span class="avatar">${UI.esc((r.userName || '客').slice(0, 1))}</span>
            <strong>${UI.esc(r.userName || '匿名用户')}</strong>
            ${UI.rating(r.score)}
            <span class="spacer"></span>
            <span class="small muted">${UI.date(r.createTime)}</span>
          </div>
          <div style="margin-top:8px;line-height:1.7">${UI.esc(r.content || '')}</div>
          <div class="review-imgs">${(r.imageList || []).map(u => `<img src="${UI.esc(u)}" alt="">`).join('')}</div>
        </div>`).join('') : UI.empty('还没有评价'));
  },

  async renderMessages() {
    const data = await Api.get('/api/message/page?page=1&size=30');
    const panel = document.getElementById('panel');
    panel.innerHTML = `
      <div class="row" style="margin-bottom:12px"><h3>消息通知</h3><span class="spacer"></span>
        <button class="btn btn-ghost btn-sm" id="readAll">全部已读</button></div>
      ${data.records.length ? data.records.map(m => `
        <div class="msg-item ${m.isRead ? '' : 'unread'}">
          <div class="row"><strong>${UI.esc(m.title)}</strong>${m.isRead ? '' : '<span class="chip brand">未读</span>'}
            <span class="spacer"></span><span class="small muted">${UI.date(m.createTime)}</span></div>
          <div class="small muted" style="margin-top:6px;line-height:1.7">${UI.esc(m.content || '')}</div>
        </div>`).join('') : UI.empty('暂无消息')}`;
    document.getElementById('readAll').onclick = async () => {
      try { await Api.put('/api/message/read-all'); UI.toast('已全部标为已读'); this.renderMessages(); }
      catch (e) { UI.toast(e.message, 'err'); }
    };
  }
};

document.addEventListener('DOMContentLoaded', () => Console.init());
