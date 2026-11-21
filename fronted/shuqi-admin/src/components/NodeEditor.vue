<template>
  <div class="node-editor" v-loading="loading">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="96px">
      <el-form-item label="节点类型" prop="nodeType">
        <el-radio-group v-model="form.nodeType" :disabled="!!nodeId">
          <el-radio-button value="TEXT">图文</el-radio-button>
          <el-radio-button value="TIMELINE">时间轴</el-radio-button>
          <el-radio-button value="QUOTE">人物口述</el-radio-button>
          <el-radio-button value="PROCESS">工序步骤</el-radio-button>
          <el-radio-button value="BRANCH">分支抉择</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="节点标题" prop="title">
        <el-input v-model="form.title" maxlength="50" />
      </el-form-item>
      <el-form-item label="副标题">
        <el-input v-model="form.subtitle" placeholder="可选" />
      </el-form-item>

      <!-- 分支抉择：引导文案 + 选项表（画布连线也可建选项，两处数据一致） -->
      <template v-if="form.nodeType === 'BRANCH'">
        <el-form-item label="引导文案">
          <el-input v-model="form.content" type="textarea" :rows="2"
            placeholder="如：你想先看哪条路线？（小程序按钮卡片上方的提示语）" />
        </el-form-item>
        <el-form-item label="选项">
          <el-table :data="form.choices" size="small" class="choice-table">
            <el-table-column label="按钮文案" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.label" maxlength="50" placeholder="如：去制胎坊" />
              </template>
            </el-table-column>
            <el-table-column label="目标节点" min-width="180">
              <template #default="{ row }">
                <el-select v-model="row.targetNodeId" filterable placeholder="选择本章节节点">
                  <el-option v-for="n in targetOptions" :key="n.id" :value="n.id"
                    :label="`#${n.sortOrder} ${n.title}（${typeLabel(n.nodeType)}）`" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ $index }">
                <el-button link type="info" :disabled="$index === 0" @click="moveChoice($index, -1)">上移</el-button>
                <el-button link type="danger" @click="form.choices.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button size="small" class="choice-add" @click="form.choices.push({ label: '', targetNodeId: null })">
            添加选项
          </el-button>
          <div class="hint">选项即小程序按钮；画布上从分支卡片把手拖线也能建选项，保存后两处一致</div>
        </el-form-item>
      </template>

      <template v-if="form.nodeType === 'TEXT'">
        <el-form-item label="插入术语">
          <el-select v-model="selectedTermId" filterable placeholder="选择术语，在光标处插入标记" style="width: 340px"
            @change="insertTerm">
            <el-option v-for="t in terms" :key="t.id" :value="t.id" :label="`[[term:${t.id}]] ${t.name}`" />
          </el-select>
          <span class="hint">标记 [[term:ID]] 在小程序中显示为可点击的术语词</span>
        </el-form-item>
        <el-form-item label="正文">
          <div class="editor-shell">
            <Toolbar class="editor-toolbar" :editor="editorRef" :default-config="toolbarConfig" />
            <Editor class="editor-content" v-model="valueHtml" :default-config="editorConfig"
              @on-created="onEditorCreated" />
          </div>
        </el-form-item>
      </template>

      <template v-if="form.nodeType === 'TIMELINE'">
        <el-form-item label="历史事件" prop="eventIds">
          <el-select v-model="form.eventIds" multiple filterable placeholder="选择已发布的历史事件（按选择顺序展示）"
            style="width: 100%">
            <el-option v-for="e in events" :key="e.id" :label="e.eventTime + ' · ' + e.title" :value="e.id" />
          </el-select>
        </el-form-item>
      </template>

      <template v-if="form.nodeType === 'QUOTE'">
        <el-form-item label="传承人" prop="inheritorId">
          <el-select v-model="form.inheritorId" filterable placeholder="选择传承人" style="width: 100%">
            <el-option v-for="p in inheritors" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="口述文本" prop="quoteText">
          <el-input v-model="form.quoteText" type="textarea" :rows="4"
            placeholder="必须是经核实并取得使用依据的真实口述，不得编造（C.6）" />
        </el-form-item>
        <el-form-item label="口述来源">
          <el-input v-model="form.quoteSource" />
        </el-form-item>
        <el-form-item label="授权说明">
          <el-input v-model="form.authorizationNote" />
        </el-form-item>
      </template>

      <template v-if="form.nodeType === 'PROCESS'">
        <el-form-item label="关联工艺" prop="craftId">
          <el-select v-model="form.craftId" filterable placeholder="选择已发布工艺" style="width: 100%" @change="loadSteps">
            <el-option v-for="c in crafts" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="引用工序">
          <el-select v-model="form.craftStepIds" multiple placeholder="不选 = 展示整工艺概览" style="width: 100%">
            <el-option v-for="s in craftSteps" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用范围" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="3"
            placeholder="发布必填：说明该工序对应的工艺与器物（6.13）" />
        </el-form-item>
        <el-form-item label="配图">
          <el-upload :action="uploadConfig.action" :headers="uploadConfig.headers()" :show-file-list="false"
            accept="image/*" :on-success="onNodeImageSuccess">
            <el-button size="small" type="primary" plain>上传配图</el-button>
          </el-upload>
        </el-form-item>
      </template>

      <el-form-item label="资料来源">
        <el-input v-model="form.source" />
      </el-form-item>
    </el-form>
    <div class="editor-actions">
      <el-button type="primary" :loading="saving" @click="save">保存节点</el-button>
      <el-button @click="$emit('cancel')">取消</el-button>
    </div>
  </div>
