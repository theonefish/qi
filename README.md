# 蜀漆云馆

漆器文化数字展馆：以章节叙事为主线，串联藏品、工艺工序、传承人、历史事件、术语与纹饰，支持分支抉择式阅读；后台负责内容编辑、审核与发布，小程序端为匿名访客提供浏览、收藏、进度与浏览记录。

系统由三部分组成：**Java 后端服务** 提供 REST 接口，**管理后台** 负责内容生产，**小程序端** 面向访客展示。

## 功能总览

| 端 | 面向对象 | 核心能力 |
| --- | --- | --- |
| 管理后台 `fronted/shuqi-admin` | 内容管理员 | 登录、数据总览、章节与分支叙事编排、藏品/工艺/传承人/术语/历史事件/纹饰全流程管理、内容关联、媒体上传、发布记录 |
| 小程序端 `fronted/漆器云馆` | 匿名访客 | 首页与云展馆、章节分支阅读、藏品/工艺/传承人/纹饰浏览、术语释义、收藏、断点续读、参观进度、浏览记录、行为上报 |
| 后端 `qi` | 两端服务 | REST API、Sa-Token 鉴权、统一响应、发布校验与发布快照、文件存储、访客数据与行为统计 |

## 仓库结构

| 目录 | 说明 | 主要技术 |
| --- | --- | --- |
| `qi/` | 后端 REST 服务 | Java 21、Spring Boot 4.1.2、MyBatis-Plus 3.5.14、Sa-Token 1.45.0、springdoc 3.2.0、MySQL 8.0 |
| `fronted/shuqi-admin/` | 管理后台前端 | Vue 3、Vite 5、Element Plus、wangEditor、Vue Flow（分支图编排） |
| `fronted/漆器云馆/` | 小程序端 | uni-app（原生 `.vue` + `uni.request`） |
| `qi/sql/` | 建库与升级脚本 | MySQL 8.0 / utf8mb4 |
| `qi/docs/` | 接口手册与设计文档 | — |

## 环境要求

- JDK 21、Maven 3.9+（或 IDE 内置 Maven）
- MySQL 8.0
- Node.js 18+（管理后台）
- HBuilderX / 微信开发者工具（小程序端）

## 管理后台功能

### 页面与路由

| 路由 | 页面 | 功能 |
| --- | --- | --- |
| `/login` | 登录 | 账号密码登录，token 与管理员名称存入 localStorage；未登录访问其他页面自动跳回登录页 |
| `/overview` | 数据总览 | 7 张统计卡（已发布正章、已发布藏品、草稿藏品、已发布工艺、已发布传承人、到访访客数、今日行为事件）+ 章节参观行为表（进入/节点浏览/完成/回到主线） |
| `/chapters` | 章节管理 | 按标题与状态筛选、分页；新增章节、发布、下架、删除、查看发布记录 |
| `/chapters/:id/edit` | 章节编辑 | 章节信息维护 + 叙事节点列表与节点编辑，右侧小程序实时预览 |
| `/chapters/:id/flow` | 分支编排 | 画布式节点摆位与连线、结构实时校验、发布章节 |
| `/artifacts` | 藏品管理 | 按名称/类别/状态筛选、分页；审核、发布、下架、删除 |
| `/artifacts/new`、`/artifacts/:id/edit` | 藏品编辑 | 档案信息、主图、三段富文本、关联管理、发布记录、审核与发布，右侧实时预览 |
| `/categories` | 类别字典 | 查询与新增藏品类别（受控字典） |
| `/crafts` | 工艺管理 | 列表、新增、发布、下架、删除 |
| `/crafts/new`、`/crafts/:id/edit` | 工艺编辑 | 概述/特色/适用范围、工序增删改与排序、关联藏品、发布，右侧实时预览 |
| `/inheritors` | 传承人管理 | 列表、审核、发布、下架、删除 |
| `/inheritors/new`、`/inheritors/:id/edit` | 传承人编辑 | 照片、简介、从艺经历、传承故事、来源；审核、关联代表作品与擅长技艺、发布，右侧实时预览 |
| `/terms`、`/terms/new`、`/terms/:id/edit` | 专业术语 | 名称、释义、来源；审核、发布、下架、删除 |
| `/events`、`/events/new`、`/events/:id/edit` | 历史事件 | 时间/时期、名称、介绍、来源；发布、下架、删除，右侧时间轴预览 |
| `/motifs`、`/motifs/new`、`/motifs/:id/edit` | 纹饰资料 | 名称、图片、文化解释、来源；发布、下架、删除，右侧实时预览 |
| `/media` | 媒体资源 | 上传媒体文件，按类型与名称筛选、分页浏览、图片预览 |

