package com.example.qi.module.craft.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.qi.common.BusinessException;
import com.example.qi.common.ContentStatus;
import com.example.qi.module.artifact.entity.Artifact;
import com.example.qi.module.artifact.mapper.ArtifactMapper;
import com.example.qi.module.craft.entity.Craft;
import com.example.qi.module.craft.entity.CraftStep;
import com.example.qi.module.craft.mapper.CraftMapper;
import com.example.qi.module.craft.mapper.CraftStepMapper;
import com.example.qi.module.media.entity.MediaAsset;
import com.example.qi.module.media.mapper.MediaAssetMapper;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.publish.service.PublishRecordService;
import com.example.qi.module.relation.entity.ContentRelation;
import com.example.qi.module.relation.service.RelationService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工艺与工序服务（ADM-05 / FR-10）。
 * 发布条件（5.2）：名称、概述、适用范围说明、至少一个有效工序；工序需名称和简介。
 * 工序数据供 PROCESS 叙事节点引用，不复制维护（7.3）。
 */
@Service
@RequiredArgsConstructor
public class CraftService {

    private final CraftMapper craftMapper;
    private final CraftStepMapper craftStepMapper;
    private final PublishRecordService publishRecordService;
    private final RelationService relationService;
    private final ArtifactMapper artifactMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final ObjectMapper objectMapper;

    public record CraftSave(@NotBlank(message = "工艺名称不能为空") String name,
                            String overview, String feature, String scope, String source) {
    }

    public record StepSave(@NotBlank(message = "工序名称不能为空") String name, String intro, Long mediaId) {
    }

    public record StepView(Long id, String name, String intro, String mediaUrl, Integer sortOrder) {
    }

    public record ArtifactCard(Long id, String name, String mainImageUrl) {
    }

    public record AppCraftDetail(Craft craft, List<StepView> steps, List<ArtifactCard> relatedArtifacts) {
    }

    // ---------------------------- 后台 ----------------------------

    public List<Craft> list() {
        return craftMapper.selectList(Wrappers.<Craft>lambdaQuery()
                .orderByDesc(Craft::getCreateTime));
    }

    public Long saveCraft(Long id, CraftSave save) {
        if (id == null) {
            Craft craft = new Craft();
            applyCraft(save, craft);
            craft.setStatus(ContentStatus.DRAFT.getCode());
            craftMapper.insert(craft);
            return craft.getId();
        }
        requireCraft(id);
        craftMapper.update(null, Wrappers.<Craft>lambdaUpdate()
                .eq(Craft::getId, id)
                .set(Craft::getName, save.name())
                .set(Craft::getOverview, save.overview())
                .set(Craft::getFeature, save.feature())
                .set(Craft::getScope, save.scope())
                .set(Craft::getSource, save.source()));
        return id;
    }

    /** 删除工艺：工序一并逻辑删除 */
    @Transactional
    public void deleteCraft(Long id) {
        requireCraft(id);
        craftStepMapper.delete(Wrappers.<CraftStep>lambdaQuery()
                .eq(CraftStep::getCraftId, id));
        craftMapper.deleteById(id);
    }

    /** 发布（5.2 校验 + 7.14 发布记录） */
    public PublishRecord publish(Long id, String modifySummary) {
        Craft craft = requireCraft(id);
        validateForPublish(craft, id);
        PublishRecord record = publishRecordService.createPublishing(
                "content", null, "craft", id, modifySummary, toJson(craft));
        try {
            craft.setStatus(ContentStatus.PUBLISHED.getCode());
            craftMapper.updateById(craft);
            publishRecordService.finish(record, true, null);
            return record;
        } catch (RuntimeException e) {
            publishRecordService.finish(record, false, e.getMessage());
            throw e;
        }
    }

    public void offShelf(Long id) {
        Craft craft = requireCraft(id);
        craft.setStatus(ContentStatus.OFF_SHELF.getCode());
        craftMapper.updateById(craft);
    }

    // ---------------------------- 工序 ----------------------------

    public List<CraftStep> listSteps(Long craftId) {
        requireCraft(craftId);
        return craftStepMapper.selectList(Wrappers.<CraftStep>lambdaQuery()
                .eq(CraftStep::getCraftId, craftId)
                .orderByAsc(CraftStep::getSortOrder)
                .orderByAsc(CraftStep::getId));
    }

    public Long saveStep(Long craftId, Long stepId, StepSave save) {
        requireCraft(craftId);
        if (stepId == null) {
            CraftStep step = new CraftStep();
            step.setCraftId(craftId);
            step.setName(save.name());
            step.setIntro(save.intro());
            step.setMediaId(save.mediaId());
            step.setSortOrder(nextStepSortOrder(craftId));
            craftStepMapper.insert(step);
            return step.getId();
        }
        requireStep(craftId, stepId);
        craftStepMapper.update(null, Wrappers.<CraftStep>lambdaUpdate()
                .eq(CraftStep::getId, stepId)
                .set(CraftStep::getName, save.name())
                .set(CraftStep::getIntro, save.intro())
                .set(CraftStep::getMediaId, save.mediaId()));
        return stepId;
    }

    public void deleteStep(Long craftId, Long stepId) {
        requireStep(craftId, stepId);
        craftStepMapper.deleteById(stepId);
    }

