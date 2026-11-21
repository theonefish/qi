package com.example.qi.module.chapter.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.qi.common.BusinessException;
import com.example.qi.module.chapter.entity.ChapterNode;
import com.example.qi.module.chapter.entity.NodeChoice;
import com.example.qi.module.chapter.mapper.ChapterNodeMapper;
import com.example.qi.module.chapter.mapper.NodeChoiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 章节流向图服务（分支叙事设计稿 §9.4）。
 * 有效后继 = next_node_id ?? sortOrder 下一节点；BRANCH 节点走向由 node_choice 决定
 * （强制选择，不参与默认线性链，其默认后继不构成图边）。
 * 供图编排接口（§7.4）与发布校验（§3.3）复用；全图边集 = 非分支有效后继 + 全部选项边。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterFlowService {

    private final ChapterNodeMapper nodeMapper;
    private final NodeChoiceMapper choiceMapper;

    /** 图编排节点视图（inDegree 由后端现算，画布直接用） */
    public record FlowNodeView(Long id, String nodeType, String title, Integer sortOrder,
                               String flowPos, Long nextNodeId, int inDegree, int choiceCount) {
    }

    /** GET /admin/chapters/{id}/flow 数据 */
    public record FlowView(List<FlowNodeView> nodes, List<NodeChoice> choices,
                           Long entryNodeId, List<Long> endNodeIds) {
    }

    /** 批量取选项：组内按 sort_order，与§9.4 约定一致 */
    public List<NodeChoice> listChoices(List<ChapterNode> nodes) {
        if (nodes.isEmpty()) {
            return List.of();
        }
        List<Long> nodeIds = nodes.stream().map(ChapterNode::getId).toList();
        return choiceMapper.selectList(Wrappers.<NodeChoice>lambdaQuery()
                .in(NodeChoice::getNodeId, nodeIds)
                .orderByAsc(NodeChoice::getNodeId)
                .orderByAsc(NodeChoice::getSortOrder)
                .orderByAsc(NodeChoice::getId));
    }

    /** 图编排数据组装 */
    public FlowView flowView(Long chapterId) {
        List<ChapterNode> sorted = listSorted(chapterId);
        List<NodeChoice> choices = listChoices(sorted);
        Map<Long, Long> next = effectiveNext(sorted);
        Map<Long, List<NodeChoice>> byNode = groupChoices(choices);

        Map<Long, Integer> inDegree = new HashMap<>();
        for (ChapterNode n : sorted) {
            // BRANCH 无默认边（强制选择），仅选项边计入入度
            if (!isBranch(n)) {
                Long to = next.get(n.getId());
                if (to != null) {
                    inDegree.merge(to, 1, Integer::sum);
                }
            }
        }
        for (NodeChoice c : choices) {
            inDegree.merge(c.getTargetNodeId(), 1, Integer::sum);
        }

        Map<Long, Long> counts = choices.stream().collect(
                Collectors.groupingBy(NodeChoice::getNodeId, Collectors.counting()));
        List<Long> endIds = new ArrayList<>();
        for (ChapterNode n : sorted) {
            boolean end = isBranch(n)
                    ? counts.getOrDefault(n.getId(), 0L) == 0
                    : next.get(n.getId()) == null;
            if (end) {
                endIds.add(n.getId());
            }
        }
        List<FlowNodeView> views = sorted.stream().map(n -> new FlowNodeView(
                n.getId(), n.getNodeType(), n.getTitle(), n.getSortOrder(), n.getFlowPos(),
                n.getNextNodeId(), inDegree.getOrDefault(n.getId(), 0),
                counts.getOrDefault(n.getId(), 0L).intValue())).toList();
        return new FlowView(views, choices, sorted.isEmpty() ? null : sorted.get(0).getId(), endIds);
    }

    /** 连线轻接口：设置/清除 next_node_id（画布拖线与删边） */
    @Transactional
    public void setNext(Long chapterId, Long nodeId, Long targetNodeId) {
        ChapterNode node = requireNode(nodeId);
        if (!node.getChapterId().equals(chapterId)) {
            throw new BusinessException("节点不属于该章节");
        }
        if ("BRANCH".equals(node.getNodeType())) {
            throw new BusinessException("分支节点走向由选项决定，不可设置流向覆盖");
        }
        if (targetNodeId != null) {
            if (targetNodeId.equals(nodeId)) {
                throw new BusinessException("流向目标不能是节点自身");
            }
            ChapterNode target = nodeMapper.selectById(targetNodeId);
            if (target == null || !target.getChapterId().equals(chapterId)) {
                throw new BusinessException("流向目标节点无效（须为本章节的其他节点）");
            }
        }
        nodeMapper.update(null, Wrappers.<ChapterNode>lambdaUpdate()
                .eq(ChapterNode::getId, nodeId)
                .set(ChapterNode::getNextNodeId, targetNodeId));
        log.info("流向覆盖更新：chapterId={} nodeId={} -> {}", chapterId, nodeId,
                targetNodeId == null ? "清除（回落默认链）" : targetNodeId);
    }

    /** 批量保存画布坐标（拖动防抖调用） */
    @Transactional
    public void saveLayout(Long chapterId, List<com.example.qi.module.chapter.dto.FlowLayoutRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        if (items.size() > 200) {
            throw new BusinessException("单次坐标保存不得超过 200 个节点");
        }
        for (com.example.qi.module.chapter.dto.FlowLayoutRequest.Item item : items) {
            ChapterNode node = requireNode(item.getNodeId());
            if (!node.getChapterId().equals(chapterId)) {
                throw new BusinessException("节点不属于该章节");
            }
            nodeMapper.update(null, Wrappers.<ChapterNode>lambdaUpdate()
                    .eq(ChapterNode::getId, node.getId())
                    .set(ChapterNode::getFlowPos, item.getFlowPos()));
        }
        // 拖动防抖高频调用，debug 级别
        log.debug("画布坐标保存：chapterId={} count={}", chapterId, items.size());
    }

    /**
     * 发布硬校验 + 软提示（§3.3 / §9.5）。
     * 硬阻断：引用合法、BRANCH≥2 选项、无环、无孤岛、收尾唯一（无环+单收尾+全可达 ⇒ 所有路径终止于收尾）。
     * 软提示仅 log.warn：分支点过多、嵌套过深、引导文案为空。
     */
    public void validateForPublish(List<ChapterNode> sorted, List<NodeChoice> choices) {
        if (sorted.isEmpty()) {
            return; // 「至少一个有效节点」由 ChapterService 校验
        }
        Set<Long> ids = sorted.stream().map(ChapterNode::getId).collect(Collectors.toSet());
        Map<Long, ChapterNode> byId = sorted.stream()
                .collect(Collectors.toMap(ChapterNode::getId, Function.identity()));
        Map<Long, Long> next = effectiveNext(sorted);
        Map<Long, List<NodeChoice>> byNode = groupChoices(choices);

        // 1 引用合法性（覆盖边同章、非自身）
        for (ChapterNode n : sorted) {
            if (n.getNextNodeId() != null
                    && (!ids.contains(n.getNextNodeId()) || n.getNextNodeId().equals(n.getId()))) {
                throw new BusinessException("发布条件不满足：节点「" + n.getTitle() + "」的流向目标无效（须为本章节其他节点）");
            }
        }
        // 2 BRANCH 选项 ≥2、文案非空、目标同章、不重复
        for (ChapterNode n : sorted) {
            if (!isBranch(n)) {
                continue;
            }
            List<NodeChoice> cs = byNode.getOrDefault(n.getId(), List.of());
            if (cs.size() < 2) {
                throw new BusinessException("发布条件不满足：分支节点「" + n.getTitle() + "」至少需要两个选项");
            }
            Set<Long> targets = new HashSet<>();
            for (NodeChoice c : cs) {
                if (!StringUtils.hasText(c.getLabel())) {
                    throw new BusinessException("发布条件不满足：分支节点「" + n.getTitle() + "」存在空选项文案");
                }
                if (!ids.contains(c.getTargetNodeId())) {
                    throw new BusinessException("发布条件不满足：分支节点「" + n.getTitle() + "」的选项目标无效");
                }
                if (!targets.add(c.getTargetNodeId())) {
                    throw new BusinessException("发布条件不满足：分支节点「" + n.getTitle() + "」多个选项指向同一目标");
                }
            }
        }
        // 全图边集：非分支有效后继 + 选项边（§9.4）
        Map<Long, List<Long>> adj = new HashMap<>();
        for (ChapterNode n : sorted) {
            List<Long> tos = new ArrayList<>();
            if (isBranch(n)) {
                for (NodeChoice c : byNode.getOrDefault(n.getId(), List.of())) {
                    tos.add(c.getTargetNodeId());
                }
            } else {
                Long to = next.get(n.getId());
                if (to != null) {
                    tos.add(to);
                }
            }
            adj.put(n.getId(), tos);
        }
        // 3 无环：迭代式三色 DFS（0 未访问 / 1 在栈 / 2 完成）
        Map<Long, Integer> state = new HashMap<>();
        for (ChapterNode n : sorted) {
            if (state.getOrDefault(n.getId(), 0) != 0) {
                continue;
            }
            Deque<long[]> stack = new ArrayDeque<>(); // [nodeId, nextEdgeIndex]
            Map<Long, Integer> cursor = new HashMap<>();
            stack.push(new long[]{n.getId(), 0});
            state.put(n.getId(), 1);
            while (!stack.isEmpty()) {
                long[] top = stack.peek();
                long cur = top[0];
                List<Long> tos = adj.getOrDefault(cur, List.of());
                int idx = cursor.merge(cur, 1, Integer::sum) - 1;
                if (idx >= tos.size()) {
                    state.put(cur, 2);
                    stack.pop();
                    continue;
                }
                long to = tos.get(idx);
                int st = state.getOrDefault(to, 0);
                if (st == 1) {
                    throw new BusinessException("发布条件不满足：章节路径存在循环（节点「"
                            + byId.get(to).getTitle() + "」）");
                }
                if (st == 0) {
                    state.put(to, 1);
                    stack.push(new long[]{to, 0});
                }
            }
        }
        // 4 无孤岛：入口（sortOrder 最小节点）正向可达全部节点
        Long entry = sorted.get(0).getId();
        Set<Long> reachable = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(entry);
        reachable.add(entry);
        while (!queue.isEmpty()) {
            for (long to : adj.getOrDefault(queue.poll(), List.of())) {
                if (reachable.add(to)) {
                    queue.add(to);
                }
            }
        }
        for (ChapterNode n : sorted) {
            if (!reachable.contains(n.getId())) {
                throw new BusinessException("发布条件不满足：节点「" + n.getTitle() + "」无法从章节开头到达");
            }
        }
        // 5 收尾唯一：无有效后继的节点必须唯一（BRANCH 因必有选项，恒非收尾）
        List<Long> ends = sorted.stream()
                .filter(n -> !isBranch(n) && next.get(n.getId()) == null)
                .map(ChapterNode::getId).toList();
        if (ends.size() != 1) {
            throw new BusinessException("发布条件不满足：章节收尾节点必须唯一，当前 " + ends.size() + " 个");
        }
        // 6 软提示（不阻断，v1 由图编排页前端预检承担展示）
        long branchCount = sorted.stream().filter(n -> isBranch(n)).count();
        if (branchCount > 2) {
            log.warn("[分支叙事软提示] 章节 {} 分支点数量 {} 超过建议值 2", sorted.get(0).getChapterId(), branchCount);
        }
        for (ChapterNode n : sorted) {
            if (isBranch(n) && branchDepth(n.getId(), adj, byId, 1) > 2) {
                log.warn("[分支叙事软提示] 章节 {} 分支节点「{}」嵌套深度超过建议值", sorted.get(0).getChapterId(), n.getTitle());
            }
            if (isBranch(n) && !StringUtils.hasText(n.getContent())) {
                log.warn("[分支叙事软提示] 章节 {} 分支节点「{}」未填写引导文案", sorted.get(0).getChapterId(), n.getTitle());
            }
        }
    }

    // ------------------------------------------------------------------

    private List<ChapterNode> listSorted(Long chapterId) {
        return nodeMapper.selectList(Wrappers.<ChapterNode>lambdaQuery()
                .eq(ChapterNode::getChapterId, chapterId)
                .orderByAsc(ChapterNode::getSortOrder)
                .orderByAsc(ChapterNode::getId));
    }

    /** 有效后继：next_node_id ?? sortOrder 的下一节点（含 null=收尾） */
    private Map<Long, Long> effectiveNext(List<ChapterNode> sorted) {
        Map<Long, Long> next = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            ChapterNode n = sorted.get(i);
            Long fallback = i + 1 < sorted.size() ? sorted.get(i + 1).getId() : null;
            next.put(n.getId(), n.getNextNodeId() != null ? n.getNextNodeId() : fallback);
        }
        return next;
    }

    private Map<Long, List<NodeChoice>> groupChoices(List<NodeChoice> choices) {
        return choices.stream().collect(Collectors.groupingBy(NodeChoice::getNodeId));
    }

    private boolean isBranch(ChapterNode node) {
        return "BRANCH".equals(node.getNodeType());
    }

    /** 沿选项边的分支嵌套深度：从分支出发经选项边最多可连续遇到的分支层数 */
    private int branchDepth(Long nodeId, Map<Long, List<Long>> adj, Map<Long, ChapterNode> byId, int depth) {
        int max = depth;
        for (long to : adj.getOrDefault(nodeId, List.of())) {
            ChapterNode t = byId.get(to);
            if (t != null && isBranch(t)) {
                max = Math.max(max, branchDepth(to, adj, byId, depth + 1));
            } else {
                max = Math.max(max, branchDepth(to, adj, byId, depth));
            }
        }
        return max;
    }

    private ChapterNode requireNode(Long nodeId) {
        ChapterNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException("叙事节点不存在");
        }
        return node;
    }
}
