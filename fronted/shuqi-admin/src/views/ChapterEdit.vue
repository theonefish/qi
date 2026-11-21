<template>
  <div class="edit-page" v-if="chapter">
    <el-row :gutter="20">
      <!-- ============ 左栏：编辑区 ============ -->
      <el-col :span="15">
        <!-- 章节信息（nodeMode = chapter） -->
        <el-card shadow="never" class="block" v-if="nodeMode === 'chapter'">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>章节信息</span>
              <div style="display: flex; gap: 10px">
                <el-button size="small" @click="$router.push('/chapters')">返回列表</el-button>
                <el-button size="small" type="primary" plain @click="$router.push(`/chapters/${chapterId}/flow`)">分支编排</el-button>
                <el-button size="small" @click="recordsVisible = true">发布记录</el-button>
                <el-button size="small" type="info" plain @click="openRelations">关联管理</el-button>
                <el-button size="small" type="warning" @click="publishDialog = true">发布章节</el-button>
              </div>
            </div>
          </template>
          <el-form :model="chapterForm" label-width="90px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="标题">
                  <el-input v-model="chapterForm.title" />
                </el-form-item>
                <el-form-item label="章节类型">
                  <el-radio-group v-model="chapterForm.chapterType">
                    <el-radio :value="0">序章</el-radio>
                    <el-radio :value="1">正章</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="排序值">
                  <el-input-number v-model="chapterForm.sortOrder" :min="1" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="封面图">
                  <el-upload :action="uploadConfig.action" :headers="uploadConfig.headers()" :show-file-list="false"
                    accept="image/*" :on-success="onCoverSuccess">
                    <el-button size="small" type="primary" plain>上传封面</el-button>
                  </el-upload>
                  <el-image v-if="chapterForm.coverUrl" :src="fullUrl(chapterForm.coverUrl)" fit="cover"
                    style="width: 140px; height: 84px; margin-left: 12px; border-radius: 8px" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="副标题">
              <el-input v-model="chapterForm.subtitle" />
            </el-form-item>
            <el-form-item label="摘要">
              <el-input v-model="chapterForm.summary" type="textarea" :rows="3" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="saveChapter">保存章节</el-button>
          </el-form>
        </el-card>

        <!-- 节点编辑器（nodeMode = node，内联而非弹窗，右侧预览实时联动） -->
        <el-card shadow="never" class="block" v-if="nodeMode === 'node'">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>{{ nodeForm.id ? '编辑节点' : '新增节点' }}（右侧预览实时更新）</span>
              <el-button size="small" @click="backToChapterMode">返回章节信息</el-button>
            </div>
          </template>
          <el-form :model="nodeForm" :rules="nodeRules" ref="nodeFormRef" label-width="100px">
            <el-form-item label="节点类型" prop="nodeType">
              <el-radio-group v-model="nodeForm.nodeType">
                <el-radio-button value="TEXT">图文</el-radio-button>
                <el-radio-button value="TIMELINE">时间轴</el-radio-button>
                <el-radio-button value="QUOTE">人物口述</el-radio-button>
                <el-radio-button value="PROCESS">工序步骤</el-radio-button>
                <el-radio-button value="BRANCH">分支抉择</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="节点标题" prop="title">
              <el-input v-model="nodeForm.title" maxlength="50" />
            </el-form-item>
            <el-form-item label="副标题">
              <el-input v-model="nodeForm.subtitle" placeholder="可选" />
            </el-form-item>

            <template v-if="nodeForm.nodeType === 'BRANCH'">
              <el-form-item label="引导文案">
                <el-input v-model="nodeForm.content" type="textarea" :rows="2"
                  placeholder="如：你想先看哪条路线？（小程序按钮卡片上方提示语）" />
              </el-form-item>
              <el-form-item label="选项">
                <span class="hint">选项与连线请在「分支编排」页维护（保存此处不会改动已配置的选项）</span>
              </el-form-item>
            </template>

            <template v-if="nodeForm.nodeType === 'TEXT'">
              <el-form-item label="插入术语">
                <el-select v-model="selectedTermId" filterable placeholder="选择术语，在光标处插入标记"
                  style="width: 340px" @change="insertTerm">
                  <el-option v-for="t in termsForInsert" :key="t.id" :value="t.id"
                    :label="`[[term:${t.id}]] ${t.name}`" />
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

            <template v-if="nodeForm.nodeType === 'TIMELINE'">
              <el-form-item label="历史事件" prop="eventIds">
                <el-select v-model="nodeForm.eventIds" multiple filterable
                  placeholder="选择已发布的历史事件（按选择顺序展示）" style="width: 100%">
                  <el-option v-for="e in events" :key="e.id" :label="e.eventTime + ' · ' + e.title" :value="e.id" />
                </el-select>
              </el-form-item>
            </template>

            <template v-if="nodeForm.nodeType === 'QUOTE'">
              <el-form-item label="传承人" prop="inheritorId">
                <el-select v-model="nodeForm.inheritorId" filterable placeholder="选择传承人" style="width: 100%">
                  <el-option v-for="p in inheritors" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="口述文本" prop="quoteText">
                <el-input v-model="nodeForm.quoteText" type="textarea" :rows="4"
                  placeholder="必须是经核实并取得使用依据的真实口述，不得编造（C.6）" />
              </el-form-item>
              <el-form-item label="口述来源">
                <el-input v-model="nodeForm.quoteSource" />
              </el-form-item>
              <el-form-item label="授权说明">
                <el-input v-model="nodeForm.authorizationNote" />
              </el-form-item>
            </template>

            <template v-if="nodeForm.nodeType === 'PROCESS'">
              <el-form-item label="关联工艺" prop="craftId">
                <el-select v-model="nodeForm.craftId" filterable placeholder="选择已发布工艺" style="width: 100%"
                  @change="loadSteps">
                  <el-option v-for="c in crafts" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="引用工序">
                <el-select v-model="nodeForm.craftStepIds" multiple placeholder="不选 = 展示整工艺概览"
                  style="width: 100%">
                  <el-option v-for="s in craftSteps" :key="s.id" :label="s.name" :value="s.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="适用范围" prop="content">
                <el-input v-model="nodeForm.content" type="textarea" :rows="3"
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
              <el-input v-model="nodeForm.source" />
            </el-form-item>
          </el-form>
          <el-button type="primary" :loading="saving" @click="saveNode">保存节点</el-button>
          <el-button @click="backToChapterMode">取消</el-button>
        </el-card>

        <!-- 节点列表 -->
        <el-card shadow="never" class="block" v-if="nodeMode === 'chapter'">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>叙事节点（{{ nodes.length }} 个，按顺序在小程序中自上而下展示）</span>
              <el-button size="small" type="danger" plain @click="openNodeDialog(null)">新增节点</el-button>
            </div>
          </template>
          <el-table :data="nodes" size="small">
            <el-table-column prop="sortOrder" label="顺序" width="70" />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">
                <el-tag size="small">{{ typeLabel(row.nodeType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column label="操作" width="290" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openNodeDialog(row)">编辑</el-button>
                <el-button link type="warning" v-if="row.nodeType === 'BRANCH'"
                  @click="$router.push(`/chapters/${chapterId}/flow`)">编排</el-button>
                <el-button link type="info" @click="moveNode(row, 'up')">上移</el-button>
                <el-button link type="info" @click="moveNode(row, 'down')">下移</el-button>
                <el-button link type="danger" @click="removeNode(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- ============ 右栏：小程序实时预览 ============ -->
      <el-col :span="9">
        <PhonePreview>
                <!-- 主视觉 -->
                <div class="pv-hero">
                  <img v-if="chapterForm.coverUrl" :src="fullUrl(chapterForm.coverUrl)" class="pv-hero-img" />
                  <div class="pv-hero-fallback" v-else><span class="pv-hero-no">{{ pad(chapterForm.sortOrder) }}</span></div>
                  <div class="pv-hero-copy">
                    <span class="pv-chapter-no">CHAPTER {{ pad(chapterForm.sortOrder) }}</span>
                    <span class="pv-hero-title">{{ chapterForm.title || '章节标题' }}</span>
                    <span class="pv-hero-sub">{{ chapterForm.subtitle || '' }}</span>
                  </div>
                </div>
                <!-- 摘要 -->
                <div class="pv-intro" v-if="chapterForm.summary">
                  <span class="pv-kicker">THE STORY BEGINS</span>
                  <p class="pv-intro-text">{{ chapterForm.summary }}</p>
                </div>
                <!-- 节点 -->
                <div class="pv-section" v-for="(n, idx) in previewNodes" :key="n.id"
                  :class="{ 'pv-editing': editingKey === String(n.id) }">
                  <div class="pv-sec-head">
                    <span class="pv-sec-index">{{ pad(idx + 1) }}</span>
                    <span class="pv-sec-title">{{ n.title || '未命名节点' }}</span>
                    <span class="pv-sec-type">{{ typeLabel(n.nodeType) }}</span>
                  </div>

                  <template v-if="n.nodeType === 'TEXT'">
                    <div class="pv-rich" v-html="n.previewHtml || previewHtmlOf(n)"></div>
                  </template>

                  <template v-else-if="n.nodeType === 'TIMELINE'">
                    <div class="pv-timeline">
                      <div class="pv-tl-item" v-for="ev in nodeEvents(n)" :key="ev.id">
                        <span class="pv-tl-time">{{ ev.eventTime }}</span>
                        <span class="pv-tl-title">{{ ev.title }}</span>
                        <p class="pv-tl-desc" v-if="ev.description" v-html="ev.description"></p>
                      </div>
                    </div>
                  </template>

                  <template v-else-if="n.nodeType === 'QUOTE'">
                    <div class="pv-quote">
                      <span class="pv-quote-mark">“</span>
                      <p class="pv-quote-text">{{ nodeQuote(n).quoteText || '口述文本…' }}</p>
                      <div class="pv-quote-person">
                        <span class="pv-avatar">{{ (nodeQuote(n).inheritorName || '匠').slice(0, 1) }}</span>
                        <span class="pv-q-name">{{ nodeQuote(n).inheritorName || '传承人' }}</span>
                      </div>
                    </div>
                  </template>

                  <template v-else-if="n.nodeType === 'BRANCH'">
                    <div class="pv-branch">
                      <p class="pv-branch-q">{{ n.content || '分支引导文案…' }}</p>
                      <div class="pv-branch-btn">选项按钮 · 以小程序实际体验为准</div>
                    </div>
                  </template>

                  <template v-else-if="n.nodeType === 'PROCESS'">
                    <div class="pv-step" v-for="(s, si) in nodeSteps(n)" :key="s.id || si">
                      <span class="pv-step-no">{{ stepLabel(si) }}</span>
                      <span class="pv-step-name">{{ s.name }}</span>
                    </div>
                    <div class="pv-note" v-if="n.content">
                      <b>适用范围：</b>{{ n.content }}
                    </div>
                  </template>
                </div>

                <!-- 关联卡 -->
                <div class="pv-section" v-if="relationDraft.length">
                  <div class="pv-sec-head"><span class="pv-sec-title">相关内容</span></div>
                  <div class="pv-relation" v-for="r in relationDraft" :key="r.targetType + r.targetId">
                    <span class="pv-rel-thumb">{{ relationLabel(r.targetType).slice(0, 1) }}</span>
                    <span class="pv-rel-name">{{ relationLabel(r.targetType) }} #{{ r.targetId }}</span>
                  </div>
                </div>

                <!-- 章节结尾 -->
                <div class="pv-finish">
                  <div class="pv-finish-mark">✓</div>
                  <p class="pv-finish-title">阅读到这里，为本章画上句号</p>
                  <div class="pv-complete-btn">完成本章</div>
                </div>
        </PhonePreview>
      </el-col>
    </el-row>

    <!-- 关联管理 -->
    <el-dialog v-model="relationsVisible" title="关联管理（章节 → 藏品/工艺/传承人）" width="560px">
      <el-table :data="relationDraft" size="small" max-height="240">
        <el-table-column label="目标类型" width="120">
          <template #default="{ row }">{{ relationLabel(row.targetType) }}</template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标 ID" />
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" @click="relationDraft.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; gap: 10px; margin-top: 14px">
        <el-select v-model="newRelation.targetType" style="width: 160px" placeholder="目标类型">
          <el-option label="藏品" value="artifact" />
          <el-option label="工艺" value="craft" />
          <el-option label="传承人" value="inheritor" />
        </el-select>
        <el-input-number v-model="newRelation.targetId" :min="1" placeholder="目标 ID" style="width: 140px" />
        <el-button @click="relationDraft.push({ ...newRelation })">添加</el-button>
      </div>
      <template #footer>
        <el-button @click="relationsVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRelations">保存关联</el-button>
      </template>
    </el-dialog>

    <!-- 发布 -->
    <el-dialog v-model="publishDialog" title="发布章节" width="480px">
      <el-input v-model="publishSummary" type="textarea" :rows="3" placeholder="修改内容说明（必填）" />
      <template #footer>
        <el-button @click="publishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="doPublish">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- 发布记录 -->
    <el-dialog v-model="recordsVisible" title="发布记录（仅查看）" width="720px">
      <el-table :data="records" size="small" max-height="360">
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="modifySummary" label="修改内容" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ recordStatus(row.publishStatus) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作管理员" width="110" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
  import { computed, onMounted, onBeforeUnmount, reactive, ref, shallowRef } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
  import '@wangeditor/editor/dist/css/style.css'
  import { chapterApi, nodeApi, craftApi, inheritorApi, eventApi, termApi, mediaApi, uploadConfig } from '../api'
  import { fullUrl } from '../api/http'
  import { splitRichByTerms } from '../utils/preview-shared.js'
  import PhonePreview from '../components/PhonePreview.vue'

  const route = useRoute()
  const router = useRouter()
  const chapterId = Number(route.params.id)

  const chapter = ref(null)
  const chapterForm = reactive({ id: null, title: '', subtitle: '', coverUrl: '', summary: '', chapterType: 1, sortOrder: null })
  const nodes = ref([])
  const nodeDetails = {}
  const saving = ref(false)
  const publishing = ref(false)

  const nodeMode = ref('chapter')
  const nodeForm = reactive({
    id: null, nodeType: 'TEXT', title: '', subtitle: '', content: '', source: '',
    media: [], eventIds: [], inheritorId: null, quoteText: '', quoteSource: '',
    authorizationNote: '', craftId: null, craftStepIds: []
  })

  const editorRef = shallowRef()
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
    const editor = editorRef.value
    if (editor) editor.destroy()
  })

  const selectedTermId = ref(null)
  const termsForInsert = ref([])
  const mediaMap = {}

  const events = ref([])
  const inheritors = ref([])
  const crafts = ref([])
  const craftSteps = ref([])

  const relationsVisible = ref(false)
  const relationDraft = ref([])
  const newRelation = reactive({ targetType: 'artifact', targetId: null })
  const recordsVisible = ref(false)
  const records = ref([])
  const publishDialog = ref(false)
  const publishSummary = ref('')

  const nodeRules = {
    nodeType: [{ required: true, message: '类型必选', trigger: 'change' }],
    title: [{ required: true, message: '标题必填', trigger: 'blur' }],
    eventIds: [{ required: true, type: 'array', min: 1, message: '至少引用一个历史事件', trigger: 'change' }],
    inheritorId: [{ required: true, message: '传承人必选', trigger: 'change' }],
    quoteText: [{ required: true, message: '口述文本必填', trigger: 'blur' }],
    craftId: [{ required: true, message: '工艺必选', trigger: 'change' }]
  }

  function pad(n) {
    return String(n || 0).padStart(2, '0')
  }
  function typeLabel(t) {
    return { TEXT: '图文', TIMELINE: '时间轴', QUOTE: '人物口述', PROCESS: '工序步骤', BRANCH: '分支抉择' }[t] || t
  }
  function relationLabel(t) {
    return { artifact: '藏品', craft: '工艺', inheritor: '传承人' }[t] || t
  }
  function recordStatus(s) {
    return { 0: '发布中', 1: '发布成功', 2: '发布失败', 3: '回滚成功', 4: '回滚失败' }[s] || s
  }
  function stepLabel(i) {
    return String.fromCharCode(65 + (i % 26))
  }

  async function load() {
    const detail = await chapterApi.detail(chapterId)
    chapter.value = detail.chapter
    Object.assign(chapterForm, detail.chapter)
    nodes.value = detail.nodes || []
    relationDraft.value = (detail.relations || []).map((r) => ({ targetType: r.targetType, targetId: r.targetId }))
    // 拉取每个节点的扩展数据（术语/事件/口述/工序），供预览使用
    for (const n of nodes.value) {
      try {
        nodeDetails[n.id] = await nodeApi.detail(chapterId, n.id)
      } catch (e) {
        nodeDetails[n.id] = null
      }
    }
  }

  async function loadOptions() {
    try { events.value = (await eventApi.page({ pageNum: 1, pageSize: 100 })).list || [] } catch (e) { }
    try { inheritors.value = (await inheritorApi.list()) || [] } catch (e) { }
    try { crafts.value = (await craftApi.list()) || [] } catch (e) { }
    try { termsForInsert.value = (await termApi.page({ pageNum: 1, pageSize: 100 })).list || [] } catch (e) { }
    try {
      const mediaPage = await mediaApi.page({ pageNum: 1, pageSize: 200 })
      ;(mediaPage.list || []).forEach((m) => { mediaMap[m.id] = m.url })
    } catch (e) { /* 媒体映射缺失仅影响历史图片块回显 */ }
  }

  function onCoverSuccess(resp) {
    if (resp && resp.code === 0 && resp.data) chapterForm.coverUrl = resp.data.url
  }

  async function saveChapter() {
    if (!chapterForm.title) {
      ElMessage.warning('标题必填')
      return
    }
    saving.value = true
    try {
      await chapterApi.update(chapterId, { ...chapterForm })
      ElMessage.success('章节已保存')
      load()
    } catch (e) { /* 拦截器已提示 */ } finally {
      saving.value = false
    }
  }

  // ---------------- 节点编辑（内联模式） ----------------

  function openNodeDialog(row) {
    Object.assign(nodeForm, {
      id: null, nodeType: 'TEXT', title: '', subtitle: '', content: '', source: '',
      media: [], eventIds: [], inheritorId: null, quoteText: '', quoteSource: '',
      authorizationNote: '', craftId: null, craftStepIds: []
    })
    valueHtml.value = '<p><br></p>'
    craftSteps.value = []
    if (row) {
      nodeMode.value = 'node'
      const d = nodeDetails[row.id]
      nodeForm.id = row.id
      nodeForm.nodeType = row.nodeType
      nodeForm.title = row.title
      nodeForm.subtitle = row.subtitle
      nodeForm.source = row.source
      if (d && d.node) Object.assign(nodeForm, { content: d.node.content || '' })
      if (d && d.quote) {
        nodeForm.inheritorId = d.quote.inheritorId
        nodeForm.quoteText = d.quote.quoteText
        nodeForm.quoteSource = d.quote.source
        nodeForm.authorizationNote = d.quote.authorizationNote
      }
      if (d && d.eventRefs) nodeForm.eventIds = d.eventRefs.map((r) => r.eventId)
      if (d && d.craftRefs && d.craftRefs.length) {
        nodeForm.craftId = d.craftRefs[0].craftId
        nodeForm.craftStepIds = d.craftRefs.map((r) => r.craftStepId).filter((x) => x)
        loadSteps()
      }
      if (nodeForm.nodeType === 'TEXT') {
        valueHtml.value = contentToHtml(
          d && d.node ? d.node.content : row.content,
          d ? d.mediaRefs : []
        )
      }
    } else {
      nodeMode.value = 'node'
    }
    // 载入后编辑器在 dialog-less 卡片中创建，稍后 onCreated 触发
  }

  function backToChapterMode() {
    nodeMode.value = 'chapter'
  }

  async function loadSteps() {
    craftSteps.value = []
    nodeForm.craftStepIds = []
    if (nodeForm.craftId) {
      try {
        craftSteps.value = (await craftApi.steps(nodeForm.craftId)) || []
      } catch (e) { /* 拦截器已提示 */ }
    }
  }

  function onNodeImageSuccess(resp) {
    if (resp && resp.code === 0 && resp.data) {
      nodeForm.media.push({ mediaId: resp.data.id, caption: '', url: resp.data.url })
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
    const term = termsForInsert.value.find((t) => t.id === id)
    ElMessage.success(`已插入术语标记：${term ? term.name : id}`)
    selectedTermId.value = null
  }

  async function saveNode() {
    await nodeFormRef.value.validate()
    saving.value = true
    try {
      const isText = nodeForm.nodeType === 'TEXT'
      const payload = {
        nodeType: nodeForm.nodeType,
        title: nodeForm.title,
        subtitle: nodeForm.subtitle,
        content: isText ? valueHtml.value : nodeForm.content,
        source: nodeForm.source,
        media: isText ? [] : (nodeForm.media || []).map((m) => ({ mediaId: m.mediaId, caption: m.caption })),
        eventIds: nodeForm.eventIds,
        inheritorId: nodeForm.inheritorId,
        quoteText: nodeForm.quoteText,
        quoteSource: nodeForm.quoteSource,
        authorizationNote: nodeForm.authorizationNote,
        craftId: nodeForm.craftId,
        craftStepIds: nodeForm.craftStepIds
      }
      if (nodeForm.id) {
        await nodeApi.update(chapterId, nodeForm.id, payload)
      } else {
        await nodeApi.create(chapterId, payload)
      }
      ElMessage.success('节点已保存')
      nodeMode.value = 'chapter'
      await load()
    } catch (e) {
      /* 拦截器已提示 */
    } finally {
      saving.value = false
    }
  }

  async function moveNode(row, direction) {
    await nodeApi.move(chapterId, row.id, direction)
    load()
  }

  async function removeNode(row) {
    await ElMessageBox.confirm(`删除节点「${row.title}」？`, '删除', { type: 'warning' })
    await nodeApi.remove(chapterId, row.id)
    ElMessage.success('已删除')
    load()
  }

  // ---------------- 关联 / 发布 / 记录 ----------------

  function openRelations() {
    relationsVisible.value = true
  }

  async function saveRelations() {
    saving.value = true
    try {
      await chapterApi.setRelations(chapterId, relationDraft.value)
      ElMessage.success('关联已保存')
      relationsVisible.value = false
    } catch (e) { /* 拦截器已提示 */ } finally {
      saving.value = false
    }
  }

  async function doPublish() {
    if (!publishSummary.value.trim()) {
      ElMessage.warning('修改内容说明必填（7.14）')
      return
    }
    publishing.value = true
    try {
      await chapterApi.publish(chapterId, publishSummary.value.trim())
      ElMessage.success('发布成功')
      publishDialog.value = false
      load()
    } catch (e) { /* 拦截器已提示 */ } finally {
      publishing.value = false
    }
  }

  // ---------------- 预览数据（实时联动） ----------------

  const editingKey = computed(() => (nodeMode.value === 'node' ? String(nodeForm.id || 'new') : null))

  /** 正在编辑的节点用表单实时值覆盖已保存数据；新增节点追加到末尾 */
  const previewNodes = computed(() => {
    const list = nodes.value.map((n) => {
      if (nodeMode.value === 'node' && nodeForm.id && nodeForm.id === n.id) {
        return liveNode(n)
      }
      return n
    })
    if (nodeMode.value === 'node' && !nodeForm.id) {
      list.push(liveNode({ id: 'new', sortOrder: nodes.value.length + 1 }))
    }
    return list
  })

  function liveNode(base) {
    const n = Object.assign({}, base, {
      nodeType: nodeForm.nodeType,
      title: nodeForm.title || base.title || '未命名节点',
      subtitle: nodeForm.subtitle,
      source: nodeForm.source
    })
    if (n.nodeType === 'TEXT') {
      n.previewHtml = markerToSpanHtml(valueHtml.value || contentToHtml(nodeForm.content, []))
    }
    if (n.nodeType === 'TIMELINE') {
      n.events = (nodeForm.eventIds || [])
        .map((id) => events.value.find((e) => e.id === id))
        .filter(Boolean)
        .map((e) => ({ id: e.id, eventTime: e.eventTime, title: e.title, description: e.description }))
    }
    if (n.nodeType === 'QUOTE') {
      const p = inheritors.value.find((x) => x.id === nodeForm.inheritorId)
      n.quoteText = nodeForm.quoteText
      n.inheritorName = p ? p.name : ''
      n.inheritorPhotoUrl = p ? p.photoUrl : ''
    }
    if (n.nodeType === 'PROCESS') {
      const c = crafts.value.find((x) => x.id === nodeForm.craftId)
      n.craft = c || null
      n.steps = (nodeForm.craftStepIds || [])
        .map((id) => craftSteps.value.find((s) => s.id === id))
        .filter(Boolean)
      n.content = nodeForm.content
    }
    return n
  }

  function nodeEvents(n) {
    if (n.events) return n.events
    const d = nodeDetails[n.id]
    return (d && d.eventRefs ? d.eventRefs : [])
      .map((r) => {
        const ev = events.value.find((e) => e.id === r.eventId)
        return ev ? { id: ev.id, eventTime: ev.eventTime, title: ev.title, description: ev.description } : null
      })
      .filter(Boolean)
  }

  function nodeQuote(n) {
    const d = nodeDetails[n.id]
    if (!d || !d.quote) return {}
    const p = inheritors.value.find((x) => x.id === d.quote.inheritorId)
    return {
      quoteText: d.quote.quoteText,
      inheritorName: p ? p.name : ''
    }
  }

  function nodeSteps(n) {
    const d = nodeDetails[n.id]
    const refs = d && d.craftRefs ? d.craftRefs : []
    return refs
      .map((r) => r.craftStepId)
      .filter(Boolean)
      .map((id) => {
        const found = craftSteps.value.find((s) => s.id === id)
        return found || { id: id, name: '工序 #' + id }
      })
  }

  /** 已保存节点 → 预览 HTML（历史块状 JSON / 纯文本自动转换，术语标记转高亮词） */
  function previewHtmlOf(node) {
    const d = nodeDetails[node.id]
    const refs = d ? d.mediaRefs : []
    return markerToSpanHtml(contentToHtml(node.content, refs))
  }

  function markerToSpanHtml(html) {
    return splitRichByTerms(html)
      .map((seg) => seg.type === 'term'
        ? `<span class="pv-term">${escapeHtml(termName(seg.id))}</span>`
        : seg.html)
      .join('')
  }

  function termName(id) {
    const t = termsForInsert.value.find((x) => x.id === id)
    return t ? t.name : '术语'
  }

  function escapeHtml(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  }

  /** 历史内容转 HTML（与小程序渲染口径一致） */
  function contentToHtml(content, mediaRefs) {
    if (!content) return ''
    const t = content.trim()
    if (t.startsWith('<')) return content
    let blocks = null
    if (t.startsWith('[')) {
      try { blocks = JSON.parse(t) } catch (e) { blocks = null }
    }
    let html = ''
    if (Array.isArray(blocks)) {
      for (const b of blocks) {
        if (b.type === 'image') {
          const url = mediaMap[b.mediaId] || ''
          html += url ? `<p><img src="${url}" style="max-width:100%;"/></p>` : ''
        } else {
          html += `<p>${escapeHtml(b.text || '')}</p>`
        }
      }
    } else {
      html = content.split(/\n+/).map((line) => `<p>${escapeHtml(line)}</p>`).join('')
      for (const r of (mediaRefs || [])) {
        const url = mediaMap[r.mediaId] || ''
        if (url) html += `<p><img src="${url}" style="max-width:100%;"/></p>`
      }
    }
    return html
  }

  onMounted(async () => {
    await load()
    loadOptions()
  })
</script>

<style scoped>
  .edit-page { }
  /* .block / .hint 统一在 styles/admin.scss */
  .editor-shell {
    width: 100%; border: 1px solid #e6dccd; border-radius: 8px;
    overflow: hidden; background: #fff; z-index: 1;
  }
  .editor-toolbar { border-bottom: 1px solid #e6dccd; }
  :deep(.w-e-drop-panel),
  :deep(.w-e-modal),
  :deep(.w-e-select-list) { z-index: 4000 !important; }

  /* 手机预览框架样式统一在 PhonePreview 组件（iPhone 12 Pro 390×844） */
  .pv-hero {
    margin: 10px 14px 0; border-radius: 16px; overflow: hidden; position: relative;
    min-height: 180px; background: radial-gradient(ellipse at 75% 28%, #a75c42 0%, #7e352d 39%, #351b1b 100%);
  }
  .pv-hero-img { position: absolute; width: 100%; height: 100%; object-fit: cover; }
  .pv-hero-fallback { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; }
  .pv-hero-no { color: rgba(232, 201, 154, .5); font-size: 48px; font-family: Georgia, serif; }
  .pv-hero-copy { position: relative; padding: 46px 20px 18px; color: #fff8ea; }
  .pv-chapter-no { display: block; font-size: 10px; letter-spacing: 2px; color: #eacb9c; font-family: Georgia, serif; }
  .pv-hero-title { display: block; font-size: 26px; letter-spacing: 4px; margin: 4px 0 6px; }
  .pv-hero-sub { display: block; font-size: 11px; color: #f1e0ce; }
  .pv-intro { padding: 20px 22px 8px; }
  .pv-kicker { font-size: 9px; color: #b58a4a; letter-spacing: 2px; display: block; margin-bottom: 6px; }
  .pv-intro-text { margin: 0; font-size: 12px; color: #655d54; line-height: 1.9; }
  .pv-section { padding: 20px 18px 16px; border-bottom: 1px solid #e6dccd; position: relative; }
  .pv-section.pv-editing { background: rgba(181, 138, 74, .08); box-shadow: inset 3px 0 0 #9c4939; }
  .pv-sec-head { display: flex; align-items: baseline; gap: 8px; margin-bottom: 10px; }
  .pv-sec-index { font-family: Georgia, serif; font-size: 18px; color: #b18c5c; }
  .pv-sec-title { font-size: 16px; font-weight: 600; color: #28231f; flex: 1; }
  .pv-sec-type { font-size: 10px; color: #b58a4a; border: 1px solid #e0d3bf; border-radius: 10px; padding: 1px 8px; }
  .pv-rich { font-size: 12px; color: #645b52; line-height: 2; }
  .pv-rich :deep(p) { margin: 0 0 10px; }
  .pv-rich :deep(img) { max-width: 100%; border-radius: 8px; display: block; margin: 8px auto; }
  .pv-term {
    color: #762d25; border-bottom: 1px dashed #9c4939; font-weight: 600;
  }
  .pv-timeline { margin-left: 8px; border-left: 1px solid #c9ad83; padding-left: 16px; }
  .pv-tl-item { position: relative; padding-bottom: 14px; }
  .pv-tl-item::before {
    content: ''; position: absolute; left: -21px; top: 5px; width: 7px; height: 7px;
    border-radius: 50%; background: #f6f1e8; border: 2px solid #a77c48;
  }
  .pv-tl-time { display: block; font-size: 11px; color: #762d25; font-weight: 600; }
  .pv-tl-title { display: block; font-size: 13px; color: #28231f; }
  .pv-tl-desc { margin: 4px 0 0; font-size: 11px; color: #6c6258; line-height: 1.8; }
  .pv-quote {
    border-radius: 12px; background: linear-gradient(135deg, #3b2924, #754337);
    padding: 16px; color: #fbf0dd;
  }
  .pv-quote-mark { font-family: Georgia, serif; font-size: 28px; color: #d0ad77; }
  .pv-quote-text { margin: 4px 0 10px; font-size: 13px; line-height: 1.9; }
  .pv-quote-person { display: flex; align-items: center; gap: 8px; border-top: 1px solid rgba(237, 213, 175, .25); padding-top: 10px; }
  .pv-avatar {
    width: 26px; height: 26px; border-radius: 50%; background: #bd9360; color: #3a2924;
    display: inline-flex; align-items: center; justify-content: center; font-size: 12px;
  }
  .pv-q-name { font-size: 11px; }
  .pv-step {
    display: flex; align-items: center; gap: 10px; border: 1px solid #e4d8c8;
    border-radius: 10px; background: #fffdf8; padding: 10px 12px; margin-bottom: 8px;
  }
  .pv-step-no {
    width: 24px; height: 24px; border-radius: 50%; background: #f1e6d7; color: #762d25;
    display: inline-flex; align-items: center; justify-content: center; font-size: 11px; font-family: Georgia, serif;
  }
  .pv-step-name { font-size: 13px; }
  .pv-branch { background: #fff7ec; border: 1px solid #ecc394; border-radius: 12px; padding: 14px; }
  .pv-branch-q { margin: 0 0 8px; font-size: 12px; color: #6b4a26; line-height: 1.8; }
  .pv-branch-btn {
    border: 1px solid #d8863c; color: #b06a1e; border-radius: 18px; padding: 6px 0;
    text-align: center; font-size: 11px;
  }
  .pv-note {
    background: #eee6da; border-radius: 10px; padding: 10px 12px;
    font-size: 11px; color: #685e52; line-height: 1.8; margin-top: 10px;
  }
  .pv-note b { color: #5a4030; }
  .pv-relation {
    display: flex; align-items: center; gap: 10px; border: 1px solid #e5d8c7;
    border-radius: 10px; background: #fffdf8; padding: 10px 12px; margin-bottom: 8px;
  }
  .pv-rel-thumb {
    width: 40px; height: 40px; border-radius: 8px; background: #eee0cb; color: #a07b4d;
    display: inline-flex; align-items: center; justify-content: center; font-size: 16px;
  }
  .pv-rel-name { font-size: 12px; color: #28231f; }
  .pv-finish { margin: 20px 14px; padding: 22px 16px; text-align: center; background: #eee5d8; border-radius: 14px; }
  .pv-finish-mark {
    width: 36px; height: 36px; border-radius: 50%; border: 1px solid #b58a4a; color: #94703e;
    display: inline-flex; align-items: center; justify-content: center; font-size: 16px;
  }
  .pv-finish-title { margin: 10px 0 6px; font-size: 16px; color: #28231f; }
  .pv-complete-btn {
    display: inline-block; margin-top: 10px; background: #762d25; color: #fff5e9;
    border-radius: 22px; padding: 8px 28px; font-size: 12px; letter-spacing: 2px;
  }
</style>
