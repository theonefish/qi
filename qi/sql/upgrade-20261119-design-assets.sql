-- ============================================================
-- 2026-11-19 章节封面与藏品主图换新
-- 用途：把演示库中的章节封面、藏品主图切换为新版设计资源
-- 适用：已执行过 init.sql 且已有演示数据的数据库
-- 说明：
--   1. 新图片已放入 qi/src/main/resources/static/seed/
--   2. 本章节同时兼容“按标题匹配”和“按旧图片路径匹配”
--   3. 可重复执行，重复执行不会产生副作用
--   4. 新版图片为 AI 概念图，正式发布前请核对工艺名称与授权说明
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 章节封面 ----------
-- 互动体验 · 分支参观：漆盘与工具，3:2
UPDATE chapter
SET cover_url = '/seed/chapter-branch-3x2.jpg'
WHERE deleted = 0
  AND (
      title LIKE '%分支参观%'
      OR cover_url LIKE '%/seed/chapter-branch.png%'
      OR cover_url LIKE '%picsum.photos/seed/chapter-branch%'
  );

-- 第一章 · 千年漆语：漆树割漆，3:2
UPDATE chapter
SET cover_url = '/seed/chapter-main-3x2.jpg'
WHERE deleted = 0
  AND (
      title LIKE '%千年漆语%'
      OR cover_url LIKE '%/seed/chapter-main.png%'
      OR cover_url LIKE '%picsum.photos/seed/chapter-main%'
  );

-- 第二章 · 器物之美：雕填漆盒陈列，3:2
UPDATE chapter
SET cover_url = '/seed/chapter-artifacts-3x2.jpg'
WHERE deleted = 0
  AND (
      title LIKE '%器物之美%'
      OR cover_url LIKE '%/seed/chapter-art.png%'
      OR cover_url LIKE '%picsum.photos/seed/chapter-art%'
  );

-- ---------- 藏品主图 ----------
-- 雕填云纹漆盒：圆形雕填漆盒，4:3
UPDATE artifact
SET main_image_url = '/seed/artifact-diaotian-box.jpg'
WHERE deleted = 0
  AND (
      name LIKE '%雕填云纹漆盒%'
      OR main_image_url LIKE '%/seed/art-box.png%'
      OR main_image_url LIKE '%picsum.photos/seed/art1%'
  );

-- 嵌锡漆碗：黑漆嵌锡云纹碗，4:3
UPDATE artifact
SET main_image_url = '/seed/artifact-tin-bowl.jpg'
WHERE deleted = 0
  AND (
      name LIKE '%嵌锡漆碗%'
      OR main_image_url LIKE '%/seed/art-dish.png%'
      OR main_image_url LIKE '%picsum.photos/seed/art2%'
  );

-- 隐花漆屏：三扇蛋壳镶嵌漆屏，4:3
UPDATE artifact
SET main_image_url = '/seed/artifact-eggshell-screen.jpg'
WHERE deleted = 0
  AND (
      name LIKE '%隐花漆屏%'
      OR main_image_url LIKE '%/seed/art-screen.png%'
      OR main_image_url LIKE '%picsum.photos/seed/art3%'
  );

-- ---------- 执行后核对 ----------
SELECT id, title, cover_url
FROM chapter
WHERE deleted = 0
ORDER BY sort_order, id;

SELECT id, name, main_image_url
FROM artifact
WHERE deleted = 0
ORDER BY id;
