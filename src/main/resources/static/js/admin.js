/* 平台后台：商家审核 / 用户 / 评价 / 笔记 / 统计 / 群发 */
const Admin = {
  async init() {
    if (!Auth.require('ADMIN')) return;
    Nav.render('admin');
    document.querySelectorAll('#side button').forEach(btn => {
      btn.onclick = () => {
        document.querySelectorAll('#side button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.show(btn.dataset.tab);
      };
    });
    await this.loadStats();
    this.show('merchants');
  },

  async loadStats() {
    try {
      const o = await Api.get('/api/statistic/admin/overview');
      document.getElementById('stats').innerHTML = `
        <div class="stat"><div class="k">用户总数</div><div class="v">${o.userCount}</div></div>
        <div class="stat brand"><div class="k">入驻商家</div><div class="v">${o.merchantCount}</div></div>
        <div class="stat"><div class="k">待审商家</div><div class="v">${o.pendingMerchantCount}</div></div>
        <div class="stat"><div class="k">评价 / 待审</div><div class="v">${o.reviewCount}<span class="small muted"> / ${o.pendingReviewCount}</span></div></div>
        <div class="stat"><div class="k">笔记 / 待审</div><div class="v">${o.noteCount}<span class="small muted"> / ${o.pendingNoteCount}</span></div></div>
        <div class="stat"><div class="k">优惠券领取</div><div class="v">${o.couponClaimCount}</div></div>`;
    } catch (e) { UI.toast(e.message, 'err'); }
  },

  async show(tab) {
    const panel = document.getElementById('panel');
    if (window.innerWidth <= 860) {
      setTimeout(() => panel.scrollIntoView({ behavior: 'smooth', block: 'start' }), 80);
    }
    panel.innerHTML = '<div class="skeleton" style="height:120px"></div>';
    try {
      if (tab === 'merchants') return this.renderMerchants();
      if (tab === 'users') return this.renderUsers();
      if (tab === 'reviews') return this.renderReviews();
      if (tab === 'notes') return this.renderNotes();
      if (tab === 'stats') return this.renderStats();
      if (tab === 'broadcast') return this.renderBroadcast();
    } catch (e) {
      panel.innerHTML = UI.empty(e.message);
      UI.toast(e.message, 'err');
    }
  },

  async renderMerchants() {
    const data = await Api.get('/api/admin/merchant/page?page=1&size=50');
    const panel = document.getElementById('panel');
    panel.innerHTML = `
      <div class="row" style="margin-bottom:12px"><h3>商家审核</h3>
        <span class="small muted">共 ${data.total} 家</span></div>
      ${data.records.length ? `
      <table class="table">
        <thead><tr><th>店铺</th><th>联系方式</th><th>分类</th><th>审核</th><th>营业</th><th style="width:250px">操作</th></tr></thead>
        <tbody>${data.records.map(m => `
          <tr>
            <td><strong>${UI.esc(m.name)}</strong><div class="small muted">评分 ${Number(m.score || 0).toFixed(1)} · 评价 ${m.reviewCount || 0}</div></td>
            <td>${UI.esc(m.phone)}<div class="small muted">${UI.esc(m.area || '')}</div></td>
            <td>${UI.esc(m.category || '')}</td>
            <td>${m.auditStatus === 0 ? '<span class="chip warn">待审核</span>' : m.auditStatus === 1 ? '<span class="chip ok">已通过</span>' : '<span class="chip danger">已驳回</span>'}</td>
            <td>${m.status === 1 ? '<span class="chip ok">营业</span>' : '<span class="chip">停业</span>'}</td>
            <td>
              ${m.auditStatus === 0 ? `
                <button class="btn btn-primary btn-sm" data-pass="${m.id}">通过</button>
                <button class="btn btn-ghost btn-sm" data-reject="${m.id}">驳回</button>` : ''}
              ${m.auditStatus === 1 ? `
                <button class="btn btn-ghost btn-sm" data-open="${m.id}" data-status="${m.status}">${m.status === 1 ? '停业整顿' : '恢复营业'}</button>` : ''}
            </td>
          </tr>`).join('')}</tbody>
      </table>` : UI.empty('没有商家数据')}`;

    panel.querySelectorAll('[data-pass]').forEach(btn => {
      btn.onclick = () => this.auditMerchant(btn.dataset.pass, true);
    });
    panel.querySelectorAll('[data-reject]').forEach(btn => {
      btn.onclick = () => this.auditMerchant(btn.dataset.reject, false);
    });
    panel.querySelectorAll('[data-open]').forEach(btn => {
      btn.onclick = async () => {
        const next = Number(btn.dataset.status) === 1 ? 0 : 1;
        try {
          await Api.put(`/api/admin/merchant/${btn.dataset.open}/status`, { status: next });
          UI.toast(next ? '已恢复营业' : '已停业整顿');
          this.renderMerchants(); this.loadStats();
        } catch (e) { UI.toast(e.message, 'err'); }
      };
    });
  },

  auditMerchant(id, pass) {
    if (pass) {
      Api.put(`/api/admin/merchant/${id}/audit`, { pass: true, remark: '资料齐全，审核通过' })
        .then(() => { UI.toast('已通过审核'); this.renderMerchants(); this.loadStats(); })
        .catch(e => UI.toast(e.message, 'err'));
      return;
    }
    UI.modal('驳回入驻申请', `
      <div class="field"><label>驳回原因</label><textarea class="textarea" id="rejectMsg" placeholder="例如：营业执照信息不清晰"></textarea></div>
      <button class="btn btn-danger btn-block" id="rejectOk">确认驳回</button>`, 420);
    document.getElementById('rejectOk').onclick = async () => {
      try {
        await Api.put(`/api/admin/merchant/${id}/audit`, {
          pass: false, remark: document.getElementById('rejectMsg').value.trim() || '资料不完整'
        });
        UI.toast('已驳回');
        document.querySelector('.overlay')?.remove();
        this.renderMerchants(); this.loadStats();
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  },

  async renderUsers() {
    const data = await Api.get('/api/admin/user/page?page=1&size=50');
    const panel = document.getElementById('panel');
    panel.innerHTML = `<h3 style="margin-bottom:12px">用户管理 <span class="small muted">共 ${data.total} 人</span></h3>
      ${data.records.length ? `
      <table class="table">
        <thead><tr><th>用户</th><th>手机号</th><th>口味偏好</th><th>状态</th><th>注册时间</th><th>操作</th></tr></thead>
        <tbody>${data.records.map(u => `
          <tr>
            <td><div class="row"><span class="avatar">${UI.esc((u.nickname || '?').slice(0, 1))}</span>${UI.esc(u.nickname || '')}</div></td>
            <td>${UI.esc(u.phone)}</td>
            <td class="small muted">${UI.esc(u.tasteCuisine || '-')} ${u.tasteFlavor ? '· ' + UI.esc(u.tasteFlavor) : ''}</td>
            <td>${u.status === 1 ? '<span class="chip ok">正常</span>' : '<span class="chip danger">已禁用</span>'}</td>
            <td class="small muted">${UI.date(u.createTime)}</td>
            <td><button class="btn ${u.status === 1 ? 'btn-danger' : 'btn-primary'} btn-sm"
              data-user="${u.id}" data-status="${u.status}">${u.status === 1 ? '禁用' : '启用'}</button></td>
          </tr>`).join('')}</tbody>
      </table>` : UI.empty('没有用户')}`;
    panel.querySelectorAll('[data-user]').forEach(btn => {
      btn.onclick = async () => {
        const next = Number(btn.dataset.status) === 1 ? 0 : 1;
        if (!await UI.confirm(next === 0 ? '确定禁用该用户吗？' : '确定启用该用户吗？')) return;
        try { await Api.put(`/api/admin/user/${btn.dataset.user}/status`, { status: next }); UI.toast(next ? '已启用' : '已禁用'); this.renderUsers(); }
        catch (e) { UI.toast(e.message, 'err'); }
      };
    });
  },

  async renderReviews() {
    const data = await Api.get('/api/admin/review/page?page=1&size=30');
    const panel = document.getElementById('panel');
    panel.innerHTML = `<h3 style="margin-bottom:12px">评价审核 <span class="small muted">共 ${data.total} 条</span></h3>
      ${data.records.length ? data.records.map(r => `
        <div class="msg-item">
          <div class="row">
            <strong>${UI.esc(r.merchantName || '商家')}</strong>
            <span class="small muted">by ${UI.esc(r.userName || '')}</span>
            ${UI.rating(r.score)} ${UI.contentStatus(r.status)}
            <span class="spacer"></span>
            <span class="small muted">${UI.date(r.createTime)}</span>
          </div>
          <div style="margin-top:8px;line-height:1.7">${UI.esc(r.content || '')}</div>
          <div class="review-imgs">${(r.imageList || []).map(u => `<img src="${UI.esc(u)}" alt="">`).join('')}</div>
          ${r.status === 0 ? `<div class="row" style="margin-top:10px">
            <button class="btn btn-primary btn-sm" data-rv="${r.id}" data-pass="1">通过</button>
            <button class="btn btn-danger btn-sm" data-rv="${r.id}" data-pass="0">违规下架</button>
          </div>` : ''}
        </div>`).join('') : UI.empty('没有待处理评价')}`;
    panel.querySelectorAll('[data-rv]').forEach(btn => {
      btn.onclick = async () => {
        const pass = btn.dataset.pass === '1';
        try {
          await Api.put(`/api/admin/review/${btn.dataset.rv}/audit`,
            { pass, remark: pass ? '内容合规' : '含违规内容' });
          UI.toast(pass ? '已通过' : '已下架');
          this.renderReviews(); this.loadStats();
        } catch (e) { UI.toast(e.message, 'err'); }
      };
    });
  },

  async renderNotes() {
    const data = await Api.get('/api/admin/note/page?page=1&size=30');
    const panel = document.getElementById('panel');
    panel.innerHTML = `<h3 style="margin-bottom:12px">笔记审核 <span class="small muted">共 ${data.total} 篇</span></h3>
      ${data.records.length ? data.records.map(n => `
        <div class="msg-item">
          <div class="row">
            <strong>${UI.esc(n.title)}</strong>
            <span class="small muted">by ${UI.esc(n.userName || '')}</span>
            ${UI.contentStatus(n.status)}
            <span class="spacer"></span>
            <span class="small muted">${UI.date(n.createTime)}</span>
          </div>
          <div class="small muted" style="margin-top:6px;line-height:1.7">${UI.esc(n.content || '')}</div>
          ${n.status === 0 ? `<div class="row" style="margin-top:10px">
            <button class="btn btn-primary btn-sm" data-nt="${n.id}" data-pass="1">通过</button>
            <button class="btn btn-danger btn-sm" data-nt="${n.id}" data-pass="0">下架</button>
          </div>` : ''}
        </div>`).join('') : UI.empty('没有待处理笔记')}`;
    panel.querySelectorAll('[data-nt]').forEach(btn => {
      btn.onclick = async () => {
        const pass = btn.dataset.pass === '1';
        try {
          await Api.put(`/api/admin/note/${btn.dataset.nt}/audit`,
            { pass, remark: pass ? '内容合规' : '含违规信息' });
          UI.toast(pass ? '已发布' : '已下架');
          this.renderNotes(); this.loadStats();
        } catch (e) { UI.toast(e.message, 'err'); }
      };
    });
  },

  async renderStats() {
    const [coupons, rank] = await Promise.all([
      Api.get('/api/statistic/admin/coupon'),
      Api.get('/api/statistic/admin/merchant-rank?limit=10')
    ]);
    const panel = document.getElementById('panel');
    panel.innerHTML = `
      <h3 style="margin-bottom:12px">优惠券领取数据</h3>
      <table class="table">
        <thead><tr><th>券</th><th>库存</th><th>已领取</th><th>已使用</th><th>领取率</th></tr></thead>
        <tbody>${coupons.map(c => {
          const rate = c.totalStock ? Math.round((Number(c.claimed) / Number(c.totalStock)) * 100) : 0;
          return `<tr>
            <td>${UI.esc(c.title)}</td>
            <td>${c.remainStock}/${c.totalStock}</td>
            <td>${c.claimed}</td>
            <td>${c.used}</td>
            <td>${rate}%</td>
          </tr>`;
        }).join('')}</tbody>
      </table>
      <h3 style="margin:24px 0 12px">店铺热度排行</h3>
      <table class="table">
        <thead><tr><th>#</th><th>店铺</th><th>热度</th><th>评分</th><th>收藏</th><th>评价</th></tr></thead>
        <tbody>${rank.map((m, i) => `
          <tr>
            <td>${i + 1}</td>
            <td><a href="/merchant.html?id=${m.id}">${UI.esc(m.name)}</a></td>
            <td>${Math.round(m.heat || 0)}</td>
            <td>${Number(m.score || 0).toFixed(1)}</td>
            <td>${m.favoriteCount || 0}</td>
            <td>${m.reviewCount || 0}</td>
          </tr>`).join('')}</tbody>
      </table>`;
  },

  renderBroadcast() {
    const panel = document.getElementById('panel');
    panel.innerHTML = `
      <h3 style="margin-bottom:14px">消息群发</h3>
      <div class="field"><label>标题</label><input class="input" id="bTitle" placeholder="例如：平台国庆活动通知"></div>
      <div class="field"><label>内容</label><textarea class="textarea" id="bContent"></textarea></div>
      <div class="form-grid">
        <div class="field"><label>接收者</label>
          <select class="select" id="bType"><option value="1">全部普通用户</option><option value="2">全部商家</option></select></div>
        <div class="field"><label>消息类型</label>
          <select class="select" id="bMsgType">
            <option value="4">系统通知</option><option value="1">新店开业</option>
            <option value="3">优惠券活动</option><option value="5">审核结果</option>
          </select></div>
      </div>
      <button class="btn btn-primary" id="bSend">发送</button>`;
    document.getElementById('bSend').onclick = async () => {
      const title = document.getElementById('bTitle').value.trim();
      if (!title) return UI.toast('请填写标题', 'warn');
      try {
        const n = await Api.post('/api/message/broadcast', {
          title,
          content: document.getElementById('bContent').value.trim(),
          receiverType: Number(document.getElementById('bType').value),
          type: Number(document.getElementById('bMsgType').value)
        });
        UI.toast('已推送 ' + n + ' 条消息');
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  }
};

document.addEventListener('DOMContentLoaded', () => Admin.init());
