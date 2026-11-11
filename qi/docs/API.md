# 蜀漆云馆后端接口手册（前端接入用）

> 对应后端代码：`qi/` 工程；生成时间 2026-11-07。
> 联调环境：`http://localhost:8080`（Spring Boot 4.1.2 / JDK 21 / MySQL 8.0）。
> 在线调试：`http://localhost:8080/swagger-ui/index.html`；原始规范 JSON：`/v3/api-docs`。

## 0. 全局约定

| 项 | 约定 |
| --- | --- |
| 统一响应体 | `{ "code": 0, "msg": "success", "data": ... }`；`code=0` 成功，非 0 失败，`msg` 为可直接展示的错误文案 |
| 常见错误码 | 400 参数/业务校验失败；401 未登录或登录过期；404 内容不存在或已下架；500 服务器错误 |
| 鉴权 | `/admin/**` 全部需要登录，请求头 `Authorization: <token>`（无 Bearer 前缀，Sa-Token）；唯一例外 `POST /admin/auth/login`；`/app/**` 全部匿名 |
| 分页 | 请求 `pageNum`（默认 1）、`pageSize`（默认 10）；响应 `data = { list, total, pageNum, pageSize }` |
| 内容状态 status | 0 草稿 / 1 已发布 / 2 已下架 |
| 发布状态 publishStatus | 0 发布中 / 1 发布成功 / 2 发布失败 / 3 回滚成功 / 4 回滚失败 |
| 文件访问 | 上传接口返回相对地址如 `/files/2026/11/xxx.jpg`，完整地址 = `http://localhost:8080` + 相对地址 |
| 跨域 | 已放行后台前端开发地址 `http://localhost:5173`；小程序端无同源限制，不受 CORS 约束 |
| 时间格式 | `yyyy-MM-dd HH:mm:ss`（LocalDateTime JSON 序列化） |
| 接口前缀规划 | `/admin/**` 管理后台用；`/app/**` 小程序用。后续新增接口遵循同一前缀 |

## 1. 管理员认证（auth）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | /admin/auth/login | 免登录 | body `{ "username": "admin", "password": "..." }`；返回 `data = { "token": "...", "displayName": "管理员" }`；登录后所有 /admin 请求携带 `Authorization: <token>` |
| POST | /admin/auth/logout | 需要 | 退出登录，data 为 null |
| GET | /admin/auth/me | 需要 | 返回当前管理员显示名称（字符串） |

## 2. 藏品模块（artifact）

### 2.1 类别字典（D-27 受控字典）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /admin/artifact-categories | 全部类别，`data = [{ id, name, sortOrder, ... }]` |
| POST | /admin/artifact-categories | body `{ "name": "漆器" }`，返回新类别 id |

### 2.2 藏品管理

| 方法 | 路径 | 请求 | 响应 data |
| --- | --- | --- | --- |
| GET | /admin/artifacts | query：pageNum、pageSize、name（模糊）、categoryId、status | PageResult\<Artifact\> |
| GET | /admin/artifacts/{id} | - | `{ artifact, categoryName, relations: [{ id, targetType, targetId, sortOrder }] }` |
| POST | /admin/artifacts | ArtifactSaveRequest | 新藏品 id |
| PUT | /admin/artifacts/{id} | 同上（路径 id 优先） | id |
| PUT | /admin/artifacts/{id}/relations | `{ "targets": [{ "targetType": "chapter/craft/inheritor", "targetId": 1 }] }` | -（整体替换关联） |
| POST | /admin/artifacts/{id}/audit | `{ "pass": true }` | -（审核人自动取当前管理员） |
| POST | /admin/artifacts/{id}/publish | `{ "modifySummary": "首次发布" }`（必填） | PublishRecord |
| POST | /admin/artifacts/{id}/off-shelf | - | - |
| GET | /admin/artifacts/{id}/publish-records | - | List\<PublishRecord\> |
| GET | /admin/artifacts/publish-records | query：pageNum、pageSize、objectType（如 artifact）、keyword | PageResult\<PublishRecord\>（内容发布记录统一查询，仅查看） |

ArtifactSaveRequest 字段：`categoryId*`、`name*`、`era`、`material`、`craftDesc`、`story`、`motifMeaning`、`mainImageUrl*`、`source*`、`authorizationNote`（* 必填）。新增后状态为草稿。

