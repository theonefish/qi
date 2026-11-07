package com.example.qi.module.artifact.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.BusinessException;
import com.example.qi.common.ContentStatus;
import com.example.qi.module.artifact.dto.ArtifactPageRequest;
import com.example.qi.module.artifact.dto.ArtifactSaveRequest;
import com.example.qi.module.artifact.entity.Artifact;
import com.example.qi.module.artifact.entity.ArtifactCategory;
import com.example.qi.module.artifact.mapper.ArtifactCategoryMapper;
import com.example.qi.module.artifact.mapper.ArtifactMapper;
import com.example.qi.module.auth.service.AuthService;
import com.example.qi.module.chapter.entity.Chapter;
import com.example.qi.module.chapter.mapper.ChapterMapper;
import com.example.qi.module.craft.entity.Craft;
import com.example.qi.module.craft.mapper.CraftMapper;
import com.example.qi.module.inheritor.entity.Inheritor;
import com.example.qi.module.inheritor.mapper.InheritorMapper;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.publish.service.PublishRecordService;
import com.example.qi.module.relation.dto.RelationSaveRequest;
import com.example.qi.module.relation.entity.ContentRelation;
import com.example.qi.module.relation.service.RelationService;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 藏品业务（ADM-04 / FR-07 / FR-08）：CRUD、简单自审、发布与下架。
 * 发布流程遵循 7.15.4：插入“发布中”记录 → 更新内容 → 更新为成功/失败，
 * 失败记录保留且内容状态不变。
 */
@Service
@RequiredArgsConstructor
public class ArtifactService {

    private final ArtifactMapper artifactMapper;
    private final ArtifactCategoryMapper categoryMapper;
    private final ChapterMapper chapterMapper;
    private final CraftMapper craftMapper;
    private final InheritorMapper inheritorMapper;
    private final RelationService relationService;
    private final PublishRecordService publishRecordService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public record ArtifactDetail(Artifact artifact, String categoryName, List<ContentRelation> relations) {
    }

    /** 小程序关联卡（5.4：仅有效目标） */
    public record RelationCard(String targetType, Long targetId, String name, String image) {
    }

    public record AppArtifactDetail(Artifact artifact, String categoryName, List<RelationCard> relations) {
    }

    /** 小程序藏品详情（FR-08）：类别名称 + 有效关联卡 */
    public AppArtifactDetail appDetail(Long id) {
        Artifact artifact = artifactMapper.selectById(id);
        if (artifact == null || artifact.getStatus() != ContentStatus.PUBLISHED.getCode()) {
            throw new BusinessException(404, "内容不存在或已下架");
        }
        ArtifactCategory category = categoryMapper.selectById(artifact.getCategoryId());
        List<RelationCard> cards = new ArrayList<>();
        for (ContentRelation relation : relationService.listBySource("artifact", id)) {
            switch (relation.getTargetType()) {
                case "chapter" -> {
                    Chapter chapter = chapterMapper.selectById(relation.getTargetId());
                    if (chapter != null && chapter.getStatus() == ContentStatus.PUBLISHED.getCode()) {
                        cards.add(new RelationCard("chapter", chapter.getId(), chapter.getTitle(), chapter.getCoverUrl()));
                    }
                }
                case "craft" -> {
                    Craft craft = craftMapper.selectById(relation.getTargetId());
                    if (craft != null && craft.getStatus() == ContentStatus.PUBLISHED.getCode()) {
                        cards.add(new RelationCard("craft", craft.getId(), craft.getName(), null));
                    }
                }
                case "inheritor" -> {
                    Inheritor inheritor = inheritorMapper.selectById(relation.getTargetId());
                    if (inheritor != null && inheritor.getStatus() == ContentStatus.PUBLISHED.getCode()) {
                        cards.add(new RelationCard("inheritor", inheritor.getId(), inheritor.getName(), inheritor.getPhotoUrl()));
                    }
                }
                default -> {
                }
            }
        }
        return new AppArtifactDetail(artifact, category == null ? null : category.getName(), cards);
    }

