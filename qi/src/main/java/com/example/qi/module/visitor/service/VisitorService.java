package com.example.qi.module.visitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.qi.common.BusinessException;
import com.example.qi.common.ContentStatus;
import com.example.qi.module.artifact.entity.Artifact;
import com.example.qi.module.artifact.mapper.ArtifactMapper;
import com.example.qi.module.chapter.entity.Chapter;
import com.example.qi.module.chapter.entity.ChapterNode;
import com.example.qi.module.chapter.mapper.ChapterMapper;
import com.example.qi.module.chapter.mapper.ChapterNodeMapper;
import com.example.qi.module.visitor.dto.BehaviorReportRequest;
import com.example.qi.module.visitor.entity.BehaviorEvent;
import com.example.qi.module.visitor.entity.UserFavorite;
import com.example.qi.module.visitor.entity.UserProgress;
import com.example.qi.module.visitor.mapper.BehaviorEventMapper;
import com.example.qi.module.visitor.mapper.UserFavoriteMapper;
import com.example.qi.module.visitor.mapper.UserProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 访客服务：收藏（FR-09）、断点续读与参观进度（FR-05 / 5.5 / 5.6）、行为上报（ADM-11 / 7.11）。
 * 全部按本地生成的匿名标识在服务端存储（D-05）。
 */
@Service
@RequiredArgsConstructor
public class VisitorService {

    private static final String TYPE_ARTIFACT = "artifact";
    /** 允许前端上报的事件类型；complete_chapter 由完成本章接口内部记录（7.11 口径） */
    private static final Set<String> REPORTABLE_EVENTS = Set.of("enter_chapter", "view_node", "click_relation", "back_main");

    private final UserFavoriteMapper favoriteMapper;
    private final UserProgressMapper progressMapper;
    private final BehaviorEventMapper behaviorEventMapper;
    private final ChapterMapper chapterMapper;
    private final ChapterNodeMapper nodeMapper;
    private final ArtifactMapper artifactMapper;

    public record FavoriteItem(Long targetId, String name, String mainImageUrl, LocalDateTime favoriteTime) {
    }

    public record CompleteResult(boolean firstTime, long completedMain, long totalMain, int progressPercent) {
    }

    public record ChapterProgressItem(Long chapterId, String title, Integer chapterType, boolean completed,
                                      LocalDateTime completeTime, Long lastNodeId, LocalDateTime lastReadTime) {
    }

    /** 首页“继续参观”定位信息（FR-01：显示章节和节点信息） */
    public record LastRead(Long chapterId, String chapterTitle, Long nodeId, String nodeTitle, LocalDateTime readTime) {
    }

    public record VisitorSummary(long completedMain, long totalMain, int progressPercent,
                                 long favoriteCount, LastRead lastRead) {
    }

    // ---------------------------- 收藏 ----------------------------