</template>

<script setup>
/**
 * 节点撰写台（分支叙事设计稿 §11.2「抽屉 = 撰写台」）。
 * 自包含组件：五类节点表单 + wangEditor 富文本 + 选项数据加载 + 保存。
 * 由 FlowEdit 抽屉内嵌使用（后续 ChapterEdit 亦可复用）；保存后 emit('saved') 由父级刷新画布。
 * BRANCH：引导文案 + 选项表（choices 随节点整体保存，三态语义见设计稿 §2.2）。
 */
import { onBeforeUnmount, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { nodeApi, craftApi, inheritorApi, eventApi, termApi, mediaApi, uploadConfig } from '../api'

const props = defineProps({
  chapterId: { type: Number, required: true },
  /** 编辑目标：{ id, nodeType, title, subtitle, source, flowPos, nextNodeId }；null=新增 */
  node: { type: Object, default: null },
  /** nodeApi.detail 的结果（编辑回显扩展数据）；新增传 null */
  detail: { type: Object, default: null },
  /** BRANCH 选项初值（来自 flow 数据） */
  initialChoices: { type: Array, default: () => [] },
  /** 选项目标下拉（本章节全部节点） */
  nodesForTarget: { type: Array, default: () => [] },
  /** 新增时的初始类型 */
  createType: { type: String, default: 'TEXT' }
})

const emit = defineEmits(['saved', 'cancel'])

const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null, nodeType: 'TEXT', title: '', subtitle: '', content: '', source: '',
  media: [], eventIds: [], inheritorId: null, quoteText: '', quoteSource: '',
  authorizationNote: '', craftId: null, craftStepIds: [],
  choices: [],
  flowPos: null, nextNodeId: null
})

const rules = {
  nodeType: [{ required: true, message: '类型必选', trigger: 'change' }],
  title: [{ required: true, message: '标题必填', trigger: 'blur' }],
  eventIds: [{ required: true, type: 'array', min: 1, message: '至少引用一个历史事件', trigger: 'change' }],
  inheritorId: [{ required: true, message: '传承人必选', trigger: 'change' }],
  quoteText: [{ required: true, message: '口述文本必填', trigger: 'blur' }],
  craftId: [{ required: true, message: '工艺必选', trigger: 'change' }]
}

function typeLabel(t) {
  return { TEXT: '图文', TIMELINE: '时间轴', QUOTE: '人物口述', PROCESS: '工序步骤', BRANCH: '分支抉择' }[t] || t
}

// 选项目标排除自身（指向自己会成环，后端也会拦）
const targetOptions = computed(() => props.nodesForTarget.filter((n) => n.id !== form.id))

// ---- wangEditor ----
const editorRef = ref()
const valueHtml = ref('<p><br></p>')
const toolbarConfig = { excludeKeys: ['group-video', 'insertVideo', 'uploadVideo'] }
const editorConfig = {
  placeholder: '开始编写正文：直接输入文字，工具栏可加粗、插图……',
  MENU_CONF: {
    uploadImage: {
      server: uploadConfig.action,
      fieldName: 'file',
      headers: uploadConfig.headers(),
      customInsert(res, insertFn) {
        if (res && res.code === 0 && res.data && res.data.url) {
          insertFn('http://localhost:8080' + res.data.url, '', '')
        } else {
          ElMessage.error((res && res.msg) || '图片上传失败')
        }
      }
    }
  }
}
function onEditorCreated(editor) {
  editorRef.value = editor
}
onBeforeUnmount(() => {
  if (editorRef.value) editorRef.value.destroy()
})

const selectedTermId = ref(null)
const terms = ref([])
const mediaMap = {}
const events = ref([])
const inheritors = ref([])
const crafts = ref([])
const craftSteps = ref([])

async function loadOptions() {
  try { events.value = (await eventApi.page({ pageNum: 1, pageSize: 100 })).list || [] } catch (e) { }
  try { inheritors.value = (await inheritorApi.list()) || [] } catch (e) { }
  try { crafts.value = (await craftApi.list()) || [] } catch (e) { }
  try { terms.value = (await termApi.page({ pageNum: 1, pageSize: 100 })).list || [] } catch (e) { }
  try {
    const page = await mediaApi.page({ pageNum: 1, pageSize: 200 })
    ;(page.list || []).forEach((m) => { mediaMap[m.id] = m.url })
  } catch (e) { /* 媒体映射缺失仅影响历史图片块回显 */ }
}

