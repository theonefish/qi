package com.example.qi.module.chapter.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.qi.common.BusinessException;
import com.example.qi.common.ContentStatus;
import com.example.qi.common.NodeType;
import com.example.qi.module.chapter.dto.NodeSaveRequest;
import com.example.qi.module.chapter.entity.ChapterNode;
import com.example.qi.module.chapter.entity.NodeCraftRelation;
import com.example.qi.module.chapter.entity.NodeEventRelation;
import com.example.qi.module.chapter.entity.NodeMediaRelation;
import com.example.qi.module.chapter.entity.NodeQuote;
import com.example.qi.module.chapter.mapper.ChapterMapper;
import com.example.qi.module.chapter.mapper.ChapterNodeMapper;
import com.example.qi.module.chapter.mapper.NodeCraftRelationMapper;
import com.example.qi.module.chapter.mapper.NodeEventRelationMapper;
import com.example.qi.module.chapter.mapper.NodeMediaRelationMapper;
import com.example.qi.module.chapter.mapper.NodeQuoteMapper;
import com.example.qi.module.craft.entity.Craft;
import com.example.qi.module.craft.mapper.CraftMapper;
import com.example.qi.module.craft.mapper.CraftStepMapper;
import com.example.qi.module.culture.entity.TimelineEvent;
import com.example.qi.module.culture.mapper.TimelineEventMapper;
import com.example.qi.module.inheritor.entity.Inheritor;
import com.example.qi.module.inheritor.mapper.InheritorMapper;
import com.example.qi.module.media.mapper.MediaAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 叙事节点服务（6.13 / C.7 / 7.3）：
 * 四类节点统一 CRUD、按类型校验必填内容、扩展数据整体替换、上下移动排序。
 * 引用目标当前只校验存在性；目标“已发布”校验待工艺/传承人/事件模块具备发布流程后补齐（5.4 TODO）。
 */
@Service
@RequiredArgsConstructor
public class ChapterNodeService {

    private final ChapterMapper chapterMapper;
    private final ChapterNodeMapper nodeMapper;
    private final NodeQuoteMapper quoteMapper;
    private final NodeEventRelationMapper eventRelationMapper;
    private final NodeCraftRelationMapper craftRelationMapper;
    private final NodeMediaRelationMapper mediaRelationMapper;
    private final CraftMapper craftMapper;
    private final CraftStepMapper craftStepMapper;
    private final InheritorMapper inheritorMapper;
    private final TimelineEventMapper timelineEventMapper;
    private final MediaAssetMapper mediaAssetMapper;

    public record NodeDetail(ChapterNode node, NodeQuote quote,
                             List<NodeEventRelation> eventRefs,
                             List<NodeCraftRelation> craftRefs,
                             List<NodeMediaRelation> mediaRefs) {
    }

    /** 节点基础信息列表（不含扩展数据），按配置顺序（6.13：节点按后台配置顺序展示） */
    public List<ChapterNode> listByChapter(Long chapterId) {
        return nodeMapper.selectList(Wrappers.<ChapterNode>lambdaQuery()
                .eq(ChapterNode::getChapterId, chapterId)
                .orderByAsc(ChapterNode::getSortOrder)
                .orderByAsc(ChapterNode::getId));
    }

    public NodeDetail detail(Long nodeId) {
        ChapterNode node = requireNode(nodeId);
        NodeQuote quote = null;
        List<NodeEventRelation> eventRefs = List.of();
        List<NodeCraftRelation> craftRefs = List.of();
        switch (node.getNodeType()) {
            case "QUOTE" -> quote = quoteMapper.selectOne(Wrappers.<NodeQuote>lambdaQuery()
                    .eq(NodeQuote::getNodeId, nodeId));
            case "TIMELINE" -> eventRefs = eventRelationMapper.selectList(Wrappers.<NodeEventRelation>lambdaQuery()
                    .eq(NodeEventRelation::getNodeId, nodeId)
                    .orderByAsc(NodeEventRelation::getSortOrder));
            case "PROCESS" -> craftRefs = craftRelationMapper.selectList(Wrappers.<NodeCraftRelation>lambdaQuery()
                    .eq(NodeCraftRelation::getNodeId, nodeId)
                    .orderByAsc(NodeCraftRelation::getSortOrder));
            default -> {
            }
        }
        List<NodeMediaRelation> mediaRefs = mediaRelationMapper.selectList(Wrappers.<NodeMediaRelation>lambdaQuery()
                .eq(NodeMediaRelation::getNodeId, nodeId)
                .orderByAsc(NodeMediaRelation::getSortOrder));
        return new NodeDetail(node, quote, eventRefs, craftRefs, mediaRefs);
    }