### 内容生产流程

1. **新建即草稿**：新建内容 `status=0`，审核类模块同时 `auditStatus=0`。
2. **编辑保存**：保存不改变发布状态；已发布内容编辑保存后**即时生效**（D-17 简化口径），发布校验由发布流程承担。
3. **审核**：藏品、传承人、术语需管理员审核（通过/不通过），审核人取当前登录账号。
4. **发布**：必须填写「修改内容说明」，后端按模块校验，通过后 `status=1`，生成发布记录与内容快照。
5. **下架**：`status=2`，小程序端不再返回该内容。
6. **发布记录**：记录发布中/成功/失败/回滚状态、操作人、时间、修改说明与快照；章节与藏品可在页面查看。

### 各模块要点

- **章节**：区分序章（不计入参观进度）与正章；维护标题、副标题、封面、摘要、排序值。节点支持 5 种类型：`TEXT` 图文、`TIMELINE` 时间轴、`QUOTE` 人物口述、`PROCESS` 工序步骤、`BRANCH` 分支抉择；节点可上下移动、删除。
- **节点编辑**：图文节点用富文本编辑，并可插入 `[[term:ID]]` 术语标记；时间轴节点选择已发布的历史事件；口述节点关联传承人并填写口述原文与来源；工序节点选择工艺及具体工序（为空表示整工艺概览）；分支节点维护引导文案与选项表（按钮文案 + 目标节点）。
- **分支编排**：Vue Flow 画布，拖拽节点摆位、拖把手连线建选项/覆盖边；区分默认边（按排序自动链）、覆盖边、选项边；画布坐标批量保存；内置结构预检（无环、无孤立节点、收尾节点唯一、分支至少两个选项），发布时后端再做强校验。
- **藏品**：类别、名称、年代、材质、主图、资料来源与授权说明；工艺说明/文化故事/纹饰寓意三段富文本；关联章节、工艺、传承人；发布前必须已审核且至少关联一个已发布的章节或工艺。
- **工艺**：概述、特色、适用范围、来源；工序流程支持新增、编辑、上下移动、删除与配图；关联藏品；发布条件为名称、概述、适用范围、至少一道含名称与简介的有效工序。
- **传承人**：照片、简介、从艺经历、传承故事、来源；审核通过后可发布；关联代表作品（藏品）与擅长技艺（工艺）。
- **历史事件**：时间或时期、事件名称、事件介绍、资料来源；发布后可被章节时间轴节点引用。
- **术语**：名称、释义、来源；审核通过后可发布；发布后小程序正文中的术语标记才可点击查看释义。
- **纹饰**：名称、图片、文化解释、来源；发布后在小程序「纹饰主题」页展示，也可作为章节/藏品的关联内容。
- **媒体资源**：上传后保存相对地址 `/files/...`，记录名称、类型、大小、来源与授权说明；支持分页与类型/关键字筛选。
- **关联管理**：内容之间可维护多类型关联（章节、藏品、工艺、传承人、术语、事件、纹饰）；小程序只展示目标仍有效的关联。

## 小程序端功能

### 导航结构

底部 4 个 Tab：**首页、云展馆、藏品、我的**；工艺、传承人、纹饰、收藏、进度、浏览记录等通过首页快捷入口或页面内跳转进入。

### 页面功能

| 页面 | 功能 |
| --- | --- |
| 首页 | 品牌主视觉；「开始参观 / 继续参观」（断点续读到上次章节与节点）；参观进度条；云展馆、藏品、漆艺工艺、传承人、我的收藏、参观进度、纹饰主题快捷入口；语音讲解占位；展馆简介 |
| 云展馆 | 正章完成进度（序章不计入分母）；章节列表（封面、序章/章号、已完成/读到此章标记）；按推荐顺序参观；下拉刷新 |
| 章节阅读 | 阅读进度、章节主视觉与摘要、节点快捷导航（滚动高亮、浏览全部）；按类型渲染图文、时间轴、口述、工序折叠卡片；分支节点强制选择，可「重选」「浏览全部」，未选路线灰显；术语点击查看释义；章节关联卡；「回到主线」；完成本章 |
| 藏品列表 | 类别筛选（仅展示有已发布藏品的类别）、滚动分页加载、最新添加排序 |
| 藏品详情 | 主图、类别/年代/材质、工艺说明/文化故事/纹饰寓意、收藏/取消收藏、相关内容跳转、回到主线、资料来源 |
| 漆艺工艺 | 工艺列表；详情展示概述、特色、适用范围、工序流程（含配图）、关联藏品与来源 |
| 传承人 | 人物列表；详情展示照片、简介、从艺经历、传承故事、代表作品、擅长技艺与来源 |
| 纹饰主题 | 纹饰图片、名称、文化解释与来源 |
| 我的收藏 | 已收藏藏品列表（含收藏时间），点击进入藏品详情 |
| 参观进度 | 全部已发布章节的完成状态与断点位置，点击从上次节点继续 |
| 浏览记录 | 章节/藏品/工艺/传承人浏览足迹，按日期分组倒序，支持清空、下拉刷新（P1） |
| 我的 | 进度与收藏概览、收藏/进度/浏览记录入口、展馆介绍、资料来源与版权、数据与隐私说明；意见反馈占位 |

