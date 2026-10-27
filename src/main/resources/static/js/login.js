/* 登录 / 注册 / 商家入驻 / 管理员登录 */
const LoginPage = {
  role: 'USER',
  mode: 'login',

  init() {
    // 已登录时访问登录页：直接回到对应首页，避免重复登录
    if (Auth.isLogin() && qs('switch') !== '1') {
      const redirect = qs('redirect');
      const target = redirect || (Auth.role() === 'MERCHANT' ? '/console.html'
        : Auth.role() === 'ADMIN' ? '/admin.html' : '/index.html');
      location.replace(target);
      return;
    }
    Nav.render('');
    const tab = qs('tab');
    if (tab === 'register') { this.mode = 'register'; }
    if (tab === 'apply') { this.role = 'MERCHANT'; this.mode = 'apply'; }
    document.querySelectorAll('#roleTabs button').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.role === this.role);
      btn.onclick = () => {
        document.querySelectorAll('#roleTabs button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.role = btn.dataset.role;
        this.mode = 'login';
        this.render();
      };
    });
    this.render();
  },

  render() {
    if (this.role === 'ADMIN') return this.renderAdmin();
    if (this.role === 'MERCHANT') return this.mode === 'apply' ? this.renderApply() : this.renderMerchant();
    return this.mode === 'register' ? this.renderRegister() : this.renderUserLogin();
  },

  afterLogin(data) {
    Auth.save(data);
    UI.toast('登录成功，欢迎 ' + (data.name || ''));
    const redirect = qs('redirect');
    if (redirect) { location.href = decodeURIComponent(redirect); return; }
    const home = data.role === 'MERCHANT' ? '/console.html' : data.role === 'ADMIN' ? '/admin.html' : '/index.html';
    setTimeout(() => location.href = home, 300);
  },

  fail() {
    UI.toast('请检查标红的必填项', 'warn');
  },

  /* ------------------------- 用户登录 ------------------------- */
  renderUserLogin() {
    document.getElementById('panel').innerHTML = `
      <div class="field"><label>手机号 <span class="req">*</span></label>
        <input class="input" id="phone" maxlength="11" placeholder="请输入 11 位手机号"><div class="err"></div></div>
      <div class="field"><label>密码 <span class="req">*</span></label>
        <input class="input" type="password" id="pwd" placeholder="请输入密码"><div class="err"></div></div>
      <button class="btn btn-primary btn-block" id="doLogin">登录</button>
      <div class="switch-line">还没有账号？<a id="toRegister">立即注册</a></div>`;

    document.getElementById('toRegister').onclick = () => { this.mode = 'register'; this.render(); };
    document.getElementById('doLogin').onclick = async () => {
      const phone = document.getElementById('phone');
      const pwd = document.getElementById('pwd');
      const ok = UI.validAll(check => {
        check(phone, /^1[3-9]\d{9}$/.test(phone.value.trim()), '请输入 11 位手机号');
        check(pwd, pwd.value.length > 0, '请输入密码');
      });
      if (!ok) return this.fail();
      try {
        const data = await Api.post('/api/user/login', {
          phone: phone.value.trim(), password: pwd.value
        }, false);
        this.afterLogin(data);
      } catch (e) { UI.toast(e.message, 'err'); }
    };
    document.getElementById('pwd').onkeydown = e => {
      if (e.key === 'Enter') document.getElementById('doLogin').click();
    };
  },

  /* ------------------------- 用户注册 ------------------------- */
  renderRegister() {
    document.getElementById('panel').innerHTML = `
      <div class="field"><label>手机号 <span class="req">*</span></label>
        <input class="input" id="phone" maxlength="11" placeholder="11 位手机号"><div class="err"></div></div>
      <div class="field"><label>密码 <span class="req">*</span></label>
        <input class="input" type="password" id="pwd" placeholder="6-20 位"><div class="err"></div></div>
      <div class="field"><label>昵称</label>
        <input class="input" id="nickname" maxlength="30" placeholder="怎么称呼你"></div>
      <div class="field">
        <label>偏好菜系 <span class="hint">可多选，用于个性化推荐</span></label>
        ${UI.cuisineChips('')}
      </div>
      <div class="form-grid">
        <div class="field"><label>口味轻重</label>
          <select class="select" id="flavor">
            <option value="">不设置</option>
            <option>清淡</option><option>微辣</option><option>中辣</option><option>重辣</option>
          </select>
        </div>
        <div class="field"><label>忌口食材</label>
          <input class="input" id="taboo" maxlength="200" placeholder="香菜,折耳根"></div>
      </div>
      <button class="btn btn-primary btn-block" id="doRegister">注册并登录</button>
      <div class="switch-line">已有账号？<a id="toLogin">去登录</a></div>`;

    UI.bindChips(document.getElementById('panel'));
    document.getElementById('toLogin').onclick = () => { this.mode = 'login'; this.render(); };
    document.getElementById('doRegister').onclick = async () => {
      const phone = document.getElementById('phone');
      const pwd = document.getElementById('pwd');
      const ok = UI.validAll(check => {
        check(phone, /^1[3-9]\d{9}$/.test(phone.value.trim()), '请输入 11 位手机号');
        check(pwd, pwd.value.length >= 6 && pwd.value.length <= 20, '密码长度需为 6-20 位');
      });
      if (!ok) return this.fail();
      try {
        const data = await Api.post('/api/user/register', {
          phone: phone.value.trim(),
          password: pwd.value,
          nickname: document.getElementById('nickname').value.trim() || null,
          tasteCuisine: UI.chipsValue(document.getElementById('panel')) || null,
          tasteFlavor: document.getElementById('flavor').value || null,
          taboo: document.getElementById('taboo').value.trim() || null
        }, false);
        this.afterLogin(data);
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  },

  /* ------------------------- 商家登录 ------------------------- */
  renderMerchant() {
    document.getElementById('panel').innerHTML = `
      <div class="field"><label>商家手机号 <span class="req">*</span></label>
        <input class="input" id="mPhone" maxlength="11" placeholder="请输入 11 位手机号"><div class="err"></div></div>
      <div class="field"><label>密码 <span class="req">*</span></label>
        <input class="input" type="password" id="mPwd" placeholder="请输入密码"><div class="err"></div></div>
      <button class="btn btn-primary btn-block" id="mLogin">登录商家后台</button>
      <div class="switch-line">还没有店铺？<a id="toApply">申请入驻</a></div>`;

    document.getElementById('toApply').onclick = () => this.renderApply();
    document.getElementById('mLogin').onclick = async () => {
      const phone = document.getElementById('mPhone');
      const pwd = document.getElementById('mPwd');
      const ok = UI.validAll(check => {
        check(phone, /^1[3-9]\d{9}$/.test(phone.value.trim()), '请输入 11 位手机号');
        check(pwd, pwd.value.length > 0, '请输入密码');
      });
      if (!ok) return this.fail();
      try {
        const data = await Api.post('/api/merchant/login', {
          phone: phone.value.trim(), password: pwd.value
        }, false);
        this.afterLogin(data);
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  },

  /* ------------------------- 商家入驻申请 ------------------------- */
  renderApply() {
    document.getElementById('panel').innerHTML = `
      <div class="field"><label>店铺名称 <span class="req">*</span></label>
        <input class="input" id="aName" maxlength="100" placeholder="例如：巷子口小馆"><div class="err"></div></div>
      <div class="form-grid">
        <div class="field"><label>登录手机号 <span class="req">*</span></label>
          <input class="input" id="aPhone" maxlength="11" placeholder="11 位手机号"><div class="err"></div></div>
        <div class="field"><label>登录密码 <span class="req">*</span></label>
          <input class="input" type="password" id="aPwd" placeholder="6-20 位"><div class="err"></div></div>
        <div class="field"><label>菜系 <span class="req">*</span></label>
          ${UI.cuisineSelect('', 'aCategory')}<div class="err"></div></div>
        <div class="field"><label>地区 <span class="req">*</span></label>
          <input class="input" id="aArea" list="areaList" maxlength="20" placeholder="例如：成都锦江区"><div class="err"></div></div>
        <div class="field full"><label>详细地址 <span class="req">*</span></label>
          <input class="input" id="aAddress" maxlength="255" placeholder="例如：成都市锦江区春熙路 88 号"><div class="err"></div></div>
        <div class="field"><label>营业时间</label>
          <input class="input" id="aHours" placeholder="10:00-22:00"><div class="err"></div></div>
        <div class="field"><label>人均消费</label>
          <input class="input" type="number" id="aPrice" min="0" step="1" placeholder="68"><div class="err"></div></div>
        <div class="field full"><label>店铺简介</label>
          <textarea class="textarea" id="aIntro" maxlength="1000" placeholder="主打菜品、环境特色…"></textarea></div>
      </div>
      <datalist id="areaList">
        <option value="成都锦江区"></option><option value="成都武侯区"></option>
        <option value="北京朝阳区"></option><option value="北京东城区"></option>
        <option value="上海静安区"></option><option value="上海徐汇区"></option>
        <option value="广州天河区"></option><option value="深圳南山区"></option>
        <option value="杭州西湖区"></option><option value="南京鼓楼区"></option>
        <option value="武汉武昌区"></option><option value="长沙岳麓区"></option>
        <option value="西安雁塔区"></option><option value="重庆渝中区"></option>
      </datalist>
      <button class="btn btn-primary btn-block" id="doApply">提交入驻申请</button>
      <div class="switch-line"><a id="backLogin">返回商家登录</a></div>
      <p class="small muted center" style="margin-top:10px">提交后需平台管理员审核，通过后即可登录商家后台</p>`;

    document.getElementById('backLogin').onclick = () => this.renderMerchant();
    document.getElementById('doApply').onclick = async () => {
      const name = document.getElementById('aName');
      const phone = document.getElementById('aPhone');
      const pwd = document.getElementById('aPwd');
      const category = document.getElementById('aCategory');
      const area = document.getElementById('aArea');
      const address = document.getElementById('aAddress');
      const hours = document.getElementById('aHours');
      const price = document.getElementById('aPrice');

      const ok = UI.validAll(check => {
        check(name, name.value.trim().length >= 2, '店铺名称至少 2 个字');
        check(phone, /^1[3-9]\d{9}$/.test(phone.value.trim()), '请输入 11 位手机号');
        check(pwd, pwd.value.length >= 6 && pwd.value.length <= 20, '密码长度需为 6-20 位');
        check(category, !!category.value, '请选择菜系');
        check(area, /^[\u4e00-\u9fa5A-Za-z·]{2,20}$/.test(area.value.trim()), '地区填中文名称，2-20 个字，不能是数字');
        check(address, address.value.trim().length >= 5 && /[\u4e00-\u9fa5]/.test(address.value), '地址至少 5 个字，并包含中文门牌');
        check(hours, !hours.value.trim() || /^([01]?\d|2[0-3]):[0-5]\d\s*[-~至]\s*([01]?\d|2[0-3]):[0-5]\d$/.test(hours.value.trim()), '营业时间格式如 10:00-22:00');
        check(price, price.value === '' || Number(price.value) >= 0, '人均消费不能为负');
      });
      if (!ok) return this.fail();

      const body = {
        name: name.value.trim(),
        phone: phone.value.trim(),
        password: pwd.value,
        category: category.value,
        area: area.value.trim(),
        address: address.value.trim(),
        businessHours: hours.value.trim() || null,
        avgPrice: Number(price.value || 0),
        intro: document.getElementById('aIntro').value.trim() || null
      };
      try {
        await Api.post('/api/merchant/apply', body, false);
        UI.toast('入驻申请已提交，请等待审核');
        this.renderMerchant();
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  },

  /* ------------------------- 管理员登录 ------------------------- */
  renderAdmin() {
    document.getElementById('panel').innerHTML = `
      <div class="field"><label>管理员账号 <span class="req">*</span></label>
        <input class="input" id="admUser" placeholder="请输入管理员账号"><div class="err"></div></div>
      <div class="field"><label>密码 <span class="req">*</span></label>
        <input class="input" type="password" id="admPwd" placeholder="请输入密码"><div class="err"></div></div>
      <button class="btn btn-primary btn-block" id="admLogin">登录平台后台</button>`;

    document.getElementById('admLogin').onclick = async () => {
      const user = document.getElementById('admUser');
      const pwd = document.getElementById('admPwd');
      const ok = UI.validAll(check => {
        check(user, user.value.trim().length > 0, '请输入管理员账号');
        check(pwd, pwd.value.length > 0, '请输入密码');
      });
      if (!ok) return this.fail();
      try {
        const data = await Api.post('/api/admin/login', {
          username: user.value.trim(), password: pwd.value
        }, false);
        this.afterLogin(data);
      } catch (e) { UI.toast(e.message, 'err'); }
    };
  }
};

document.addEventListener('DOMContentLoaded', () => LoginPage.init());