Artifact 实体关键字段：`id, categoryId, name, era, material, craftDesc, story, motifMeaning, mainImageUrl, source, authorizationNote, auditStatus(0/1), auditBy, auditTime, status, createTime`。

### 2.3 发布校验规则（发布接口返回的 400 文案即对应原因）

名称、类别、主图、资料来源必填；审核状态须为已审核；至少一条关联；全部通过后 status 变为 1 并生成发布记录。

## 3. 媒体资源（media）

| 方法 | 路径 | 请求 | 响应 data |
| --- | --- | --- | --- |
| POST | /admin/media/upload | multipart/form-data：`file`（必填）、name、source、authorizationNote（可选） | MediaAsset，含 `url`（/files/... 相对地址） |
| GET | /admin/media | query：pageNum、pageSize、mediaType（image/audio/video/file）、keyword | PageResult\<MediaAsset\> |

上传白名单：jpg/jpeg/png/gif/webp/bmp/mp3/mp4/wav/m4a/pdf；单文件 ≤ 20MB。文件直接通过 `http://localhost:8080/files/...` 访问。

## 4. 工艺与工序（craft）

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| GET | /admin/crafts | - | List\<Craft\> |
| POST | /admin/crafts | `{ name*, overview, feature, scope, source }` | 新建为草稿 |
| PUT | /admin/crafts/{id} | 同上 | 编辑 |
| DELETE | /admin/crafts/{id} | - | 逻辑删除（工序一并删除） |
| GET | /admin/crafts/{id}/steps | - | List\<CraftStep\>（按 sortOrder 排序） |
| POST | /admin/crafts/{id}/steps | `{ name*, intro, mediaId }` | 新工序，自动排到末尾 |
| PUT | /admin/crafts/{id}/steps/{stepId} | 同上 | 编辑 |
| DELETE | /admin/crafts/{id}/steps/{stepId} | - | 删除 |
| POST | /admin/crafts/{id}/steps/{stepId}/move | query：direction=up/down | 上下移动排序 |
| POST | /admin/crafts/{id}/publish | `{ "modifySummary": "..." }`（必填） | PublishRecord（校验：名称/概述/适用范围/至少一个有效工序，5.2） |
| POST | /admin/crafts/{id}/off-shelf | - | - |

Craft 字段：`id, name, overview, feature, scope, source, status, createTime`。CraftStep 字段：`id, craftId, name, intro, mediaId, sortOrder`。

## 5. 传承人（inheritor）

| 方法 | 路径 | 请求 |
| --- | --- | --- |
| GET | /admin/inheritors | -（List\<Inheritor\>） |
| POST | /admin/inheritors | `{ name*, photoUrl, intro, career, story, source }` |
| PUT | /admin/inheritors/{id} | 同上 |
| DELETE | /admin/inheritors/{id} | - |
| POST | /admin/inheritors/{id}/audit | `{ "pass": true }` | 简单自审（D-20） |
| POST | /admin/inheritors/{id}/publish | `{ "modifySummary": "..." }` | PublishRecord（校验：姓名/简介/照片/来源/审核通过，5.2） |
| POST | /admin/inheritors/{id}/off-shelf | - | - |

Inheritor 字段：`id, name, photoUrl, intro, career, story, source, auditStatus, auditBy, auditTime, status, createTime`。

## 6. 历史事件（timeline-event）

| 方法 | 路径 | 请求 |
| --- | --- | --- |
| GET | /admin/timeline-events | query：pageNum、pageSize、keyword（分页） |
| POST | /admin/timeline-events | `{ eventTime*, title*, description, mediaId, source* }` |
| PUT | /admin/timeline-events/{id} | 同上 |
| DELETE | /admin/timeline-events/{id} | - |
| POST | /admin/timeline-events/{id}/publish | `{ "modifySummary": "..." }` | PublishRecord（校验：时间/名称/事件介绍/来源，5.2） |
| POST | /admin/timeline-events/{id}/off-shelf | - | - |

TimelineEvent 字段：`id, eventTime, title, description, mediaId, source, status, createTime`。

## 7. 章节与叙事节点（chapter）

### 7.1 章节管理

