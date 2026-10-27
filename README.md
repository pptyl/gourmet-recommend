# 美食推荐系统（SpringBoot 后端）

面向 **普通用户 / 入驻商家 / 平台管理员** 三类角色的美食推荐平台后端，覆盖商家浏览、菜品管理、UGC 探店笔记、个性化智能推荐、评价收藏、限时优惠券抢购、消息通知与数据统计等完整业务。

- 后端：Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + MySQL 8 / H2 + Redis + JWT
- 文档：Swagger / OpenAPI 3（`/swagger-ui.html`）
- 规模：133 个 Java 源文件（含集成测试）、16 张表、98 个 REST 接口、14 个 Controller
- 可直接用 IntelliJ IDEA 打开，默认 `dev` 环境使用 H2 内存库，**无需安装 MySQL 即可启动**

---

## 一、技术栈

| 分类 | 选型 |
| --- | --- |
| 基础框架 | Spring Boot 3.2.5（Java 17） |
| 持久层 | MyBatis-Plus 3.5.7（分页插件、逻辑删除、自动填充、防全表更新） |
| 数据库 | MySQL 8.x（生产）/ H2 内存库（开发，MySQL 兼容模式） |
| 缓存 / 秒杀 | Spring Data Redis + Lettuce，Redisson 分布式锁，Lua 原子扣减，Redis Stream 异步削峰 |
| 认证鉴权 | JWT（jjwt 0.11.5）+ 自定义拦截器 + `@RequireRole` 注解 |
| 密码加密 | BCrypt（spring-security-crypto） |
| 接口文档 | springdoc-openapi 2.3.0 |
| 参数校验 | Jakarta Validation |
| 前端 | 原生 HTML + CSS + JavaScript（无构建、无依赖，随 Jar 一起发布） |
| 其他 | Lombok、HikariCP、全局异常、统一返回、本地文件上传 |

---

## 二、工程结构

```
gourmet-recommend
├── pom.xml
├── README.md
├── docker-compose.yml                # 一键起 MySQL + Redis（可选）
└── src/main
    ├── java/com/gourmet
    │   ├── GourmetApplication.java   # 启动类
    │   ├── common
    │   │   ├── annotation/           # @RequireRole
    │   │   ├── constant/             # BizConstants 全局业务常量
    │   │   ├── context/              # UserContext（ThreadLocal 登录上下文）
    │   │   ├── exception/            # BusinessException + 全局异常处理器
    │   │   ├── interceptor/          # JWT 认证 + 角色鉴权拦截器
    │   │   ├── model/                # LoginUser
    │   │   ├── query/                # PageQuery 分页基类
    │   │   ├── redis/                # RedisService、RedisLockUtil
    │   │   ├── result/               # Result、ResultCode、PageResult
    │   │   └── util/                 # JwtUtil、JwtProperties
    │   ├── config/                   # Web/CORS/上传/Redis/MyBatis/Jackson/OpenAPI
    │   ├── controller/               # 13 个业务 Controller + 文件上传
    │   ├── dto/                      # 24 个请求参数对象
    │   ├── entity/                   # 14 个实体
    │   ├── mapper/                   # 14 个 Mapper
    │   ├── service + service/impl    # 13 个业务 Service
    │   └── vo/                       # 7 个返回视图对象
    └── resources
        ├── application.yml           # 公共配置
        ├── application-dev.yml       # H2 开发环境（默认）
        ├── application-mysql.yml     # MySQL 环境
        ├── application-prod.yml      # 生产环境（环境变量注入）
        ├── db/h2-schema.sql          # H2 建表
        ├── db/h2-data.sql            # H2 初始化数据
        ├── lua/seckill.lua           # 秒杀原子脚本
        └── static/                   # 前端页面（html / css / js）
            ├── index.html            # 首页：推荐 + 榜单 + 商家筛选
            ├── merchant.html         # 商家详情：菜品 / 评价 / 优惠券
            ├── login.html            # 三类角色登录 / 注册 / 入驻
            ├── profile.html          # 个人中心
            ├── notes.html            # 探店笔记
            ├── console.html          # 商家后台
            ├── admin.html            # 平台后台
            ├── css/app.css
            └── js/                   # api / ui / nav + 各页面脚本
```

---

## 三、快速开始

### 0. Maven 环境（本机已配好）

| 项 | 值 |
| --- | --- |
| 版本 | **Maven 3.9.16**（官方二进制，阿里云镜像下载） |
| 安装目录 | `D:\maven\apache-maven-3.9.16` |
| 全局配置 | `D:\maven\apache-maven-3.9.16\conf\settings.xml` |
| 用户配置 | `C:\Users\27253\.m2\settings.xml`（内容相同，IDEA / Wrapper 也会读取） |
| 本地仓库 | `D:\MavenRepository\maven\local` |
| 镜像 | 阿里云公共仓库 `https://maven.aliyun.com/repository/public`（`mirrorOf=*`） |
| 项目 Wrapper | `mvnw` / `mvnw.cmd`，固定 3.9.16，分发地址也走阿里云 |

```bash
mvn -v                 # 应显示 Apache Maven 3.9.16 / Java 17
mvn clean package -DskipTests

# 不依赖 PATH 的写法（项目自带 wrapper，首次会从阿里云下载 Maven）
.\mvnw.cmd clean package -DskipTests
```