// ---- 初始化（编辑回显 / 新增空表单） ----
async function init() {
  loading.value = true
  loadOptions()
  try {
    if (props.node && props.node.id) {
      const d = props.detail
      form.id = props.node.id
      form.nodeType = props.node.nodeType
      form.title = props.node.title
      form.subtitle = props.node.subtitle
      form.source = props.node.source
      form.flowPos = props.node.flowPos
      form.nextNodeId = props.node.nextNodeId
      form.content = d && d.node ? d.node.content || '' : ''
      if (d && d.quote) {
        form.inheritorId = d.quote.inheritorId
        form.quoteText = d.quote.quoteText
        form.quoteSource = d.quote.source
        form.authorizationNote = d.quote.authorizationNote
      }
      if (d && d.eventRefs) form.eventIds = d.eventRefs.map((r) => r.eventId)
      if (d && d.craftRefs && d.craftRefs.length) {
        form.craftId = d.craftRefs[0].craftId
        form.craftStepIds = d.craftRefs.map((r) => r.craftStepId).filter((x) => x)
        loadSteps()
      }
      if (form.nodeType === 'TEXT') {
        valueHtml.value = d && d.node && d.node.content && d.node.content.trim().startsWith('<')
          ? d.node.content : '<p><br></p>'
      }
      if (form.nodeType === 'BRANCH') {
        form.choices = (props.initialChoices || []).map((c) => ({ label: c.label, targetNodeId: c.targetNodeId }))
      }
    } else {
      form.nodeType = props.createType || 'TEXT'
      form.choices = []
    }
  } finally {
    loading.value = false
  }
}
init()

async function loadSteps() {
  craftSteps.value = []
  form.craftStepIds = []
  if (form.craftId) {
    try {
      craftSteps.value = (await craftApi.steps(form.craftId)) || []
    } catch (e) { /* 拦截器已提示 */ }
  }
}

function onNodeImageSuccess(resp) {
  if (resp && resp.code === 0 && resp.data) {
    form.media.push({ mediaId: resp.data.id, caption: '', url: resp.data.url })
  } else {
    ElMessage.error((resp && resp.msg) || '上传失败')
  }
}

function insertTerm(id) {
  if (!editorRef.value) {
    ElMessage.warning('请先等待编辑器初始化完成')
    return
  }
  editorRef.value.insertText(`[[term:${id}]]`)
  const term = terms.value.find((t) => t.id === id)
  ElMessage.success(`已插入术语标记：${term ? term.name : id}`)
  selectedTermId.value = null
}

function moveChoice(index, delta) {
  const target = index + delta
  const arr = form.choices
  if (target < 0 || target >= arr.length) return
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
  form.choices = [...arr]
}

async function save() {
  await formRef.value.validate()
  if (form.nodeType === 'BRANCH') {
    const empty = form.choices.find((c) => !c.label || !c.targetNodeId)
    if (form.choices.length && empty) {
      ElMessage.warning('选项的按钮文案与目标节点都要填写')
      return
    }
  }
  saving.value = true
  try {
    const isText = form.nodeType === 'TEXT'
    const payload = {
      nodeType: form.nodeType,
      title: form.title,
      subtitle: form.subtitle,
      content: isText ? valueHtml.value : form.content,
      source: form.source,
      media: isText ? [] : (form.media || []).map((m) => ({ mediaId: m.mediaId, caption: m.caption })),
      eventIds: form.eventIds,
      inheritorId: form.inheritorId,
      quoteText: form.quoteText,
      quoteSource: form.quoteSource,
      authorizationNote: form.authorizationNote,
      craftId: form.craftId,
      craftStepIds: form.craftStepIds,
      // 结构字段回传防清空；choices 仅 BRANCH 传（非分支类型不带，后端只对 BRANCH 处理）
      nextNodeId: form.nextNodeId,
      flowPos: form.flowPos,
      choices: form.nodeType === 'BRANCH' ? form.choices : undefined
    }
    if (form.id) {
      await nodeApi.update(props.chapterId, form.id, payload)
    } else {
      await nodeApi.create(props.chapterId, payload)
    }
    ElMessage.success('节点已保存')
    emit('saved', form.id || null)
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.editor-shell {
  width: 100%; border: 1px solid #e6dccd; border-radius: 8px;
  overflow: hidden; background: #fff; z-index: 1;
}
.editor-toolbar { border-bottom: 1px solid #e6dccd; }
.editor-content { height: 260px; overflow-y: auto; }
:deep(.w-e-drop-panel),
:deep(.w-e-modal),
:deep(.w-e-select-list) { z-index: 4000 !important; }
.choice-table { width: 100%; }
.choice-add { margin-top: 8px; }
.editor-actions { padding-left: 96px; }
.hint { margin-left: 10px; font-size: 12px; color: #a08a70; }
</style>