| 方法 | 路径 | 请求 | 响应 data |
| --- | --- | --- | --- |
| GET | /admin/chapters | query：pageNum、pageSize、status、keyword | PageResult\<Chapter\>（按 sortOrder 升序） |
| GET | /admin/chapters/{id} | - | `{ chapter, nodes: [ChapterNode], relations: [...] }` |
| POST | /admin/chapters | `{ title*, subtitle, coverUrl, summary, chapterType*(0 序章/1 正章), sortOrder }` | id |
| PUT | /admin/chapters/{id} | 同上 | id |
| DELETE | /admin/chapters/{id} | - | -（章节与节点一并逻辑删除） |
| PUT | /admin/chapters/{id}/relations | 同藏品关联结构 | - |
| POST | /admin/chapters/{id}/publish | `{ "modifySummary": "..." }`（必填） | PublishRecord |
| POST | /admin/chapters/{id}/off-shelf | - | - |
| GET | /admin/chapters/{id}/publish-records | - | List\<PublishRecord\> |

Chapter 字段：`id, title, subtitle, coverUrl, summary, chapterType, sortOrder, status, createTime`。
章节发布校验：标题/封面/摘要/类型/排序必填 + 至少一个通过类型校验的叙事节点 + 分支流向图规则（无环 / 无孤岛 / 收尾节点唯一 / BRANCH 选项 ≥2，见 7.4）。

### 7.2 叙事节点管理（前缀 `/admin/chapters/{chapterId}/nodes`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /admin/chapters/{chapterId}/nodes | 节点基础列表（按配置顺序，不含扩展数据） |
| GET | /admin/chapters/{chapterId}/nodes/{nodeId} | 节点详情：`{ node, quote, eventRefs[], craftRefs[], mediaRefs[] }`（编辑表单回显用） |
| POST | /admin/chapters/{chapterId}/nodes | NodeSaveRequest，返回节点 id |
| PUT | /admin/chapters/{chapterId}/nodes/{nodeId} | 同上 |
| DELETE | /admin/chapters/{chapterId}/nodes/{nodeId} | 删除节点（扩展数据物理删除；分支关系级联清理：自身选项、指向自身的选项、指向自身的覆盖边） |
| POST | /admin/chapters/{chapterId}/nodes/{nodeId}/move | query：direction=up/down |
| PUT | /admin/chapters/{chapterId}/nodes/{nodeId}/next | 流向覆盖连线（7.4）：body `{ "targetNodeId": 5 }` 或 `{ "targetNodeId": null }` 清除；BRANCH 节点不可设置（走向由选项决定） |

**NodeSaveRequest（多态请求体，按 nodeType 校验）：**

```json
{
  "nodeType": "TEXT | TIMELINE | QUOTE | PROCESS | BRANCH",
  "title": "节点标题（必填）",
  "subtitle": "副标题（可选）",
  "content": "TEXT：正文，术语写 [[term:ID]] 标记；PROCESS：适用范围说明；BRANCH：分支引导文案",
  "source": "资料来源",

  "media":  [ { "mediaId": 1, "caption": "图片说明" } ],
  "eventIds": [1, 2],
  "inheritorId": 1,
  "quoteText": "口述原文",
  "quoteSource": "口述来源",
  "authorizationNote": "授权说明",
  "craftId": 1,
  "craftStepIds": [3, 4],

  "nextNodeId": null,
  "flowPos": "320,180",
  "choices": [ { "label": "去制胎坊", "targetNodeId": 5 } ]
}
```

- `nextNodeId`：显式流向覆盖（分支叙事）；null=按 sortOrder 的下一节点。BRANCH 恒为 null
- `flowPos`：图编排画布坐标，随节点保存（不传会被清空——结构变更必须全量回传，见 7.4）
- `choices`：仅 BRANCH 有效；保存语义 **null=保留现有选项、[] =清空、非空=整体替换**（ChapterEdit 表单保存不带此字段，避免覆盖分支编排页配置）

按类型的必填项：

| 类型 | 必填 | 选填 |
| --- | --- | --- |
| TEXT 图文 | title、content | subtitle、source、media[] |
| TIMELINE 时间轴 | title、eventIds（≥1，事件必须存在） | source |
| QUOTE 人物口述 | title、inheritorId（必须存在）、quoteText | quoteSource、authorizationNote、source |
| PROCESS 工序步骤 | title、craftId（必须存在）、content（适用范围说明） | craftStepIds（空=整工艺概览，须属于该工艺）、media[]、source |
| BRANCH 分支抉择 | title、choices（target 须为同章存在节点且不重复；非空时每项须有文案。「≥2」由发布校验强制，保存允许 [] 与单选项中间态） | content（引导文案，建议填写）、subtitle、source |

