package com.example.qi.module.relation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.qi.common.ContentStatus;
import com.example.qi.module.artifact.entity.Artifact;
import com.example.qi.module.artifact.mapper.ArtifactMapper;
import com.example.qi.module.chapter.entity.Chapter;
import com.example.qi.module.chapter.mapper.ChapterMapper;
import com.example.qi.module.craft.entity.Craft;
import com.example.qi.module.craft.mapper.CraftMapper;
import com.example.qi.module.culture.entity.TimelineEvent;
import com.example.qi.module.culture.mapper.TimelineEventMapper;
import com.example.qi.module.inheritor.entity.Inheritor;
import com.example.qi.module.inheritor.mapper.InheritorMapper;
import com.example.qi.module.motif.entity.Motif;
import com.example.qi.module.motif.mapper.MotifMapper;
import com.example.qi.module.relation.entity.ContentRelation;
import com.example.qi.module.relation.mapper.ContentRelationMapper;
import com.example.qi.module.term.entity.Term;
import com.example.qi.module.term.mapper.TermMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 内容关联服务（ADM-09）。
 * isTargetVisible 统一实现 5.4：关联目标必须存在且已发布；发布校验与前台关联卡共用该口径。
 */
@Service
@RequiredArgsConstructor
public class RelationService {

    private final ContentRelationMapper contentRelationMapper;
    private final ChapterMapper chapterMapper;
    private final ArtifactMapper artifactMapper;
    private final CraftMapper craftMapper;
    private final InheritorMapper inheritorMapper;
    private final TermMapper termMapper;
    private final TimelineEventMapper timelineEventMapper;
    private final MotifMapper motifMapper;

    /** 整体替换某内容对象的关联列表 */
    @Transactional
    public void setRelations(String sourceType, Long sourceId, List<ContentRelation> relations) {
        contentRelationMapper.delete(Wrappers.<ContentRelation>lambdaQuery()
                .eq(ContentRelation::getSourceType, sourceType)
                .eq(ContentRelation::getSourceId, sourceId));
        for (int i = 0; i < relations.size(); i++) {
            ContentRelation relation = relations.get(i);
            relation.setId(null);
            relation.setSourceType(sourceType);
            relation.setSourceId(sourceId);
            relation.setSortOrder(i);
            contentRelationMapper.insert(relation);
        }
    }

    public List<ContentRelation> listBySource(String sourceType, Long sourceId) {
        return contentRelationMapper.selectList(Wrappers.<ContentRelation>lambdaQuery()
                .eq(ContentRelation::getSourceType, sourceType)
                .eq(ContentRelation::getSourceId, sourceId)
                .orderByAsc(ContentRelation::getSortOrder));
    }

    /** 反向关联：某内容被哪些内容引用（7.9） */
    public List<ContentRelation> listByTarget(String targetType, Long targetId) {
        return contentRelationMapper.selectList(Wrappers.<ContentRelation>lambdaQuery()
                .eq(ContentRelation::getTargetType, targetType)
                .eq(ContentRelation::getTargetId, targetId));
    }

    /** 原始关联计数（不做目标状态过滤） */
    public long countBySource(String sourceType, Long sourceId) {
        Long count = contentRelationMapper.selectCount(Wrappers.<ContentRelation>lambdaQuery()
                .eq(ContentRelation::getSourceType, sourceType)
                .eq(ContentRelation::getSourceId, sourceId));
        return count == null ? 0 : count;
    }

    /** 有效关联计数：目标存在且已发布（5.2 / 5.4，发布校验用） */
    public long countValidRelations(String sourceType, Long sourceId) {
        long valid = 0;
        for (ContentRelation relation : listBySource(sourceType, sourceId)) {
            if (isTargetVisible(relation.getTargetType(), relation.getTargetId())) {
                valid++;
            }
        }
        return valid;
    }

    /** 目标前台可见性：存在且已发布（5.3） */
    public boolean isTargetVisible(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return false;
        }
        return switch (targetType) {
            case "chapter" -> {
                Chapter chapter = chapterMapper.selectById(targetId);
                yield chapter != null && chapter.getStatus() == ContentStatus.PUBLISHED.getCode();
            }
            case "artifact" -> {
                Artifact artifact = artifactMapper.selectById(targetId);
                yield artifact != null && artifact.getStatus() == ContentStatus.PUBLISHED.getCode();
            }
            case "craft" -> {
                Craft craft = craftMapper.selectById(targetId);
                yield craft != null && craft.getStatus() == ContentStatus.PUBLISHED.getCode();
            }
            case "inheritor" -> {
                Inheritor inheritor = inheritorMapper.selectById(targetId);
                yield inheritor != null && inheritor.getStatus() == ContentStatus.PUBLISHED.getCode();
            }
            case "term" -> {
                Term term = termMapper.selectById(targetId);
                yield term != null && term.getStatus() == ContentStatus.PUBLISHED.getCode();
            }
            case "event" -> {
                TimelineEvent event = timelineEventMapper.selectById(targetId);
                yield event != null && event.getStatus() == ContentStatus.PUBLISHED.getCode();
            }
            case "motif" -> {
                Motif motif = motifMapper.selectById(targetId);
                yield motif != null && motif.getStatus() == ContentStatus.PUBLISHED.getCode();
            }
            default -> false;
        };
    }
}
