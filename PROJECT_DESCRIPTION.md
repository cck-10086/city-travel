# 项目技术说明

## 项目定位

城市轻旅行攻略与预订小站是一个 Spring Boot 单体应用，前端静态资源与 REST API 由同一服务提供。系统围绕游客、商家、管理员三类角色，实现城市旅行内容浏览、商家发布、平台审核、订单预订和 AI 行程规划。

## 模块结构

```text
src/main/java/com/citytravel/citytravel
├── common       统一响应
├── config       Security、Redis、MVC、异常处理与 AI 成本配置
├── controller   用户、内容、订单、评论、收藏、统计与 AI 接口
├── entity       MyBatis-Plus 实体
├── mapper       数据访问接口
├── service      业务服务、缓存和 AI 调用
└── util         订单状态等工具
```

## 数据模型

根目录 `schema.sql` 定义 10 张表：

| 表 | 用途 |
|---|---|
| `user` | 用户、角色和审核状态 |
| `category` | 景点、民宿和路线分类 |
| `attraction` | 景点内容与商家归属 |
| `homestay` | 民宿内容与商家归属 |
| `route` | 路线内容与商家归属 |
| `orders` | 预订订单与状态流转 |
| `strategy` | 用户旅行攻略 |
| `comment` | 评论与回复关系 |
| `favorite` | 攻略及旅行商品收藏 |
| `order_evaluation` | 已完成订单评价 |

## 安全边界

- 匿名用户可访问登录、注册、公开内容查询、本地行程生成和 AI 可用状态。
- 登录用户可使用个人中心、评论、收藏、订单及 AI 行程生成。
- 已审核商家可管理本人旅行商品和对应订单。
- 管理员可访问 `/api/admin/**`、`/api/statistics/**`、审核接口和 AI 成本统计。
- 所有凭据通过环境变量注入；演示账号初始化默认关闭。

## 运行与验证

环境配置、启动命令和测试方式见根目录 `README.md`。项目使用 Maven Wrapper，执行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

完整联调需要本地 MySQL 和 Redis；单元及 Web 安全测试不依赖外部数据库、缓存或真实 AI Key。
