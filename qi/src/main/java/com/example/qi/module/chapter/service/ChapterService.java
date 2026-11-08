package com.example.qi.module.chapter.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.BusinessException;
import com.example.qi.common.ContentStatus;
import com.example.qi.module.artifact.entity.Artifact;
import com.example.qi.module.artifact.mapper.ArtifactMapper;
import com.example.qi.module.chapter.dto.ChapterSaveRequest;
import com.example.qi.module.chapter.entity.Chapter;
import com.example.qi.module.chapter.entity.ChapterNode;
import com.example.qi.module.chapter.entity.NodeChoice;
import com.example.qi.module.chapter.mapper.ChapterMapper;
import com.example.qi.module.chapter.mapper.ChapterNodeMapper;
import com.example.qi.module.craft.entity.Craft;
import com.example.qi.module.craft.entity.CraftStep;
import com.example.qi.module.craft.mapper.CraftMapper;
import com.example.qi.module.craft.mapper.CraftStepMapper;
import com.example.qi.module.culture.entity.TimelineEvent;
import com.example.qi.module.culture.mapper.TimelineEventMapper;
import com.example.qi.module.inheritor.entity.Inheritor;
import com.example.qi.module.inheritor.mapper.InheritorMapper;
import com.example.qi.module.media.entity.MediaAsset;
import com.example.qi.module.media.mapper.MediaAssetMapper;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.publish.service.PublishRecordService;
import com.example.qi.module.relation.dto.RelationSaveRequest;
import com.example.qi.module.relation.entity.ContentRelation;
import com.example.qi.module.relation.service.RelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 章节业务（ADM-03 / FR-02 / FR-03）：CRUD、发布校验与发布记录、关联、小程序视图。
 * 发布流程与藏品一致（7.15.4）；发布校验含“至少一个有效叙事节点”（5.2）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterMapper chapterMapper;
    private final ChapterNodeMapper chapterNodeMapper;
    private final ChapterNodeService chapterNodeService;
    private final ChapterFlowService chapterFlowService;
    private final RelationService relationService;
    private final PublishRecordService publishRecordService;
    private final ArtifactMapper artifactMapper;
    private final CraftMapper craftMapper;
    private final CraftStepMapper craftStepMapper;
    private final InheritorMapper inheritorMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final TimelineEventMapper timelineEventMapper;
    private final ObjectMapper objectMapper;

    /** 后台章节详情：章节 + 节点基础列表 + 关联原始列表 */
    public record AdminChapterDetail(Chapter chapter, List<ChapterNode> nodes, List<ContentRelation> relations) {
    }

    /** 小程序关联卡（5.4：前台只展示有效目标） */
    public record AppRelationCard(String targetType, Long targetId, String name) {
    }

    /** 小程序节点视图：五类节点字段扁平化，前端按 nodeType 取用；TEXT 的 contentHtml 为图文混排富文本 */
    public record AppNodeView(Long id, String nodeType, String title, String subtitle,
                              String content, String source,
                              String contentHtml,
                              List<MediaAsset> media, List<TimelineEvent> events,
                              String quoteText, String quoteSource,
                              String inheritorName, String inheritorPhotoUrl,
                              Craft craft, List<CraftStep> steps,
                              Long nextNodeId, List<AppChoiceView> choices) {
    }

    /** 小程序分支选项视图（仅 BRANCH 节点非空，分支叙事设计稿 §9.7） */
    public record AppChoiceView(Long id, String label, Long targetNodeId) {
    }

    public record AppChapterDetail(Chapter chapter, List<AppNodeView> nodes, List<AppRelationCard> relations) {
    }

    // ---------------------------- 后台 ----------------------------

    public Page<Chapter> page(long pageNum, long pageSize, Integer status, String keyword) {
        return chapterMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<Chapter>lambdaQuery()
                        .eq(status != null, Chapter::getStatus, status)
                        .like(StringUtils.hasText(keyword), Chapter::getTitle, keyword)
                        .orderByAsc(Chapter::getSortOrder)
                        .orderByAsc(Chapter::getId));
    }

    public AdminChapterDetail detail(Long id) {
        Chapter chapter = requireChapter(id);
        return new AdminChapterDetail(chapter,
                chapterNodeService.listByChapter(id),
                relationService.listBySource("chapter", id));
    }

    public Long save(ChapterSaveRequest request) {
        if (request.getChapterType() == null
                || (request.getChapterType() != 0 && request.getChapterType() != 1)) {
            throw new BusinessException("章节类型无效（0 序章 / 1 正章，D-02）");
        }
        if (request.getId() == null) {
            Chapter chapter = new Chapter();
            copyProperties(request, chapter);
            chapter.setStatus(ContentStatus.DRAFT.getCode());
            if (chapter.getSortOrder() == null) {
                chapter.setSortOrder(nextSortOrder());
            }
            chapterMapper.insert(chapter);
            return chapter.getId();
        }
        requireChapter(request.getId());
        chapterMapper.update(null, Wrappers.<Chapter>lambdaUpdate()
                .eq(Chapter::getId, request.getId())
                .set(Chapter::getTitle, request.getTitle())
                .set(Chapter::getSubtitle, request.getSubtitle())
                .set(Chapter::getCoverUrl, request.getCoverUrl())
                .set(Chapter::getSummary, request.getSummary())
                .set(Chapter::getChapterType, request.getChapterType())
                .set(request.getSortOrder() != null, Chapter::getSortOrder, request.getSortOrder()));
        return request.getId();
    }

    /** 删除章节：节点一并逻辑删除（7.10 软删除） */
    public void delete(Long id) {
        Chapter chapter = requireChapter(id);
        chapterNodeMapper.delete(Wrappers.<ChapterNode>lambdaQuery()
                .eq(ChapterNode::getChapterId, id));
        chapterMapper.deleteById(id);
        log.info("章节删除（节点一并删除）：chapterId={} title={}", id, chapter.getTitle());
    }

    public PublishRecord publish(Long id, String modifySummary) {
        Chapter chapter = requireChapter(id);
        validateForPublish(chapter, id);
        String snapshot = toJson(chapter);
        PublishRecord record = publishRecordService.createPublishing(
                "content", null, "chapter", id, modifySummary, snapshot);
        try {
            chapter.setStatus(ContentStatus.PUBLISHED.getCode());
            chapterMapper.updateById(chapter);
            publishRecordService.finish(record, true, null);
            log.info("章节发布成功：chapterId={} title={} modifySummary={}", id, chapter.getTitle(), modifySummary);
            return record;
        } catch (RuntimeException e) {
            publishRecordService.finish(record, false, e.getMessage());
            log.error("章节发布失败：chapterId={} title={}", id, chapter.getTitle(), e);
            throw e;
        }
    }

    public void offShelf(Long id) {
        Chapter chapter = requireChapter(id);
        chapter.setStatus(ContentStatus.OFF_SHELF.getCode());
        chapterMapper.updateById(chapter);
        log.info("章节下架：chapterId={} title={}", id, chapter.getTitle());
    }

    public void setRelations(Long id, RelationSaveRequest request) {
        requireChapter(id);
        List<ContentRelation> relations = new ArrayList<>();
        if (request.getTargets() != null) {
            for (RelationSaveRequest.TargetItem item : request.getTargets()) {
                ContentRelation relation = new ContentRelation();
                relation.setTargetType(item.getTargetType());
                relation.setTargetId(item.getTargetId());
                relations.add(relation);
            }
        }
        relationService.setRelations("chapter", id, relations);
    }

    // ---------------------------- 小程序端 ----------------------------

    /** 云展馆章节列表（FR-02）：仅已发布，按配置顺序，D-02 数量不写死 */
    public List<Chapter> appList() {
        return chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery()
                .eq(Chapter::getStatus, ContentStatus.PUBLISHED.getCode())
                .orderByAsc(Chapter::getSortOrder)
                .orderByAsc(Chapter::getId));
    }

    /** 章节详情（FR-03）：节点按配置顺序 + 类型扩展数据 + 有效关联卡；BRANCH 节点附选项（分支叙事） */
    public AppChapterDetail appDetail(Long id) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null || chapter.getStatus() != ContentStatus.PUBLISHED.getCode()) {
            throw new BusinessException(404, "章节不存在或已下架");
        }
        List<ChapterNode> stored = chapterNodeService.listByChapter(id);
        // 选项一次 IN 查询分组（§9.7：避免节点循环内 N 次查询）
        List<NodeChoice> allChoices = chapterFlowService.listChoices(stored);
        Map<Long, List<NodeChoice>> choiceMap = allChoices.stream()
                .collect(Collectors.groupingBy(NodeChoice::getNodeId));
        List<AppNodeView> nodes = new ArrayList<>();
        for (ChapterNode node : stored) {
            nodes.add(toAppNodeView(node, chapterNodeService.detail(node.getId()),
                    choiceMap.getOrDefault(node.getId(), List.of())));
        }
        return new AppChapterDetail(chapter, nodes, buildRelationCards(id));
    }

    // ---------------------------- 私有方法 ----------------------------

    private AppNodeView toAppNodeView(ChapterNode node, ChapterNodeService.NodeDetail detail,
                                      List<NodeChoice> choices) {
        List<MediaAsset> media = resolveMedia(detail.mediaRefs());
        List<TimelineEvent> events = List.of();
        String quoteText = null;
        String quoteSource = null;
        String inheritorName = null;
        String inheritorPhotoUrl = null;
        Craft craft = null;
        List<CraftStep> steps = List.of();
        String contentHtml = "";
        switch (node.getNodeType()) {
            case "TIMELINE" -> {
                List<Long> eventIds = detail.eventRefs().stream()
                        .map(com.example.qi.module.chapter.entity.NodeEventRelation::getEventId).toList();
                if (!eventIds.isEmpty()) {
                    Map<Long, TimelineEvent> map = timelineEventMapper.selectBatchIds(eventIds).stream()
                            .collect(Collectors.toMap(TimelineEvent::getId, e -> e));
                    events = eventIds.stream().map(map::get).filter(Objects::nonNull).toList();
                }
            }
            case "QUOTE" -> {
                if (detail.quote() != null) {
                    quoteText = detail.quote().getQuoteText();
                    quoteSource = detail.quote().getSource();
                    Inheritor inheritor = inheritorMapper.selectById(detail.quote().getInheritorId());
                    if (inheritor != null) {
                        inheritorName = inheritor.getName();
                        inheritorPhotoUrl = inheritor.getPhotoUrl();
                    }
                }
            }
            case "PROCESS" -> {
                List<com.example.qi.module.chapter.entity.NodeCraftRelation> refs = detail.craftRefs();
                if (!refs.isEmpty()) {
                    craft = craftMapper.selectById(refs.get(0).getCraftId());
                    List<Long> stepIds = refs.stream()
                            .map(com.example.qi.module.chapter.entity.NodeCraftRelation::getCraftStepId)
                            .filter(Objects::nonNull).toList();
                    if (!stepIds.isEmpty()) {
                        Map<Long, CraftStep> map = craftStepMapper.selectBatchIds(stepIds).stream()
                                .collect(Collectors.toMap(CraftStep::getId, s -> s));
                        steps = stepIds.stream().map(map::get).filter(Objects::nonNull).toList();
                    }
                }
            }
            default -> {
                // TEXT：输出图文混排富文本 HTML，历史块状 JSON/纯文本自动转换
                contentHtml = buildTextHtml(node.getNodeType(), node.getContent(), detail.mediaRefs());
            }
        }
        List<AppChoiceView> choiceViews = choices.stream()
                .map(c -> new AppChoiceView(c.getId(), c.getLabel(), c.getTargetNodeId())).toList();
        return new AppNodeView(node.getId(), node.getNodeType(), node.getTitle(), node.getSubtitle(),
                node.getContent(), node.getSource(), contentHtml, media, events,
                quoteText, quoteSource, inheritorName, inheritorPhotoUrl, craft, steps,
                node.getNextNodeId(), choiceViews);
    }

    /**
     * TEXT 正文输出为 HTML：富文本编辑器产物（< 开头）直接输出；
     * 历史块状 JSON / 纯文本自动转换为段落 + 图片混排 HTML（图片补访问地址，术语标记保留为文本）。
     */
    private String buildTextHtml(String nodeType, String content,
                                 List<com.example.qi.module.chapter.entity.NodeMediaRelation> refs) {
        if (!"TEXT".equals(nodeType)) {
            return "";
        }
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("<")) {
            return content;
        }
        StringBuilder html = new StringBuilder();
        List<Map<String, Object>> blocks = tryParseBlocks(trimmed);
        if (blocks != null) {
            for (Map<String, Object> block : blocks) {
                String type = String.valueOf(block.get("type"));
                if ("image".equals(type)) {
                    String caption = block.get("caption") == null ? "" : String.valueOf(block.get("caption"));
                    html.append(imageHtml(asMediaUrl(block.get("mediaId")), caption));
                } else {
                    html.append(paragraphHtml(block.get("text") == null ? "" : String.valueOf(block.get("text"))));
                }
            }
            return html.toString();
        }
        if (StringUtils.hasText(trimmed)) {
            for (String line : content.split("\n+")) {
                html.append(paragraphHtml(line));
            }
        }
        for (com.example.qi.module.chapter.entity.NodeMediaRelation ref : refs) {
            html.append(imageHtml(asMediaUrl(ref.getMediaId()), ref.getCaption() == null ? "" : ref.getCaption()));
        }
        return html.toString();
    }

    private String asMediaUrl(Object mediaId) {
        if (mediaId == null) {
            return "";
        }
        MediaAsset asset = mediaAssetMapper.selectById(Long.valueOf(String.valueOf(mediaId)));
        return asset == null ? "" : asset.getUrl();
    }

    private String paragraphHtml(String text) {
        String safe = escapeHtml(text == null ? "" : text.trim());
        return safe.isEmpty() ? "" : "<p>" + safe + "</p>";
    }

    private String imageHtml(String url, String caption) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<p><img src=\"").append(url).append("\" style=\"max-width:100%;\" /></p>");
        if (StringUtils.hasText(caption)) {
            sb.append("<p style=\"color:#81776d;font-size:12px;\">").append(escapeHtml(caption)).append("</p>");
        }
        return sb.toString();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tryParseBlocks(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String trimmed = content.trim();
        if (!trimmed.startsWith("[")) {
            return null;
        }
        try {
            return objectMapper.readValue(trimmed, List.class);
        } catch (Exception e) {
            return null;
        }
    }

    private List<MediaAsset> resolveMedia(List<com.example.qi.module.chapter.entity.NodeMediaRelation> refs) {
        if (refs.isEmpty()) {
            return List.of();
        }
        List<Long> ids = refs.stream()
                .map(com.example.qi.module.chapter.entity.NodeMediaRelation::getMediaId).toList();
        Map<Long, MediaAsset> map = mediaAssetMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MediaAsset::getId, m -> m));
        return refs.stream().map(r -> map.get(r.getMediaId())).filter(Objects::nonNull).toList();
    }

    /** 关联卡过滤：藏品要求已发布；工艺/传承人当前仅要求存在（TODO 目标已发布校验，5.4） */
    private List<AppRelationCard> buildRelationCards(Long chapterId) {
        List<AppRelationCard> cards = new ArrayList<>();
        for (ContentRelation relation : relationService.listBySource("chapter", chapterId)) {
            switch (relation.getTargetType()) {
                case "artifact" -> {
                    Artifact artifact = artifactMapper.selectById(relation.getTargetId());
                    if (artifact != null && artifact.getStatus() == ContentStatus.PUBLISHED.getCode()) {
                        cards.add(new AppRelationCard("artifact", artifact.getId(), artifact.getName()));
                    }
                }
                case "craft" -> {
                    Craft craft = craftMapper.selectById(relation.getTargetId());
                    if (craft != null && craft.getStatus() == ContentStatus.PUBLISHED.getCode()) {
                        cards.add(new AppRelationCard("craft", craft.getId(), craft.getName()));
                    }
                }
                case "inheritor" -> {
                    Inheritor inheritor = inheritorMapper.selectById(relation.getTargetId());
                    if (inheritor != null && inheritor.getStatus() == ContentStatus.PUBLISHED.getCode()) {
                        cards.add(new AppRelationCard("inheritor", inheritor.getId(), inheritor.getName()));
                    }
                }
                default -> {
                    // 章节关联卡暂不支持其他目标类型
                }
            }
        }
        return cards;
    }

    /** 章节发布校验（5.2：标题、封面、摘要、章节类型、排序值、至少一个有效叙事节点） */
    private void validateForPublish(Chapter chapter, Long id) {
        if (!StringUtils.hasText(chapter.getTitle()) || !StringUtils.hasText(chapter.getCoverUrl())
                || !StringUtils.hasText(chapter.getSummary()) || chapter.getChapterType() == null
                || chapter.getSortOrder() == null) {
            throw new BusinessException("发布条件不满足：标题、封面、摘要、章节类型和排序值必填（5.2）");
        }
        List<ChapterNode> nodes = chapterNodeService.listByChapter(id);
        if (nodes.isEmpty()) {
            throw new BusinessException("发布条件不满足：至少一个有效叙事节点（5.2）");
        }
        for (ChapterNode node : nodes) {
            chapterNodeService.validateStoredNode(node);
        }
        // 分支叙事流向图校验（§3.3 硬阻断 + 软提示；BRANCH 类型数据由 validateStoredNode 之外的图规则覆盖）
        List<NodeChoice> choices = chapterFlowService.listChoices(nodes);
        chapterFlowService.validateForPublish(nodes, choices);
    }

    private void copyProperties(ChapterSaveRequest request, Chapter chapter) {
        chapter.setTitle(request.getTitle());
        chapter.setSubtitle(request.getSubtitle());
        chapter.setCoverUrl(request.getCoverUrl());
        chapter.setSummary(request.getSummary());
        chapter.setChapterType(request.getChapterType());
        chapter.setSortOrder(request.getSortOrder());
    }

    private int nextSortOrder() {
        Chapter last = chapterMapper.selectOne(Wrappers.<Chapter>lambdaQuery()
                .orderByDesc(Chapter::getSortOrder)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 1 : last.getSortOrder() + 1;
    }

    private Chapter requireChapter(Long id) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        return chapter;
    }

    private String toJson(Chapter chapter) {
        try {
            return objectMapper.writeValueAsString(chapter);
        } catch (Exception e) {
            throw new BusinessException("生成发布快照失败");
        }
    }
}
