# Campus\_Second-hand\_Website

> 「旧的怎么办」校园二手交易平台 —— PC 网站 + 微信小程序 + Spring Boot 后端

## 项目背景

打造一款聚焦校园场景的二手交易平台，覆盖 PC 网站与移动端，为学生提供安全、便捷、高效的闲置物品交易服务。

与开放的二手平台不同，校园场景最核心的诉求是**可信**。因此平台以「校园认证 + 平台审核 + 交易治理」为设计主线：用户需完成学号 / 校区认证后才能发布与下单，商品上架需经管理员审核，并配套举报、纠纷仲裁与双向评价机制，形成从发布、交易到售后仲裁的完整闭环。

## 技术路线

整体采用「Vue 3 PC 端 + 原生微信小程序 + Spring Boot 单体后端」的**三端一后端**架构，三端共用同一套 REST 接口与账号体系。

### 1. 后端（Spring Boot）

| 层次  | 方案                                            |
| --- | --------------------------------------------- |
| 框架  | Spring Boot 3.5.0 + JDK 21                    |
| 持久层 | MyBatis 3.0.4 注解式 SQL（无 XML）、MySQL 8，下划线自动转驼峰 |
| 鉴权  | 自研 JWT（jjwt 0.12.6，HS256），登录态 72 小时           |
| 密码  | MD5 加盐存储（SecureRandom 随机盐）                    |
| 文件  | FileStorageService，后缀白名单校验 + UUID 命名 + 静态资源映射 |
| 调度  | `@EnableScheduling`，定时关闭超时未支付订单               |

**分层与统一约定**：严格 Controller → Service → Mapper 三层；所有接口统一返回 `ApiResponse{success, code, message, data}`，由 `GlobalExceptionHandler` 统一处理业务异常（400）、参数校验异常与兜底异常（500）。

**鉴权链路**：`JwtTokenProvider` 生成 / 解析 Token → `AuthInterceptor` 拦截 `/api/**`，校验通过后把用户身份写入 `UserContext`（ThreadLocal），请求结束回收；仅商品列表、求购列表等少数 GET 接口对游客开放。写操作（发布 / 下单 / 求购）额外要求通过校园认证（`realname_status = VERIFIED`）。

**数据库自愈能力**：`SchemaUpgradeRunner` 在应用启动时执行幂等迁移——补齐缺失字段、兼容历史字段命名、清理指向旧表的外键，并初始化管理员账号。这使后端可以平滑运行在结构不一致的旧库之上。

**功能模块**（15 个 Controller）：认证、商品、我的发布、订单、消息、收藏、浏览足迹、求购、评价、纠纷、举报、管理员统计与商品审核、用户管理、个人中心。

**数据模型**（共 18 张表）：核心为 `user_account`、`user_auth`、`item`、`item_image`、`trade_order`、`conversation`、`message`、`review`、`favorite`、`browse_history`、`wanted_post`、`report_record`、`dispute_record`。

### 2. PC 网页端（Vue 3）

- **技术栈**：Vue 3.5（`<script setup>` 组合式 API）+ Vue Router 4.6 + Vite 8，无第三方 UI 库，界面为手写 CSS 实现的电商风格。
- **状态与请求**：不使用 Pinia/Vuex，而是以单个 composable（`useMarketplace`）作为模块级 `reactive` 单例充当全局 store；请求基于原生 `fetch` 封装，自动注入 `Bearer Token`、统一解包响应体、401 自动登出，另提供 `requestForm` 处理 multipart 上传。
- **本地持久化**：Token、用户信息、购物车、收货地址、聊天历史均保存在 `localStorage`。
- **页面**（10 个路由）：首页、商品列表、商品详情、购物车、发布中心、订单中心、消息中心、个人中心、微信登录入口、管理员后台。
- **交互细节**：筛选条件与 URL query 双向同步、游客/登录双套首页视图、购物车多选与批量结算、订单 30 分钟支付倒计时、管理员仪表盘与审核 / 纠纷 / 用户治理面板。

### 3. 微信小程序（原生）

- **技术栈**：原生小程序（未使用 Taro / uni-app），9 个页面、5 个 tabBar（首页 / 商品 / 发布 / 购物车 / 我）。
- **请求封装**：`wx.request` 的 Promise 化（自动带 Token、401 清会话）、`wx.uploadFile` 图片上传、`wx.login` 换取登录态。
- **会话管理**：`wx` Storage 统一封装，`app.js` 提供 `setSession / restoreSession / clearSession`。
- **能力覆盖**：首页推荐与求购、商品浏览 / 收藏 / 加购、详情下单与举报、发布商品（最多 6 图）、购物车结算、订单全状态流转（含 1 秒支付倒计时）、消息沟通、校园认证。

小程序端与 PC 端共享同一套后端接口与数据规范化工具，保证两端字段与展示逻辑一致。

### 4. 核心交易流程

```
游客浏览商品/求购 → 注册登录 → 校园认证 → 发布商品（管理员审核）
→ 浏览/收藏/加购 → 下单 → 支付 → 发货 → 收货 → 评价
→（可选）纠纷/举报 → 管理员仲裁
```

### 技术栈

| 端     | 技术                                                                               |
| ----- | -------------------------------------------------------------------------------- |
| 后端    | Spring Boot 3.5.0、JDK 21、MyBatis 3、MySQL 8、JJWT 0.12.6、Spring WebSocket、Actuator |
| PC 前端 | Vue 3.5、Vue Router 4.6、Vite 8、原生 fetch、localStorage                              |
| 小程序   | 原生微信小程序（wx.request / wx.uploadFile / wx Storage）                                 |

### 目录结构

```
Campus_Second-hand_Website/
├── README.md
└── “旧的怎么办”/
    ├── pom.xml                     # 后端 Maven 构建
    ├── package.json / vite.config.js / index.html
    ├── miniprogram/                # 微信小程序
    │   ├── app.js / app.json / app.wxss
    │   ├── pages/                  # 9 个页面
    │   └── utils/                  # 请求 / 存储 / 工具封装
    └── src/
        ├── main.js / App.vue / style.css
        ├── router/                 # 路由表
        ├── pages/                  # 10 个 PC 页面
        ├── components/             # 首页视图、举报弹窗
        ├── composables/            # useMarketplace 状态与请求中枢
        └── main/
            ├── java/com/campus/trade/   # Spring Boot 后端（controller/mapper/security/service/websocket）
            └── resources/               # application.yml、db/schema.sql
```

## 我的贡献

**项目经理 / 产品负责人 / 小程序端开发负责人**

1. **项目管理**：负责全周期进度与里程碑拆解、风险管控与团队协调，建立周报机制，统筹答辩与交付材料的输出；
2. **产品管理**：完成需求梳理与 PRD 文档输出，绘制原型并定义页面结构与交互规则，管理需求变更与优先级，确立「校园认证 + 商品审核 + 交易治理」的产品主线；
3. **小程序端开发**：独立完成微信小程序全流程开发——9 个页面与 5 个 tabBar 的搭建、基于 `wx.request` 的请求封装与 Token 会话管理、商品浏览 / 收藏 / 加购、发布（最多 6 图上传）、购物车结算、订单全状态流转（含支付倒计时）、消息沟通与校园认证等功能的实现与兼容性适配；
4. **联调统筹**：牵头前后端与双端联调测试，统一接口契约与字段规范，推动核心交易链路（发布 → 下单 → 支付 → 发货 → 收货 → 评价 → 纠纷仲裁）从开发到闭环的全流程验证。

