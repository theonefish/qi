package com.example.qi.module.motif.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.BusinessException;
import com.example.qi.common.ContentStatus;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.publish.service.PublishRecordService;
import com.example.qi.module.motif.entity.Motif;
import com.example.qi.module.motif.mapper.MotifMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * 纹饰资料服务（ADM-07）：CRUD、发布与下架。
 * 发布条件：名称、文化解释、资料来源（5.2）。P0 在章节/藏品中引用展示，独立主题页为 P1。
 */
@Service
@RequiredArgsConstructor
public class MotifService {

    private final MotifMapper motifMapper;
    private final PublishRecordService publishRecordService;
    private final ObjectMapper objectMapper;

    public record MotifSave(@NotBlank(message = "纹饰名称不能为空") String name,
                            String imageUrl, String culturalExplain, String source) {
    }

    public Page<Motif> page(long pageNum, long pageSize, String keyword) {
        return motifMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<Motif>lambdaQuery()
                        .like(StringUtils.hasText(keyword), Motif::getName, keyword)
                        .orderByDesc(Motif::getCreateTime));
    }

    public Long save(Long id, MotifSave save) {
        if (id == null) {
            Motif motif = new Motif();
            apply(save, motif);
            motif.setStatus(ContentStatus.DRAFT.getCode());
            motifMapper.insert(motif);
            return motif.getId();
        }
        requireMotif(id);
        motifMapper.update(null, Wrappers.<Motif>lambdaUpdate()
                .eq(Motif::getId, id)
                .set(Motif::getName, save.name())
                .set(Motif::getImageUrl, save.imageUrl())
                .set(Motif::getCulturalExplain, save.culturalExplain())
                .set(Motif::getSource, save.source()));
        return id;
    }

    public void delete(Long id) {
        requireMotif(id);
        motifMapper.deleteById(id);
    }

    /** 发布（5.2：名称、文化解释、资料来源；无审核要求） */
    public PublishRecord publish(Long id, String modifySummary) {
        Motif motif = requireMotif(id);
        if (!StringUtils.hasText(motif.getName()) || !StringUtils.hasText(motif.getCulturalExplain())
                || !StringUtils.hasText(motif.getSource())) {
            throw new BusinessException("发布条件不满足：名称、文化解释和资料来源必填（5.2）");
        }
        PublishRecord record = publishRecordService.createPublishing(
                "content", null, "motif", id, modifySummary, toJson(motif));
        try {
            motif.setStatus(ContentStatus.PUBLISHED.getCode());
            motifMapper.updateById(motif);
            publishRecordService.finish(record, true, null);
            return record;
        } catch (RuntimeException e) {
            publishRecordService.finish(record, false, e.getMessage());
            throw e;
        }
    }

    public void offShelf(Long id) {
        Motif motif = requireMotif(id);
        motif.setStatus(ContentStatus.OFF_SHELF.getCode());
        motifMapper.updateById(motif);
    }

    private void apply(MotifSave save, Motif motif) {
        motif.setName(save.name());
        motif.setImageUrl(save.imageUrl());
        motif.setCulturalExplain(save.culturalExplain());
        motif.setSource(save.source());
    }

    private Motif requireMotif(Long id) {
        Motif motif = motifMapper.selectById(id);
        if (motif == null) {
            throw new BusinessException("纹饰资料不存在");
        }
        return motif;
    }

    private String toJson(Motif motif) {
        try {
            return objectMapper.writeValueAsString(motif);
        } catch (Exception e) {
            throw new BusinessException("生成发布快照失败");
        }
    }
}
