# 压测说明（JMeter）

## 安装位置

```
D:\jmeter\apache-jmeter-5.6.3
```

- 版本 **5.6.3**，启动 GUI：双击 `D:\jmeter\启动JMeter.bat`（已内置 JAVA_HOME）
- 命令行：`D:\jmeter\apache-jmeter-5.6.3\bin\jmeter.bat`
- 正式压测用命令行 `-n`，GUI 只用来编辑脚本

## 本目录文件

| 文件 | 作用 |
|---|---|
| `seckill.jmx` | 登录 + 抢券两个线程组（小规模功能压测） |
| `seckill-load.jmx` | 纯抢券高并发压测（用预生成 JWT，绕开 BCrypt 登录） |
| `gen-load-users.mjs` | 生成 N 个压测用户 + 对应 JWT（Node 脚本） |
| `users.csv` | 11 个演示账号（seckill.jmx 用） |
| `run-jmeter.ps1` | 一键压测 + 汇总 RT/吞吐 |
| `report-steady/` | 最近一次高并发压测的 HTML 报告（`index.html`） |

## 一、功能压测（登录 + 抢券）

```powershell
.\run-jmeter.ps1 -CouponId 4 -LoginThreads 100 -SeckillThreads 11
```

结果见下表「功能压测」一栏。

## 二、高并发抢券压测（QPS）

登录接口有 BCrypt（单次 ~70ms，CPU 密集），无法代表抢券吞吐，所以高并发压测用**预生成 JWT**：

```powershell
# 1) 生成 6000 个压测用户和 token（会写 docs/benchmark/load-users.sql、tokens.csv）
node gen-load-users.mjs

# 2) 导入用户
D:\mysql8\mysql-8.0.29-winx64\bin\mysql.exe -uroot -p123456 --default-character-set=utf8mb4 `
  -e "source C:/.../docs/benchmark/load-users.sql"

# 3) 启动服务（mysql 环境）
java -jar target/gourmet-recommend.jar --spring.profiles.active=mysql

# 4) 商家后台建一张 5000 库存的券，拿到券 ID，然后：
D:\jmeter\apache-jmeter-5.6.3\bin\jmeter.bat -n -t seckill-load.jmx -l result.jtl -e -o report `
  "-JcouponId=<券ID>" "-JloadThreads=300" "-JloadLoops=20" "-Jramp=2"
```

> `tokens.csv` / `load-users.sql` 每次测完会删掉，需要时用 `gen-load-users.mjs` 重新生成。

## 三、优化项

| 优化 | 说明 |
|---|---|
| 关闭 MyBatis SQL DEBUG | `com.gourmet: info`，去掉每条 SQL 的日志开销 |
| Tomcat 线程池 | max 500 / min-spare 50，accept-count 2000，max-connections 20000 |
| Hikari 连接池 | 20 → 50，异步落库与查询不再抢连接 |
| Lettuce 连接池 | 16 → 64 |
| 优惠券本地缓存 | Caffeine 3 秒，抢券入口不再每次查库 |
| Redis 可用性缓存 | 2 秒，去掉每请求一次 PING |
| 去掉券级 Redisson 锁 | Lua 本身原子，少一次 Redis 往返与锁竞争 |
| 去掉每请求 `hasKey` 探测 | 只在 Lua 返回 `-3` 时补库存并重试一次 |
| 每请求 INFO 日志 → DEBUG | 高并发下日志是最容易被忽视的瓶颈 |
| 消费者批量 ACK + 连读 | 每批 500，最多连读 20 批，ack 合并 |
| 用户级锁默认关闭 | Lua 的 SISMEMBER 已保证一人一单；需要双保险时 `SECKILL_USER_LOCK=true` |

## 四、实测结果（本机，16 核）

**功能压测**（登录 100 线程 × 2 次 + 抢券 11 线程）：

| 请求 | 次数 | 平均 | P90 | 最大 | 错误 |
|---|---|---|---|---|---|
| 登录 | 100 | 71.4 ms | 84 ms | 117 ms | 0 |
| 抢券 | 11 | 21.9 ms | 105 ms | 105 ms | 0 |

**高并发抢券**（300 线程 × 20 轮 = 6000 请求，库存 5000，用户锁关闭）：

| 指标 | 结果 |
|---|---|
| 吞吐 | **1956.9 req/s**（时间窗 3.07s） |
| 平均 RT | 80.7 ms |
| P50 / P90 / P95 / P99 | 71 / 151 / 196 / 258 ms |
| 最大 RT | 357 ms |
| 错误率 | 0% |
| 数据一致性 | 6000 请求 → **恰好 5000 条领取记录**，库存 0，Redis 库存 0，无超卖 |

> 开启用户级 Redisson 锁（`SECKILL_USER_LOCK=true`）时同样压测：**607 req/s**、平均 403ms，锁的两次 Redis 往返是主要成本。

## 五、压测后清理

```sql
DELETE FROM user_coupon WHERE coupon_id = <测试券ID>;
DELETE FROM coupon WHERE id = <测试券ID>;
DELETE FROM sys_user WHERE id BETWEEN 2001 AND 8000;
```

同时删除 Redis 的 `seckill:stock:<id>` 和 `seckill:users:<id>`。
