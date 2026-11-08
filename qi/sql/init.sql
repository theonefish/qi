-- =============================================================
-- 蜀漆云馆 建库脚本（对应主文档 v0.7 第 9.2 节逻辑表建议）
-- 环境：MySQL 8.0 / utf8mb4
-- 约定：
--   1. 所有内容表统一带 create_time / update_time；
--   2. 内容表带 deleted 软删除字段（5.1、7.10：被引用数据不能物理删除）；
--   3. 行为事件、发布记录为追加型数据，不设软删除（7.14：历史记录不可覆盖）；
--   4. 藏品/传承人/术语带简单自审字段（D-20：审核状态、审核人、审核时间）；
--   5. status 统一取值：0 草稿 / 1 已发布 / 2 已下架（5.3）。
-- 默认管理员账号由应用启动时初始化（BCrypt 哈希），不在此插入明文密码。
-- =============================================================

CREATE DATABASE IF NOT EXISTS shuqi_cloud_museum
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE shuqi_cloud_museum;

-- 1. 后台管理员（ADM-01）
CREATE TABLE admin_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password     VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希，禁止明文（7.1）',
    display_name VARCHAR(50)  NOT NULL COMMENT '显示名称（发布记录的操作管理员）',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '后台管理员';

-- 2. 藏品类别字典（D-27：受控字典，表单下拉选择，不使用自由文本）
CREATE TABLE artifact_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '类别名称',
    sort_order  INT         NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '藏品类别字典';

-- 3. 章节（ADM-03；D-02：类型仅序章/正章，数量不写死）
CREATE TABLE chapter (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(100) NOT NULL COMMENT '章节标题',
    subtitle     VARCHAR(200) NULL COMMENT '副标题',
    cover_url    VARCHAR(500) NULL COMMENT '封面图地址',
    summary      VARCHAR(500) NULL COMMENT '摘要',
    chapter_type TINYINT      NOT NULL DEFAULT 1 COMMENT '0 序章 / 1 正章',
    sort_order   INT          NOT NULL DEFAULT 0 COMMENT '展示顺序',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 已发布 / 2 已下架',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_sort (sort_order)
) ENGINE = InnoDB COMMENT = '章节';

-- 4. 章节叙事节点（6.13：四类节点共用一张表，类型相关扩展数据见 node_* 表）
CREATE TABLE chapter_node (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    chapter_id BIGINT       NOT NULL COMMENT '所属章节',
    node_type  VARCHAR(20)  NOT NULL COMMENT 'TEXT / TIMELINE / QUOTE / PROCESS / BRANCH（分支抉择）',
    title      VARCHAR(100) NOT NULL COMMENT '节点标题',
    subtitle   VARCHAR(200) NULL COMMENT '副标题（图文节点可选）',
    content    LONGTEXT     NULL COMMENT 'TEXT：正文块状 JSON（段落/图片混排）或历史纯文本；PROCESS：适用范围说明；BRANCH：分支引导文案；术语以轻量标记存储（D-26）',
    source     VARCHAR(500) NULL COMMENT '资料来源',
    sort_order INT          NOT NULL DEFAULT 0 COMMENT '节点排序',
    next_node_id BIGINT     NULL COMMENT '显式流向覆盖；NULL=按 sortOrder 的下一节点（分支叙事，并行段回汇用）',
    flow_pos   VARCHAR(50)  NULL COMMENT '图编排画布坐标（x,y）',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_chapter (chapter_id),
    KEY idx_next (next_node_id)
) ENGINE = InnoDB COMMENT = '章节叙事节点';

-- 5. 人物口述节点扩展（node_type = QUOTE）
CREATE TABLE node_quote (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    node_id           BIGINT       NOT NULL COMMENT '对应叙事节点，一节点一条',
    inheritor_id      BIGINT       NOT NULL COMMENT '关联传承人',
    quote_text        TEXT         NOT NULL COMMENT '口述文本（须为经核实授权的真实口述）',
    source            VARCHAR(500) NULL COMMENT '口述来源',
    authorization_note VARCHAR(500) NULL COMMENT '授权说明',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_node (node_id)
) ENGINE = InnoDB COMMENT = '人物口述节点扩展';

