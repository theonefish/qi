-- =============================================================
-- 升级脚本：visitor 模块（2026-11-07）
-- 适用于已执行过 init.sql 的库；全新环境直接执行最新 init.sql 即可。
-- =============================================================

USE shuqi_cloud_museum;

-- 行为事件增加访问会话标识：同一访问会话内 enter_chapter / view_node 去重（7.11 事件口径）
ALTER TABLE behavior_event
    ADD COLUMN visit_id VARCHAR(64) NULL AFTER anonymous_id
    COMMENT '访问会话标识（前端冷启动生成，用于会话内去重）';
