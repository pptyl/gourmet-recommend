import { writeFileSync } from 'node:fs';
import crypto from 'node:crypto';

const SECRET = 'gourmet-recommend-jwt-secret-key-for-demo-please-change-in-prod';
const HASH = '$2a$10$Na0ftfMIynruK2fzHhKJre0lUtG5KhpNegVSXluvEQg8Bd00gJWgi'; // 123456
const START = 2001;
const COUNT = 6000;

const b64 = obj => Buffer.from(JSON.stringify(obj)).toString('base64url');

function token(uid) {
  const now = Math.floor(Date.now() / 1000);
  const header = b64({ alg: 'HS256', typ: 'JWT' });
  const payload = b64({ sub: String(uid), role: 'USER', name: 'load' + uid, iat: now, exp: now + 720 * 60 });
  const data = header + '.' + payload;
  const sig = crypto.createHmac('sha256', SECRET).update(data).digest('base64url');
  return data + '.' + sig;
}

const sql = [];
sql.push('USE gourmet_recommend;');
sql.push('DELETE FROM sys_user WHERE id BETWEEN 2001 AND 8000;');
const rows = [];
const csv = ['token,uid'];
for (let i = 0; i < COUNT; i++) {
  const id = START + i;
  const phone = '139' + String(10000000 + i).padStart(8, '0');
  rows.push(`(${id}, '${phone}', '${HASH}', 'load${id}', NULL, '${phone}', NULL, NULL, NULL, 1, NOW(), NOW(), 0)`);
  csv.push(`${token(id)},${id}`);
}
sql.push('INSERT INTO sys_user (id, phone, password, nickname, avatar, contact_phone, taste_cuisine, taste_flavor, taboo, status, create_time, update_time, deleted) VALUES');
sql.push(rows.join(',\n') + ';');

writeFileSync('C:/Users/27253/Desktop/deepseek/gourmet-recommend/docs/benchmark/load-users.sql', sql.join('\n'), 'utf8');
writeFileSync('C:/Users/27253/Desktop/deepseek/gourmet-recommend/docs/benchmark/tokens.csv', csv.join('\n'), 'utf8');
console.log('生成完成：用户 ' + COUNT + '，示例 token ' + csv[1].slice(0, 40) + '...');