    /** 新增或编辑节点（含扩展数据整体替换） */
    @Transactional
    public Long save(Long chapterId, NodeSaveRequest request) {
        requireChapter(chapterId);
        String type = request.getNodeType();
        if (java.util.Arrays.stream(NodeType.values()).noneMatch(t -> t.getCode().equals(type))) {
            throw new BusinessException("不支持的节点类型：" + type);
        }
        validateRequest(type, request);

        ChapterNode node = new ChapterNode();
        node.setChapterId(chapterId);
        node.setNodeType(type);
        node.setTitle(request.getTitle());
        node.setSubtitle(request.getSubtitle());
        node.setContent(request.getContent());
        node.setSource(request.getSource());
        if (request.getId() == null) {
            node.setSortOrder(nextSortOrder(chapterId));
            nodeMapper.insert(node);
        } else {
            ChapterNode existing = requireNode(request.getId());
            if (!existing.getChapterId().equals(chapterId)) {
                throw new BusinessException("节点不属于该章节");
            }
            node.setId(existing.getId());
            node.setSortOrder(existing.getSortOrder());
            // 显式 set 允许字段被清空
            nodeMapper.update(null, Wrappers.<ChapterNode>lambdaUpdate()
                    .eq(ChapterNode::getId, node.getId())
                    .set(ChapterNode::getNodeType, type)
                    .set(ChapterNode::getTitle, node.getTitle())
                    .set(ChapterNode::getSubtitle, node.getSubtitle())
                    .set(ChapterNode::getContent, node.getContent())
                    .set(ChapterNode::getSource, node.getSource()));
        }
        syncExtData(node, request);
        return node.getId();
    }

    /** 删除节点：扩展数据物理删除，节点逻辑删除（7.3：删除草稿） */
    @Transactional
    public void delete(Long nodeId) {
        ChapterNode node = requireNode(nodeId);
        quoteMapper.delete(Wrappers.<NodeQuote>lambdaQuery().eq(NodeQuote::getNodeId, nodeId));
        eventRelationMapper.delete(Wrappers.<NodeEventRelation>lambdaQuery().eq(NodeEventRelation::getNodeId, nodeId));
        craftRelationMapper.delete(Wrappers.<NodeCraftRelation>lambdaQuery().eq(NodeCraftRelation::getNodeId, nodeId));
        mediaRelationMapper.delete(Wrappers.<NodeMediaRelation>lambdaQuery().eq(NodeMediaRelation::getNodeId, nodeId));
        nodeMapper.deleteById(node.getId());
    }

    /** 上下移动排序（7.3） */
    @Transactional
    public void move(Long chapterId, Long nodeId, String direction) {
        List<ChapterNode> nodes = listByChapter(chapterId);
        int index = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(nodeId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new BusinessException("节点不存在或不属于该章节");
        }
        int target = "up".equals(direction) ? index - 1 : index + 1;
        if (target < 0 || target >= nodes.size()) {
            return; // 已到头，静默返回
        }
        ChapterNode current = nodes.get(index);
        ChapterNode neighbor = nodes.get(target);
        nodeMapper.update(null, Wrappers.<ChapterNode>lambdaUpdate()
                .eq(ChapterNode::getId, current.getId())
                .set(ChapterNode::getSortOrder, neighbor.getSortOrder()));
        nodeMapper.update(null, Wrappers.<ChapterNode>lambdaUpdate()
                .eq(ChapterNode::getId, neighbor.getId())
                .set(ChapterNode::getSortOrder, current.getSortOrder()));
    }

