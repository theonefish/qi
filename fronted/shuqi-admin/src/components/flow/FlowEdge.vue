<template>
  <BaseEdge :path="path" :style="strokeStyle" :marker-end="markerEnd" />
  <EdgeLabelRenderer>
    <div class="fe-label" :style="{ transform: `translate(-50%,-50%) translate(${labelX}px,${labelY}px)` }">
      <span v-if="labelText" class="fe-text" :class="'k-' + data.kind">{{ labelText }}</span>
      <button v-if="data.kind !== 'default'" class="fe-del" title="删除此连线" @click="remove">×</button>
    </div>
  </EdgeLabelRenderer>
</template>

<script setup>
/**
 * 分支编排画布自定义边（§11.2）：三态样式 + label + × 删除。
 * spike 规则 1：path 本体 pointer-events:none（否则盖住把手）；删除走 × 按钮。
 */
import { computed } from 'vue'
import { BaseEdge, EdgeLabelRenderer, getSmoothStepPath } from '@vue-flow/core'

const props = defineProps({
  id: { type: String, required: true },
  sourceX: { type: Number, required: true },
  sourceY: { type: Number, required: true },
  targetX: { type: Number, required: true },
  targetY: { type: Number, required: true },
  sourcePosition: { type: String, default: 'right' },
  targetPosition: { type: String, default: 'left' },
  data: { type: Object, required: true }
})

const [path, labelX, labelY] = getSmoothStepPath({
  sourceX: props.sourceX,
  sourceY: props.sourceY,
  sourcePosition: props.sourcePosition,
  targetX: props.targetX,
  targetY: props.targetY,
  targetPosition: props.targetPosition
})

const strokeStyle = computed(() => ({
  stroke: { default: '#b7ab9b', override: '#4a7dbb', choice: '#d8863c' }[props.data.kind] || '#b7ab9b',
  strokeWidth: 2,
  strokeDasharray: props.data.kind === 'choice' ? '6 4' : undefined,
  pointerEvents: 'none'
}))

const labelText = computed(() => props.data.label || (props.data.kind === 'override' ? '流向覆盖' : ''))

function remove() {
  if (props.data.onDelete) props.data.onDelete(props.id)
}
</script>

<style scoped>
.fe-label { position: absolute; pointer-events: all; display: flex; align-items: center; gap: 4px; }
.fe-text {
  background: #fffdf8; border: 1px solid #e0d3bd; border-radius: 6px;
  padding: 1px 8px; font-size: 11px; color: #6b5b48; white-space: nowrap;
}
.fe-text.k-choice { color: #b06a1e; border-color: #ecc394; }
.fe-text.k-override { color: #3d6ba3; border-color: #b8cee8; }
.fe-del {
  width: 16px; height: 16px; line-height: 13px; border-radius: 50%;
  border: 1px solid #d8c4a8; background: #fff; color: #a75c42; cursor: pointer;
  font-size: 12px; padding: 0; text-align: center;
}
.fe-del:hover { background: #a75c42; color: #fff; }
</style>
