<template>
  <div class="flow-page" v-if="loaded">

    <header class="flow-header">
      <el-button size="small" @click="$router.push(`/chapters/${chapterId}/edit`)">返回编辑</el-button>
      <b class="flow-title">{{ chapter ? chapter.title : '' }}</b>
      <el-tag size="small" :type="chapter && chapter.status === 1 ? 'success' : 'info'">
        {{ chapter && chapter.status === 1 ? '已发布' : chapter && chapter.status === 2 ? '已下架' : '草稿' }}
      </el-tag>
      <el-dropdown trigger="click" @command="createNode">
        <el-button size="small" type="primary">新增节点</el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="TEXT">图文节点</el-dropdown-item>
            <el-dropdown-item command="TIMELINE">时间轴节点</el-dropdown-item>
            <el-dropdown-item command="QUOTE">人物口述节点</el-dropdown-item>
            <el-dropdown-item command="PROCESS">工序步骤节点</el-dropdown-item>
            <el-dropdown-item command="BRANCH" divided>分支抉择节点</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button size="small" :type="errorCount ? 'danger' : 'success'" plain @click="issuesVisible = true">
        校验问题（{{ issues.length }}）
      </el-button>
      <el-button size="small" @click="fitAll">适配视图</el-button>
      <el-button size="small" type="warning" @click="publishDialog = true">发布章节</el-button>
      <span class="flow-tip">点卡片撰写 · 拖把手连线 · 拖卡片摆位</span>
    </header>

    <div class="flow-canvas">
      <VueFlow v-model:nodes="nodes" v-model:edges="edges" :node-types="nodeTypes" :edge-types="edgeTypes"
        :is-valid-connection="isValidConnection" fit-view-on-init
        @connect="onConnect" @node-click="openDrawer" @node-drag-stop="onDragStop" />
    </div>

    <!-- 校验面板（前端预检；发布以后端校验为准） -->
    <el-drawer v-model="issuesVisible" title="结构校验（实时预检）" size="380px">
      <div v-if="!issues.length" class="issue-ok">✓ 未发现问题，可以发布</div>
      <div v-else class="issue-list">
        <div v-for="(it, i) in issues" :key="i" class="issue-item" :class="'is-' + it.level" @click="focusNode(it.nodeId)">
          <el-tag size="small" :type="it.level === 'error' ? 'danger' : 'warning'">
            {{ it.level === 'error' ? '阻断' : '建议' }}
          </el-tag>
          <span class="issue-msg">{{ it.msg }}</span>
        </div>
      </div>
      <div class="issue-foot">发布以后端校验为准；「建议」不阻断发布</div>
    </el-drawer>

    <!-- 撰写台抽屉：内嵌 NodeEditor（§11.2） -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="70%" :destroy-on-close="true">
      <NodeEditor v-if="drawerVisible" :key="drawerKey" :chapter-id="chapterId" :node="drawerNode"
        :detail="drawerDetail" :initial-choices="drawerChoices" :nodes-for-target="flowNodesRaw"
        :create-type="createType" @saved="onNodeSaved" @cancel="drawerVisible = false" />
    </el-drawer>

    <!-- 发布 -->
    <el-dialog v-model="publishDialog" title="发布章节" width="480px">
      <el-input v-model="publishSummary" type="textarea" :rows="3" placeholder="修改内容说明（必填）" />
      <template #footer>
        <el-button @click="publishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="doPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>

  <!-- 加载失败兜底（不再静默空白） -->
  <div class="flow-loaderror" v-else>
    <p>编排数据加载失败：</p>
    <pre>{{ loadError }}</pre>
    <el-button size="small" @click="load">重试</el-button>
  </div>
</template>

<script setup>
/**
 * 分支编排页（分支叙事设计稿 §11）：图连线编排 + 抽屉撰写台一站式。
 * 集成三规则（spike 实测，§11.6）：
 *  1. 自定义边 path pointer-events:none（FlowEdge 内处理）
 *  2. 数据单向流：本组件数组是唯一事实源，增删一律数组替换，不用 store 的 addEdges/removeEdges
 *  3. isValidConnection 只放幂等结构校验；语义校验（一选项一边）在 onConnect 拦新建
 */
