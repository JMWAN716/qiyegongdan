# 🕹️ 企业工单管理系统（Qiyegongdan）

一个前后端分离的**企业级工单管理系统**，覆盖工单从**创建、分配、处理、跟踪到关闭**的全流程生命周期管理。
前端采用 **8-bit 复古像素风格** 的响应式设计，后端提供 **RESTful API** 并基于 **Spring Boot** 实现数据持久化，
内置 **JWT 认证、RBAC 权限控制、操作日志、自定义字段、自动分配策略** 等企业级能力。

---

## ✨ 核心功能

### 工单全流程管理
- **创建工单**：支持多种工单类型、多级优先级、灵活的**自定义字段**
- **工单分配**：支持**自动分配**（轮询 `ROUND_ROBIN` / 最少负载 `LEAST_LOAD`）与**手动指定处理人**
- **工单处理**：完整状态流转、评论回复、附件上传（20MB 上限）、工单流转日志
- **工单查询**：多维筛选（状态 / 优先级 / 类型 / 处理人 / 关键词）+ 分页
- **统计报表**：数据看板（状态分布、趋势、分类统计）可视化

### 用户与权限（RBAC）
- 支持 **ADMIN / MANAGER / AGENT / REPORTER** 四类角色
- 基于角色的菜单与接口级权限控制（Spring Security + `@PreAuthorize`）
- 用户管理：增删改查、启停、角色分配；数据可见范围按权限隔离

### 企业级安全
- **JWT** 无状态认证（`jjwt`，可配置过期时间）
- **操作日志**：对用户增删改、工单操作等关键行为自动审计
- 全局异常处理、参数校验（Bean Validation）、`bcrypt` 密码加密

---

## 🛠️ 技术栈

| 分层 | 技术 |
| ---- | ---- |
| 后端框架 | Spring Boot **4.1.1**、Spring MVC |
| 安全 | Spring Security + JWT（jjwt 0.12.6） |
| 持久化 | Spring Data JPA、Hibernate |
| 数据库 | H2（文件持久化，可切换 MySQL） |
| 校验 | Bean Validation（Hibernate Validator） |
| 构建 | Gradle（Java **26** toolchain） |
| 前端 | 原生 HTML / CSS / JavaScript（单页应用） |

---

## 📁 项目结构

```text
qiyegongdan/
├── build.gradle                     # Gradle 构建与依赖
└── src/main/
    ├── java/com/example/qiyegongdan/
    │   ├── config/                  # SecurityConfig、DataSeeder（种子数据）
    │   ├── controller/              # REST 控制器（用户/工单/类型/登录/统计/日志/附件）
    │   ├── service/                 # 业务逻辑（工单、分配、统计、审计、文件等）
    │   ├── repository/              # Spring Data JPA 数据访问层
    │   ├── entity/                  # JPA 实体（工单、用户、评论、附件、日志、类型等）
    │   ├── dto/                     # 请求/响应数据对象
    │   ├── enums/                   # 状态、优先级、角色、字段类型枚举
    │   ├── security/                # JWT 过滤器、认证与工具类
    │   └── exception/               # 全局异常与统一错误处理
    └── resources/
        ├── application.properties   # 全部配置项
        └── static/                  # 前端（index.html、css/、js/）
```

---

## 🚀 快速开始

### 环境要求
- **JDK 26**（项目使用 Java 26 toolchain）
- Gradle（可使用项目自带 `gradlew.bat`）

### 运行步骤

```powershell
# 1. 设置 JDK 路径（按本地实际 JDK 所在目录调整）
$env:JAVA_HOME="C:\Users\32318\.jdks\openjdk-26.0.2.1"

# 2. 启动应用（默认端口 8080）
.\gradlew.bat bootRun
```

### 访问入口
| 入口 | 地址 |
| ---- | ---- |
| 系统前端 | http://localhost:8080/ |
| H2 控制台 | http://localhost:8080/h2-console |
| 接口文档 | API 见下方「接口概览」（RESTful JSON） |

### 演示账号（由 `DataSeeder` 自动创建）
| 账号 | 密码 | 角色 |
| ---- | ---- | ---- |
| `admin` | `admin123` | ADMIN |
| `manager` | `123456` | MANAGER |
| `agent1` | `123456` | AGENT |
| `reporter1` | `123456` | REPORTER |

> 默认管理员用户名/密码可通过 `app.seed-admin-username` / `app.seed-admin-password` 配置。

---

## 🗄️ 数据库与配置

默认使用 **H2 文件数据库**，数据持久化到 `./data/qiyegongdan`（重启不丢数据）；上传附件存于 `./data/uploads`。

核心配置（`src/main/resources/application.properties`）：

```properties
server.port=8080
spring.datasource.url=jdbc:h2:file:./data/qiyegongdan;AUTO_SERVER=TRUE
spring.jpa.hibernate.ddl-auto=update
app.jwt.secret=<JWT 密钥>        # 生产环境务必修改
app.jwt.expiration=86400000      # token 有效期（毫秒）
app.upload-dir=./data/uploads
```