### 匿名访客机制

- 首次启动生成 `anonymousId`（UUID）并长期保存在本地，作为收藏、进度、浏览记录与行为统计的唯一标识。
- 每次冷启动生成新的 `visitId`，用于会话级行为去重。
- 进入章节、浏览节点、点击关联、回到主线、选择分支等行为按 2 秒窗口攒批上报，失败静默丢弃，不阻塞阅读；完成章节由「完成本章」接口内部记录。
- 阅读位置、分支选择路径随进度保存，再次进入可从上次节点继续并还原分支路线。
- 收藏当前仅支持藏品；内容下架或删除后，收藏、浏览记录与关联卡自动隐藏。
- 首页与「我的」页数据统计口径：只统计已发布正章，序章不计入进度分母。

## 后端功能

### 分层与模块

后端按业务领域分包，统一位于 `qi/src/main/java/com/example/qi/`：

| 模块 | 职责 |
| --- | --- |
| `auth` | 管理员登录、退出、当前账号、默认管理员初始化（BCrypt） |
| `artifact` | 藏品类别字典、藏品 CRUD、审核、发布、下架、关联、发布记录 |
| `chapter` | 章节 CRUD、叙事节点（TEXT/TIMELINE/QUOTE/PROCESS/BRANCH）、节点排序、分支选项、流向覆盖、画布坐标、结构校验 |
| `craft` | 工艺与工序步骤 CRUD、排序、发布、关联 |
| `culture` | 历史事件管理 |
| `inheritor` | 传承人管理、审核、发布、关联 |
| `term` / `motif` | 术语与纹饰管理、审核、发布 |
| `media` / `relation` / `publish` | 媒体上传与查询、跨内容关联、发布记录与快照 |
| `visitor` / `stats` | 访客收藏、进度、浏览记录、行为上报，后台统计 |
| `common` / `config` / `storage` | 统一响应、异常处理、状态枚举、鉴权与跨域配置、本地文件存储 |

### 鉴权与响应约定

- `/admin/**` 需登录，请求头 `Authorization: <token>`（无 `Bearer` 前缀，Sa-Token），唯一例外 `POST /admin/auth/login`；`/app/**` 全部匿名。
- 统一响应体 `{ "code": 0, "msg": "success", "data": ... }`；`code=0` 成功，`msg` 可直接展示。
- 分页参数 `pageNum`/`pageSize`，返回 `{ list, total, pageNum, pageSize }`。
- 内容 `status`：0 草稿 / 1 已发布 / 2 已下架；发布记录 `publishStatus`：0 发布中 / 1 成功 / 2 失败 / 3 回滚成功 / 4 回滚失败。
- 时间格式 `yyyy-MM-dd HH:mm:ss`；上传文件相对地址通过 `/files/**` 访问。

### 发布校验规则

| 模块 | 校验条件 |
| --- | --- |
| 章节 | 标题、封面、摘要、章节类型、排序值必填；至少一个有效叙事节点；分支图无环、入口可达全部节点、收尾节点唯一；BRANCH 至少两个选项且文案非空、目标合法且不重复 |
| 藏品 | 名称、类别、主图、资料来源必填；已审核；至少关联一个已发布的章节或工艺 |
| 工艺 | 名称、概述、适用范围必填；至少一道有效工序（名称与简介齐全） |
| 传承人 | 姓名、简介、照片、资料来源必填；已审核 |
| 术语 | 名称、释义必填；已审核 |
| 历史事件 | 时间、名称、事件介绍、资料来源必填 |
| 纹饰 | 名称、文化解释、资料来源必填 |

### 文件存储与统计

- 上传白名单：jpg/jpeg/png/gif/webp/bmp/mp3/mp4/wav/m4a/pdf；单文件 ≤ 20MB。
- 本地存储目录由 `file.storage-dir` 指定，按日期分目录，映射为 `/files/**`；`FileStorageService` 接口便于后续替换对象存储。
- 统计接口：总览（已发布数量、访客数、今日事件数）、章节行为统计（进入/节点浏览/完成/回到主线）、关联跳转 Top 50。

