<template>
  <div>
    <el-row :gutter="20">
      <!-- ============ 左栏：编辑区 ============ -->
      <el-col :span="15">
        <el-card shadow="never" class="block">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>{{ craftId ? '编辑工艺' : '新增工艺' }}</span>
              <div style="display: flex; gap: 10px">
                <el-button size="small" @click="$router.push('/crafts')">返回列表</el-button>
                <el-button size="small" type="warning" v-if="craftId" @click="publishDialog = true">发布工艺</el-button>
              </div>
            </div>
          </template>
          <el-form :model="craftForm" :rules="craftRules" ref="craftFormRef" label-width="90px">
            <el-form-item label="名称" prop="name">
              <el-input v-model="craftForm.name" maxlength="60" />
            </el-form-item>
            <el-form-item label="概述" prop="overview">
              <RichEditor v-model="craftForm.overview" height="180px" />
            </el-form-item>
            <el-form-item label="特色说明">
              <RichEditor v-model="craftForm.feature" height="150px" />
            </el-form-item>
            <el-form-item label="适用范围" prop="scope">
              <RichEditor v-model="craftForm.scope" height="130px" />
            </el-form-item>
            <el-form-item label="资料来源">
              <el-input v-model="craftForm.source" placeholder="工艺资料的出处" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="saveCraft">
              {{ craftId ? '保存工艺' : '创建工艺' }}
            </el-button>
            <span class="hint" v-if="!craftId">创建后即可在下方维护工序，并在右侧预览效果</span>
          </el-form>
        </el-card>

        <!-- 工序管理（创建后可用） -->
        <el-card shadow="never" class="block" v-if="craftId">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>工序流程（{{ steps.length }} 道，按顺序在小程序中展示）</span>
              <el-button size="small" type="danger" plain @click="openStepDialog(null)">新增工序</el-button>
            </div>
          </template>
          <el-table :data="steps" size="small">
            <el-table-column prop="sortOrder" label="顺序" width="70" />
            <el-table-column prop="name" label="工序名称" width="150" />
            <el-table-column label="简介" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ plainText(row.intro) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openStepDialog(row)">编辑</el-button>
                <el-button link type="info" @click="moveStep(row, 'up')">上移</el-button>
                <el-button link type="info" @click="moveStep(row, 'down')">下移</el-button>
                <el-button link type="danger" @click="removeStep(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 关联管理 -->
        <el-card shadow="never" class="block" v-if="craftId">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>关联藏品（小程序工艺详情页展示）</span>
              <el-button size="small" type="info" plain @click="relationsVisible = true">管理关联</el-button>
            </div>
          </template>
          <div v-if="relationArtifacts.length" style="display: flex; gap: 10px; flex-wrap: wrap">
            <el-tag v-for="a in relationArtifacts" :key="a.id">{{ a.name }}</el-tag>
          </div>
          <span v-else class="hint">暂未关联藏品</span>
        </el-card>
      </el-col>

      <!-- ============ 右栏：小程序实时预览 ============ -->
      <el-col :span="9">
        <PhonePreview>
                <div class="pv-head">
                  <span class="pv-kicker">CRAFT · 工艺档案</span>
                  <span class="pv-name">{{ craftForm.name || '工艺名称' }}</span>
                  <div class="pv-head-rich" v-if="craftForm.overview" v-html="richPreview(craftForm.overview)"></div>
                  <div class="pv-head-rich" v-if="craftForm.feature" v-html="richPreview(craftForm.feature)"></div>
                </div>

                <div class="pv-note" v-if="craftForm.scope">
                  <b>适用范围：</b>
                  <div v-html="richLight(craftForm.scope)"></div>
                </div>

                <div class="pv-section" v-if="steps.length">
                  <div class="pv-sec-title">工序流程</div>
                  <div class="pv-step" v-for="(s, idx) in steps" :key="s.id || idx">
                    <span class="pv-step-no">{{ idx + 1 }}</span>
                    <span class="pv-step-name">{{ s.name }}</span>
                  </div>
                </div>

                <div class="pv-section" v-if="relationArtifacts.length">
                  <div class="pv-sec-title">关联藏品</div>
                  <div class="pv-relation" v-for="a in relationArtifacts" :key="a.id">
                    <img v-if="a.mainImageUrl" :src="fullUrl(a.mainImageUrl)" class="pv-rel-img" />
                    <span v-else class="pv-rel-img pv-rel-fallback">藏</span>
                    <span class="pv-rel-name">{{ a.name }}</span>
                  </div>
                </div>

                <div class="pv-finish">
                  <div class="pv-complete-btn">查看关联藏品</div>
                </div>
        </PhonePreview>
      </el-col>
    </el-row>

    <!-- 工序表单 -->
    <el-dialog v-model="stepDialogVisible" :title="stepForm.id ? '编辑工序' : '新增工序'" width="480px" destroy-on-close
      append-to-body>
      <el-form :model="stepForm" :rules="stepRules" ref="stepFormRef" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="stepForm.name" />
        </el-form-item>
        <el-form-item label="简介" prop="intro">
          <RichEditor v-model="stepForm.intro" height="140px" />
        </el-form-item>
        <el-form-item label="配图">
          <el-upload :action="uploadConfig.action" :headers="uploadConfig.headers()" :show-file-list="false"
            accept="image/*" :on-success="onStepImageSuccess">
            <el-button size="small" type="primary" plain>上传配图</el-button>
          </el-upload>
          <el-image v-if="stepForm.imageUrl" :src="fullUrl(stepForm.imageUrl)" fit="cover"
            style="width: 100px; height: 72px; margin-left: 12px; border-radius: 8px; vertical-align: middle" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stepDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStep">保存</el-button>
      </template>
    </el-dialog>

    <!-- 关联管理 -->
    <el-dialog v-model="relationsVisible" title="关联管理（工艺 → 藏品）" width="560px">
      <el-table :data="relationDraft" size="small" max-height="240">
        <el-table-column label="名称" min-width="160">
          <template #default="{ row }">{{ artifactName(row.targetId) }}</template>
        </el-table-column>
        <el-table-column prop="targetId" label="藏品 ID" width="100" />
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" @click="relationDraft.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; gap: 10px; margin-top: 14px; align-items: center">
        <el-select v-model="newRelationTargetId" filterable placeholder="选择藏品" style="width: 260px">
          <el-option v-for="a in artifacts" :key="a.id" :label="a.name" :value="a.id" />
        </el-select>
        <el-button @click="addRelation">添加</el-button>
      </div>
      <template #footer>
        <el-button @click="relationsVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRelations">保存关联</el-button>
      </template>
    </el-dialog>

    <!-- 发布 -->
    <el-dialog v-model="publishDialog" title="发布工艺" width="480px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px"
        title="发布校验：名称/概述/适用范围 + 至少一个有效工序（每道工序需名称与简介）" />
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
  import { artifactApi, craftApi, relationApi, uploadConfig } from '../api'
  import { fullUrl } from '../api/http'
  import RichEditor from '../components/RichEditor.vue'

  const route = useRoute()
  const router = useRouter()
  const craftId = route.params.id ? Number(route.params.id) : null

  const craftForm = reactive({ id: null, name: '', overview: '', feature: '', scope: '', source: '' })
  const craftRules = {
    name: [{ required: true, message: '名称必填', trigger: 'blur' }],
    overview: [{ required: true, message: '概述必填（发布条件）', trigger: 'blur' }],
    scope: [{ required: true, message: '适用范围必填（发布条件）', trigger: 'blur' }]
  }

  const steps = ref([])
  const saving = ref(false)
  const publishing = ref(false)

  const stepDialogVisible = ref(false)
  const stepFormRef = ref()
  const stepForm = reactive({ id: null, name: '', intro: '', mediaId: null, imageUrl: '' })
  const stepRules = {
    name: [{ required: true, message: '名称必填', trigger: 'blur' }],
    intro: [{ required: true, message: '简介必填（发布条件）', trigger: 'blur' }]
  }

  const artifacts = ref([])
  const relationsVisible = ref(false)
  const relationDraft = ref([])
  const newRelationTargetId = ref(null)

  const publishDialog = ref(false)
  const publishSummary = ref('')

  const relationArtifacts = computed(() => {
    return relationDraft.value
      .filter((r) => r.targetType === 'artifact')
      .map((r) => artifacts.value.find((a) => a.id === r.targetId))
      .filter(Boolean)
  })

  function artifactName(id) {
    const a = artifacts.value.find((x) => x.id === id)
    return a ? a.name : '藏品 #' + id
  }

  function plainText(html) {
    return String(html || '').replace(/<[^>]+>/g, ' ').trim()
  }

  function richPreview(text) {
    if (!text) return ''
    const t = String(text).trim()
    if (t.startsWith('<')) return t
    return t.split('\n').map((line) => line.trim()).filter((line) => line).map((line) => {
      const safe = line.split('&').join('&amp;').split('<').join('&lt;').split('>').join('&gt;')
      return '<p style="margin:0 0 10px;font-size:12px;line-height:1.9;color:#f1e0ce;">' + safe + '</p>'
    }).join('')
  }

  function richLight(text) {
    if (!text) return ''
    const t = String(text).trim()
    if (t.startsWith('<')) return t
    return t.split('\n').map((line) => line.trim()).filter((line) => line).map((line) => {
      const safe = line.split('&').join('&amp;').split('<').join('&lt;').split('>').join('&gt;')
      return '<p style="margin:0 0 8px;font-size:11px;line-height:1.8;color:#685e52;">' + safe + '</p>'
    }).join('')
  }

  async function load() {
    if (craftId) {
      const list = (await craftApi.list()) || []
      const craft = list.find((c) => c.id === craftId)
      if (craft) {
        Object.assign(craftForm, craft)
      } else {
        ElMessage.error('工艺不存在')
        router.push('/crafts')
        return
      }
      steps.value = (await craftApi.steps(craftId)) || []
      try {
        const relations = await relationApi.listBySource({ sourceType: 'craft', sourceId: craftId })
        relationDraft.value = (relations || []).map((r) => ({ targetType: r.targetType, targetId: r.targetId }))
      } catch (e) { /* 关联读取失败不阻塞 */ }
    }
    try {
      artifacts.value = (await artifactApi.page({ pageNum: 1, pageSize: 200 })).list || []
    } catch (e) { /* 拦截器已提示 */ }
  }

  async function saveCraft() {
    await craftFormRef.value.validate()
    saving.value = true
    try {
      if (craftId) {
        await craftApi.update(craftId, { ...craftForm })
        ElMessage.success('工艺已保存')
      } else {
        const id = await craftApi.create({ ...craftForm })
        ElMessage.success('工艺已创建，可维护工序与关联')
        router.replace('/crafts/' + id + '/edit')
        return
      }
    } catch (e) {
      return
    } finally {
      saving.value = false
    }
  }

  // ---------------- 工序 ----------------

  function openStepDialog(row) {
    Object.assign(stepForm, {
      id: row ? row.id : null,
      name: row ? row.name : '',
      intro: row ? row.intro : '',
      mediaId: row ? row.mediaId : null,
      imageUrl: ''
    })
    stepDialogVisible.value = true
  }

  function onStepImageSuccess(resp) {
    if (resp && resp.code === 0 && resp.data) {
      stepForm.mediaId = resp.data.id
      stepForm.imageUrl = resp.data.url
    }
  }

  async function saveStep() {
    await stepFormRef.value.validate()
    saving.value = true
    try {
      const data = { name: stepForm.name, intro: stepForm.intro, mediaId: stepForm.mediaId }
      if (stepForm.id) await craftApi.updateStep(craftId, stepForm.id, data)
      else await craftApi.createStep(craftId, data)
      ElMessage.success('工序已保存')
      stepDialogVisible.value = false
      steps.value = (await craftApi.steps(craftId)) || []
    } catch (e) { /* 拦截器已提示 */ } finally {
      saving.value = false
    }
  }

  async function moveStep(row, direction) {
    await craftApi.moveStep(craftId, row.id, direction)
    steps.value = (await craftApi.steps(craftId)) || []
  }

  async function removeStep(row) {
    await ElMessageBox.confirm(`删除工序「${row.name}」？`, '删除', { type: 'warning' })
    await craftApi.deleteStep(craftId, row.id)
    steps.value = (await craftApi.steps(craftId)) || []
  }

  // ---------------- 关联 ----------------

  function addRelation() {
    if (!newRelationTargetId.value) {
      ElMessage.warning('请选择藏品')
      return
    }
    const exists = relationDraft.value.some((r) => r.targetId === newRelationTargetId.value)
    if (exists) {
      ElMessage.warning('该藏品已在关联列表中')
      return
    }
    relationDraft.value.push({ targetType: 'artifact', targetId: newRelationTargetId.value })
    newRelationTargetId.value = null
  }

  async function saveRelations() {
    saving.value = true
    try {
      await craftApi.setRelations(craftId, relationDraft.value)
      ElMessage.success('关联已保存')
      relationsVisible.value = false
    } catch (e) { /* 拦截器已提示 */ } finally {
      saving.value = false
    }
  }

  // ---------------- 发布 ----------------

  async function doPublish() {
    if (!publishSummary.value.trim()) {
      ElMessage.warning('修改内容说明必填（7.14）')
      return
    }
    publishing.value = true
    try {
      await craftApi.publish(craftId, publishSummary.value.trim())
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

  /* 手机预览框架样式统一在 PhonePreview 组件（iPhone 12 Pro 390×844） */
  .pv-head {
    padding: 36px 30px; color: #fbf0dd;
    background: radial-gradient(ellipse at 75% 20%, #a75c42 0%, #7e352d 45%, #351b1b 100%);
  }
  .pv-kicker { font-size: 9px; letter-spacing: 2px; color: #e8c99a; display: block; }
  .pv-name { display: block; font-size: 24px; font-weight: 500; letter-spacing: 3px; margin: 8px 0 12rpx; }
  .pv-head-rich { font-size: 12px; }
  .pv-note {
    margin: 20px 22px 0; background: #eee6da; border-radius: 10px;
    padding: 10px 12px; font-size: 11px; color: #685e52; line-height: 1.8;
  }
  .pv-note b { color: #5a4030; }
  .pv-section { padding: 18px 22px 14px; }
  .pv-sec-title { font-size: 15px; font-weight: 600; color: #28231f; margin-bottom: 12px; padding-left: 8px; border-left: 3px solid #b58a4a; }
  .pv-step {
    display: flex; align-items: center; gap: 10px; border: 1px solid #e4d8c8;
    border-radius: 10px; background: #fffdf8; padding: 10px 12px; margin-bottom: 8px;
  }
  .pv-step-no {
    width: 24px; height: 24px; border-radius: 50%; background: #f1e6d7; color: #762d25;
    display: inline-flex; align-items: center; justify-content: center; font-size: 11px;
  }
  .pv-step-name { font-size: 13px; }
  .pv-relation {
    display: flex; align-items: center; gap: 10px; border: 1px solid #e5d8c7;
    border-radius: 10px; background: #fffdf8; padding: 10px 12px; margin-bottom: 8px;
  }
  .pv-rel-img { width: 44px; height: 40px; border-radius: 8px; object-fit: cover; background: #e8dfd2; }
  .pv-rel-fallback { display: inline-flex; align-items: center; justify-content: center; color: #a07b4d; font-size: 16px; }
  .pv-rel-name { font-size: 12px; color: #28231f; }
  .pv-finish { margin: 16px 14px; text-align: center; }
  .pv-complete-btn {
    display: inline-block; background: #762d25; color: #fff5e9;
    border-radius: 22px; padding: 8px 26px; font-size: 12px; letter-spacing: 2px;
  }
</style>