IDEA 里确认一次：`Settings → Build, Execution, Deployment → Build Tools → Maven`

- Maven home path：`D:\maven\apache-maven-3.9.16`
- User settings file：`C:\Users\27253\.m2\settings.xml`
- Local repository：`D:\MavenRepository\maven\local`

> 旧的 3.9.9 / 3.9.11 已删除。原来系统 PATH 里有一条被截断的路径（`n\apache-maven-3.9.11-bin\...`，前面 `D:\mave` 丢了），这是之前 `mvn` 找不到命令的原因；用户 PATH 已替换为 `D:\maven\apache-maven-3.9.16\bin` 并设置 `MAVEN_HOME`。系统级 PATH 里的残缺项需要管理员权限清理，留着不影响使用。

### 1. 用 IDEA 打开

`File → Open → 选择 gourmet-recommend 目录`，等待 Maven 依赖下载完成。
项目 JDK 选择 **17**（`File → Project Structure → SDK`）。

### 2. 直接运行（H2，零依赖）

运行 `com.gourmet.GourmetApplication`，默认激活 `dev` 环境：

- 服务地址：<http://localhost:8080>
- Swagger 文档：<http://localhost:8080/swagger-ui.html>
- H2 控制台：<http://localhost:8080/h2-console>（JDBC URL：`jdbc:h2:mem:gourmet`，用户 `sa`，密码空）

第一次启动会自动执行 `db/h2-schema.sql` 与 `db/h2-data.sql`，自带 **25 家商家、67 道菜、40 条评价、18 篇笔记、13 张优惠券、12 个用户** 以及点赞、收藏、心愿单等演示数据。

### 3. 切换 MySQL

> 本机 MySQL 的 `gourmet_recommend` 库已经初始化完成（16 张表 + 演示数据），项目内不再保留 MySQL 初始化脚本；
> 完整备份（结构 + 数据）见 `docs/db/gourmet_recommend.sql`，可用 IDEA 直接打开查看，也可用 mysql < docs/db/gourmet_recommend.sql 恢复。

```bash
# 启动时指定 mysql 环境（按需修改账号密码）
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
# 或在 IDEA 中设置 Active profiles = mysql
```

`application-mysql.yml` 支持环境变量：`MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DB`、`MYSQL_USER`、`MYSQL_PASSWORD`。

#### 本机已装的 MySQL 8.0.29（免安装版，替代 phpStudy 的 5.5.53）

phpStudy 自带的是 MySQL **5.5.53（2015 年，已停止维护）**，与 Connector/J 8 存在兼容风险，因此本机另装了一份官方 **MySQL 8.0.29 免安装版**：

| 项目 | 值 |
| --- | --- |
| 安装目录 | `D:\mysql8\mysql-8.0.29-winx64` |
| 配置文件 | `D:\mysql8\my.ini`（端口 3306、utf8mb4、`+08:00`、`mysql_native_password`） |
| 账号密码 | `root` / `123456` |
| 数据库 | `gourmet_recommend`（16 张表 + 演示数据已就绪） |
| 启动 | 双击 `D:\mysql8\start-mysql.bat` |
| 停止 | 双击 `D:\mysql8\stop-mysql.bat` |
| 错误日志 | `D:\mysql8\mysql-error.log` |

> 注意：phpStudy 的 MySQL 与它都监听 3306，**不要同时启动**。需要换成 Windows 服务时（管理员权限）：
> `mysqld --install MySQL80 --defaults-file=D:\mysql8\my.ini`。

### 4. 连接虚拟机内的 Redis

`dev` 环境默认指向虚拟机 **`192.168.31.63:6379`**（单密码模式，密码已写入 `application-dev.yml`，仅用于本地开发）。
Redis 在别处或使用 Redis 6+ ACL 账号时，用环境变量覆盖：

```bash
# Windows PowerShell
$env:REDIS_HOST="192.168.31.63"        # 虚拟机 IP（ip -4 addr 查看）
$env:REDIS_PORT="6379"
$env:REDIS_USERNAME=""                 # ACL 用户名；单密码模式留空
$env:REDIS_PASSWORD="你的Redis密码"
mvn spring-boot:run
```

启动日志会打印自检结果（含真实失败原因）：

```
Redis 连接成功：192.168.31.63:6379，认证用户=default(单密码)，秒杀与榜单缓存已启用
# 或
Redis 不可用（192.168.31.63:6379，用户=redis）：RedisCommandExecutionException: WRONGPASS invalid username-password pair
优惠券抢购与榜单缓存自动降级为数据库方案。
```

> 连不上时按顺序排查：① 虚拟机 IP 能否 ping 通；② 虚拟机里 `ss -lntp | grep 6379` 是否监听 `0.0.0.0`（若为 `127.0.0.1` 需改 `bind 0.0.0.0`）；③ `redis-cli -h <ip> -a <密码> ping`（或加 `--user <用户名>`）是否返回 PONG；④ 桥接/NAT 网络是否正常。
> 未配置或 Redis 不可用时，系统会**自动降级**：抢券走数据库乐观锁，榜单不走缓存，业务不中断。

