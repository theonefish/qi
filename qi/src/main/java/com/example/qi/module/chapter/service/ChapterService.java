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
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterMapper chapterMapper;
    private final ChapterNodeMapper chapterNodeMapper;
    private final ChapterNodeService chapterNodeService;
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

    /** 小程序节点视图：四类节点字段扁平化，前端按 nodeType 取用 */
    public record AppNodeView(Long id, String nodeType, String title, String subtitle,
                              String content, String source,
                              List<MediaAsset> media, List<TimelineEvent> events,
                              String quoteText, String quoteSource,
                              String inheritorName, String inheritorPhotoUrl,
                              Craft craft, List<CraftStep> steps) {
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
        requireChapter(id);
        chapterNodeMapper.delete(Wrappers.<ChapterNode>lambdaQuery()
                .eq(ChapterNode::getChapterId, id));
        chapterMapper.deleteById(id);
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
            return record;
        } catch (RuntimeException e) {
            publishRecordService.finish(record, false, e.getMessage());
            throw e;
        }
    }

    public void offShelf(Long id) {
        Chapter chapter = requireChapter(id);
        chapter.setStatus(ContentStatus.OFF_SHELF.getCode());
        chapterMapper.updateById(chapter);
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

    /** 章节详情（FR-03）：节点按配置顺序 + 类型扩展数据 + 有效关联卡 */
    public AppChapterDetail appDetail(Long id) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null || chapter.getStatus() != ContentStatus.PUBLISHED.getCode()) {
            throw new BusinessException(404, "章节不存在或已下架");
        }
        List<AppNodeView> nodes = new ArrayList<>();
        for (ChapterNode node : chapterNodeService.listByChapter(id)) {
            nodes.add(toAppNodeView(node, chapterNodeService.detail(node.getId())));
        }
        return new AppChapterDetail(chapter, nodes, buildRelationCards(id));
    }

    // ---------------------------- 私有方法 ----------------------------

    private AppNodeView toAppNodeView(ChapterNode node, ChapterNodeService.NodeDetail detail) {
        List<MediaAsset> media = resolveMedia(detail.mediaRefs());
        List<TimelineEvent> events = List.of();
        String quoteText = null;
        String quoteSource = null;
        String inheritorName = null;
        String inheritorPhotoUrl = null;
        Craft craft = null;
        List<CraftStep> steps = List.of();
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
            }
        }
        return new AppNodeView(node.getId(), node.getNodeType(), node.getTitle(), node.getSubtitle(),
                node.getContent(), node.getSource(), media, events,
                quoteText, quoteSource, inheritorName, inheritorPhotoUrl, craft, steps);
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
