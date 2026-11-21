<template>
  <div class="flow-card" :class="[cls, { selected }]">
    <Handle type="target" :position="Position.Left" id="in" />
    <div class="fc-head">
      <span class="fc-badge">{{ typeLabel(data.kind) }}</span>
      <span class="fc-seq">#{{ data.seq }}</span>
      <span v-if="data.merge" class="fc-tag merge">汇合</span>
      <span v-if="data.end" class="fc-tag end">章尾</span>
    </div>
    <div class="fc-title">{{ data.title || '未命名节点' }}</div>
    <div class="fc-meta" v-if="data.kind === 'BRANCH'">选项 {{ (data.choices || []).length }}</div>

    <!-- BRANCH：每个选项一个独立把手 + 恒备「新建选项」把手（增量编辑随时可拖线加选项） -->
    <template v-if="data.kind === 'BRANCH'">
      <Handle v-for="(c, i) in data.choices || []" :key="c.id" type="source" :position="Position.Right"
        :id="'c-' + c.id" class="fc-handle-choice"
        :style="{ top: handleTop(i, (data.choices || []).length + 1) }" :title="'选项：' + c.label" />
      <Handle type="source" :position="Position.Right" id="c-new" class="fc-handle-new"
        :style="{ top: handleTop((data.choices || []).length, (data.choices || []).length + 1) }"
        title="拖线新建选项" />
    </template>
    <Handle v-else type="source" :position="Position.Right" id="out" class="fc-handle-next" title="拖线设置流向覆盖" />
  </div>
</template>

<script setup>
/**
 * 分支编排画布节点卡片（§11.2）。
 * 纯 HTML 渲染（Element Plus 不进画布，spike 规则 1）；BRANCH 每选项一个把手。
 * BRANCH 无选项时提供「新建选项」把手，保证第一个选项也能拖出来。
 */
import { Handle, Position } from '@vue-flow/core'

const props = defineProps({
  id: { type: String, default: '' },
  data: { type: Object, required: true },
  selected: { type: Boolean, default: false }
})

const cls = {
  TEXT: 'is-text',
  TIMELINE: 'is-timeline',
  QUOTE: 'is-quote',
  PROCESS: 'is-process',
  BRANCH: 'is-branch'
}[props.data.kind] || 'is-text'

function typeLabel(t) {
  return { TEXT: '图文', TIMELINE: '时间轴', QUOTE: '人物口述', PROCESS: '工序步骤', BRANCH: '分支抉择' }[t] || t
}

function handleTop(i, total) {
  // 在卡片右侧纵向均分布局，避免把手重叠
  const percent = total === 1 ? 50 : 26 + (i * 48) / (total - 1)
  return percent + '%'
}
</script>

<style scoped>
.flow-card {
  width: 200px; background: #fffdf8; border: 1.5px solid #cbb89d; border-radius: 10px;
  padding: 10px 12px; box-shadow: 0 2px 8px rgba(80, 60, 40, .12); font-size: 12px;
}
.flow-card.selected { border-color: #a75c42; box-shadow: 0 0 0 2px rgba(167, 92, 66, .25); }
.flow-card.is-branch { border-color: #d8863c; background: #fff7ec; }
.fc-head { display: flex; align-items: center; gap: 6px; }
.fc-badge { background: #eee0cb; color: #8a6a42; border-radius: 4px; padding: 1px 6px; font-size: 10px; }
.is-branch .fc-badge { background: #d8863c; color: #fff; }
.is-quote .fc-badge { background: #3b2924; color: #d0ad77; }
.is-timeline .fc-badge { background: #a77c48; color: #fff; }
.is-process .fc-badge { background: #496b4f; color: #fff; }
.fc-seq { color: #b18c5c; font-family: Georgia, serif; }
.fc-tag { margin-left: auto; border-radius: 4px; padding: 1px 6px; font-size: 10px; }
.fc-tag.merge { background: #fde9d2; color: #b06a1e; }
.fc-tag.end { background: #e2f0e2; color: #3d7a3d; }
.fc-title { margin-top: 6px; font-weight: 600; color: #4a3a2c; line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.fc-meta { margin-top: 4px; color: #a08a70; font-size: 11px; }
.fc-handle-next { width: 11px; height: 11px; background: #7a94b8; }
.fc-handle-choice { width: 11px; height: 11px; background: #d8863c; }
.fc-handle-new { width: 11px; height: 11px; background: #fff; border: 2px dashed #d8863c; }
</style>