### 5. 打包部署

```bash
mvn clean package -DskipTests
java -jar target/gourmet-recommend.jar --spring.profiles.active=mysql
```

---

## 四、默认账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 平台管理员 | `admin` | `admin123` |
| 普通用户 1（重辣，偏好川菜/湘菜） | `13800000001` | `123456` |
| 普通用户 2（清淡，偏好江浙菜/粤菜） | `13800000002` | `123456` |
| 普通用户 3 | `13800000003` | `123456` |
| 普通用户 4 | `13800000004` | `123456` |
| 商家（川味小厨） | `13900000001` | `123456` |
| 商家（江南味道） | `13900000002` | `123456` |
| …其余商家 | `1390000000X` | `123456` |
| 待审核商家（甜蜜烘焙坊） | `13900000009` | `123456` |

所有密码均为 BCrypt 加盐哈希存储。

---

## 五、接口清单（98 个）

> 统一前缀 `/api`，统一返回 `{ code, message, data, timestamp }`，`code = 200` 表示成功。
> 除标注「公开」的接口外，均需请求头 `Authorization: Bearer {token}`。

### 1. 用户账号（UserController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/user/register` | 注册（公开，直接返回 token） |
| POST | `/api/user/login` | 手机号 + 密码登录（公开） |
| POST | `/api/user/logout` | 退出登录 |
| GET | `/api/user/profile` | 查看个人资料 |
| PUT | `/api/user/profile` | 修改昵称 / 头像 / 联系电话 |
| PUT | `/api/user/password` | 修改登录密码 |
| GET | `/api/user/taste` | 查看口味偏好 |
| PUT | `/api/user/taste` | 设置菜系、口味轻重、忌口食材 |
| GET | `/api/user/center` | 个人中心（资料 + 统计 + 未读数） |

### 2. 商家（MerchantController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/merchant/apply` | 提交入驻申请（公开） |
| POST | `/api/merchant/login` | 商家登录（公开，需审核通过） |
| GET | `/api/merchant/profile` | 本店资料 |
| PUT | `/api/merchant/profile` | 修改店铺信息、封面、相册 |
| GET | `/api/merchant/page` | 商家分页：地区 / 菜系 / 最低评分 / 关键字 / 排序（公开） |
| GET | `/api/merchant/{id}/detail` | 商家详情，浏览量 +1，记录浏览历史（公开） |
| GET | `/api/merchant/{id}/dishes` | 旗下在售菜品（公开） |
| GET | `/api/merchant/{id}/reviews` | 商家评价列表（公开） |
| GET | `/api/merchant/statistics` | 本店数据看板 |

### 3. 菜品（DishController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/dish` | 新增菜品（商家，自动给收藏用户发“上新”消息） |
| PUT | `/api/dish/{id}` | 编辑菜品 |
| PUT | `/api/dish/{id}/status?status=0/1` | 上架 / 下架 |
| DELETE | `/api/dish/{id}` | 删除（逻辑删除） |
| GET | `/api/dish/detail/{id}` | 菜品详情（公开） |
| GET | `/api/dish/page` | 菜品分页（公开） |
| GET | `/api/dish/merchant/{merchantId}` | 某商家在售菜品（公开） |
| GET | `/api/dish/mine` | 本店菜品分页（含下架） |
| GET | `/api/dish/mine/count` | 本店菜品总数 |

### 4. 评价（ReviewController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/review` | 发布图文评价（1-5 星，最多 9 图，进入待审核） |
| GET | `/api/review/my` | 我的历史评价（含待审核） |
| GET | `/api/review/merchant/{merchantId}` | 商家评价（公开，仅审核通过） |
| GET | `/api/review/detail/{id}` | 评价详情 |
| DELETE | `/api/review/{id}` | 删除自己的评价 |
| POST | `/api/review/{id}/like` | 点赞评价（返回最新点赞数） |
| DELETE | `/api/review/{id}/like` | 取消点赞 |
| GET | `/api/review/{id}/like` | 查询点赞状态 |

### 5. 收藏 / 心愿单（FavoriteController、WishListController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/favorite/{merchantId}` | 收藏店铺 |
| DELETE | `/api/favorite/{merchantId}` | 取消收藏 |
| GET | `/api/favorite/page` | 收藏列表 |
| GET | `/api/favorite/check/{merchantId}` | 是否已收藏 |
| GET | `/api/favorite/count` | 收藏数量 |
| POST | `/api/wishlist` | 加入想去清单 |
| PUT | `/api/wishlist/{id}` | 编辑备注 / 计划日期 |
| DELETE | `/api/wishlist/{id}` | 删除条目 |
| GET | `/api/wishlist/page` | 心愿单分页 |
| GET | `/api/wishlist/count` | 心愿单数量 |

### 6. 探店笔记（NoteController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/note` | 发布笔记（绑定商家，待审核） |
| PUT | `/api/note/{id}` | 编辑（重新进入审核） |
| DELETE | `/api/note/{id}` | 删除 |
| GET | `/api/note/page` | 全网公开笔记（公开） |
| GET | `/api/note/detail/{id}` | 详情，浏览量 +1（公开） |
| GET | `/api/note/my` | 我的笔记 |
| POST | `/api/note/{id}/like` | 点赞笔记（返回最新点赞数） |
| DELETE | `/api/note/{id}/like` | 取消点赞 |
| GET | `/api/note/{id}/like` | 查询点赞状态 |

