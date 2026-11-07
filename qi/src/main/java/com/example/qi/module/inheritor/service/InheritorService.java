package com.example.qi.module.inheritor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.qi.common.BusinessException;
import com.example.qi.common.ContentStatus;
import com.example.qi.module.artifact.entity.Artifact;
import com.example.qi.module.artifact.mapper.ArtifactMapper;
import com.example.qi.module.auth.service.AuthService;
import com.example.qi.module.craft.entity.Craft;
import com.example.qi.module.craft.mapper.CraftMapper;
import com.example.qi.module.inheritor.entity.Inheritor;
import com.example.qi.module.inheritor.mapper.InheritorMapper;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.publish.service.PublishRecordService;
import com.example.qi.module.relation.entity.ContentRelation;
import com.example.qi.module.relation.service.RelationService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 传承人服务（ADM-06 / FR-11）。
 * 发布条件（5.2）：姓名、简介、人物照片、资料来源、审核记录。
 */
@Service
@RequiredArgsConstructor
public class InheritorService {

    private final InheritorMapper inheritorMapper;
    private final PublishRecordService publishRecordService;
    private final AuthService authService;
    private final RelationService relationService;
    private final ArtifactMapper artifactMapper;
    private final CraftMapper craftMapper;
    private final ObjectMapper objectMapper;

    public record InheritorSave(@NotBlank(message = "姓名不能为空") String name,
                                String photoUrl, String intro, String career, String story, String source) {
    }

    public record ArtifactCard(Long id, String name, String mainImageUrl) {
    }

    public record CraftCard(Long id, String name) {
    }

    public record AppInheritorDetail(Inheritor inheritor, List<ArtifactCard> works, List<CraftCard> crafts) {
    }

    // ---------------------------- 后台 ----------------------------

    public List<Inheritor> list() {
        return inheritorMapper.selectList(Wrappers.<Inheritor>lambdaQuery()
                .orderByDesc(Inheritor::getCreateTime));
    }

    public Long save(Long id, InheritorSave save) {
        if (id == null) {
            Inheritor inheritor = new Inheritor();
            apply(save, inheritor);
            inheritor.setStatus(ContentStatus.DRAFT.getCode());
            inheritor.setAuditStatus(0);
            inheritorMapper.insert(inheritor);
            return inheritor.getId();
        }
        requireInheritor(id);
        inheritorMapper.update(null, Wrappers.<Inheritor>lambdaUpdate()
                .eq(Inheritor::getId, id)
                .set(Inheritor::getName, save.name())
                .set(Inheritor::getPhotoUrl, save.photoUrl())
                .set(Inheritor::getIntro, save.intro())
                .set(Inheritor::getCareer, save.career())
                .set(Inheritor::getStory, save.story())
                .set(Inheritor::getSource, save.source()));
        return id;
    }

    public void delete(Long id) {
        requireInheritor(id);
        inheritorMapper.deleteById(id);
    }

    /** 简单自审（D-20）：审核人自动取当前管理员，不通过时清空审核人/时间 */
    public void audit(Long id, boolean pass) {
        requireInheritor(id);
        inheritorMapper.update(null, Wrappers.<Inheritor>lambdaUpdate()
                .eq(Inheritor::getId, id)
                .set(Inheritor::getAuditStatus, pass ? 1 : 0)
                .set(pass, Inheritor::getAuditBy, authService.currentOperator().name())
                .set(pass, Inheritor::getAuditTime, LocalDateTime.now())
                .set(!pass, Inheritor::getAuditBy, null)
                .set(!pass, Inheritor::getAuditTime, null));
    }