    public void addFavorite(String anonymousId, String targetType, Long targetId) {
        checkAnonymous(anonymousId);
        requireArtifactTarget(targetType, targetId);
        Long count = favoriteMapper.selectCount(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getAnonymousId, anonymousId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId));
        if (count == null || count == 0) {
            UserFavorite favorite = new UserFavorite();
            favorite.setAnonymousId(anonymousId);
            favorite.setTargetType(targetType);
            favorite.setTargetId(targetId);
            favoriteMapper.insert(favorite);
        }
    }

    public void removeFavorite(String anonymousId, String targetType, Long targetId) {
        checkAnonymous(anonymousId);
        favoriteMapper.delete(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getAnonymousId, anonymousId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId));
    }

    public boolean isFavorite(String anonymousId, String targetType, Long targetId) {
        checkAnonymous(anonymousId);
        Long count = favoriteMapper.selectCount(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getAnonymousId, anonymousId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId));
        return count != null && count > 0;
    }

    /** 收藏列表：已下架或删除的藏品隐藏，不产生死链（5.7） */
    public List<FavoriteItem> listFavorites(String anonymousId) {
        checkAnonymous(anonymousId);
        List<UserFavorite> favorites = favoriteMapper.selectList(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getAnonymousId, anonymousId)
                .orderByDesc(UserFavorite::getCreateTime));
        List<FavoriteItem> items = new ArrayList<>();
        for (UserFavorite favorite : favorites) {
            if (!TYPE_ARTIFACT.equals(favorite.getTargetType())) {
                continue;
            }
            Artifact artifact = artifactMapper.selectById(favorite.getTargetId());
            if (artifact == null || artifact.getStatus() != ContentStatus.PUBLISHED.getCode()) {
                continue;
            }
            items.add(new FavoriteItem(artifact.getId(), artifact.getName(),
                    artifact.getMainImageUrl(), favorite.getCreateTime()));
        }
        return items;
    }

    // ---------------------------- 进度与断点续读 ----------------------------

    /** 浏览到节点时更新阅读位置（5.6）；从详情页返回主线由前端控制不调用本接口 */
    public void updateReadPosition(String anonymousId, Long chapterId, Long nodeId) {
        checkAnonymous(anonymousId);
        Chapter chapter = requirePublishedChapter(chapterId);
        ChapterNode node = nodeMapper.selectById(nodeId);
        if (node == null || !chapter.getId().equals(node.getChapterId())) {
            throw new BusinessException("节点不存在或不属于该章节");
        }
        upsertProgress(anonymousId, chapterId, progress -> {
            progress.setLastNodeId(nodeId);
            progress.setLastReadTime(LocalDateTime.now());
        });
    }

    /** 完成本章（幂等）：序章同样标记完成但不计入进度分母（D-02）；首次完成记录行为事件 */
    @Transactional
    public CompleteResult completeChapter(String anonymousId, Long chapterId) {
        checkAnonymous(anonymousId);
        requirePublishedChapter(chapterId);
        UserProgress existing = progressMapper.selectOne(Wrappers.<UserProgress>lambdaQuery()
                .eq(UserProgress::getAnonymousId, anonymousId)
                .eq(UserProgress::getChapterId, chapterId));
        boolean firstTime = existing == null || existing.getCompleted() == null || existing.getCompleted() != 1;
        if (existing == null) {
            UserProgress progress = new UserProgress();
            progress.setAnonymousId(anonymousId);
            progress.setChapterId(chapterId);
            progress.setCompleted(1);
            progress.setCompleteTime(LocalDateTime.now());
            progressMapper.insert(progress);
        } else if (firstTime) {
            progressMapper.update(null, Wrappers.<UserProgress>lambdaUpdate()
                    .eq(UserProgress::getId, existing.getId())
                    .set(UserProgress::getCompleted, 1)
                    .set(UserProgress::getCompleteTime, LocalDateTime.now()));
        }
        if (firstTime) {
            insertEvent(anonymousId, null, "complete_chapter", chapterId, null, null, null);
        }
        long[] progress = calcProgress(anonymousId);
        return new CompleteResult(firstTime, progress[0], progress[1], (int) progress[2]);
    }

    /** 参观进度页 / 云展馆章节状态：全部已发布章节 + 用户完成状态与最后位置 */
    public List<ChapterProgressItem> listProgress(String anonymousId) {
        checkAnonymous(anonymousId);
        List<Chapter> chapters = chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery()
                .eq(Chapter::getStatus, ContentStatus.PUBLISHED.getCode())
                .orderByAsc(Chapter::getSortOrder)
                .orderByAsc(Chapter::getId));
        Map<Long, UserProgress> progressMap = progressMapper.selectList(Wrappers.<UserProgress>lambdaQuery()
                        .eq(UserProgress::getAnonymousId, anonymousId)).stream()
                .collect(Collectors.toMap(UserProgress::getChapterId, Function.identity(), (a, b) -> a));
        List<ChapterProgressItem> items = new ArrayList<>();
        for (Chapter chapter : chapters) {
            UserProgress progress = progressMap.get(chapter.getId());
            items.add(new ChapterProgressItem(chapter.getId(), chapter.getTitle(), chapter.getChapterType(),
                    progress != null && progress.getCompleted() != null && progress.getCompleted() == 1,
                    progress == null ? null : progress.getCompleteTime(),
                    progress == null ? null : progress.getLastNodeId(),
                    progress == null ? null : progress.getLastReadTime()));
        }
        return items;
    }

    /** 首页汇总：进度 N/M + 收藏数 + 继续参观定位（失效位置不产生死链，FR-01） */
    public VisitorSummary summary(String anonymousId) {
        checkAnonymous(anonymousId);
        long[] progress = calcProgress(anonymousId);
        long favoriteCount = listFavorites(anonymousId).size();
        UserProgress latest = progressMapper.selectOne(Wrappers.<UserProgress>lambdaQuery()
                .eq(UserProgress::getAnonymousId, anonymousId)
                .isNotNull(UserProgress::getLastReadTime)
                .orderByDesc(UserProgress::getLastReadTime)
                .last("LIMIT 1"));
        LastRead lastRead = null;
        if (latest != null) {
            Chapter chapter = chapterMapper.selectById(latest.getChapterId());
            // 章节失效时回云展馆（5.6）：返回 null，由前端处理
            if (chapter != null && chapter.getStatus() == ContentStatus.PUBLISHED.getCode()) {
                Long nodeId = latest.getLastNodeId();
                String nodeTitle = null;
                if (nodeId != null) {
                    ChapterNode node = nodeMapper.selectById(nodeId);
                    // 节点失效回退章节开头（5.6）：nodeId 置空
                    if (node == null || !chapter.getId().equals(node.getChapterId())) {
                        nodeId = null;
                    } else {
                        nodeTitle = node.getTitle();
                    }
                }
                lastRead = new LastRead(chapter.getId(), chapter.getTitle(), nodeId, nodeTitle,
                        latest.getLastReadTime());
            }
        }
        return new VisitorSummary(progress[0], progress[1], (int) progress[2], favoriteCount, lastRead);
    }

    // ---------------------------- 行为上报 ----------------------------

    /**
     * 行为上报（7.11）：enter_chapter / view_node 依赖 visitId 会话内去重；
     * 返回实际记录条数。非法或重复事件静默跳过，不报错（上报不阻塞阅读）。
     */
    public int reportBehaviors(String anonymousId, String visitId, List<BehaviorReportRequest.Item> items) {
        checkAnonymous(anonymousId);
        if (!StringUtils.hasText(visitId) || items == null || items.isEmpty()) {
            return 0;
        }
        int saved = 0;
        for (BehaviorReportRequest.Item item : items) {
            if (item == null || !REPORTABLE_EVENTS.contains(item.getEventType())) {
                continue;
            }
            BehaviorEvent event = new BehaviorEvent();
            event.setAnonymousId(anonymousId);
            event.setVisitId(visitId);
            event.setEventType(item.getEventType());
            event.setChapterId(item.getChapterId());
            event.setNodeId(item.getNodeId());
            event.setTargetType(item.getTargetType());
            event.setTargetId(item.getTargetId());
            if (!isValidEvent(event) || isDuplicate(event)) {
                continue;
            }
            behaviorEventMapper.insert(event);
            saved++;
        }
        return saved;
    }

    // ---------------------------- 私有方法 ----------------------------

    private void requireArtifactTarget(String targetType, Long targetId) {
        if (!TYPE_ARTIFACT.equals(targetType)) {
            throw new BusinessException("当前仅支持收藏藏品（5.7 / D-16）");
        }
        Artifact artifact = artifactMapper.selectById(targetId);
        if (artifact == null || artifact.getStatus() != ContentStatus.PUBLISHED.getCode()) {
            throw new BusinessException("藏品不存在或已下架");
        }
    }

    private Chapter requirePublishedChapter(Long chapterId) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null || chapter.getStatus() != ContentStatus.PUBLISHED.getCode()) {
            throw new BusinessException("章节不存在或已下架");
        }
        return chapter;
    }

    private void upsertProgress(String anonymousId, Long chapterId,
                                java.util.function.Consumer<UserProgress> mutator) {
        UserProgress progress = progressMapper.selectOne(Wrappers.<UserProgress>lambdaQuery()
                .eq(UserProgress::getAnonymousId, anonymousId)
                .eq(UserProgress::getChapterId, chapterId));
        if (progress == null) {
            progress = new UserProgress();
            progress.setAnonymousId(anonymousId);
            progress.setChapterId(chapterId);
            mutator.accept(progress);
            progressMapper.insert(progress);
        } else {
            mutator.accept(progress);
            progressMapper.update(null, Wrappers.<UserProgress>lambdaUpdate()
                    .eq(UserProgress::getId, progress.getId())
                    .set(UserProgress::getLastNodeId, progress.getLastNodeId())
                    .set(UserProgress::getLastReadTime, progress.getLastReadTime()));
        }
    }

    /** 进度计算（5.5）：分母 = 已发布正章数，序章不计入 */
    private long[] calcProgress(String anonymousId) {
        List<Long> mainChapterIds = chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery()
                        .eq(Chapter::getStatus, ContentStatus.PUBLISHED.getCode())
                        .eq(Chapter::getChapterType, 1)
                        .select(Chapter::getId)).stream()
                .map(Chapter::getId).toList();
        long totalMain = mainChapterIds.size();
        long completedMain = 0;
        if (totalMain > 0) {
            Long count = progressMapper.selectCount(Wrappers.<UserProgress>lambdaQuery()
                    .eq(UserProgress::getAnonymousId, anonymousId)
                    .eq(UserProgress::getCompleted, 1)
                    .in(UserProgress::getChapterId, mainChapterIds));
            completedMain = count == null ? 0 : count;
        }
        long percent = totalMain == 0 ? 0 : Math.round(completedMain * 100.0 / totalMain);
        return new long[]{completedMain, totalMain, percent};
    }

    private void insertEvent(String anonymousId, String visitId, String eventType, Long chapterId,
                             Long nodeId, String targetType, Long targetId) {
        BehaviorEvent event = new BehaviorEvent();
        event.setAnonymousId(anonymousId);
        event.setVisitId(visitId);
        event.setEventType(eventType);
        event.setChapterId(chapterId);
        event.setNodeId(nodeId);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        behaviorEventMapper.insert(event);
    }

    private boolean isValidEvent(BehaviorEvent event) {
        return switch (event.getEventType()) {
            case "enter_chapter" -> event.getChapterId() != null;
            case "view_node" -> event.getChapterId() != null && event.getNodeId() != null;
            case "click_relation" -> event.getChapterId() != null
                    && StringUtils.hasText(event.getTargetType()) && event.getTargetId() != null;
            case "back_main" -> event.getChapterId() != null;
            default -> false;
        };
    }

    /** 会话内去重（7.11）：同一 visitId 下 enter_chapter 按章节、view_node 按节点只记一次 */
    private boolean isDuplicate(BehaviorEvent event) {
        return switch (event.getEventType()) {
            case "enter_chapter" -> countSame(event, true) > 0;
            case "view_node" -> countSame(event, false) > 0;
            default -> false;
        };
    }

    private long countSame(BehaviorEvent event, boolean byChapter) {
        Long count = behaviorEventMapper.selectCount(Wrappers.<BehaviorEvent>lambdaQuery()
                .eq(BehaviorEvent::getAnonymousId, event.getAnonymousId())
                .eq(BehaviorEvent::getVisitId, event.getVisitId())
                .eq(BehaviorEvent::getEventType, event.getEventType())
                .eq(byChapter, BehaviorEvent::getChapterId, event.getChapterId())
                .eq(!byChapter, BehaviorEvent::getNodeId, event.getNodeId()));
        return count == null ? 0 : count;
    }

    private void checkAnonymous(String anonymousId) {
        if (!StringUtils.hasText(anonymousId)) {
            throw new BusinessException("缺少匿名标识");
        }
    }
}