### 7. 个性化推荐（RecommendController，公开）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/recommend/home?limit=10` | 首页智能推荐（带 token 则个性化） |
| GET | `/api/recommend/similar/{merchantId}?limit=6` | 相似商家推荐 |
| GET | `/api/recommend/hot?limit=10` | 美食排行榜（Redis 缓存 5 分钟） |
| GET | `/api/recommend/cold?limit=10` | 冷启动随机热门 |

### 8. 推荐反馈 / 日志（RecommendFeedbackController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/feedback` | 喜欢 / 不感兴趣 |
| GET | `/api/feedback/my` | 我的反馈记录 |
| GET | `/api/feedback/logs/my` | 我的推荐日志 |
| GET | `/api/feedback/logs` | 全部推荐日志（管理员） |

### 9. 优惠券（CouponController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/coupon` | 创建优惠券（商家） |
| PUT | `/api/coupon/{id}` | 编辑 |
| PUT | `/api/coupon/{id}/status?status=0/1` | 上架 / 下架 |
| DELETE | `/api/coupon/{id}` | 删除 |
| GET | `/api/coupon/page` | 券分页（商家看本店 / 管理员看全部） |
| GET | `/api/coupon/center` | 领券中心：全平台可领取 / 即将开始（公开，分页） |
| GET | `/api/coupon/merchant/{merchantId}` | 商家进行中的券（公开） |
| GET | `/api/coupon/detail/{id}` | 券详情：库存、倒计时、是否已领（公开） |
| POST | `/api/coupon/{id}/seckill` | **限时抢券**：Redisson 锁 + Lua 原子扣减 + Stream 异步落库，返回排队状态 |
| GET | `/api/coupon/{id}/seckill/status` | 查询抢券处理状态（QUEUED / DONE） |
| GET | `/api/coupon/my?status=` | 我的券包：0 未使用 / 1 已使用 / 2 已过期 |
| POST | `/api/coupon/redeem` | **商家凭 12 位核销码核销**（一券一码，核销即失效） |
| GET | `/api/coupon/{id}/records` | 某张券领取记录 |
| GET | `/api/coupon/statistics` | 本店领券数据统计 |

### 10. 消息（MessageController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/message/page?isRead=` | 消息分页 |
| GET | `/api/message/unread-count` | 未读数 |
| PUT | `/api/message/{id}/read` | 标记已读 |
| PUT | `/api/message/read-all` | 全部已读 |
| DELETE | `/api/message/{id}` | 删除 |
| POST | `/api/message/broadcast` | 管理员群发 |

### 11. 统计（StatisticController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/statistic/user` | 个人探店数据（评价/收藏/领券/笔记/浏览） |
| GET | `/api/statistic/merchant` | 商家看板 |
| GET | `/api/statistic/admin/overview` | 平台总览 |
| GET | `/api/statistic/admin/merchant-rank` | 店铺热度排行 |
| GET | `/api/statistic/admin/coupon` | 优惠券领取数据 |

### 12. 平台后台（AdminController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/admin/login` | 管理员登录（公开） |
| GET | `/api/admin/user/page` | 用户列表 |
| PUT | `/api/admin/user/{id}/status` | 启用 / 禁用用户 |
| GET | `/api/admin/merchant/page` | 商家列表（含待审核） |
| PUT | `/api/admin/merchant/{id}/audit` | 审核入驻申请（通过后广播开业消息） |
| PUT | `/api/admin/merchant/{id}/status` | 正常营业 / 停业整顿 |
| GET | `/api/admin/review/page` | 评价审核列表 |
| PUT | `/api/admin/review/{id}/audit` | 通过 / 违规下架 |
| GET | `/api/admin/note/page` | 笔记审核列表 |
| PUT | `/api/admin/note/{id}/audit` | 通过 / 下架 |

### 13. 文件上传（FileController）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/file/upload` | `multipart/form-data`，字段 `file` + `type`（avatar/dish/review/note/merchant） |

---

## 六、核心设计说明

### 1. JWT 认证与角色鉴权

- 登录成功签发 JWT，载荷含 `sub`（账号 ID）、`role`（USER/MERCHANT/ADMIN）、`name`；
- `JwtAuthInterceptor` 拦截 `/api/**`：带 token 先解析写入 `UserContext`（ThreadLocal）；
- `app.security.permit-all` 配置的公开路径允许匿名访问，但**携带 token 时仍会识别登录用户**（首页推荐据此个性化）；
- 非公开路径未登录返回 `401`，`@RequireRole` 角色不匹配返回 `403`，网关侧无需再做处理。

### 2. 统一返回与全局异常

`Result<T>` + `ResultCode` 统一结构；`GlobalExceptionHandler` 覆盖业务异常、参数校验、类型错误、上传超限、404、兜底异常，避免堆栈直接暴露给前端。

### 3. 个性化推荐算法

可解释的加权打分（`RecommendServiceImpl`）：