    /** 工序上下移动（7.5：步骤顺序） */
    @Transactional
    public void moveStep(Long craftId, Long stepId, String direction) {
        List<CraftStep> steps = listSteps(craftId);
        int index = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getId().equals(stepId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new BusinessException("工序不存在或不属于该工艺");
        }
        int target = "up".equals(direction) ? index - 1 : index + 1;
        if (target < 0 || target >= steps.size()) {
            return;
        }
        CraftStep current = steps.get(index);
        CraftStep neighbor = steps.get(target);
        craftStepMapper.update(null, Wrappers.<CraftStep>lambdaUpdate()
                .eq(CraftStep::getId, current.getId())
                .set(CraftStep::getSortOrder, neighbor.getSortOrder()));
        craftStepMapper.update(null, Wrappers.<CraftStep>lambdaUpdate()
                .eq(CraftStep::getId, neighbor.getId())
                .set(CraftStep::getSortOrder, current.getSortOrder()));
    }

    // ---------------------------- 小程序端（FR-10） ----------------------------

    /** 工艺总览列表：仅已发布 */
    public List<Craft> appList() {
        return craftMapper.selectList(Wrappers.<Craft>lambdaQuery()
                .eq(Craft::getStatus, ContentStatus.PUBLISHED.getCode())
                .orderByDesc(Craft::getCreateTime));
    }

    /** 工艺详情：工序步骤（含配图地址）+ 有效关联藏品（5.4） */
    public AppCraftDetail appDetail(Long id) {
        Craft craft = craftMapper.selectById(id);
        if (craft == null || craft.getStatus() != ContentStatus.PUBLISHED.getCode()) {
            throw new BusinessException(404, "工艺不存在或已下架");
        }
        List<CraftStep> steps = listSteps(id);
        List<Long> mediaIds = steps.stream()
                .map(CraftStep::getMediaId).filter(Objects::nonNull).toList();
        Map<Long, MediaAsset> mediaMap = mediaIds.isEmpty() ? Map.of()
                : mediaAssetMapper.selectBatchIds(mediaIds).stream()
                        .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
        List<StepView> stepViews = steps.stream()
                .map(step -> new StepView(step.getId(), step.getName(), step.getIntro(),
                        step.getMediaId() == null ? null
                                : mediaMap.get(step.getMediaId()) == null ? null
                                : mediaMap.get(step.getMediaId()).getUrl(),
                        step.getSortOrder()))
                .toList();

        List<ArtifactCard> relatedArtifacts = new ArrayList<>();
        for (ContentRelation relation : relationService.listBySource("craft", id)) {
            if (!"artifact".equals(relation.getTargetType())) {
                continue;
            }
            Artifact artifact = artifactMapper.selectById(relation.getTargetId());
            if (artifact != null && artifact.getStatus() == ContentStatus.PUBLISHED.getCode()) {
                relatedArtifacts.add(new ArtifactCard(artifact.getId(), artifact.getName(),
                        artifact.getMainImageUrl()));
            }
        }
        return new AppCraftDetail(craft, stepViews, relatedArtifacts);
    }

    // ---------------------------- 私有方法 ----------------------------

    /** 发布校验（5.2 工艺行）：名称、概述、适用范围说明、至少一个有效工序 */
    private void validateForPublish(Craft craft, Long id) {
        if (!StringUtils.hasText(craft.getName()) || !StringUtils.hasText(craft.getOverview())
                || !StringUtils.hasText(craft.getScope())) {
            throw new BusinessException("发布条件不满足：名称、概述和适用范围说明必填（5.2）");
        }
        List<CraftStep> steps = listSteps(id);
        if (steps.isEmpty()) {
            throw new BusinessException("发布条件不满足：至少一个有效工序（5.2）");
        }
        for (CraftStep step : steps) {
            if (!StringUtils.hasText(step.getName()) || !StringUtils.hasText(step.getIntro())) {
                throw new BusinessException("发布条件不满足：工序「" + step.getName() + "」缺少名称或简介（5.2）");
            }
        }
    }

    private void applyCraft(CraftSave save, Craft craft) {
        craft.setName(save.name());
        craft.setOverview(save.overview());
        craft.setFeature(save.feature());
        craft.setScope(save.scope());
        craft.setSource(save.source());
    }

    private int nextStepSortOrder(Long craftId) {
        CraftStep last = craftStepMapper.selectOne(Wrappers.<CraftStep>lambdaQuery()
                .eq(CraftStep::getCraftId, craftId)
                .orderByDesc(CraftStep::getSortOrder)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 1 : last.getSortOrder() + 1;
    }

    private Craft requireCraft(Long id) {
        Craft craft = craftMapper.selectById(id);
        if (craft == null) {
            throw new BusinessException("工艺不存在");
        }
        return craft;
    }

    private void requireStep(Long craftId, Long stepId) {
        CraftStep step = craftStepMapper.selectById(stepId);
        if (step == null || !step.getCraftId().equals(craftId)) {
            throw new BusinessException("工序不存在或不属于该工艺");
        }
    }

    private String toJson(Craft craft) {
        try {
            return objectMapper.writeValueAsString(craft);
        } catch (Exception e) {
            throw new BusinessException("生成发布快照失败");
        }
    }
}