-- 6. 时间轴节点与历史事件引用（node_type = TIMELINE）
CREATE TABLE node_event_relation (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    node_id    BIGINT NOT NULL COMMENT '叙事节点',
    event_id   BIGINT NOT NULL COMMENT '历史事件',
    sort_order INT    NOT NULL DEFAULT 0 COMMENT '节点内展示顺序',
    PRIMARY KEY (id),
    KEY idx_node (node_id),
    KEY idx_event (event_id)
) ENGINE = InnoDB COMMENT = '叙事节点与历史事件引用';

-- 7. 工序节点与工艺/工序引用（node_type = PROCESS）
CREATE TABLE node_craft_relation (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    node_id      BIGINT NOT NULL COMMENT '叙事节点',
    craft_id     BIGINT NOT NULL COMMENT '关联工艺',
    craft_step_id BIGINT NULL COMMENT '引用的工序（可空表示整工艺概览）',
    sort_order   INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_node (node_id),
    KEY idx_craft (craft_id)
) ENGINE = InnoDB COMMENT = '工序叙事节点引用';

-- 8. 节点图片引用（图文节点等的多图支持）
CREATE TABLE node_media_relation (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    node_id    BIGINT       NOT NULL COMMENT '叙事节点',
    media_id   BIGINT       NOT NULL COMMENT '媒体资源',
    caption    VARCHAR(200) NULL COMMENT '图片说明',
    sort_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_node (node_id)
) ENGINE = InnoDB COMMENT = '叙事节点与媒体引用';

-- 8.1 分支抉择选项（node_type = BRANCH，分支叙事）
CREATE TABLE node_choice (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    node_id        BIGINT      NOT NULL COMMENT '所属 BRANCH 节点',
    label          VARCHAR(50) NOT NULL COMMENT '按钮文案，如「去制胎坊」',
    target_node_id BIGINT      NOT NULL COMMENT '选中后到达的同章节点',
    sort_order     INT         NOT NULL DEFAULT 0 COMMENT '按钮排序',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_node (node_id)
) ENGINE = InnoDB COMMENT = '分支抉择选项（BRANCH 节点的按钮）';