```
raw = 综合评分 × 12
    + ln(收藏量 + 1) × 4
    + ln(评价数 + 1) × 3
    + ln(浏览量 + 1) × 1.5
    + 菜系权重 × 4          （口味偏好 3 / 收藏·高分评价·喜欢反馈 2 / 浏览 1）
    - 已收藏惩罚 6
```

- 候选集为审核通过且在营业的商家，剔除「不感兴趣」商家；
- 结果归一化到 10–99 分，返回 `recommendScore` / `recommendReason` / `recommendAlgorithm`；
- 每次推送写入 `recommend_log`（含批次号 `requestId`），支持反馈回流；
- 无口味且无任何行为时自动降级为冷启动：热度头部 20 家随机打散；
- 相似推荐按「同菜系 → 人均价差 → 评分」排序，同菜系不足时用评分相近商家补齐；
- 排行榜：`收藏×3 + 评价×2 + 浏览×0.2 + 评分×10`，Redis 缓存 5 分钟并用分布式锁防击穿。

### 4. 优惠券秒杀（技术亮点）

**三道防线：**

1. **Redis Lua 原子脚本**（`lua/seckill.lua`）：一人一单校验 → 库存判断 → 扣减 → 记录用户，单线程原子执行，杜绝超卖与重复领取；
2. **数据库兜底**：`update coupon set remain_stock = remain_stock - 1 where id = ? and remain_stock > 0`（乐观锁）+ `(user_id, coupon_id)` 唯一索引，保证最终一致；
3. **自动降级**：Redis 不可用或密码错误时，自动切换到数据库方案，业务不中断；
4. **失败补偿**：数据库落库异常时回滚 Redis 库存与用户标记。

库存预热：商家发布/上架优惠券时写入 `seckill:stock:{couponId}`，并清理 `seckill:users:{couponId}`。

### 5. 内容审核流

评价、笔记发布后状态为 `0 待审核`，管理员通过后置 `1` 并在商家侧展示；评价审核会触发商家综合评分重算（`refreshMerchantScore`），并给作者推送审核结果消息。

### 6. 消息通知

| 场景 | 触发点 | 接收者 |
| --- | --- | --- |
| 新店开业 | 管理员审核商家通过 | 全平台用户 |
| 商家新品上架 | 商家新增菜品 / 重新上架 | 收藏该店的用户 |
| 优惠券开抢 | 商家发布优惠券 | 收藏该店的用户 |
| 审核结果 | 商家入驻 / 评价 / 笔记审核 | 对应商家或用户 |
| 系统公告 | 管理员群发 | 全平台用户或商家 |

### 7. 文件上传

本地磁盘按 `type/yyyy/MM/dd/UUID.ext` 存储，`WebConfig` 把 `/uploads/**` 映射到磁盘目录；校验扩展名、Content-Type、大小（默认 10MB），文件名由服务端生成，规避路径穿越。

---

## 七、数据库表

| 表名 | 说明 |
| --- | --- |
| `sys_user` | 普通用户（手机号唯一、口味偏好、状态） |
| `sys_admin` | 平台管理员 |
| `merchant` | 商家（入驻申请复用：audit_status 0 待审核 / 1 通过 / 2 驳回） |
| `dish` | 菜品 |
| `review` | 评价（状态 0/1/2） |
| `note_like` | 笔记点赞（user_id + note_id 唯一） |
| `review_like` | 评价点赞（user_id + review_id 唯一） |
| `favorite` | 收藏（user_id + merchant_id 唯一） |
| `wish_list` | 想去清单 |
| `note` | 探店笔记 |
| `recommend_feedback` | 推荐反馈（喜欢 / 不感兴趣） |
| `recommend_log` | 推荐日志 |
| `coupon` | 优惠券（库存、抢购时段、券有效期） |
| `user_coupon` | 用户券包（user_id + coupon_id 唯一） |
| `message` | 站内消息（接收者类型区分用户/商家） |
| `browse_history` | 浏览历史（推荐依据） |

索引：手机号、商家菜系/地区/审核状态、评价与收藏外键、消息已读、浏览时间等。

---

## 八、已验证的功能

本项目已完成编译与运行验证（H2 环境），下列链路均实测通过：

- 注册 / 登录 / 改密 / 口味偏好 / 个人中心；
- 商家分页与筛选、商家详情（浏览量 +1、收藏标记）、旗下菜品；
- 登录用户首页推荐（川菜偏好命中 99 分、已拉黑商家被排除）、相似推荐、排行榜、冷启动；
- 评价发布 → 管理员审核 → 商家评分重算；笔记发布 → 审核 → 公开列表；
- 收藏 / 心愿单 / 消息未读与已读；
- 抢券成功、重复领取被拦截（409）、券包状态分类；
- 商家入驻申请 → 管理员审核 → 商家登录 → 改资料 / 菜品上下架 / 发券 / 领券记录 / 数据看板；
- 管理员总览、用户启用禁用、内容审核、热度排行、领券统计、群发消息；
- 笔记 / 评价点赞：列表回显已赞状态、点赞与取消、计数一致；
- 未登录 401、越权 403 拦截；
- **MySQL 8.0.29（本机 `D:\mysql8`）**：`gourmet_recommend` 库已含 16 张表与演示数据；`mysql` profile 下登录、商家分页、个性化推荐、管理员总览、抢券全部正常，`recommend_log` / `user_coupon` / `message` 均能正确落库。