import { computed, markRaw, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VueFlow, useVueFlow, MarkerType } from '@vue-flow/core'
import '@vue-flow/core/dist/style.css'
import { chapterApi, nodeApi, flowApi, uploadConfig } from '../api'
import { buildPayloadFromDetail } from '../utils/node-payload.js'
import { validateFlow } from '../utils/flow-validate.js'
import NodeEditor from '../components/NodeEditor.vue'
import FlowNodeCard from '../components/flow/FlowNodeCard.vue'
import FlowEdge from '../components/flow/FlowEdge.vue'

const route = useRoute()
const router = useRouter()
const chapterId = Number(route.params.id)

const nodeTypes = { 'flow-card': markRaw(FlowNodeCard) }
const edgeTypes = { 'flow-edge': markRaw(FlowEdge) }

const { fitView } = useVueFlow()

const loaded = ref(false)
const loadError = ref('')
const chapter = ref(null)
const flowData = ref(null)   // flowApi.get 原始数据（校验面板用）
const nodes = ref([])
const edges = ref([])
const flowNodesRaw = ref([]) // 选项目标下拉用（全部节点）

// ---- 加载与画布数据组装 ----
async function load() {
  try {
    const detail = await chapterApi.detail(chapterId)
    chapter.value = detail.chapter
    const flow = await flowApi.get(chapterId)
    flowData.value = flow
    flowNodesRaw.value = [...flow.nodes].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
    rebuild(flow)
    loaded.value = true
  } catch (e) {
    loadError.value = String((e && (e.message || e.stack)) || e)
    ElMessage.error('编排数据加载失败')
  }
}

function rebuild(flow) {
  const sorted = [...flow.nodes].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
  const byId = Object.fromEntries(sorted.map((n) => [n.id, n]))
  const choicesByNode = {}
  for (const c of flow.choices || []) (choicesByNode[c.nodeId] ||= []).push(c)
  const next = {}
  sorted.forEach((n, i) => {
    next[n.id] = n.nextNodeId != null ? n.nextNodeId : (sorted[i + 1] ? sorted[i + 1].id : null)
  })
  const layout = computeLayout(sorted, next, choicesByNode)
  const choiceEdgeIds = new Set()

  nodes.value = sorted.map((n) => ({
    id: String(n.id),
    type: 'flow-card',
    position: parsePos(n.flowPos) || layout[n.id] || { x: 0, y: 0 },
    data: {
      kind: n.nodeType, seq: n.sortOrder, title: n.title,
      merge: (flow.choices || []).filter((c) => c.targetNodeId === n.id).length >= 1 || countIn(n.id) >= 2,
      end: n.nodeType !== 'BRANCH' && next[n.id] == null,
      choices: (choicesByNode[n.id] || []).map((c) => ({ id: c.id, label: c.label }))
    }
  }))

  function countIn(id) {
    let deg = 0
    for (const m of sorted) {
      if (m.nodeType !== 'BRANCH' && next[m.id] === id) deg++
      deg += (choicesByNode[m.id] || []).filter((c) => c.targetNodeId === id).length
    }
    return deg
  }

  const newEdges = []
  for (const n of sorted) {
    if (n.nodeType === 'BRANCH') continue // BRANCH 无默认边（强制选择）
    const to = next[n.id]
    if (to != null && byId[to]) {
      newEdges.push({
        id: 'e' + n.id, source: String(n.id), target: String(to), type: 'flow-edge',
        data: { kind: n.nextNodeId != null ? 'override' : 'default', onDelete: deleteEdge }
      })
    }
  }
  for (const c of flow.choices || []) {
    choiceEdgeIds.add('c' + c.id)
    newEdges.push({
      id: 'c' + c.id, source: String(c.nodeId), sourceHandle: 'c-' + c.id,
      target: String(c.targetNodeId), type: 'flow-edge',
      label: c.label, data: { kind: 'choice', label: c.label, choiceId: c.id, onDelete: deleteEdge },
      markerEnd: MarkerType.ArrowClosed
    })
  }
  edges.value = newEdges
}

function parsePos(flowPos) {
  if (!flowPos) return null
  const [x, y] = flowPos.split(',').map(Number)
  return Number.isFinite(x) && Number.isFinite(y) ? { x, y } : null
}