    /** 发布（5.2：姓名、简介、照片、资料来源、审核记录） */
    public PublishRecord publish(Long id, String modifySummary) {
        Inheritor inheritor = requireInheritor(id);
        validateForPublish(inheritor);
        PublishRecord record = publishRecordService.createPublishing(
                "content", null, "inheritor", id, modifySummary, toJson(inheritor));
        try {
            inheritor.setStatus(ContentStatus.PUBLISHED.getCode());
            inheritorMapper.updateById(inheritor);
            publishRecordService.finish(record, true, null);
            return record;
        } catch (RuntimeException e) {
            publishRecordService.finish(record, false, e.getMessage());
            throw e;
        }
    }

    public void offShelf(Long id) {
        Inheritor inheritor = requireInheritor(id);
        inheritor.setStatus(ContentStatus.OFF_SHELF.getCode());
        inheritorMapper.updateById(inheritor);
    }

    // ---------------------------- 小程序端（FR-11） ----------------------------

    /** 传承人列表：仅已发布，含姓名、照片、简介 */
    public List<Inheritor> appList() {
        return inheritorMapper.selectList(Wrappers.<Inheritor>lambdaQuery()
                .eq(Inheritor::getStatus, ContentStatus.PUBLISHED.getCode())
                .orderByDesc(Inheritor::getCreateTime));
    }

    /** 传承人详情：代表作品（关联藏品）与擅长技艺（关联工艺），仅有效目标（5.4） */
    public AppInheritorDetail appDetail(Long id) {
        Inheritor inheritor = inheritorMapper.selectById(id);
        if (inheritor == null || inheritor.getStatus() != ContentStatus.PUBLISHED.getCode()) {
            throw new BusinessException(404, "传承人不存在或已下架");
        }
        List<ArtifactCard> works = new ArrayList<>();
        List<CraftCard> crafts = new ArrayList<>();
        for (ContentRelation relation : relationService.listBySource("inheritor", id)) {
            if ("artifact".equals(relation.getTargetType())) {
                Artifact artifact = artifactMapper.selectById(relation.getTargetId());
                if (artifact != null && artifact.getStatus() == ContentStatus.PUBLISHED.getCode()) {
                    works.add(new ArtifactCard(artifact.getId(), artifact.getName(), artifact.getMainImageUrl()));
                }
            } else if ("craft".equals(relation.getTargetType())) {
                Craft craft = craftMapper.selectById(relation.getTargetId());
                if (craft != null && craft.getStatus() == ContentStatus.PUBLISHED.getCode()) {
                    crafts.add(new CraftCard(craft.getId(), craft.getName()));
                }
            }
        }
        return new AppInheritorDetail(inheritor, works, crafts);
    }

    // ---------------------------- 私有方法 ----------------------------

    private void validateForPublish(Inheritor inheritor) {
        if (!StringUtils.hasText(inheritor.getName()) || !StringUtils.hasText(inheritor.getIntro())
                || !StringUtils.hasText(inheritor.getPhotoUrl()) || !StringUtils.hasText(inheritor.getSource())) {
            throw new BusinessException("发布条件不满足：姓名、简介、人物照片和资料来源必填（5.2）");
        }
        if (inheritor.getAuditStatus() == null || inheritor.getAuditStatus() != 1) {
            throw new BusinessException("发布条件不满足：人物资料尚未审核通过（D-20）");
        }
    }

    private void apply(InheritorSave save, Inheritor inheritor) {
        inheritor.setName(save.name());
        inheritor.setPhotoUrl(save.photoUrl());
        inheritor.setIntro(save.intro());
        inheritor.setCareer(save.career());
        inheritor.setStory(save.story());
        inheritor.setSource(save.source());
    }

    private Inheritor requireInheritor(Long id) {
        Inheritor inheritor = inheritorMapper.selectById(id);
        if (inheritor == null) {
            throw new BusinessException("传承人不存在");
        }
        return inheritor;
    }

    private String toJson(Inheritor inheritor) {
        try {
            return objectMapper.writeValueAsString(inheritor);
        } catch (Exception e) {
            throw new BusinessException("生成发布快照失败");
        }
    }
}
