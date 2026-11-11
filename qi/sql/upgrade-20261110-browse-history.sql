-- =============================================================
-- 升级脚本：浏览记录（2026-11-10）
-- 个人中心「浏览记录」由灰显改为可用：新增 browse_history 表，
-- 与行为事件 behavior_event 分离，清空浏览记录不影响 ADM-11 统计。
-- 全新环境直接执行最新 init.sql，无需本脚本。
-- =============================================================

USE shuqi_cloud_museum;

CREATE TABLE IF NOT EXISTS browse_history (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    anonymous_id  VARCHAR(64) NOT NULL COMMENT '匿名标识',
    target_type   VARCHAR(20) NOT NULL COMMENT 'chapter / artifact / craft / inheritor',
    target_id     BIGINT      NOT NULL,
    visit_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近浏览时间（重复浏览只更新）',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_anon_target (anonymous_id, target_type, target_id),
    KEY idx_anon_time (anonymous_id, visit_time)
) ENGINE = InnoDB COMMENT = '匿名用户浏览记录';