### 7.3 小程序章节详情返回结构（GET /app/chapters/{id}）

```json
{
  "chapter": { "id": 1, "title": "第二章：百工成器", "chapterType": 1, "sortOrder": 2, "...": "..." },
  "nodes": [
    {
      "id": 10, "nodeType": "TEXT", "title": "...", "subtitle": "...", "content": "...（含 [[term:1]] 标记）", "source": "...",
      "media": [ { "id": 1, "url": "/files/2026/11/x.jpg", "caption": "..." } ],
      "events": [], "quoteText": null, "quoteSource": null, "inheritorName": null, "inheritorPhotoUrl": null,
      "craft": null, "steps": [],
      "nextNodeId": null, "choices": []
    },
    {
      "id": 11, "nodeType": "TIMELINE", "...": "...",
      "events": [ { "id": 1, "eventTime": "商周时期", "title": "...", "description": "..." } ]
    },
    {
      "id": 12, "nodeType": "QUOTE", "...": "...",
      "quoteText": "...", "quoteSource": "...", "inheritorName": "张三", "inheritorPhotoUrl": "/files/..."
    },
    {
      "id": 13, "nodeType": "PROCESS", "...": "...",
      "craft": { "id": 1, "name": "...", "scope": "..." },
      "steps": [ { "id": 3, "name": "制胎", "intro": "..." } ]
    },
    {
      "id": 14, "nodeType": "BRANCH", "content": "你想先看哪条路线？", "nextNodeId": null,
      "choices": [ { "id": 1, "label": "去制胎坊", "targetNodeId": 15 }, { "id": 2, "label": "去饰纹坊", "targetNodeId": 16 } ]
    }
  ],
  "relations": [ { "targetType": "artifact", "targetId": 5, "name": "汉代漆案" } ]
}
```

每个节点均带 `nextNodeId`（显式流向覆盖，null=按 sortOrder 的下一节点）与 `choices`（仅 BRANCH 非空）。接口**全量下发**节点——「重选」「浏览全部」需要未选分支的内容，前端本地按活跃路径裁剪渲染。

前端渲染规则：默认按活跃路径渲染（入口 → 沿所选分支 / 覆盖边 / sortOrder 默认链 → 收尾节点）；BRANCH 节点渲染为抉择卡片（引导文案 + 按钮组），未选择不渲染后续内容（强制选择）；提供「重选」与「浏览全部」（未走分支灰显）；TEXT 正文中的 `[[term:ID]]` 由前端解析为术语点击样式。实现见 `pages/chapter/chapter.vue`（分支叙事设计稿 §10）。

### 7.4 分支编排接口（/admin/chapters/{id}/flow，图编排页专用）

| 方法 | 路径 | 请求 | 响应 data |
| --- | --- | --- | --- |
| GET | /admin/chapters/{id}/flow | - | `{ nodes: [{ id, nodeType, title, sortOrder, flowPos, nextNodeId, inDegree, choiceCount }], choices: [{ id, nodeId, label, targetNodeId, sortOrder }], entryNodeId, endNodeIds[] }`（inDegree 后端现算） |
| PUT | /admin/chapters/{id}/flow-layout | `{ items: [{ nodeId, flowPos }] }` | -（批量保存画布坐标，拖动防抖调用；单批 ≤200） |
| PUT | /admin/chapters/{id}/nodes/{nodeId}/next | `{ targetNodeId: Long \| null }` | -（覆盖边连线/清除；BRANCH 不可设置） |

画布边语义（§11.2）：默认边=sortOrder 相邻链（灰、自动、不可编辑）；覆盖边=next_node_id（蓝、可拖拽改接/删除）；选项边=node_choice（橙虚线、label=按钮文案）。BRANCH 节点无默认边（强制选择）。选项增删走节点全量保存（7.2 的 choices 字段），payload 必须回传 `flowPos`/`nextNodeId` 防显式 set 清空。

发布校验规则（POST /publish 时硬阻断，400 文案）：BRANCH 选项 ≥2 且文案非空；选项/覆盖目标须为同章节点且 ≠ 自身；全图无环；入口可达全部节点（无孤岛）；收尾节点唯一。软提示仅日志：分支点 >2、嵌套深度 >2、引导文案为空。

