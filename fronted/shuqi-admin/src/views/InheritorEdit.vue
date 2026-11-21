<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="15">
        <el-card shadow="never" class="block">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>{{ inheritorId ? '编辑传承人' : '新增传承人' }}</span>
              <div style="display: flex; gap: 10px">
                <el-button size="small" @click="$router.push('/inheritors')">返回列表</el-button>
                <el-button size="small" type="success" plain v-if="inheritorId" @click="audit">审核</el-button>
                <el-button size="small" type="info" plain @click="openRelations">关联管理</el-button>
                <el-button size="small" type="warning" @click="publishDialog = true" v-if="inheritorId">发布</el-button>
              </div>
            </div>
          </template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="form.name" />
                </el-form-item>
                <el-form-item label="资料来源" prop="source">
                  <el-input v-model="form.source" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="照片" prop="photoUrl">
                  <el-upload :action="uploadConfig.action" :headers="uploadConfig.headers()" :show-file-list="false"
                    accept="image/*" :on-success="onUploadSuccess">
                    <el-button type="primary" plain>上传照片</el-button>
                  </el-upload>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="简介" prop="intro">
              <RichEditor v-model="form.intro" height="150px" />
            </el-form-item>
            <el-form-item label="从艺经历">
              <RichEditor v-model="form.career" height="170px" />
            </el-form-item>
            <el-form-item label="传承故事">
              <RichEditor v-model="form.story" height="170px" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="save">
              {{ inheritorId ? '保存传承人' : '创建传承人' }}
            </el-button>
            <span class="hint" v-if="!inheritorId">创建后可维护关联（代表作品/擅长技艺），右侧预览实时更新</span>
          </el-form>
        </el-card>

        <el-card shadow="never" class="block" v-if="inheritorId">
          <template #header>关联（代表作品 → 藏品，擅长技艺 → 工艺）</template>
          <div v-if="works.length || crafts.length" style="display: flex; gap: 10px; flex-wrap: wrap">
            <el-tag v-for="w in works" :key="'w' + w.id" type="success">{{ w.name }}</el-tag>
            <el-tag v-for="c in crafts" :key="'c' + c.id">{{ c.name }}</el-tag>
          </div>
          <span v-else class="hint">暂未关联作品与技艺</span>
        </el-card>
      </el-col>

      <el-col :span="9">
        <PhonePreview>
          <div class="pv-head">
            <img v-if="form.photoUrl" :src="fullUrl(form.photoUrl)" class="pv-photo" />
            <div v-else class="pv-photo pv-photo-fallback"><span>匠</span></div>
            <div class="pv-head-info">
              <span class="pv-kicker">INHERITOR · 传承人</span>
              <span class="pv-name">{{ form.name || '传承人姓名' }}</span>
              <div class="pv-intro-rich" v-if="form.intro" v-html="previewTextHtml(form.intro, true)"></div>
            </div>
          </div>
          <div class="pv-section" v-if="form.career">
            <span class="pv-s-title">从艺经历</span>
            <div class="pv-rich" v-html="previewTextHtml(form.career)"></div>
          </div>
          <div class="pv-section" v-if="form.story">
            <span class="pv-s-title">传承故事</span>
            <div class="pv-rich" v-html="previewTextHtml(form.story)"></div>
          </div>
          <div class="pv-section" v-if="works.length">
            <span class="pv-s-title">代表作品</span>
            <div class="pv-rel" v-for="w in works" :key="w.id">
              <span class="pv-rel-thumb">藏</span><span class="pv-rel-name">{{ w.name }}</span>
            </div>
          </div>
          <div class="pv-section" v-if="crafts.length">
            <span class="pv-s-title">擅长技艺</span>
            <div class="pv-chips">
              <span class="pv-chip" v-for="c in crafts" :key="c.id">{{ c.name }}</span>
            </div>
          </div>
          <div class="pv-source" v-if="form.source">资料来源：{{ form.source }}</div>
        </PhonePreview>
      </el-col>
    </el-row>

    <el-dialog v-model="relationsVisible" title="关联管理（传承人 → 藏品/工艺）" width="600px">
      <el-table :data="relationDraft" size="small" max-height="240">
        <el-table-column label="目标类型" width="110">
          <template #default="{ row }">{{ row.targetType === 'artifact' ? '代表作品' : '擅长技艺' }}</template>
        </el-table-column>
        <el-table-column label="目标" min-width="160">
          <template #default="{ row }">{{ targetName(row.targetType, row.targetId) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" @click="relationDraft.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; gap: 10px; margin-top: 14px">
        <el-select v-model="newRelation.targetType" style="width: 130px" @change="newRelation.targetId = null">
          <el-option label="代表作品（藏品）" value="artifact" />
          <el-option label="擅长技艺（工艺）" value="craft" />
        </el-select>
        <el-select v-model="newRelation.targetId" filterable placeholder="选择目标" style="width: 240px">
          <el-option v-for="opt in targetOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-button @click="addRelation">添加</el-button>
      </div>
      <template #footer>
        <el-button @click="relationsVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRelations">保存关联</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishDialog" title="发布传承人" width="480px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px"
        title="发布校验：姓名/简介/照片/资料来源 + 审核通过（5.2 / D-20）" />
      <el-input v-model="publishSummary" type="textarea" :rows="3" placeholder="修改内容说明（必填）" />
      <template #footer>
        <el-button @click="publishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="doPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { artifactApi, craftApi, inheritorApi, relationApi, uploadConfig } from '../api'
  import { fullUrl } from '../api/http'
  import RichEditor from '../components/RichEditor.vue'
  import PhonePreview from '../components/PhonePreview.vue'
  import { previewTextHtml } from '../utils/preview-shared'

  const route = useRoute()
  const router = useRouter()
  const inheritorId = route.params.id ? Number(route.params.id) : null

  const form = reactive({ id: null, name: '', photoUrl: '', intro: '', career: '', story: '', source: '' })
  const formRef = ref()
  const saving = ref(false)
  const publishing = ref(false)
  const artifacts = ref([])

  const relationDraft = ref([])
  const newRelation = reactive({ targetType: 'artifact', targetId: null })
  const relationsVisible = ref(false)
  const publishDialog = ref(false)
  const publishSummary = ref('')

  const works = computed(() => relationDraft.value
    .filter((r) => r.targetType === 'artifact')
    .map((r) => artifacts.value.find((a) => a.id === r.targetId)).filter(Boolean))
  const crafts = computed(() => relationDraft.value
    .filter((r) => r.targetType === 'craft')
    .map((r) => craftsList.value.find((c) => c.id === r.targetId)).filter(Boolean))
  const craftsList = ref([])

  const rules = {
    name: [{ required: true, message: '姓名必填', trigger: 'blur' }],
    intro: [{ required: true, message: '简介必填（发布条件）', trigger: 'blur' }],
    photoUrl: [{ required: true, message: '照片必传（发布条件）', trigger: 'change' }],
    source: [{ required: true, message: '资料来源必填', trigger: 'blur' }]
  }

  const targetOptions = computed(() => {
    if (newRelation.targetType === 'artifact') {
      return artifacts.value.map((a) => ({ value: a.id, label: a.name }))
    }
    return craftsList.value.map((c) => ({ value: c.id, label: c.name }))
  })

  function targetName(type, id) {
    if (type === 'artifact') {
      const a = artifacts.value.find((x) => x.id === id)
      return a ? a.name : '#' + id
    }
    const c = craftsList.value.find((x) => x.id === id)
    return c ? c.name : '#' + id
  }

  async function load() {
    try {
      artifacts.value = (await artifactApi.page({ pageNum: 1, pageSize: 200 })).list || []
    } catch (e) { }
    try { craftsList.value = (await craftApi.list()) || [] } catch (e) { }
    if (!inheritorId) return
    try {
      const list = (await inheritorApi.list()) || []
      const self = list.find((p) => p.id === inheritorId)
      if (self) Object.assign(form, self)
      const relations = await relationApi.listBySource({
        sourceType: 'inheritor', sourceId: inheritorId
      })
      relationDraft.value = (relations || []).map((r) => ({
        targetType: r.targetType, targetId: r.targetId
      }))
    } catch (e) { /* 拦截器已提示 */ }
  }

  function onUploadSuccess(resp) {
    if (resp && resp.code === 0 && resp.data) {
      form.photoUrl = resp.data.url
      formRef.value?.validateField('photoUrl')
    }
  }

  async function save() {
    await formRef.value.validate()
    saving.value = true
    try {
      if (inheritorId) {
        await inheritorApi.update(inheritorId, { ...form })
        ElMessage.success('传承人已保存')
      } else {
        const id = await inheritorApi.create({ ...form })
        ElMessage.success('传承人已创建，可维护关联并在右侧预览')
        router.replace('/inheritors/' + id + '/edit')
        return
      }
    } catch (e) {
      return
    } finally {
      saving.value = false
    }
  }

  function audit() {
    ElMessageBox.confirm('审核操作：通过后才能发布（D-20）。确认为「已审核」？', '简单自审', { type: 'warning' })
      .then(async () => {
        await inheritorApi.audit(inheritorId, true)
        ElMessage.success('已更新审核状态')
      })
      .catch(() => {})
  }

  function openRelations() {
    relationsVisible.value = true
  }

  function addRelation() {
    if (!newRelation.targetId) {
      ElMessage.warning('请选择目标')
      return
    }
    const exists = relationDraft.value.some((r) =>
      r.targetType === newRelation.targetType && r.targetId === newRelation.targetId)
    if (exists) {
      ElMessage.warning('该目标已在关联列表中')
      return
    }
    relationDraft.value.push({ targetType: newRelation.targetType, targetId: newRelation.targetId })
    newRelation.targetId = null
  }

  async function saveRelations() {
    saving.value = true
    try {
      await inheritorApi.setRelations(inheritorId, relationDraft.value)
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
      await inheritorApi.publish(inheritorId, publishSummary.value.trim())
      ElMessage.success('发布成功')
      publishDialog.value = false
    } catch (e) { /* 拦截器已提示 */ } finally {
      publishing.value = false
    }
  }

  onMounted(() => { load() })
</script>

<style scoped>
  /* .block / .hint 统一在 styles/admin.scss */
  .hint { margin-left: 14px; }
  .pv-head {
    padding: 36px 30px; display: flex; gap: 20px; align-items: center; color: #fbf0dd;
    background: linear-gradient(135deg, #3b2924, #754337);
  }
  .pv-photo { width: 80px; height: 80px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
  .pv-photo-fallback {
    display: flex; align-items: center; justify-content: center;
    background: #bd9360; color: #3a2924; font-size: 32px;
  }
  .pv-head-info { flex: 1; }
  .pv-kicker { font-size: 9px; letter-spacing: 2px; color: #d9c1a0; display: block; }
  .pv-name { display: block; font-size: 24px; font-weight: 500; letter-spacing: 3px; margin: 6px 0 10px; }
  .pv-intro-rich { font-size: 12px; }
  .pv-chips { display: flex; gap: 8px; flex-wrap: wrap; }
  .pv-chip {
    border: 1px solid #9c4939; color: #762d25; border-radius: 20px;
    padding: 4px 16px; font-size: 12px; background: rgba(156, 73, 57, .05);
  }
  /* pv-section / pv-s-title / pv-rel / pv-source 统一在 styles/admin.scss */
</style>
