-- =============================================================
-- 种子图片替换脚本（2026-11-13）
-- 将早期演示数据中的随机图（picsum.photos）替换为与展馆视觉一致的
-- 红黑金主题插画。图片位于 qi/src/main/resources/static/seed/，
-- 由 Spring Boot 默认静态资源映射以 /seed/**.png 提供。
--
-- 说明：
--   1. 图片由 qi/seed-assets/generate_seed_images.py 程序化生成，
--      需要调整画风时修改脚本后重新执行即可。
--   2. 实体表（chapter / artifact / inheritor / motif）存相对地址，
--      由前端 fullUrl() 拼接 BASE_URL。
--   3. chapter_node.content 是富文本，rich-text 不解析相对路径，
--      因此写入绝对地址；换域名部署时请同步替换 http://localhost:8080。
--   4. 新环境执行 init.sql 即可，本脚本用于已有演示库的升级。
-- =============================================================

USE shuqi_cloud_museum;

-- ---------- 章节封面 ----------
UPDATE chapter SET cover_url = '/seed/chapter-main.png'
WHERE cover_url LIKE '%picsum.photos/seed/chapter-main%';

UPDATE chapter SET cover_url = '/seed/chapter-branch.png'
WHERE cover_url LIKE '%picsum.photos/seed/chapter-branch%';

UPDATE chapter SET cover_url = '/seed/chapter-art.png'
WHERE cover_url LIKE '%picsum.photos/seed/chapter-art%';

-- ---------- 藏品主图 ----------
UPDATE artifact SET main_image_url = '/seed/art-box.png'
WHERE main_image_url LIKE '%picsum.photos/seed/art1%';

UPDATE artifact SET main_image_url = '/seed/art-dish.png'
WHERE main_image_url LIKE '%picsum.photos/seed/art2%';

UPDATE artifact SET main_image_url = '/seed/art-screen.png'
WHERE main_image_url LIKE '%picsum.photos/seed/art3%';

-- ---------- 传承人照片 ----------
UPDATE inheritor SET photo_url = '/seed/inheritor-1.png'
WHERE photo_url LIKE '%picsum.photos/seed/inheritor1%';

UPDATE inheritor SET photo_url = '/seed/inheritor-2.png'
WHERE photo_url LIKE '%picsum.photos/seed/inheritor2%';

-- ---------- 纹饰图片 ----------
UPDATE motif SET image_url = '/seed/motif-cloud.png'
WHERE image_url LIKE '%picsum.photos/seed/motif-cloud%';

UPDATE motif SET image_url = '/seed/motif-grass.png'
WHERE image_url LIKE '%picsum.photos/seed/motif-grass%';

UPDATE motif SET image_url = '/seed/motif-bird.png'
WHERE image_url LIKE '%picsum.photos/seed/motif-bird%';

-- ---------- 章节富文本内联图（绝对地址） ----------
UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/flow-intro/1200/700',
    'http://localhost:8080/seed/node-view.png')
WHERE content LIKE '%picsum.photos/seed/flow-intro%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/flow-trip-1/1200/700',
    'http://localhost:8080/seed/node-trip.png')
WHERE content LIKE '%picsum.photos/seed/flow-trip-1%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/flow-trip-2/1200/700',
    'http://localhost:8080/seed/node-trip.png')
WHERE content LIKE '%picsum.photos/seed/flow-trip-2%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/flow-deco-1/1200/700',
    'http://localhost:8080/seed/node-deco.png')
WHERE content LIKE '%picsum.photos/seed/flow-deco-1%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/flow-deco-2/1200/700',
    'http://localhost:8080/seed/node-deco.png')
WHERE content LIKE '%picsum.photos/seed/flow-deco-2%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/flow-ending/1200/700',
    'http://localhost:8080/seed/node-ending.png')
WHERE content LIKE '%picsum.photos/seed/flow-ending%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/text-lacquer-tree/1200/700',
    'http://localhost:8080/seed/node-tree.png')
WHERE content LIKE '%picsum.photos/seed/text-lacquer-tree%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/text-lacquer-coat/1200/700',
    'http://localhost:8080/seed/node-deco.png')
WHERE content LIKE '%picsum.photos/seed/text-lacquer-coat%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/text-art-view/1200/700',
    'http://localhost:8080/seed/node-view.png')
WHERE content LIKE '%picsum.photos/seed/text-art-view%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/ch1-life/1200/700',
    'http://localhost:8080/seed/node-life.png')
WHERE content LIKE '%picsum.photos/seed/ch1-life%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/ch2-art1/1200/700',
    'http://localhost:8080/seed/node-art.png')
WHERE content LIKE '%picsum.photos/seed/ch2-art1%';

UPDATE chapter_node SET content = REPLACE(content,
    'https://picsum.photos/seed/ch2-art3/1200/700',
    'http://localhost:8080/seed/node-art.png')
WHERE content LIKE '%picsum.photos/seed/ch2-art3%';
