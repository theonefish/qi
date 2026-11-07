package com.example.qi.module.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.qi.module.artifact.entity.Artifact;
import com.example.qi.module.artifact.mapper.ArtifactMapper;
import com.example.qi.module.chapter.entity.Chapter;
import com.example.qi.module.chapter.mapper.ChapterMapper;
import com.example.qi.module.craft.mapper.CraftMapper;
import com.example.qi.module.inheritor.mapper.InheritorMapper;
import com.example.qi.module.visitor.entity.BehaviorEvent;
import com.example.qi.module.visitor.mapper.BehaviorEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 参观行为统计（ADM-11 / ADM-02）：汇总口径见主文档 7.11。
 * 聚合使用常量列名的分组查询，不拼接外部输入。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final BehaviorEventMapper behaviorEventMapper;
    private final ChapterMapper chapterMapper;
    private final ArtifactMapper artifactMapper;
    private final CraftMapper craftMapper;
    private final InheritorMapper inheritorMapper;

    public record Overview(long publishedChapters, long publishedArtifacts, long draftArtifacts,
                           long publishedCrafts, long publishedInheritors,
                           long visitorCount, long todayEventCount) {
    }

    public record ChapterStat(Long chapterId, String title, long enterCount, long viewNodeCount,
                              long completeCount, long backMainCount) {
    }

    /** 关联跳转统计：按来源章节与目标内容（7.11：按来源内容和目标内容统计跳转） */
    public record RelationStat(Long chapterId, String targetType, Long targetId, long count) {
    }

    public Overview overview() {
        long publishedChapters = chapterMapper.selectCount(
                new QueryWrapper<Chapter>().eq("status", 1).eq("chapter_type", 1));
        long publishedArtifacts = artifactMapper.selectCount(new QueryWrapper<Artifact>().eq("status", 1));
        long draftArtifacts = artifactMapper.selectCount(new QueryWrapper<Artifact>().eq("status", 0));
        long publishedCrafts = craftMapper.selectCount(new QueryWrapper<com.example.qi.module.craft.entity.Craft>().eq("status", 1));
        long publishedInheritors = inheritorMapper.selectCount(
                new QueryWrapper<com.example.qi.module.inheritor.entity.Inheritor>().eq("status", 1));
        List<Object> visitorRows = behaviorEventMapper.selectObjs(
                new QueryWrapper<BehaviorEvent>().select("COUNT(DISTINCT anonymous_id) AS cnt"));
        long visitorCount = visitorRows.isEmpty() || visitorRows.get(0) == null
                ? 0 : asLong(visitorRows.get(0));
        Long todayEvents = behaviorEventMapper.selectCount(
                new QueryWrapper<BehaviorEvent>().ge("occur_time", LocalDate.now().atStartOfDay()));
        return new Overview(publishedChapters, publishedArtifacts, draftArtifacts,
                publishedCrafts, publishedInheritors, visitorCount, todayEvents == null ? 0 : todayEvents);
    }

    public List<ChapterStat> chapterStats() {
        List<Chapter> chapters = chapterMapper.selectList(
                new QueryWrapper<Chapter>().orderByAsc("sort_order").orderByAsc("id"));
        List<Map<String, Object>> rows = behaviorEventMapper.selectMaps(
                new QueryWrapper<BehaviorEvent>()
                        .select("chapter_id", "event_type", "COUNT(*) AS cnt")
                        .isNotNull("chapter_id")
                        .groupBy("chapter_id", "event_type"));
        Map<Long, Map<String, Long>> counters = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long chapterId = asLong(row.get("chapter_id"));
            String eventType = String.valueOf(row.get("event_type"));
            long cnt = asLong(row.get("cnt"));
            counters.computeIfAbsent(chapterId, k -> new HashMap<>()).merge(eventType, cnt, Long::sum);
        }
        List<ChapterStat> stats = new ArrayList<>();
        for (Chapter chapter : chapters) {
            Map<String, Long> byType = counters.getOrDefault(chapter.getId(), Map.of());
            stats.add(new ChapterStat(chapter.getId(), chapter.getTitle(),
                    byType.getOrDefault("enter_chapter", 0L),
                    byType.getOrDefault("view_node", 0L),
                    byType.getOrDefault("complete_chapter", 0L),
                    byType.getOrDefault("back_main", 0L)));
        }
        return stats;
    }

    public List<RelationStat> relationStats() {
        List<Map<String, Object>> rows = behaviorEventMapper.selectMaps(
                new QueryWrapper<BehaviorEvent>()
                        .select("chapter_id", "target_type", "target_id", "COUNT(*) AS cnt")
                        .eq("event_type", "click_relation")
                        .groupBy("chapter_id", "target_type", "target_id")
                        .last("ORDER BY cnt DESC LIMIT 50"));
        List<RelationStat> stats = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            stats.add(new RelationStat(asLong(row.get("chapter_id")),
                    String.valueOf(row.get("target_type")),
                    asLong(row.get("target_id")),
                    asLong(row.get("cnt"))));
        }
        return stats;
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }
}