## 8. 小程序端接口（/app，全部匿名，仅返回已发布内容）

| 方法 | 路径 | 请求 | 响应 data |
| --- | --- | --- | --- |
| GET | /app/chapters | - | List\<Chapter\>（云展馆章节列表，按配置顺序；进度 N/M 按正章数计算，序章 chapterType=0 不计入分母） |
| GET | /app/chapters/{id} | - | AppChapterDetail（见 7.3） |
| GET | /app/artifacts | query：pageNum、pageSize、categoryId | PageResult\<Artifact\>（"最新添加"排序） |
| GET | /app/artifacts/{id} | - | Artifact（草稿/下架返回 404） |
| GET | /app/artifacts/categories | - | List\<ArtifactCategory\>（仅含实际有已发布藏品的类别） |
| GET | /app/crafts | - | List\<Craft\>（工艺总览，仅已发布） |
| GET | /app/crafts/{id} | - | `{ craft, steps: [{ id, name, intro, mediaUrl, sortOrder }], relatedArtifacts: [{ id, name, mainImageUrl }] }`（工序因器而异说明在 craft.scope） |
| GET | /app/inheritors | - | List\<Inheritor\>（姓名/照片/简介，仅已发布） |
| GET | /app/inheritors/{id} | - | `{ inheritor, works: [{ id, name, mainImageUrl }], crafts: [{ id, name }] }`（代表作品与擅长技艺，仅有效目标） |
| GET | /app/terms | query：ids=1,2,3 | List：`{ id, name, definition, source }`（仅已发布；正文 [[term:ID]] 标记解析后批量调用） |
| GET | /app/motifs | - | List：`{ id, name, imageUrl, culturalExplain, source }`（仅已发布；纹饰主题页，FR 补充） |

### 8.1 访客接口（/app/visitor，匿名；anonymousId = 前端本地生成并长期保存的 UUID）

| 方法 | 路径 | 请求 | 响应 data |
| --- | --- | --- | --- |
| GET | /app/visitor/summary | query：anonymousId | `{ completedMain, totalMain, progressPercent, favoriteCount, lastRead }`；lastRead = `{ chapterId, chapterTitle, nodeId, nodeTitle, readTime }` 或 null（章节失效时，前端回云展馆） |
| GET | /app/visitor/progress | query：anonymousId | List：`{ chapterId, title, chapterType, completed, completeTime, lastNodeId, lastReadTime, choicePath }`（全部已发布章节，含序章已读标记；choicePath = 分支选择路径 JSON `[{"n":分支节点id,"c":选项id}]`，续读时还原，分支叙事 §10.3） |
| POST | /app/visitor/progress/read | body `{ anonymousId, chapterId, nodeId, choicePath? }` | -（浏览到节点时更新阅读位置，5.6；choicePath 可选，≤500 字符原样存储，前端写前端读） |
| POST | /app/visitor/progress/complete | body `{ anonymousId, chapterId }` | `{ firstTime, completedMain, totalMain, progressPercent }`（幂等；序章完成不计入进度分母；首次完成自动记录 complete_chapter 事件） |
| POST | /app/visitor/favorites | body `{ anonymousId, targetType: "artifact", targetId }` | -（幂等；P0 仅藏品） |
| DELETE | /app/visitor/favorites | query：anonymousId、targetType、targetId | - |
| GET | /app/visitor/favorites | query：anonymousId | List：`{ targetId, name, mainImageUrl, favoriteTime }`（已下架/删除藏品自动隐藏） |
| GET | /app/visitor/favorites/exists | query：anonymousId、targetType、targetId | Boolean（详情页收藏按钮状态） |
| POST | /app/visitor/history | body `{ anonymousId, targetType, targetId }` | -（P1 浏览记录；targetType=chapter/artifact/craft/inheritor；同一对象重复浏览只刷新时间） |
| GET | /app/visitor/history | query：anonymousId、limit（可选，默认 50，最大 100） | List：`{ targetType, targetId, title, subtitle, imageUrl, visitTime }`（按最近浏览倒序；失效/下架内容自动隐藏） |
| DELETE | /app/visitor/history | query：anonymousId | -（清空个人浏览记录，不影响行为统计） |
| POST | /app/visitor/behaviors | BehaviorReportRequest | Integer（实际记录条数） |