    /** 发布校验用：校验已存储节点的类型必需数据与引用目标状态（5.2 / 5.4） */
    public void validateStoredNode(ChapterNode node) {
        String prefix = "节点「" + node.getTitle() + "」";
        switch (node.getNodeType()) {
            case "TEXT" -> {
                if (!StringUtils.hasText(node.getContent())) {
                    throw new BusinessException(prefix + "正文不能为空（C.7）");
                }
            }
            case "TIMELINE" -> {
                List<NodeEventRelation> refs = eventRelationMapper.selectList(Wrappers.<NodeEventRelation>lambdaQuery()
                        .eq(NodeEventRelation::getNodeId, node.getId()));
                if (refs.isEmpty()) {
                    throw new BusinessException(prefix + "至少引用一个历史事件");
                }
                for (NodeEventRelation ref : refs) {
                    TimelineEvent event = timelineEventMapper.selectById(ref.getEventId());
                    if (event == null || event.getStatus() != ContentStatus.PUBLISHED.getCode()) {
                        throw new BusinessException(prefix + "引用的历史事件不存在或未发布（5.4）");
                    }
                }
            }
            case "QUOTE" -> {
                NodeQuote quote = quoteMapper.selectOne(Wrappers.<NodeQuote>lambdaQuery()
                        .eq(NodeQuote::getNodeId, node.getId()));
                if (quote == null || !StringUtils.hasText(quote.getQuoteText())) {
                    throw new BusinessException(prefix + "口述文本不能为空");
                }
                Inheritor inheritor = inheritorMapper.selectById(quote.getInheritorId());
                if (inheritor == null || inheritor.getStatus() != ContentStatus.PUBLISHED.getCode()) {
                    throw new BusinessException(prefix + "关联的传承人不存在或未发布（5.4）");
                }
            }
            case "PROCESS" -> {
                List<NodeCraftRelation> refs = craftRelationMapper.selectList(Wrappers.<NodeCraftRelation>lambdaQuery()
                        .eq(NodeCraftRelation::getNodeId, node.getId()));
                if (refs.isEmpty()) {
                    throw new BusinessException(prefix + "至少引用一个工艺");
                }
                for (NodeCraftRelation ref : refs) {
                    Craft craft = craftMapper.selectById(ref.getCraftId());
                    if (craft == null || craft.getStatus() != ContentStatus.PUBLISHED.getCode()) {
                        throw new BusinessException(prefix + "关联的工艺不存在或未发布（5.4）");
                    }
                }
                if (!StringUtils.hasText(node.getContent())) {
                    throw new BusinessException(prefix + "缺少适用范围说明（6.13）");
                }
            }
            default -> throw new BusinessException(prefix + "节点类型无效");
        }
    }

    // ------------------------------------------------------------------

    private void validateRequest(String type, NodeSaveRequest request) {
        switch (type) {
            case "TEXT" -> {
                if (!StringUtils.hasText(request.getContent())) {
                    throw new BusinessException("图文节点正文不能为空（C.7）");
                }
            }
            case "TIMELINE" -> {
                List<Long> eventIds = request.getEventIds();
                if (eventIds == null || eventIds.isEmpty()) {
                    throw new BusinessException("时间轴节点至少引用一个历史事件");
                }
                checkAllExist(timelineEventMapper.selectBatchIds(eventIds).size(),
                        new HashSet<>(eventIds).size(), "引用的历史事件不存在");
            }
            case "QUOTE" -> {
                if (request.getInheritorId() == null
                        || inheritorMapper.selectById(request.getInheritorId()) == null) {
                    throw new BusinessException("口述节点必须关联存在的传承人");
                }
                if (!StringUtils.hasText(request.getQuoteText())) {
                    throw new BusinessException("口述文本不能为空");
                }
            }
            case "PROCESS" -> {
                if (request.getCraftId() == null || craftMapper.selectById(request.getCraftId()) == null) {
                    throw new BusinessException("工序节点必须关联存在的工艺");
                }
                List<Long> stepIds = request.getCraftStepIds();
                if (stepIds != null && !stepIds.isEmpty()) {
                    long owned = craftStepMapper.selectCount(Wrappers.<com.example.qi.module.craft.entity.CraftStep>lambdaQuery()
                            .in(com.example.qi.module.craft.entity.CraftStep::getId, stepIds)
                            .eq(com.example.qi.module.craft.entity.CraftStep::getCraftId, request.getCraftId()));
                    checkAllExist((int) owned, new HashSet<>(stepIds).size(), "引用的工序不存在或不属于该工艺");
                }
            }
            default -> {
            }
        }
        List<NodeSaveRequest.MediaItem> media = request.getMedia();
        if (media != null && !media.isEmpty()) {
            List<Long> mediaIds = media.stream().map(NodeSaveRequest.MediaItem::getMediaId).toList();
            checkAllExist(mediaAssetMapper.selectBatchIds(mediaIds).size(),
                    new HashSet<>(mediaIds).size(), "引用的媒体资源不存在");
        }
    }