### 接口分组

| 分组 | 路径前缀 | 代表能力 |
| --- | --- | --- |
| 认证 | `/admin/auth` | 登录、退出、当前管理员 |
| 统计 | `/admin/stats` | 总览、章节行为、关联跳转 |
| 章节 | `/admin/chapters` | CRUD、节点、排序、关联、发布、下架、流程数据与画布坐标 |
| 藏品 | `/admin/artifacts`、`/admin/artifact-categories` | CRUD、审核、关联、发布、下架、发布记录 |
| 工艺 | `/admin/crafts` | CRUD、工序 CRUD 与排序、关联、发布、下架 |
| 传承人 | `/admin/inheritors` | CRUD、审核、关联、发布、下架 |
| 文化资料 | `/admin/terms`、`/admin/timeline-events`、`/admin/motifs` | CRUD、审核（术语）、发布、下架 |
| 媒体与关联 | `/admin/media`、`/admin/relations` | 上传、分页查询、按来源/目标查关联 |
| 小程序 | `/app/chapters`、`/app/artifacts`、`/app/crafts`、`/app/inheritors`、`/app/terms`、`/app/motifs` | 仅返回已发布内容 |
| 访客 | `/app/visitor` | 摘要、进度读写、收藏、浏览记录、行为上报 |

完整请求体字段、响应结构与错误文案见 [qi/docs/API.md](qi/docs/API.md)。

## 数据库

`qi/sql/init.sql` 自带 `CREATE DATABASE shuqi_cloud_museum`，共 24 张表：

| 分组 | 表 |
| --- | --- |
| 认证 | `admin_user` |
| 章节叙事 | `chapter`、`chapter_node`、`node_quote`、`node_event_relation`、`node_craft_relation`、`node_media_relation`、`node_choice` |
| 内容 | `artifact`、`artifact_category`、`craft`、`craft_step`、`inheritor`、`timeline_event`、`term`、`motif` |
| 媒体与关联 | `media_asset`、`content_relation` |
| 发布 | `publish_record` |
| 访客 | `user_favorite`、`user_progress`、`browse_history`、`behavior_event` |
| 规划中 | `page_config` |

## 演示图片素材

演示数据中的章节封面、藏品主图、传承人照片、纹饰图与章节内文配图，均已替换为与展馆视觉（朱漆 / 黑漆 / 描金）一致的主题插画，不依赖外网图床。

- 存放位置：`qi/src/main/resources/static/seed/`，启动后通过 `http://localhost:8080/seed/<name>.png` 访问。
- 生成脚本：`qi/seed-assets/generate_seed_images.py`（Python 3 + Pillow + NumPy），重新生成：

  ```bash
  python qi/seed-assets/generate_seed_images.py
  ```

- 旧演示库升级：执行 `qi/sql/upgrade-20261113-seed-images.sql`。
- 实体表存相对地址（前端 `fullUrl()` 拼接 `BASE_URL`）；章节富文本内联图为满足 `rich-text` 渲染，写入的是绝对地址，换域名部署时需同步替换。

| 文件组 | 用途 | 尺寸 |
| --- | --- | --- |
| `chapter-main`、`chapter-branch`、`chapter-art` | 章节封面 | 1500×840 |
| `art-box`、`art-dish`、`art-screen` | 藏品主图 | 1200×900 |
| `inheritor-1`、`inheritor-2` | 传承人照片 | 800×800 |
| `motif-cloud`、`motif-grass`、`motif-bird` | 纹饰图片 | 900×720 |
| `node-tree`、`node-trip`、`node-deco`、`node-ending`、`node-view`、`node-life`、`node-art` | 章节内文配图 | 1200×700 |

## 快速开始

### 1. 初始化数据库

新环境执行 `qi/sql/init.sql`：

```bash
mysql -u root -p < qi/sql/init.sql
```

Windows PowerShell 不支持 `<` 重定向，可用：

```powershell
Get-Content qi/sql/init.sql -Raw -Encoding UTF8 | mysql -u root -p
```

已有旧库且需要「浏览记录」功能时，改执行 `qi/sql/upgrade-20261110-browse-history.sql`；全新环境无需执行升级脚本。

如果旧库的演示数据还在使用 `picsum.photos` 随机图，可执行 `qi/sql/upgrade-20261113-seed-images.sql` 替换为内置主题插画。

### 2. 配置并启动后端

按本机情况修改 `qi/src/main/resources/application.yml`（详见下方配置表），然后：