/** 自动布局：BFS 深度分层（§11.2），仅对无存量坐标的节点生效 */
function computeLayout(sorted, next, choicesByNode) {
  const adj = {}
  for (const n of sorted) {
    const tos = []
    if (n.nodeType === 'BRANCH') {
      for (const c of choicesByNode[n.id] || []) tos.push(String(c.targetNodeId))
    } else if (next[n.id] != null) {
      tos.push(String(next[n.id]))
    }
    adj[String(n.id)] = tos
  }
  const depth = {}
  const entry = sorted.length ? String(sorted[0].id) : null
  if (entry) {
    const queue = [entry]
    depth[entry] = 0
    while (queue.length) {
      const cur = queue.shift()
      for (const to of adj[cur] || []) {
        if (depth[to] === undefined) {
          depth[to] = depth[cur] + 1
          queue.push(to)
        }
      }
    }
  }
  const perDepth = {}
  const layout = {}
  for (const n of sorted) {
    if (n.flowPos) continue // 存量坐标优先
    const d = depth[String(n.id)] ?? 0
    const idx = perDepth[d] = (perDepth[d] ?? -1) + 1
    layout[String(n.id)] = { x: d * 300, y: idx * 150 }
  }
  return layout
}

// ---- 校验预检 ----
const issues = computed(() => validateFlow(flowData.value))
const errorCount = computed(() => issues.value.filter((i) => i.level === 'error').length)
const issuesVisible = ref(false)

function focusNode(nodeId) {
  if (!nodeId) return
  issuesVisible.value = false
  fitView({ nodes: [String(nodeId)], duration: 400, padding: 0.3 })
}

function fitAll() {
  fitView({ duration: 400, padding: 0.15 })
}

// ---- 连线（§11.2 交互落点） ----
function isValidConnection(conn) {
  // 仅幂等结构校验（spike 规则 3：该函数会被用于复验已有边）
  return !!(conn && conn.source && conn.target && conn.source !== conn.target)
}

async function onConnect(conn) {
  const src = nodes.value.find((n) => n.id === conn.source)
  const srcData = src && src.data
  if (srcData && srcData.kind === 'BRANCH') {
    // 语义校验：一个选项把手只允许一条边
    const dup = edges.value.find((e) => e.source === conn.source && e.sourceHandle === conn.sourceHandle)
    if (dup) {
      ElMessage.warning('该选项已有连线：一个选项只能指向一个目标')
      return
    }
    const { value } = await ElMessageBox.prompt('输入按钮文案（存入 node_choice）', '新增选项', { inputValue: '新选项' })
    const label = (value || '').trim()
    if (!label) return
    await mutateChoices(conn.source, (list) => [...list, { label, targetNodeId: Number(conn.target) }])
    ElMessage.success(`选项「${label}」已创建`)
  } else {
    await flowApi.setNext(chapterId, Number(conn.source), Number(conn.target))
    ElMessage.success('流向覆盖已保存')
    await load()
  }
}

/** 删边：选项边 → 全量保存去 choices；覆盖边 → setNext(null)；默认边不可删 */
async function deleteEdge(edgeId) {
  const edge = edges.value.find((e) => e.id === edgeId)
  if (!edge) return
  if (edge.data.kind === 'choice') {
    await mutateChoices(edge.source, (list) => list.filter((c) => 'c' + c.id !== edgeId))
    ElMessage.success('选项已删除')
  } else if (edge.data.kind === 'override') {
    await flowApi.setNext(chapterId, Number(edge.source), null)
    ElMessage.success('已清除流向覆盖（回落默认链）')
  }
}

/**
 * BRANCH 选项的结构变更：detail → buildPayloadFromDetail 改 choices → 全量 update。
 * choices 非空 = 整体替换（§2.2）；payload 必须带 flowPos/nextNodeId 防显式 set 清空。
 */
async function mutateChoices(branchNodeId, mutator) {
  const detail = await nodeApi.detail(chapterId, Number(branchNodeId))
  const current = (flowData.value.choices || []).filter((c) => String(c.nodeId) === branchNodeId)
    .map((c) => ({ label: c.label, targetNodeId: c.targetNodeId }))
  const nextChoices = mutator(current)
  const payload = buildPayloadFromDetail(detail, { choices: nextChoices })
  await nodeApi.update(chapterId, Number(branchNodeId), payload)
  await load()
}

// ---- 撰写台抽屉 ----
const drawerVisible = ref(false)
const drawerKey = ref(0)
const drawerNode = ref(null)   // { id, nodeType, title, subtitle, source, flowPos, nextNodeId }
const drawerDetail = ref(null)
const drawerChoices = ref([])
const createType = ref('TEXT')

