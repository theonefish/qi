package com.example.qi.module.term.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.BusinessException;
import com.example.qi.common.ContentStatus;
import com.example.qi.common.HtmlTextUtils;
import com.example.qi.module.auth.service.AuthService;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.publish.service.PublishRecordService;
import com.example.qi.module.term.entity.Term;
import com.example.qi.module.term.mapper.TermMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 术语服务（ADM-07 / FR-06）：CRUD、简单自审、发布与下架。
 * 发布条件：名称、释义、审核记录（5.2）。
 */
@Service
@RequiredArgsConstructor
public class TermService {

    private final TermMapper termMapper;
    private final PublishRecordService publishRecordService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public record TermSave(@NotBlank(message = "术语名称不能为空") String name,
                           String definition,
                           String source) {
    }

    /** 小程序术语卡数据（FR-06：名称、释义和来源） */
    public record AppTerm(Long id, String name, String definition, String source) {
    }

    public Page<Term> page(long pageNum, long pageSize, String keyword) {
        return termMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<Term>lambdaQuery()
                        .like(StringUtils.hasText(keyword), Term::getName, keyword)
                        .orderByDesc(Term::getCreateTime));
    }

    public Long save(Long id, TermSave save) {
        if (id == null) {
            Term term = new Term();
            apply(save, term);
            term.setStatus(ContentStatus.DRAFT.getCode());
            term.setAuditStatus(0);
            termMapper.insert(term);
            return term.getId();
        }
        requireTerm(id);
        termMapper.update(null, Wrappers.<Term>lambdaUpdate()
                .eq(Term::getId, id)
                .set(Term::getName, save.name())
                .set(Term::getDefinition, save.definition())
                .set(Term::getSource, save.source()));
        return id;
    }

    public void delete(Long id) {
        requireTerm(id);
        termMapper.deleteById(id);
    }

    /** 简单自审（D-20）：审核人自动取当前管理员，不通过时清空审核人/时间 */
    public void audit(Long id, boolean pass) {
        requireTerm(id);
        termMapper.update(null, Wrappers.<Term>lambdaUpdate()
                .eq(Term::getId, id)
                .set(Term::getAuditStatus, pass ? 1 : 0)
                .set(pass, Term::getAuditBy, authService.currentOperator().name())
                .set(pass, Term::getAuditTime, LocalDateTime.now())
                .set(!pass, Term::getAuditBy, null)
                .set(!pass, Term::getAuditTime, null));
    }

    public PublishRecord publish(Long id, String modifySummary) {
        Term term = requireTerm(id);
        validateForPublish(term);
        PublishRecord record = publishRecordService.createPublishing(
                "content", null, "term", id, modifySummary, toJson(term));
        try {
            term.setStatus(ContentStatus.PUBLISHED.getCode());
            termMapper.updateById(term);
            publishRecordService.finish(record, true, null);
            return record;
        } catch (RuntimeException e) {
            publishRecordService.finish(record, false, e.getMessage());
            throw e;
        }
    }

    public void offShelf(Long id) {
        Term term = requireTerm(id);
        term.setStatus(ContentStatus.OFF_SHELF.getCode());
        termMapper.updateById(term);
    }

    /** 小程序批量释义查询：仅返回已发布术语（FR-06） */
    public List<AppTerm> appByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return termMapper.selectBatchIds(ids).stream()
                .filter(t -> t.getStatus() == ContentStatus.PUBLISHED.getCode())
                .map(t -> new AppTerm(t.getId(), t.getName(), t.getDefinition(), t.getSource()))
                .toList();
    }

    private void validateForPublish(Term term) {
        if (!StringUtils.hasText(term.getName()) || !HtmlTextUtils.hasText(term.getDefinition())) {
            throw new BusinessException("发布条件不满足：名称和释义必填（5.2）");
        }
        if (term.getAuditStatus() == null || term.getAuditStatus() != 1) {
            throw new BusinessException("发布条件不满足：术语尚未审核通过（D-20）");
        }
    }

    private void apply(TermSave save, Term term) {
        term.setName(save.name());
        term.setDefinition(save.definition());
        term.setSource(save.source());
    }

    private Term requireTerm(Long id) {
        Term term = termMapper.selectById(id);
        if (term == null) {
            throw new BusinessException("术语不存在");
        }
        return term;
    }

    private String toJson(Term term) {
        try {
            return objectMapper.writeValueAsString(term);
        } catch (Exception e) {
            throw new BusinessException("生成发布快照失败");
        }
    }
}