行为上报请求体：

```json
{
  "anonymousId": "uuid-本地生成-长期保存",
  "visitId": "uuid-每次冷启动重新生成",
  "events": [
    { "eventType": "enter_chapter", "chapterId": 1 },
    { "eventType": "view_node", "chapterId": 1, "nodeId": 10 },
    { "eventType": "click_relation", "chapterId": 1, "targetType": "artifact", "targetId": 5 },
    { "eventType": "back_main", "chapterId": 1 },
    { "eventType": "choose_branch", "chapterId": 1, "nodeId": 14, "targetType": "choice", "targetId": 2 }
  ]
}
```

前端调用时机（7.11 口径）：进入章节详情页报 `enter_chapter`；节点首次进入视口报 `view_node`（同会话同节点服务端自动去重）；点击关联卡报 `click_relation`（带 targetType/targetId）；点击回到主线报 `back_main`（chapterId = 来源章节）；**分支抉择点选按钮报 `choose_branch`**（nodeId=BRANCH 节点，targetType='choice'，targetId=选项 id——分支受欢迎统计的数据源，分支叙事 §10.4）。**完成章节不要上报**，由 complete 接口内部记录。事件可攒批合并上报以减少请求数；上报失败不要阻塞阅读，静默重试或丢弃即可。

## 9. 数据统计（/admin/stats，需登录）

| 方法 | 路径 | 响应 data |
| --- | --- | --- |
| GET | /admin/stats/overview | `{ publishedChapters, publishedArtifacts, draftArtifacts, publishedCrafts, publishedInheritors, visitorCount, todayEventCount }`（数据总览卡片） |
| GET | /admin/stats/chapters | List：`{ chapterId, title, enterCount, viewNodeCount, completeCount, backMainCount }` |
| GET | /admin/stats/relations | List：`{ chapterId, targetType, targetId, count }`（关联跳转 Top 50，按来源章节与目标内容统计） |

## 10. 规划中、尚未实现的接口（前端暂勿调用）

| 模块 | 计划接口 | 对应需求 |
| --- | --- | --- |
| 页面配置 page_config | 8 个页面配置的草稿/预览/发布/版本对比/回滚（后端最后一块 P0，只影响后台，不阻塞小程序联调） | ADM-12/13/14 |

## 11. 术语与纹饰管理（term / motif）

### 11.1 术语（/admin/terms）

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| GET | /admin/terms | query：pageNum、pageSize、keyword | 分页 |
| POST | /admin/terms | `{ name*, definition, source }` | 新建为草稿 |
| PUT | /admin/terms/{id} | 同上 | 编辑 |
| DELETE | /admin/terms/{id} | - | 删除 |
| POST | /admin/terms/{id}/audit | `{ "pass": true }` | 简单自审（D-20） |
| POST | /admin/terms/{id}/publish | `{ "modifySummary": "..." }` | 发布（名称/释义/审核通过，5.2） |
| POST | /admin/terms/{id}/off-shelf | - | 下架 |

### 11.2 纹饰（/admin/motifs）

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| GET | /admin/motifs | query：pageNum、pageSize、keyword | 分页 |
| POST | /admin/motifs | `{ name*, imageUrl, culturalExplain, source }` | 新建为草稿 |
| PUT | /admin/motifs/{id} | 同上 | 编辑 |
| DELETE | /admin/motifs/{id} | - | 删除 |
| POST | /admin/motifs/{id}/publish | `{ "modifySummary": "..." }` | 发布（名称/文化解释/来源，5.2） |
| POST | /admin/motifs/{id}/off-shelf | - | 下架（P0 在章节/藏品中引用展示，独立主题页 P1） |

## 12. 联调注意事项

1. 默认管理员 `admin / admin123`（首次启动自动创建，见启动日志）。
2. 修改 `application.yml` 的数据库密码后需重启。
3. 本地演示环境不上线：小程序开发工具需开启"不校验合法域名"；正式上线前的域名/HTTPS/隐私指引清单见主文档 10.5。
4. 所有错误 `msg` 已按可直接展示的中文文案返回，前端 Toast 直接用即可。
5. 接口有增改时同步更新本文件；可用 `/v3/api-docs` 导出 JSON 生成最新版核对。