### Redis 秒杀专项验证（真实 Redis 5.0.14 + requirepass）

用一个临时 Redis 实例（非项目依赖）跑通完整秒杀链路，实测结果：

| 场景 | 实测结果 |
| --- | --- |
| 发布 2 张券 | `seckill:stock:{id}` = 2，TTL ≈ 抢购结束 + 1 天 |
| 用户 1 第 1 次抢 | 200，Redis 库存 2 → 1，`seckill:users:{id}` 加入成员 1 |
| 用户 1 第 2 次抢 | 409「每人限领 1 张」，库存仍为 1（Lua 提前拦截，未重复扣减） |
| 用户 2 抢 | 200，库存 1 → 0 |
| 用户 3 抢 | 409「手慢了」，数据库 `remain_stock` = 0 |
| 领取记录 | 恰好 2 条，与总库存一致 |
| 1 张券 + 两用户并发 | 1 成功、1 售罄，数据库库存 0、记录 1 条（**无超卖**） |
| 榜单缓存 | `cache:hot-rank` 存在，TTL = 300s，二次调用 11ms |
| 分布式锁 | 缓存重建完成后 `lock:cache:hot-rank` 已释放 |
| 停掉 Redis 再抢券 | 200，日志提示「Redis 不可用…使用数据库乐观锁扣减库存」，库存正确递减 |

复现方式：

```bash
# 任意 Redis 5+ 实例（开启 requirepass）
# Windows: set REDIS_HOST=127.0.0.1 & set REDIS_PORT=6380 & set REDIS_PASSWORD=你的密码
export REDIS_HOST=127.0.0.1 REDIS_PORT=6380 REDIS_PASSWORD=你的密码
mvn spring-boot:run

# 观察库存与抢购用户集合
redis-cli -h 127.0.0.1 -p 6380 -a 你的密码 GET seckill:stock:1
redis-cli -h 127.0.0.1 -p 6380 -a 你的密码 SMEMBERS seckill:users:1
```

> 上述结果也已在**虚拟机 Redis（`192.168.31.63:6379`，单密码模式）**上复测通过：
> 抢券后 `DBSIZE=3`、`seckill:stock:6=0`、`seckill:users:6={1,2}`、`cache:hot-rank` TTL≈300s、
> `lock:cache:hot-rank` 已释放，与本地实例表现一致。

> 说明：MySQL 脚本未在本机执行（本机未安装 MySQL，虚拟机也未开放 3306 端口），脚本按 MySQL 8.x 语法编写并与实体字段逐一核对；H2 版脚本已随每次启动实际执行。

---

## 九、常见问题

1. **端口占用**：`server.port` 改成 8081，或结束占用 8080 的进程。
2. **Redis 认证失败**：设置 `REDIS_USERNAME` / `REDIS_PASSWORD` 环境变量（Redis 6+ ACL 账号需同时给用户名）；不设置也能跑，系统会自动降级为数据库方案，启动日志会明确提示。
3. **MySQL 连接失败**：确认库名 `gourmet_recommend`、账号密码与 `application-mysql.yml` 一致，时区 `Asia/Shanghai`。
4. **上传后图片 404**：确认 `app.upload.path` 存在写权限，访问地址以 `/uploads/` 开头。
5. **中文乱码**：IDEA 设置 `File Encodings = UTF-8`，JVM 增加 `-Dfile.encoding=UTF-8`。
6. **JWT 过期**：默认 720 分钟，可调 `app.jwt.expire-minutes`；生产必须更换 `app.jwt.secret`。

---

## 十、后续可扩展方向

- 前端：Vue3 + Element Plus（用户端 / 商家端 / 管理端三套路由）；
- 推荐：接入协同过滤或向量召回，离线跑批替换实时打分；
- 秒杀：RabbitMQ 异步下单 + 订单超时取消；
- 部署：Docker Compose / Nginx 反向代理 + HTTPS；
- 监控：Actuator + Prometheus + 日志采集；
- 对象存储：把本地文件上传替换为 MinIO / OSS。

---

## 附录 A：内置前端页面

启动后端后直接访问 <http://localhost:8080> 即可使用网页，**无需另起前端工程**（静态文件在 `src/main/resources/static`，随 Jar 一起发布）。

| 页面 | 路径 | 角色 | 主要功能 |
| --- | --- | --- | --- |
| 首页 | `/` | 全部 | 搜索、地区/菜系/评分筛选、猜你喜欢、热门榜单、商家分页 |
| 领券中心 | `/coupons.html` | 全部 / 用户 | 全平台优惠券集中领取、可领取/即将开始筛选、我的券包入口 |
| 商家详情 | `/merchant.html?id=1` | 全部 | 店铺信息、菜品、优惠券抢购、收藏/心愿单、评价与发评价、相似推荐 |
| 登录注册 | `/login.html` | 全部 | 用户登录/注册、商家登录/入驻申请、管理员登录 |
| 探险笔记 | `/notes.html` | 全部 / 用户 | 笔记瀑布流、详情弹窗、写笔记（带图） |
| 个人中心 | `/profile.html` | 用户 | 资料、口味偏好、收藏、心愿单、评价、笔记、券包、消息 |
| 商家后台 | `/console.html` | 商家 | 店铺资料、菜品增删改上下架、优惠券发布与领取记录、评价、消息 |
| 平台后台 | `/admin.html` | 管理员 | 商家审核、用户启停、评价/笔记审核、领券统计、热度榜、消息群发 |