> ⚠️ 生产部署请更换 `app.jwt.secret`、设置数据库密码，并考虑将数据库切换为 MySQL。

---

## 🧮 业务模型

### 工单状态流转
状态机按**相邻顺序**推进，非法跳转将被拒绝：

```
OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED
   └────────── └→ CANCELED（仅 OPEN / ASSIGNED 可取消）
```

| 枚举值 | 说明 |
| ------ | ---- |
| `OPEN` | 已提交待分配 |
| `ASSIGNED` | 已分配处理人 |
| `IN_PROGRESS` | 处理中（未分配不可直接开始） |
| `RESOLVED` | 已解决 |
| `CLOSED` | 已关闭 |
| `CANCELED` | 已取消 |

### 优先级
`LOW`、`MEDIUM`、`HIGH`、`URGENT`

### 自定义字段类型
`TEXT`（文本）、`NUMBER`（数字）、`SELECT`（下拉）、`DATE`（日期）、`BOOLEAN`（布尔）

### 自动分配策略
- **ROUND_ROBIN**：轮询分配处理人（默认）
- **LEAST_LOAD**：分派给当前未完工单最少的处理人

---

## 🔐 权限矩阵（RBAC）

| 功能 | ADMIN | MANAGER | AGENT | REPORTER |
| ---- | :---: | :-----: | :---: | :------: |
| 数据看板 / 统计 | ✅ | ✅ | ✅ | ✅ |
| 创建工单 | ✅ | ✅ | ✅ | ✅ |
| 全量工单查询/分配 | ✅ | ✅ | 仅本人/上报 | 仅本人 |
| 处理/回复/上传 | ✅ | ✅ | 负责人 ⚠️ | 本人 |
| 用户管理（增删改） | ✅ | ❌ | ❌ | ❌ |
| 工单类型管理 | ✅ | ✅ | ❌ | ❌ |
| 操作日志查看 | ✅ | ❌ | ❌ | ❌ |

> 说明：AGENT 仅能查看、处理分配给自己的工单及其上报的工单；REPORTER 仅能查看自己上报的工单。

---

## 🧭 接口概览（REST API）

所有接口返回统一包装结构：`{ "success": true, "message": "...", "data": ... }`。

认证接口需在请求头携带：`Authorization: Bearer <token>`

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/api/auth/login` | 登录（返回 JWT） |
| POST | `/api/auth/register` | 注册（默认 REPORTER） |
| GET | `/api/auth/me` | 当前登录用户信息 |
| GET | `/api/users` | 用户列表（角色筛选） |
| POST | `/api/users` | 创建用户（ADMIN） |
| PUT | `/api/users/{id}` | 更新用户（ADMIN） |
| DELETE | `/api/users/{id}` | 删除用户（ADMIN） |
| GET | `/api/users/agents` | 处理人列表 |
| GET | `/api/tickets` | 工单分页查询（多维筛选） |
| POST | `/api/tickets` | 创建工单（可自动分配） |
| GET | `/api/tickets/{id}` | 工单详情 |
| PUT | `/api/tickets/{id}/assign` | 指派/重新分配 |
| PUT | `/api/tickets/{id}/status` | 状态流转 |
| POST | `/api/tickets/{id}/comments` | 添加评论 |
| GET | `/api/tickets/{id}/comments` | 评论列表 |
| GET | `/api/tickets/{id}/attachments` | 附件列表 |
| POST | `/api/tickets/{id}/attachments` | 上传附件（multipart） |
| GET | `/api/attachments/download/{id}` | 下载附件 |
| GET | `/api/ticket-types` | 工单类型列表 |
| POST | `/api/ticket-types` | 新建工单类型 |
| GET | `/api/ticket-types/{id}/fields` | 类型的自定义字段定义 |
| POST | `/api/ticket-types/{id}/fields` | 添加自定义字段 |
| GET | `/api/statistics/dashboard` | 看板统计 |
| GET | `/api/statistics/trend` | 趋势统计 |
| GET | `/api/logs` | 操作日志分页查询 |

---

## 🖥️ 前端说明

- 单页应用（`static/index.html` + `js/app.js`），按角色动态渲染菜单与视图
- **8-bit 像素风设计**：直角硬边框、像素投影、按压反馈按钮、像素字体（`Press Start 2P`，离线自动回退）
- 响应式布局：窄屏自动收起侧边栏为抽屉式导航，适配移动设备

---

## 🔮 后续可扩展方向

- 接入 MySQL / PostgreSQL，实现多实例部署
- 邮件/站内通知（工单分配、状态变化提醒）
- 工单 SLA 与超时自动升级、数据导出 Excel
- 单元测试与 CI/CD 集成、Swagger/OpenAPI 接口文档