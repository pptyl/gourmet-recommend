import { setTimeout as sleep } from 'node:timers/promises';

const BASE = 'http://localhost:8080';

async function api(path, { method = 'GET', body, token } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const r = await fetch(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const j = await r.json();
  return j;
}

const merchant = (await api('/api/merchant/login', { method: 'POST', body: { phone: '13900000001', password: '123456' } })).data;
const tokens = [];
for (const phone of ['13800000002', '13800000003', '13800000004', '13800000101', '13800000102',
  '13800000103', '13800000104', '13800000105', '13800000106', '13800000001']) {
  const j = await api('/api/user/login', { method: 'POST', body: { phone, password: '123456' } });
  if (j.code === 200) tokens.push({ phone, token: j.data.token });
}

const pad = n => String(n).padStart(2, '0');
const fmt = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
const now = new Date();
const begin = fmt(new Date(now.getTime() - 3600_000));
const end = fmt(new Date(now.getTime() + 10 * 86400_000));
const validEnd = fmt(new Date(now.getTime() + 40 * 86400_000));

async function createCoupon(stock, title) {
  const j = await api('/api/coupon', {
    method: 'POST', token: merchant.token,
    body: {
      title, description: '压测用券', thresholdAmount: 50, discountAmount: 10,
      totalStock: stock, perLimit: 1,
      grabBeginTime: begin, grabEndTime: end, validStartTime: begin, validEndTime: validEnd
    }
  });
  if (j.code !== 200) throw new Error('发券失败: ' + j.message);
  return j.data;
}

// ---------- 1. 单人异步链路 ----------
console.log('\n=== 1. 单人抢券（异步落库） ===');
const c1 = await createCoupon(3, '异步链路测试券');
const t1 = Date.now();
const r1 = await api(`/api/coupon/${c1.id}/seckill`, { method: 'POST', token: tokens[0].token });
const rt1 = Date.now() - t1;
console.log('响应:', JSON.stringify(r1.data), 'RT=' + rt1 + 'ms');
console.log('立即查券包:', (await api('/api/coupon/my', { token: tokens[0].token })).data.filter(x => x.couponId === c1.id).length);
await sleep(1500);
const st = await api(`/api/coupon/${c1.id}/seckill/status`, { token: tokens[0].token });
const my = await api('/api/coupon/my', { token: tokens[0].token });
const detail1 = await api(`/api/coupon/detail/${c1.id}`, { token: tokens[0].token });
console.log('1.5s 后状态:', JSON.stringify(st.data), '| 券包条数:', my.data.filter(x => x.couponId === c1.id).length,
  '| DB 剩余库存:', detail1.data.remainStock);
const dup = await api(`/api/coupon/${c1.id}/seckill`, { method: 'POST', token: tokens[0].token });
console.log('重复领取:', dup.code, dup.message);

// ---------- 2. 并发抢 5 张 ----------
console.log('\n=== 2. 并发压测：10 人抢 5 张 ===');
const c2 = await createCoupon(5, '并发压测券-5张');
const t0 = Date.now();
const results = await Promise.all(tokens.map(async u => {
  const start = Date.now();
  const j = await api(`/api/coupon/${c2.id}/seckill`, { method: 'POST', token: u.token });
  return { code: j.code, msg: j.message, rt: Date.now() - start, data: j.data };
}));
const elapsed = Date.now() - t0;
const ok = results.filter(r => r.code === 200);
const soldOut = results.filter(r => r.code === 409 && (r.msg || '').includes('抢光'));
const dupErr = results.filter(r => r.code === 409 && (r.msg || '').includes('每人'));
const other = results.filter(r => r.code !== 200 && r.code !== 409);
console.log(`总耗时 ${elapsed}ms，成功 ${ok.length}，售罄 ${soldOut.length}，重复 ${dupErr.length}，其它 ${other.length}`);
console.log('平均 RT:', Math.round(results.reduce((s, r) => s + r.rt, 0) / results.length) + 'ms',
  '| 最大 RT:', Math.max(...results.map(r => r.rt)) + 'ms');
if (other.length) console.log('其它响应:', JSON.stringify(other.slice(0, 3)));

await sleep(2500);
const recs = await api(`/api/coupon/${c2.id}/records?page=1&size=50`, { token: merchant.token });
const d2 = await api(`/api/coupon/detail/${c2.id}`, { token: tokens[0].token });
console.log(`2.5s 后：领取记录 ${recs.data.total} 条，DB 剩余库存 ${d2.data.remainStock}（应为 0）`);
console.log('结论:', recs.data.total === 5 && d2.data.remainStock === 0 ? '✅ 无超卖、无多发' : '❌ 数据异常');
console.log('COUPON_IDS', c1.id, c2.id);
