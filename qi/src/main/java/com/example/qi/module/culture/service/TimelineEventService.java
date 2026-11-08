package com.example.qi.module.culture.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.BusinessException;
import com.example.qi.common.ContentStatus;
import com.example.qi.common.HtmlTextUtils;
import com.example.qi.module.culture.entity.TimelineEvent;
import com.example.qi.module.culture.mapper.TimelineEventMapper;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.publish.service.PublishRecordService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * 历史事件服务（ADM-07）：CRUD、发布与下架。
 * 发布条件（5.2）：事件名称、时间或年代说明、事件介绍、资料来源。
 */
@Service
@RequiredArgsConstructor
public class TimelineEventService {

    private final TimelineEventMapper timelineEventMapper;
    private final PublishRecordService publishRecordService;
    private final ObjectMapper objectMapper;

    public record EventSave(@NotBlank(message = "时间或时期不能为空") String eventTime,
                            @NotBlank(message = "事件名称不能为空") String title,
                            String description, Long mediaId,
                            @NotBlank(message = "资料来源不能为空") String source) {
    }

    public Page<TimelineEvent> page(long pageNum, long pageSize, String keyword) {
        return timelineEventMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<TimelineEvent>lambdaQuery()
                        .like(StringUtils.hasText(keyword), TimelineEvent::getTitle, keyword)
                        .orderByDesc(TimelineEvent::getCreateTime));
    }

    public Long save(Long id, EventSave save) {
        if (id == null) {
            TimelineEvent event = new TimelineEvent();
            apply(save, event);
            event.setStatus(ContentStatus.DRAFT.getCode());
            timelineEventMapper.insert(event);
            return event.getId();
        }
        requireEvent(id);
        timelineEventMapper.update(null, Wrappers.<TimelineEvent>lambdaUpdate()
                .eq(TimelineEvent::getId, id)
                .set(TimelineEvent::getEventTime, save.eventTime())
                .set(TimelineEvent::getTitle, save.title())
                .set(TimelineEvent::getDescription, save.description())
                .set(TimelineEvent::getMediaId, save.mediaId())
                .set(TimelineEvent::getSource, save.source()));
        return id;
    }

    public void delete(Long id) {
        requireEvent(id);
        timelineEventMapper.deleteById(id);
    }

    /** 发布（5.2：名称、时间、介绍、来源；介绍为事件说明必填） */
    public PublishRecord publish(Long id, String modifySummary) {
        TimelineEvent event = requireEvent(id);
        if (!StringUtils.hasText(event.getEventTime()) || !StringUtils.hasText(event.getTitle())
                || !HtmlTextUtils.hasText(event.getDescription()) || !StringUtils.hasText(event.getSource())) {
            throw new BusinessException("发布条件不满足：时间、名称、事件介绍和资料来源必填（5.2）");
        }
        PublishRecord record = publishRecordService.createPublishing(
                "content", null, "event", id, modifySummary, toJson(event));
        try {
            event.setStatus(ContentStatus.PUBLISHED.getCode());
            timelineEventMapper.updateById(event);
            publishRecordService.finish(record, true, null);
            return record;
        } catch (RuntimeException e) {
            publishRecordService.finish(record, false, e.getMessage());
            throw e;
        }
    }

    public void offShelf(Long id) {
        TimelineEvent event = requireEvent(id);
        event.setStatus(ContentStatus.OFF_SHELF.getCode());
        timelineEventMapper.updateById(event);
    }

    private void apply(EventSave save, TimelineEvent event) {
        event.setEventTime(save.eventTime());
        event.setTitle(save.title());
        event.setDescription(save.description());
        event.setMediaId(save.mediaId());
        event.setSource(save.source());
    }

    private TimelineEvent requireEvent(Long id) {
        TimelineEvent event = timelineEventMapper.selectById(id);
        if (event == null) {
            throw new BusinessException("历史事件不存在");
        }
        return event;
    }

    private String toJson(TimelineEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new BusinessException("生成发布快照失败");
        }
    }
}
