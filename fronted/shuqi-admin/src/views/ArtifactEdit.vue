<template>
  <div>
    <el-row :gutter="20">
      <!-- ============ 左栏：编辑区 ============ -->
      <el-col :span="15">
        <el-card shadow="never" class="block">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>{{ artifactId ? '编辑藏品' : '新增藏品' }}</span>
              <div style="display: flex; gap: 10px">
                <el-button size="small" @click="$router.push('/artifacts')">返回列表</el-button>
                <el-button size="small" @click="recordsVisible = true">发布记录</el-button>
                <el-button size="small" type="info" plain @click="openRelations">关联管理</el-button>
                <el-button size="small" type="warning" @click="publishDialog = true" v-if="artifactId">发布藏品</el-button>
              </div>
            </div>
          </template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="名称" prop="name">
                  <el-input v-model="form.name" maxlength="50" />
                </el-form-item>
                <el-form-item label="类别" prop="categoryId">
                  <el-select v-model="form.categoryId" placeholder="选择类别（字典维护）" style="width: 100%">
                    <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="年代">
                  <el-input v-model="form.era" placeholder="如：汉代" />
                </el-form-item>
                <el-form-item label="材质">
                  <el-input v-model="form.material" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="主展示图" prop="mainImageUrl">
                  <el-upload :action="uploadConfig.action" :headers="uploadConfig.headers()" :show-file-list="false"
                    accept="image/*" :on-success="onUploadSuccess" :on-error="() => ElMessage.error('上传失败')">
                    <el-button type="primary" plain>上传图片</el-button>
                  </el-upload>
                  <el-image v-if="form.mainImageUrl" :src="fullUrl(form.mainImageUrl)" fit="cover"
                    style="width: 120px; height: 90px; margin-left: 12px; border-radius: 8px" />
                </el-form-item>
                <el-form-item label="资料来源" prop="source">
                  <el-input v-model="form.source" placeholder="必填：可核验来源" />
                </el-form-item>
                <el-form-item label="授权说明">
                  <el-input v-model="form.authorizationNote" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="工艺说明">
              <RichEditor v-model="form.craftDesc" height="170px" />
            </el-form-item>
            <el-form-item label="文化故事">
              <RichEditor v-model="form.story" height="170px" />
            </el-form-item>
            <el-form-item label="纹饰寓意">
              <RichEditor v-model="form.motifMeaning" height="150px" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="save">
              {{ artifactId ? '保存藏品' : '创建藏品' }}
            </el-button>
            <el-button type="success" plain v-if="artifactId" @click="audit">审核</el-button>
            <span class="hint" v-if="!artifactId">创建后可维护关联，并在右侧预览效果</span>
          </el-form>
        </el-card>
      </el-col>

      <!-- ============ 右栏：小程序实时预览 ============ -->
      <el-col :span="9">
        <PhonePreview>
          <img v-if="form.mainImageUrl" :src="fullUrl(form.mainImageUrl)" class="pv-main-img" />
          <div v-else class="pv-main-img pv-main-fallback"><span>藏</span></div>
          <div class="pv-head">
            <span class="pv-name">{{ form.name || '藏品名称' }}</span>
            <div class="pv-chips">
              <span class="pv-chip" v-if="categoryName">{{ categoryName }}</span>
              <span class="pv-chip" v-if="form.era">{{ form.era }}</span>
              <span class="pv-chip" v-if="form.material">{{ form.material }}</span>
            </div>
            <span class="pv-fav">♡ 收藏</span>
          </div>
          <div class="pv-section" v-if="form.craftDesc">
            <span class="pv-s-title">工艺说明</span>
            <div class="pv-rich" v-html="previewTextHtml(form.craftDesc)"></div>
          </div>
          <div class="pv-section" v-if="form.story">
            <span class="pv-s-title">文化故事</span>
            <div class="pv-rich" v-html="previewTextHtml(form.story)"></div>
          </div>
          <div class="pv-section" v-if="form.motifMeaning">
            <span class="pv-s-title">纹饰寓意</span>
            <div class="pv-rich" v-html="previewTextHtml(form.motifMeaning)"></div>
          </div>
          <div class="pv-section" v-if="relationDraft.length">
            <span class="pv-s-title">相关内容</span>
            <div class="pv-rel" v-for="r in relationDraft" :key="r.targetType + r.targetId">
              <span class="pv-rel-thumb">{{ typeChar(r.targetType) }}</span>
              <span class="pv-rel-name">{{ relationLabel(r.targetType) }} #{{ r.targetId }}</span>
            </div>
          </div>
          <div class="pv-source" v-if="form.source">资料来源：{{ form.source }}</div>
        </PhonePreview>
      </el-col>
    </el-row>

    <!-- 关联管理 -->
    <el-dialog v-model="relationsVisible" title="关联管理（藏品 → 章节/工艺/传承人）" width="600px">
      <el-table :data="relationDraft" size="small" max-height="240">
        <el-table-column label="目标类型" width="110">
          <template #default="{ row }">{{ relationLabel(row.targetType) }}</template>
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
          <el-option label="章节" value="chapter" />
          <el-option label="工艺" value="craft" />
          <el-option label="传承人" value="inheritor" />
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

    <!-- 发布 -->
    <el-dialog v-model="publishDialog" title="发布藏品" width="480px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px"
        title="发布校验：必填字段、资料来源、审核状态、有效关联（目标须已发布）" />
      <el-input v-model="publishSummary" type="textarea" :rows="3" placeholder="修改内容说明（必填，将记入发布记录）" />
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
        <el-table-column prop="failReason" label="失败原因" min-width="120" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { artifactApi, categoryApi, chapterApi, craftApi, inheritorApi, uploadConfig } from '../api'
  import { fullUrl } from '../api/http'
  import RichEditor from '../components/RichEditor.vue'
  import PhonePreview from '../components/PhonePreview.vue'
  import { previewTextHtml } from '../utils/preview-shared'

  const route = useRoute()
  const router = useRouter()
  const artifactId = route.params.id ? Number(route.params.id) : null

  const form = reactive({
    id: null, name: '', categoryId: null, era: '', material: '',
    craftDesc: '', story: '', motifMeaning: '', mainImageUrl: '', source: '', authorizationNote: ''
  })
  const formRef = ref()
  const categories = ref([])
  const chapters = ref([])
  const crafts = ref([])
  const inheritors = ref([])
  const saving = ref(false)
  const publishing = ref(false)

  const relationDraft = ref([])
  const newRelation = reactive({ targetType: 'chapter', targetId: null })
  const relationsVisible = ref(false)

  const publishDialog = ref(false)
  const publishSummary = ref('')
  const recordsVisible = ref(false)
  const records = ref([])

  const rules = {
    name: [{ required: true, message: '名称必填', trigger: 'blur' }],
    categoryId: [{ required: true, message: '类别必选', trigger: 'change' }],
    mainImageUrl: [{ required: true, message: '主展示图必传', trigger: 'change' }],
    source: [{ required: true, message: '资料来源必填', trigger: 'blur' }]
  }

  const categoryName = computed(() => {
    const c = categories.value.find((x) => x.id === form.categoryId)
    return c ? c.name : ''
  })

  const targetOptions = computed(() => {
    if (newRelation.targetType === 'chapter') {
      return chapters.value.map((c) => ({ value: c.id, label: c.title }))
    }
    if (newRelation.targetType === 'craft') {
      return crafts.value.map((c) => ({ value: c.id, label: c.name }))
    }
    return inheritors.value.map((p) => ({ value: p.id, label: p.name }))
  })

  function relationLabel(t) {
    return { chapter: '关联章节', craft: '相关工艺', inheritor: '相关人物' }[t] || '关联内容'
  }
  function typeChar(t) {
    return { chapter: '章', craft: '工', inheritor: '匠' }[t] || '·'
  }
  function targetName(type, id) {
    if (type === 'chapter') {
      const c = chapters.value.find((x) => x.id === id)
      return c ? c.title : '#' + id
    }
    if (type === 'craft') {
      const c = crafts.value.find((x) => x.id === id)
      return c ? c.name : '#' + id
    }
    const p = inheritors.value.find((x) => x.id === id)
    return p ? p.name : '#' + id
  }
  function recordStatus(s) {
    return { 0: '发布中', 1: '发布成功', 2: '发布失败', 3: '回滚成功', 4: '回滚失败' }[s] || s
  }

  async function load() {
    try { categories.value = (await categoryApi.list()) || [] } catch (e) { }
    try { chapters.value = (await chapterApi.page({ pageNum: 1, pageSize: 100 })).list || [] } catch (e) { }
    try { crafts.value = (await craftApi.list()) || [] } catch (e) { }
    try { inheritors.value = (await inheritorApi.list()) || [] } catch (e) { }
    if (!artifactId) return
    try {
      const detail = await artifactApi.detail(artifactId)
      Object.assign(form, detail.artifact)
      relationDraft.value = (detail.relations || []).map((r) => ({
        targetType: r.targetType, targetId: r.targetId
      }))
    } catch (e) { /* 拦截器已提示 */ }
  }

  function onUploadSuccess(resp) {
    if (resp && resp.code === 0 && resp.data && resp.data.url) {
      form.mainImageUrl = resp.data.url
      formRef.value?.validateField('mainImageUrl')
    } else {
      ElMessage.error((resp && resp.msg) || '上传失败')
    }
  }

  async function save() {
    await formRef.value.validate()
    saving.value = true
    try {
      if (artifactId) {
        await artifactApi.update(artifactId, { ...form })
        ElMessage.success('藏品已保存')
      } else {
        const id = await artifactApi.create({ ...form })
        ElMessage.success('藏品已创建，可维护关联并在右侧预览')
        router.replace('/artifacts/' + id + '/edit')
        return
      }
    } catch (e) {
      return
    } finally {
      saving.value = false
    }
  }

  function audit() {
    if (!artifactId) return
    ElMessageBox.confirm('审核操作：通过后才能发布（D-20）。确认为「已审核」？', '简单自审', { type: 'warning' })
      .then(async () => {
        await artifactApi.audit(artifactId, true)
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
      await artifactApi.setRelations(artifactId, relationDraft.value)
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
      await artifactApi.publish(artifactId, publishSummary.value.trim())
      ElMessage.success('发布成功')
      publishDialog.value = false
    } catch (e) { /* 校验失败已由拦截器提示 */ } finally {
      publishing.value = false
    }
  }

  onMounted(() => { load() })
</script>

<style scoped>
  /* .block / .hint 统一在 styles/admin.scss */
  .hint { margin-left: 14px; }
  .pv-main-img { width: 100%; height: 300px; object-fit: cover; display: block; }
  .pv-main-fallback {
    display: flex; align-items: center; justify-content: center;
    background: radial-gradient(ellipse at 70% 30%, #a75c42, #351b1b); color: #e8c99a; font-size: 60px;
  }
  .pv-head { padding: 24rpx 30rpx; position: relative; }
  .pv-name { font-size: 20px; font-weight: 600; color: #28231f; padding-right: 90px; }
  .pv-chips { display: flex; gap: 6px; margin-top: 8px; flex-wrap: wrap; }
  .pv-chip { font-size: 10px; background: rgba(118, 45, 37, .08); color: #762d25; border-radius: 12px; padding: 2px 10px; }
  .pv-fav {
    position: absolute; right: 14px; top: 16px; border: 1px solid #ddd0be; border-radius: 14px;
    padding: 4px 12px; font-size: 11px; color: #81776d;
  }
  /* pv-section / pv-s-title / pv-rel / pv-source 统一在 styles/admin.scss */
</style>