    public Page<Artifact> page(ArtifactPageRequest request) {
        return artifactMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()),
                Wrappers.<Artifact>lambdaQuery()
                        .like(StringUtils.hasText(request.getName()), Artifact::getName, request.getName())
                        .eq(request.getCategoryId() != null, Artifact::getCategoryId, request.getCategoryId())
                        .eq(request.getStatus() != null, Artifact::getStatus, request.getStatus())
                        .orderByDesc(Artifact::getCreateTime));
    }

    public ArtifactDetail getDetail(Long id) {
        Artifact artifact = requireArtifact(id);
        ArtifactCategory category = categoryMapper.selectById(artifact.getCategoryId());
        List<ContentRelation> relations = relationService.listBySource("artifact", id);
        return new ArtifactDetail(artifact, category == null ? null : category.getName(), relations);
    }

    public Long save(ArtifactSaveRequest request) {
        requireCategory(request.getCategoryId());
        Artifact artifact = new Artifact();
        copyProperties(request, artifact);
        if (request.getId() == null) {
            artifact.setStatus(ContentStatus.DRAFT.getCode());
            artifact.setAuditStatus(0);
            artifactMapper.insert(artifact);
            return artifact.getId();
        }
        artifact.setId(request.getId());
        requireArtifact(artifact.getId());
        // 已发布内容编辑即时生效（D-17 简化口径：保存后校验由发布流程承担，严格模式后续迭代细化）
        artifactMapper.updateById(artifact);
        return artifact.getId();
    }

    /** 简单自审（D-20）：审核人自动取当前管理员；不通过时显式清空审核人/时间 */
    public void audit(Long id, boolean pass) {
        requireArtifact(id);
        artifactMapper.update(null, Wrappers.<Artifact>lambdaUpdate()
                .eq(Artifact::getId, id)
                .set(Artifact::getAuditStatus, pass ? 1 : 0)
                .set(pass, Artifact::getAuditBy, authService.currentOperator().name())
                .set(pass, Artifact::getAuditTime, LocalDateTime.now())
                .set(!pass, Artifact::getAuditBy, null)
                .set(!pass, Artifact::getAuditTime, null));
    }

    public PublishRecord publish(Long id, String modifySummary) {
        Artifact artifact = requireArtifact(id);
        validateForPublish(artifact);
        String snapshot = toJson(artifact);
        PublishRecord record = publishRecordService.createPublishing(
                "content", null, "artifact", id, modifySummary, snapshot);
        try {
            artifact.setStatus(ContentStatus.PUBLISHED.getCode());
            artifactMapper.updateById(artifact);
            publishRecordService.finish(record, true, null);
            return record;
        } catch (RuntimeException e) {
            // 发布失败保留失败记录（7.14），内容状态由事务外最后更新保护
            publishRecordService.finish(record, false, e.getMessage());
            throw e;
        }
    }

    public void offShelf(Long id) {
        Artifact artifact = requireArtifact(id);
        artifact.setStatus(ContentStatus.OFF_SHELF.getCode());
        artifactMapper.updateById(artifact);
    }

    public void setRelations(Long id, RelationSaveRequest request) {
        requireArtifact(id);
        List<ContentRelation> relations = new ArrayList<>();
        if (request.getTargets() != null) {
            for (RelationSaveRequest.TargetItem item : request.getTargets()) {
                ContentRelation relation = new ContentRelation();
                relation.setTargetType(item.getTargetType());
                relation.setTargetId(item.getTargetId());
                relations.add(relation);
            }
        }
        relationService.setRelations("artifact", id, relations);
    }

    private void validateForPublish(Artifact artifact) {
        if (!StringUtils.hasText(artifact.getName()) || artifact.getCategoryId() == null
                || !StringUtils.hasText(artifact.getMainImageUrl()) || !StringUtils.hasText(artifact.getSource())) {
            throw new BusinessException("发布条件不满足：名称、类别、主图和资料来源必填（5.2）");
        }
        if (artifact.getAuditStatus() == null || artifact.getAuditStatus() != 1) {
            throw new BusinessException("发布条件不满足：内容尚未审核通过（D-20）");
        }
        if (relationService.countValidRelations("artifact", artifact.getId()) < 1) {
            throw new BusinessException("发布条件不满足：至少关联一个已发布的章节或工艺（5.2 / 5.4）");
        }
    }

    private Artifact requireArtifact(Long id) {
        Artifact artifact = artifactMapper.selectById(id);
        if (artifact == null) {
            throw new BusinessException("藏品不存在");
        }
        return artifact;
    }

    private void requireCategory(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException("藏品类别不存在，请先在类别字典中维护（D-27）");
        }
    }

    private void copyProperties(ArtifactSaveRequest request, Artifact artifact) {
        artifact.setCategoryId(request.getCategoryId());
        artifact.setName(request.getName());
        artifact.setEra(request.getEra());
        artifact.setMaterial(request.getMaterial());
        artifact.setCraftDesc(request.getCraftDesc());
        artifact.setStory(request.getStory());
        artifact.setMotifMeaning(request.getMotifMeaning());
        artifact.setMainImageUrl(request.getMainImageUrl());
        artifact.setSource(request.getSource());
        artifact.setAuthorizationNote(request.getAuthorizationNote());
    }

    private String toJson(Artifact artifact) {
        try {
            return objectMapper.writeValueAsString(artifact);
        } catch (Exception e) {
            throw new BusinessException("生成发布快照失败");
        }
    }
}