    private void syncExtData(ChapterNode node, NodeSaveRequest request) {
        Long nodeId = node.getId();
        quoteMapper.delete(Wrappers.<NodeQuote>lambdaQuery().eq(NodeQuote::getNodeId, nodeId));
        eventRelationMapper.delete(Wrappers.<NodeEventRelation>lambdaQuery().eq(NodeEventRelation::getNodeId, nodeId));
        craftRelationMapper.delete(Wrappers.<NodeCraftRelation>lambdaQuery().eq(NodeCraftRelation::getNodeId, nodeId));
        mediaRelationMapper.delete(Wrappers.<NodeMediaRelation>lambdaQuery().eq(NodeMediaRelation::getNodeId, nodeId));

        switch (node.getNodeType()) {
            case "TEXT" -> insertMedia(nodeId, request.getMedia());
            case "TIMELINE" -> {
                int order = 0;
                for (Long eventId : request.getEventIds()) {
                    NodeEventRelation ref = new NodeEventRelation();
                    ref.setNodeId(nodeId);
                    ref.setEventId(eventId);
                    ref.setSortOrder(order++);
                    eventRelationMapper.insert(ref);
                }
            }
            case "QUOTE" -> {
                NodeQuote quote = new NodeQuote();
                quote.setNodeId(nodeId);
                quote.setInheritorId(request.getInheritorId());
                quote.setQuoteText(request.getQuoteText());
                quote.setSource(request.getQuoteSource());
                quote.setAuthorizationNote(request.getAuthorizationNote());
                quoteMapper.insert(quote);
            }
            case "PROCESS" -> {
                int order = 0;
                List<Long> stepIds = request.getCraftStepIds();
                if (stepIds == null || stepIds.isEmpty()) {
                    // 整工艺概览：仅引用工艺，不引用具体工序
                    NodeCraftRelation ref = new NodeCraftRelation();
                    ref.setNodeId(nodeId);
                    ref.setCraftId(request.getCraftId());
                    ref.setSortOrder(order);
                    craftRelationMapper.insert(ref);
                } else {
                    for (Long stepId : stepIds) {
                        NodeCraftRelation ref = new NodeCraftRelation();
                        ref.setNodeId(nodeId);
                        ref.setCraftId(request.getCraftId());
                        ref.setCraftStepId(stepId);
                        ref.setSortOrder(order++);
                        craftRelationMapper.insert(ref);
                    }
                }
                insertMedia(nodeId, request.getMedia());
            }
            default -> {
            }
        }
    }

    private void insertMedia(Long nodeId, List<NodeSaveRequest.MediaItem> media) {
        if (media == null) {
            return;
        }
        int order = 0;
        for (NodeSaveRequest.MediaItem item : media) {
            NodeMediaRelation ref = new NodeMediaRelation();
            ref.setNodeId(nodeId);
            ref.setMediaId(item.getMediaId());
            ref.setCaption(item.getCaption());
            ref.setSortOrder(order++);
            mediaRelationMapper.insert(ref);
        }
    }

    private int nextSortOrder(Long chapterId) {
        ChapterNode last = nodeMapper.selectOne(Wrappers.<ChapterNode>lambdaQuery()
                .eq(ChapterNode::getChapterId, chapterId)
                .orderByDesc(ChapterNode::getSortOrder)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 1 : last.getSortOrder() + 1;
    }

    private void checkAllExist(int found, int expected, String message) {
        if (found < expected) {
            throw new BusinessException(message);
        }
    }

    private ChapterNode requireNode(Long nodeId) {
        ChapterNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException("叙事节点不存在");
        }
        return node;
    }

    private void requireChapter(Long chapterId) {
        if (chapterMapper.selectById(chapterId) == null) {
            throw new BusinessException("章节不存在");
        }
    }
}