### 前端技术说明

- **原生 HTML/CSS/JS**，零构建、零依赖，打开即用；
- `js/api.js` 统一封装 `fetch`、JWT 存取（`localStorage`）与 401 自动跳登录；
- `js/ui.js` 提供 Toast、确认框、模态框、分页、评分星级、无图占位封面等通用能力；
- 笔记 / 评价支持**点赞与取消**（♥ 按钮），状态与计数实时更新；
- 首页筛选下拉**选中即生效**，搜索会清空其他筛选避免空结果；
- 首屏无图时用「店名首字 + 暖色渐变」占位，避免空白；
- 响应式布局：窄屏时个人中心/后台的侧栏自动变成**横向滚动标签**，切换后自动滚到内容区；
- 顶部导航：首页 / 领券中心 / 探店笔记 / 榜单 / 个人中心（或商家/平台后台）；
- 所有写操作都调用后端接口，权限与审核逻辑仍以后端为准。

### 表单校验（前后端双层）

必填项带红色 `*`，不通过时输入框标红并给出具体原因；后端 DTO 用 `@Pattern / @Size / @DecimalMin` 同步兜底，直接调接口也绕不过去。

| 字段 | 规则 |
| --- | --- |
| 菜系 | 前端为下拉框 / 多选胶囊（23 个预设），后端只接受 2–10 位中英文，`1` 之类会被拒绝 |
| 地区 | 2–20 位中英文，禁止纯数字 |
| 详细地址 | 至少 5 个字且包含中文门牌 |
| 营业时间 | `10:00-22:00` 格式（可留空） |
| 手机号 / 密码 | 11 位手机号；密码 6–20 位 |
| 人均消费 / 菜品价格 | 必须 ≥ 0（菜品 > 0） |
| 优惠券 | 减免金额 ≤ 满减门槛、库存 ≥ 1、结束时间晚于开始时间 |

| 商家入驻 | 用户注册（菜系多选） |
| --- | --- |
| ![入住](docs/screenshots/08-merchant-apply.png) | ![注册](docs/screenshots/09-register.png) |

![笔记点赞](docs/screenshots/10-note-like.png)

![领券中心](docs/screenshots/11-coupon-center.png)

### 修改前端后如何生效

```bash
mvn package -DskipTests        # 静态文件打进 jar
java -jar target/gourmet-recommend.jar
```

开发期想改完立即生效，可用文件目录作为静态资源（无需重启）：

```bash
java -jar target/gourmet-recommend.jar \
  --spring.web.resources.static-locations=file:src/main/resources/static/,classpath:/static/
```
### 界面预览

截图位于 `docs/screenshots/`，可直接用于课程报告。

| 首页（推荐 / 榜单 / 筛选） | 商家详情 |
| --- | --- |
| ![首页](docs/screenshots/01-home.png) | ![商家详情](docs/screenshots/02-merchant-detail.png) |

| 登录 / 注册 / 入驻 | 探店笔记 |
| --- | --- |
| ![登录](docs/screenshots/03-login.png) | ![笔记](docs/screenshots/04-notes.png) |

| 个人中心 | 商家后台 | 平台后台 |
| --- | --- | --- |
| ![个人中心](docs/screenshots/05-profile.png) | ![商家后台](docs/screenshots/06-merchant-console.png) | ![平台后台](docs/screenshots/07-admin.png) |
---

## 附录 B：抢券架构（第三版 Redisson 锁 + 第四版 MQ 异步削峰）

### 完整链路

```
客户端
  │ POST /api/coupon/{id}/seckill
  ▼
① 活动校验（状态 / 抢购时间窗）
  ▼
② Redisson 用户级幂等锁   lock:seckill:user:{couponId}:{userId}
   tryLock(0ms, 3s)：拿不到 = 同一用户并发重复提交 → 409
  ▼
③ Redisson 券级库存锁     lock:seckill:stock:{couponId}
   tryLock(200ms, 3s)：把「判断库存 + 扣减」区间串行化，跨节点不超发
  ▼
④ Lua 原子脚本（单线程执行，不可分割）
   SISMEMBER 一人一单 → GET 库存 → 判断 → DECR → SADD 用户
   返回 1 / -1 售罄 / -2 重复 / -3 未初始化
  ▼
⑤ 成功：XADD stream:coupon:order（requestId、userId、couponId、receiveTime）
   接口立即返回 { queued: true, status: "QUEUED", requestId }
  ▼
⑥ 消费者（每 200ms，XREADGROUP COUNT 32 BLOCK 300ms）
   批量落库：user_coupon 幂等插入 + remain_stock 原子 -1 → XACK
  ▼
⑦ 兜底：Redis 不可用 / Lua 异常 → 数据库乐观锁 + (user_id,coupon_id) 唯一索引
```

### 关键实现