```bash
cd qi
mvn spring-boot:run
# 或打包运行：mvn clean package && java -jar target/qi-0.0.1-SNAPSHOT.jar
```

仓库忽略了 `.mvn/wrapper/maven-wrapper.jar`，仅保留 `maven-wrapper.properties`；如需使用 `mvnw`/`mvnw.cmd`，先执行 `mvn -N wrapper:wrapper` 生成 wrapper，否则直接使用本机 Maven。

首次启动 `DataInitializer` 会创建默认管理员 **admin / admin123**（BCrypt 哈希入库），正式使用前请修改密码。

### 3. 启动管理后台

```bash
cd fronted/shuqi-admin
npm install
npm run dev        # http://localhost:5173
```

### 4. 运行小程序端

用 HBuilderX 导入 `fronted/漆器云馆` 并运行到微信开发者工具（也可打开已编译产物 `unpackage/dist/mp-weixin`）。本地联调需在开发者工具中开启「不校验合法域名」。

两端后端地址分别集中在一处，改成实际地址即可：

- 小程序：`fronted/漆器云馆/utils/api.js` 的 `BASE_URL`
- 管理后台：`fronted/shuqi-admin/src/api/http.js` 的 `BASE_URL`

默认值均为 `http://localhost:8080`。

## 关键地址与配置

| 项 | 值 |
| --- | --- |
| 后端 | `http://localhost:8080` |
| 管理后台 | `http://localhost:5173`（CORS 白名单已放行，见 `qi/config/WebConfig.java`） |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html`（另配置 `/swagger-ui.html` 入口） |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| 上传文件访问 | `http://localhost:8080/files/...` |
| 种子图片访问 | `http://localhost:8080/seed/...`（内置演示素材，无需上传） |

`qi/src/main/resources/application.yml` 主要配置：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | 后端端口 |
| `spring.datasource.*` | `shuqi_cloud_museum` / `root` / `123456` | 库名与账号密码 |
| `spring.servlet.multipart.*` | 单文件 20MB / 请求 50MB | 上传大小限制 |
| `sa-token.token-name` | `Authorization` | 请求头名，无 `Bearer` 前缀 |
| `sa-token.timeout` | `604800` | 会话 7 天 |
| `file.storage-dir` | `D:/shuqi-upload/` | 本地媒体目录，映射为 `/files/**`，需保证存在且可写 |
| `logging.file.name` | `logs/qi-backend.log` | 控制台 + 文件日志 |

## 文档索引

- `qi/docs/API.md` —— 接口手册（前端接入用，含全部路径、字段、发布校验与联调注意事项）
- `qi/docs/分支叙事设计.md` —— BRANCH 分支叙事设计与发布校验规则
- `qi/HELP.md` —— Spring Initializr 生成的默认帮助文档

## 当前状态与上线前检查

- 页面配置 `page_config`（草稿/预览/发布/版本对比/回滚）表已建，接口尚未实现，前端暂勿调用（见 `qi/docs/API.md` §10）。
- 类别字典的新增/查询已实现；后台「删除类别」按钮调用 `DELETE /admin/artifact-categories/{id}`，后端暂未提供该接口。
- 小程序「语音讲解」「意见反馈」为占位入口，尚未接入功能。
- 上线前需处理：修改默认管理员与数据库密码、收紧 CORS 白名单、将本地文件存储替换为对象存储、配置域名与 HTTPS、补齐小程序合法域名与隐私指引。

## 常见问题

| 现象 | 排查 |
| --- | --- |
| 所有 `/admin` 接口 401 | 检查 `Authorization` 头是否携带 token、会话是否过期；Swagger 中先点 Authorize |
| 后台前端「网络连接失败」 | 后端是否启动、`BASE_URL` 是否正确、`5173` 是否在 CORS 白名单（预检 OPTIONS 已放行） |
| 图片/音频 404 | `file.storage-dir` 目录是否存在、`/files/**` 映射是否生效、前端是否用 `fullUrl()` 拼接相对地址；种子图 404 检查 `/seed/**` 是否随 `static/seed/` 打包 |
| 发布失败提示 400 | 按「发布校验规则」逐项核对必填项、审核状态与关联目标；分支章节先看「校验问题」面板 |
| 后端启动失败 | 确认 JDK 21、MySQL 8.0 与库名账号；Spring Boot 4 下 MyBatis-Plus 自动配置被排除，核心 Bean 由 `MybatisPlusDataSourceConfig` 手动装配，勿移除该配置 |
| 小程序请求不通 | 检查 `BASE_URL` 与「不校验合法域名」设置 |