-- 9. 历史事件（ADM-07；时间轴数据源）
CREATE TABLE timeline_event (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    event_time  VARCHAR(100) NOT NULL COMMENT '时间或时期（年代不明用时期表述，不编造年份）',
    title       VARCHAR(100) NOT NULL COMMENT '事件名称',
    description LONGTEXT     NULL COMMENT '事件说明（富文本 HTML）',
    media_id    BIGINT       NULL COMMENT '配图（可空）',
    source      VARCHAR(500) NOT NULL COMMENT '资料来源（5.2 必填）',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 已发布 / 2 已下架',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '历史事件';

-- 10. 藏品（ADM-04；D-20 自审字段；D-16 收藏目标）
CREATE TABLE artifact (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    category_id  BIGINT       NOT NULL COMMENT '类别（artifact_category 字典）',
    name         VARCHAR(100) NOT NULL COMMENT '藏品名称',
    era          VARCHAR(100) NULL COMMENT '年代',
    material     VARCHAR(200) NULL COMMENT '材质',
    craft_desc   LONGTEXT     NULL COMMENT '工艺说明（富文本 HTML）',
    story        LONGTEXT     NULL COMMENT '文化故事（富文本 HTML）',
    motif_meaning LONGTEXT    NULL COMMENT '纹饰寓意（富文本 HTML）',
    main_image_url VARCHAR(500) NOT NULL COMMENT '主展示图',
    source       VARCHAR(500) NOT NULL COMMENT '资料来源',
    authorization_note VARCHAR(500) NULL COMMENT '授权说明',
    audit_status TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未审核 / 1 已审核（D-20）',
    audit_by     VARCHAR(50)  NULL COMMENT '审核人（自动取当前管理员）',
    audit_time   DATETIME     NULL COMMENT '审核时间',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 已发布 / 2 已下架',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_category (category_id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '藏品档案';

-- 11. 工艺（ADM-05）
CREATE TABLE craft (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL COMMENT '工艺名称',
    overview    LONGTEXT     NULL COMMENT '概述（富文本 HTML）',
    feature     LONGTEXT     NULL COMMENT '特色说明（富文本 HTML）',
    scope       VARCHAR(500) NULL COMMENT '适用范围（工序因器而异说明）',
    source      VARCHAR(500) NULL COMMENT '资料来源',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 已发布 / 2 已下架',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '工艺档案';

-- 12. 工序步骤（工艺一对多）
CREATE TABLE craft_step (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    craft_id    BIGINT       NOT NULL COMMENT '所属工艺',
    name        VARCHAR(100) NOT NULL COMMENT '工序名称',
    intro       TEXT         NULL COMMENT '简介',
    media_id    BIGINT       NULL COMMENT '图片或说明图',
    sort_order  INT          NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_craft (craft_id)
) ENGINE = InnoDB COMMENT = '工序步骤';

-- 13. 传承人（ADM-06；D-20 自审字段）
CREATE TABLE inheritor (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(50)  NOT NULL COMMENT '姓名',
    photo_url    VARCHAR(500) NULL COMMENT '人物照片',
    intro        LONGTEXT     NULL COMMENT '简介（富文本 HTML）',
    career       LONGTEXT     NULL COMMENT '从艺经历（富文本 HTML）',
    story        LONGTEXT     NULL COMMENT '传承故事（富文本 HTML）',
    source       VARCHAR(500) NOT NULL COMMENT '资料来源',
    audit_status TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未审核 / 1 已审核（D-20）',
    audit_by     VARCHAR(50)  NULL COMMENT '审核人',
    audit_time   DATETIME     NULL COMMENT '审核时间',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 已发布 / 2 已下架',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '传承人档案';

-- 14. 专业术语（ADM-07；FR-06 术语卡数据源；D-20 自审字段）
CREATE TABLE term (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(50)  NOT NULL COMMENT '术语名称',
    definition   LONGTEXT     NOT NULL COMMENT '释义（富文本 HTML）',
    source       VARCHAR(500) NULL COMMENT '来源',
    audit_status TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未审核 / 1 已审核',
    audit_by     VARCHAR(50)  NULL COMMENT '审核人',
    audit_time   DATETIME     NULL COMMENT '审核时间',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 已发布 / 2 已下架',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '专业术语';

-- 15. 纹饰资料（ADM-07；P1 纹饰主题页数据源）
CREATE TABLE motif (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL COMMENT '纹饰名称',
    image_url   VARCHAR(500) NULL COMMENT '纹饰图片',
    cultural_explain LONGTEXT NULL COMMENT '文化解释（富文本 HTML，须有可靠依据）',
    source      VARCHAR(500) NOT NULL COMMENT '资料来源',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 已发布 / 2 已下架',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '纹饰资料';

-- 16. 媒体资源元数据（ADM-08；图片二进制不入库，4.1）
CREATE TABLE media_asset (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(200) NOT NULL COMMENT '资源名称',
    url         VARCHAR(500) NOT NULL COMMENT '文件访问地址（/files/...）',
    media_type  VARCHAR(20)  NOT NULL DEFAULT 'image' COMMENT 'image / audio / video / file',
    caption     VARCHAR(200) NULL COMMENT '图片说明',
    source      VARCHAR(500) NULL COMMENT '来源',
    authorization_note VARCHAR(500) NULL COMMENT '版权/授权说明',
    file_size   BIGINT       NULL COMMENT '文件大小（字节）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_type (media_type)
) ENGINE = InnoDB COMMENT = '媒体资源元数据';

-- 17. 内容关联（ADM-09；多对多统一关联表，8.3）
CREATE TABLE content_relation (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    source_type VARCHAR(20) NOT NULL COMMENT '来源类型：chapter/artifact/craft/inheritor/term/event/motif',
    source_id   BIGINT      NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型，同上',
    target_id   BIGINT      NOT NULL,
    sort_order  INT         NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_source (source_type, source_id),
    KEY idx_target (target_type, target_id)
) ENGINE = InnoDB COMMENT = '内容对象关联';

-- 18. 匿名用户收藏（FR-09；D-16：P0 仅藏品；按匿名标识服务端存储，D-05）
CREATE TABLE user_favorite (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    anonymous_id VARCHAR(64) NOT NULL COMMENT '本地生成的匿名标识',
    target_type  VARCHAR(20) NOT NULL COMMENT '收藏目标类型（P0 仅 artifact）',
    target_id    BIGINT      NOT NULL,
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_anon_target (anonymous_id, target_type, target_id)
) ENGINE = InnoDB COMMENT = '匿名用户收藏';

-- 19. 匿名用户进度（FR-05 断点续读 + 章节完成状态）
CREATE TABLE user_progress (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    anonymous_id   VARCHAR(64) NOT NULL,
    chapter_id     BIGINT      NOT NULL,
    last_node_id   BIGINT      NULL COMMENT '最后阅读节点',
    choice_path    VARCHAR(500) NULL COMMENT '分支选择路径 JSON：[{"n":分支节点id,"c":选项id}]（分支叙事断点续读）',
    completed      TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已完成本章（点击完成本章触发）',
    complete_time  DATETIME    NULL COMMENT '完成时间',
    last_read_time DATETIME    NULL COMMENT '最近阅读时间',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_anon_chapter (anonymous_id, chapter_id)
) ENGINE = InnoDB COMMENT = '匿名用户阅读进度';

-- 20. 行为事件（ADM-11；口径定义见 7.11，追加型数据）
CREATE TABLE behavior_event (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    anonymous_id VARCHAR(64) NOT NULL COMMENT '匿名标识',
    visit_id     VARCHAR(64) NULL COMMENT '访问会话标识（前端冷启动生成，用于会话内去重，7.11）',
    event_type   VARCHAR(30) NOT NULL COMMENT 'enter_chapter/view_node/complete_chapter/click_relation/back_main',
    chapter_id   BIGINT      NULL,
    node_id      BIGINT      NULL,
    target_type  VARCHAR(20) NULL COMMENT '关联跳转的目标类型',
    target_id    BIGINT      NULL COMMENT '关联跳转的目标 ID',
    occur_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_type_time (event_type, occur_time),
    KEY idx_chapter (chapter_id)
) ENGINE = InnoDB COMMENT = '参观行为事件';

-- 21. 小程序页面配置（ADM-12；范围 8 个页面，D-22）
CREATE TABLE page_config (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    page_code      VARCHAR(50)  NOT NULL COMMENT '页面编码，如 index/exhibition/artifacts',
    page_name      VARCHAR(50)  NOT NULL COMMENT '页面名称',
    draft_config   LONGTEXT     NULL COMMENT '草稿配置（JSON，字段白名单见 7.13）',
    online_config  LONGTEXT     NULL COMMENT '当前线上配置（JSON）',
    online_version INT          NOT NULL DEFAULT 0 COMMENT '当前线上版本号，从 v1 递增',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_page_code (page_code)
) ENGINE = InnoDB COMMENT = '小程序页面配置';

-- 22. 发布记录（ADM-13/14；页面配置与内容发布统一记录；回滚仅页面配置，D-23）
CREATE TABLE publish_record (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    record_type  VARCHAR(10)  NOT NULL COMMENT 'page 页面配置 / content 内容对象',
    page_code    VARCHAR(50)  NULL COMMENT '页面编码（record_type=page 时）',
    object_type  VARCHAR(20)  NULL COMMENT '内容对象类型（record_type=content 时）',
    object_id    BIGINT       NULL COMMENT '内容对象 ID',
    version      INT          NOT NULL COMMENT '版本号，按页面/对象独立递增',
    batch_no     VARCHAR(50)  NULL COMMENT '发布批次号（多页面同批发布时）',
    modify_summary VARCHAR(500) NOT NULL COMMENT '修改内容说明（必填）',
    auto_changes LONGTEXT     NULL COMMENT '系统自动汇总的变更项（JSON）',
    publish_status TINYINT    NOT NULL DEFAULT 0 COMMENT '0 发布中 / 1 发布成功 / 2 发布失败 / 3 回滚成功 / 4 回滚失败',
    rollback_from_version INT NULL COMMENT '回滚来源版本（回滚记录时）',
    operator_id  BIGINT       NOT NULL COMMENT '操作管理员 ID（自动取会话）',
    operator_name VARCHAR(50) NOT NULL COMMENT '操作管理员名称',
    snapshot     LONGTEXT     NULL COMMENT '发布成功时的配置/内容快照（JSON）',
    fail_reason  VARCHAR(500) NULL COMMENT '失败原因',
    publish_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间（服务端记录）',
    PRIMARY KEY (id),
    KEY idx_page_version (page_code, version),
    KEY idx_object (object_type, object_id),
    KEY idx_time (publish_time)
) ENGINE = InnoDB COMMENT = '发布记录';
