/**
 * 分支叙事画布前端预检（分支叙事设计稿 §11.3）。
 * 与后端 ChapterFlowService.validateForPublish 同规则；发布以后端校验为准。
 * 注：引导文案为空的软提示在抽屉保存时提示（FlowView 不含节点 content）。
 *
 * 输入：flowApi.get 的 FlowView { nodes, choices, entryNodeId, endNodeIds }
 * 输出：[{ level: 'error'|'warn', msg, nodeId }]
 */

/** 有效后继：nextNodeId ?? sortOrder 的下一节点（与后端 effectiveNext 一致） */
function effectiveNext(sorted) {
  const next = {}
  sorted.forEach((n, i) => {
    next[n.id] = n.nextNodeId != null ? n.nextNodeId : (sorted[i + 1] ? sorted[i + 1].id : null)
  })
  return next
}

/** 全图邻接：非分支有效后继 + 选项边（BRANCH 无默认边） */
function adjacency(sorted, next, choicesByNode) {
  const adj = {}
  for (const n of sorted) {
    const tos = []
    if (n.nodeType === 'BRANCH') {
      for (const c of choicesByNode[n.id] || []) tos.push(c.targetNodeId)
    } else if (next[n.id] != null) {
      tos.push(next[n.id])
    }
    adj[n.id] = tos
  }
  return adj
}

function maxBranchDepth(nodeId, adj, byId, depth) {
  let max = depth
  for (const to of adj[nodeId] || []) {
    const t = byId[to]
    if (t && t.nodeType === 'BRANCH') max = Math.max(max, maxBranchDepth(to, adj, byId, depth + 1))
    else max = Math.max(max, maxBranchDepth(to, adj, byId, depth))
  }
  return max
}

export function validateFlow(view) {
  const issues = []
  if (!view || !view.nodes || view.nodes.length === 0) {
    return [{ level: 'error', msg: '章节还没有叙事节点', nodeId: null }]
  }
  const sorted = [...view.nodes].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
  const ids = new Set(sorted.map((n) => n.id))
  const byId = Object.fromEntries(sorted.map((n) => [n.id, n]))
  const choicesByNode = {}
  for (const c of view.choices || []) (choicesByNode[c.nodeId] ||= []).push(c)
  const next = effectiveNext(sorted)

  // 引用合法 + BRANCH 选项规则
  for (const n of sorted) {
    if (n.nextNodeId != null) {
      if (n.nextNodeId === n.id) {
        issues.push({ level: 'error', msg: `节点「${n.title}」的流向指向自身`, nodeId: n.id })
      } else if (!ids.has(n.nextNodeId)) {
        issues.push({ level: 'error', msg: `节点「${n.title}」的流向目标不在本章节`, nodeId: n.id })
      }
    }
    if (n.nodeType === 'BRANCH') {
      const cs = choicesByNode[n.id] || []
      if (cs.length < 2) {
        issues.push({ level: 'error', msg: `分支「${n.title}」至少需要两个选项（当前 ${cs.length}）`, nodeId: n.id })
      }
      const seen = new Set()
      for (const c of cs) {
        if (!ids.has(c.targetNodeId)) {
          issues.push({ level: 'error', msg: `分支「${n.title}」的选项「${c.label}」目标不在本章节`, nodeId: n.id })
        }
        if (seen.has(c.targetNodeId)) {
          issues.push({ level: 'error', msg: `分支「${n.title}」多个选项指向同一目标（请合并）`, nodeId: n.id })
        }
        seen.add(c.targetNodeId)
      }
    }
  }

  // 软提示：分支点数量
  const branchCount = sorted.filter((n) => n.nodeType === 'BRANCH').length
  if (branchCount > 2) {
    issues.push({ level: 'warn', msg: `分支点数量 ${branchCount} 超过建议值 2，建议精简`, nodeId: null })
  }

  // 无环（三色 DFS，与后端一致）
  const adj = adjacency(sorted, next, choicesByNode)
  const state = {}
  let cycleFound = false
  for (const n of sorted) {
    if (state[n.id] || cycleFound) continue
    const stack = [[n.id, 0]]
    state[n.id] = 1
    while (stack.length) {
      const top = stack[stack.length - 1]
      const tos = adj[top[0]] || []
      if (top[1] >= tos.length) {
        state[top[0]] = 2
        stack.pop()
        continue
      }
      const to = tos[top[1]]
      top[1] += 1
      if (state[to] === 1) {
        issues.push({ level: 'error', msg: `章节路径存在循环（节点「${byId[to].title}」）`, nodeId: to })
        cycleFound = true
        break
      }
      if (!state[to]) {
        state[to] = 1
        stack.push([to, 0])
      }
    }
  }

  // 无孤岛：入口（sortOrder 最小节点）可达
  const reachable = new Set([sorted[0].id])
  const queue = [sorted[0].id]
  while (queue.length) {
    for (const to of adj[queue.shift()] || []) {
      if (!reachable.has(to)) {
        reachable.add(to)
        queue.push(to)
      }
    }
  }
  for (const n of sorted) {
    if (!reachable.has(n.id)) {
      issues.push({ level: 'error', msg: `节点「${n.title}」无法从章节开头到达（孤岛）`, nodeId: n.id })
    }
  }

  // 收尾唯一
  const ends = sorted.filter((n) => n.nodeType !== 'BRANCH' && next[n.id] == null)
  if (ends.length !== 1) {
    issues.push({
      level: 'error',
      msg: `章节收尾节点必须唯一（当前 ${ends.length} 个${ends.length ? '：' + ends.map((e) => byId[e.id].title).join('、') : ''}）`,
      nodeId: ends.length ? ends[0].id : null
    })
  }

  // 嵌套深度软提示
  for (const n of sorted.filter((x) => x.nodeType === 'BRANCH')) {
    const depth = maxBranchDepth(n.id, adj, byId, 1)
    if (depth > 2) {
      issues.push({ level: 'warn', msg: `分支「${n.title}」嵌套深度 ${depth} 超过建议值`, nodeId: n.id })
    }
  }

  // 去重（msg 相同的合并）
  const seen = new Set()
  return issues.filter((i) => (seen.has(i.msg) ? false : (seen.add(i.msg), true)))
}