| 能力 | 位置 |
| --- | --- |
| Redisson 客户端与锁封装（懒加载 + 启动预热 + 自动降级） | `common/redis/RedissonLockManager.java` |
| 抢券主链路（锁 → Lua → 入队 / 落库） | `service/impl/CouponSeckillServiceImpl.java` |
| Stream 消费者（消费组、批量、ACK、退避） | `mq/SeckillOrderConsumer.java` |
| Lua 脚本 | `resources/lua/seckill.lua` |
| 异步开关与队列参数 | `app.seckill.async-enabled / stream-key / consumer-group / batch-size` |

### 压测结果（本机实测，10 个账号并发）

| 指标 | 结果 |
| --- | --- |
| 10 人并发抢 5 张券 | **5 成功 / 5 售罄 / 0 超卖**，总耗时 131 ms |
| 接口平均 RT | 86 ms（P100 129 ms） |
| 首请求 RT | 冷启动 554 ms → Redisson 预热后 **63~97 ms** |
| 异步落库 | 响应返回后 1.5 s 内券包可见（实测已生成 5 条记录） |
| 重复领取 | 409「每人限领 1 张」 |
| 数据一致性 | 领取记录 5 = 总库存 5，数据库剩余 0，Redis 库存 0，消费组 pending 0 |
| 降级链路 | 停掉 Redis 再抢券仍成功（数据库乐观锁），库存正确递减 |

复现脚本：`docs/benchmark/seckill-test.mjs`（先启动服务，再执行 `node docs/benchmark/seckill-test.mjs`）。
### 优惠券核销（一券一码）

领券成功后，`user_coupon` 会同时生成一个 **12 位唯一核销码**（`redeem_code`，唯一索引）：

```
用户券包 → 到店出示核销码（C 开头或 12 位数字）
商家后台 → 优惠券 → 核销优惠券 → 输入核销码 → 核销
```

- 核销码在「领取」时生成，落库带唯一索引，重复打印也不会重复生效；
- 商家只能核销**本店**的券，跨店核销返回 403；
- 核销是原子更新 `status=0 → 1`，并发重复核销只有一个成功，其余返回 409；
- 过期券不能核销，返回「该券已过期，无法核销」；
- 核销成功立刻回显：券名、满减、顾客昵称/手机号、核销时间。
### FAQ：IDEA 报「程序包 org.redisson 不存在」

命令行 `mvn package` 能过、只有 IDEA 报红，说明是 **IDE 没有重新导入 pom**，不是代码问题：

1. 右侧 Maven 面板 → 点 ⟳ **Reload All Maven Projects**（或右键 `pom.xml` → Maven → Reload project）；
2. 确认 `Settings → Build Tools → Maven`：
   - Maven home：`D:/maven/apache-maven-3.9.16`
   - User settings file：`C:\Users\27253\.m2\settings.xml`
   - Local repository：`D:\MavenRepository\maven\local`
3. 仍报红就 `File → Invalidate Caches / Restart`。

校验命令：`mvn dependency:tree -Dincludes=org.redisson:*` 应输出 `org.redisson:redisson:jar:3.27.2:compile`。
---

## 附录 C：性能优化与压测（实测 1957 req/s）

### 优化清单

| 优化项 | 效果 |
| --- | --- |
| 关闭 MyBatis SQL DEBUG（`com.gourmet: info`） | 去掉每请求 SQL 日志开销 |
| Tomcat：max 500 / accept 2000 / max-connections 20000 | 扛住 300+ 并发连接 |
| Hikari 20 → 50，Lettuce 16 → 64 | 异步落库与 Redis 命令不再排队 |
| Caffeine 本地缓存优惠券（3s） | 抢券入口不再每次查库 |
| Redis 可用性探测缓存 2s | 去掉每请求 PING |
| 去掉券级 Redisson 锁 | Lua 已原子，少一次 Redis 往返与锁竞争 |
| 去掉每请求 `hasKey` 探测 | 仅在 Lua 返回 -3 时补库存并重试 |
| 每请求 INFO 日志 → DEBUG | 高并发下日志是隐藏瓶颈 |
| 消费者批量 ACK + 连读 20 批（每批 500） | 6000 订单数秒内落库 |
| 用户级锁默认关闭 | Lua SISMEMBER 保证一人一单；`SECKILL_USER_LOCK=true` 可开双保险 |

### 实测（本机 16 核，300 线程 × 20 轮 = 6000 请求，库存 5000）

| 指标 | 结果 |
| --- | --- |
| 吞吐 | **1956.9 req/s**（最佳一次 2140 req/s） |
| 平均 RT | 80.7 ms |
| P50 / P90 / P95 / P99 | 71 / 151 / 196 / 258 ms |
| 错误率 | 0% |
| 数据一致性 | 6000 请求 → 恰好 5000 条领取记录，库存 0，**无超卖** |
| 对照：开启用户级 Redisson 锁 | 607 req/s，平均 403 ms |

登录接口受 BCrypt（~70ms/次，CPU 密集）限制，不适合作为 QPS 指标；高并发压测用预生成 JWT（`docs/benchmark/gen-load-users.mjs`），RT 只包含鉴权 + Redis 扣减 + 入队。

压测脚本与 HTML 报告在 `docs/benchmark/`（`report-steady/index.html`），压测数据已清理。