const drawerTitle = computed(() => (drawerNode.value && drawerNode.value.id
  ? `撰写节点：${drawerNode.value.title || '未命名'}`
  : `新增节点（${{ TEXT: '图文', TIMELINE: '时间轴', QUOTE: '人物口述', PROCESS: '工序步骤', BRANCH: '分支抉择' }[createType.value]}）`))

async function openDrawer({ node }) {
  const id = Number(node.id)
  const raw = flowNodesRaw.value.find((n) => n.id === id)
  drawerDetail.value = await nodeApi.detail(chapterId, id)
  drawerNode.value = {
    id, nodeType: raw.nodeType, title: raw.title, subtitle: raw.subtitle,
    source: null, flowPos: raw.flowPos, nextNodeId: raw.nextNodeId
  }
  // detail.node 有全字段，标题/副标题/来源以其为准
  if (drawerDetail.value && drawerDetail.value.node) {
    Object.assign(drawerNode.value, {
      title: drawerDetail.value.node.title,
      subtitle: drawerDetail.value.node.subtitle,
      source: drawerDetail.value.node.source
    })
  }
  drawerChoices.value = (flowData.value.choices || []).filter((c) => c.nodeId === id)
  createType.value = raw.nodeType
  drawerKey.value++
  drawerVisible.value = true
}

function createNode(type) {
  drawerNode.value = null
  drawerDetail.value = null
  drawerChoices.value = []
  createType.value = type
  drawerKey.value++
  drawerVisible.value = true
}

async function onNodeSaved() {
  drawerVisible.value = false
  await load()
}

// ---- 拖卡片：坐标防抖保存（§11.2） ----
const pendingLayout = {}
let layoutTimer = null
function onDragStop({ node }) {
  pendingLayout[node.id] = `${Math.round(node.position.x)},${Math.round(node.position.y)}`
  clearTimeout(layoutTimer)
  layoutTimer = setTimeout(() => {
    flushLayout()
  }, 800)
}

async function flushLayout() {
  const items = Object.entries(pendingLayout).map(([nodeId, flowPos]) => ({ nodeId: Number(nodeId), flowPos }))
  if (!items.length) return
  for (const key of Object.keys(pendingLayout)) delete pendingLayout[key]
  try {
    await flowApi.saveLayout(chapterId, items)
  } catch (e) { /* 拦截器已提示 */ }
}

// ---- 发布 ----
const publishDialog = ref(false)
const publishSummary = ref('')
const publishing = ref(false)

async function doPublish() {
  if (!publishSummary.value.trim()) {
    ElMessage.warning('修改内容说明必填（7.14）')
    return
  }
  await flushLayout()
  publishing.value = true
  try {
    await chapterApi.publish(chapterId, publishSummary.value.trim())
    ElMessage.success('发布成功')
    publishDialog.value = false
    await load()
  } catch (e) {
    /* 拦截器已提示（后端校验文案直接展示） */
  } finally {
    publishing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.flow-page { display: flex; flex-direction: column; height: calc(100vh - 90px); padding: 12px; gap: 10px; }
.flow-header { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.flow-title { font-size: 16px; color: #28231f; }
.flow-tip { margin-left: auto; font-size: 12px; color: #a08a70; }
.flow-canvas {
  flex: 1; border: 1px solid #d8c9b4; border-radius: 10px; overflow: hidden;
  background-image: radial-gradient(#e6dcc8 1px, transparent 1px); background-size: 18px 18px;
}
.issue-ok { color: #3d7a3d; font-size: 14px; padding: 20px 0; text-align: center; }
.issue-list { display: flex; flex-direction: column; gap: 10px; }
.issue-item {
  display: flex; align-items: flex-start; gap: 8px; padding: 10px;
  border: 1px solid #eee0cb; border-radius: 8px; cursor: pointer; background: #fffdf8;
}
.issue-item:hover { background: #fff7ec; }
.issue-msg { font-size: 13px; color: #4a3a2c; line-height: 1.6; }
.issue-foot { margin-top: 14px; font-size: 12px; color: #a08a70; }
.flow-loaderror { padding: 40px; color: #6b4a26; }
.flow-loaderror pre { background: #fff7ec; border: 1px solid #ecc394; border-radius: 8px; padding: 12px;
  font-size: 12px; white-space: pre-wrap; word-break: break-all; margin: 10px 0; }
</style>
