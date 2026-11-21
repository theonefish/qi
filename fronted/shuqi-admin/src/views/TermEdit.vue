<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="15">
        <el-card shadow="never" class="block">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>{{ termId ? '编辑术语' : '新增术语' }}</span>
              <div style="display: flex; gap: 10px">
                <el-button size="small" @click="$router.push('/terms')">返回列表</el-button>
                <el-button size="small" type="success" plain v-if="termId" @click="audit">审核</el-button>
                <el-button size="small" type="warning" @click="publishDialog = true" v-if="termId">发布</el-button>
              </div>
            </div>
          </template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
            <el-form-item label="术语" prop="name">
              <el-input v-model="form.name" maxlength="20" />
            </el-form-item>
            <el-form-item label="释义" prop="definition">
              <RichEditor v-model="form.definition" height="200px" />
            </el-form-item>
            <el-form-item label="来源">
              <el-input v-model="form.source" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="save">
              {{ termId ? '保存术语' : '创建术语' }}
            </el-button>
            <span class="hint">发布后，章节正文中对应的 [[term:ID]] 标记在小程序中可点击展示此释义</span>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="9">
        <PhonePreview>
          <div class="pv-dim">
            <div class="pv-chapter-hint">第二章 · 百工成器（章节正文示意）</div>
            <p class="pv-sample">髹漆是漆器制作中需要重点了解的工艺环节。阅读时可以点击
              <span class="pv-term-inline">{{ form.name || '术语' }}</span>
              等术语，查看经审核的释义。
            </p>
          </div>
          <div class="pv-sheet">
            <div class="pv-sheet-handle"></div>
            <span class="pv-sheet-kicker">CULTURAL KNOWLEDGE</span>
            <span class="pv-sheet-title">{{ form.name || '术语名称' }}</span>
            <div class="pv-sheet-body" v-if="form.definition" v-html="previewTextHtml(form.definition)"></div>
            <div class="pv-sheet-src" v-if="form.source">来源：{{ form.source }}</div>
            <div class="pv-sheet-close">返回章节</div>
          </div>
        </PhonePreview>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { termApi } from '../api'
  import RichEditor from '../components/RichEditor.vue'
  import PhonePreview from '../components/PhonePreview.vue'
  import { previewTextHtml } from '../utils/preview-shared'

  const route = useRoute()
  const router = useRouter()
  const termId = route.params.id ? Number(route.params.id) : null

  const form = reactive({ id: null, name: '', definition: '', source: '' })
  const formRef = ref()
  const saving = ref(false)
  const publishing = ref(false)

  const publishDialog = ref(false)
  const publishSummary = ref('')
  let publishTarget = null

  const rules = {
    name: [{ required: true, message: '术语必填', trigger: 'blur' }],
    definition: [{ required: true, message: '释义必填（发布条件）', trigger: 'blur' }]
  }

  async function load() {
    if (!termId) return
    try {
      const page = await termApi.page({ pageNum: 1, pageSize: 100 })
      const self = (page.list || []).find((t) => t.id === termId)
      if (self) Object.assign(form, self)
    } catch (e) { /* 拦截器已提示 */ }
  }

  async function save() {
    await formRef.value.validate()
    saving.value = true
    try {
      if (termId) await termApi.update(termId, { ...form })
      else await termApi.create({ ...form })
      ElMessage.success('已保存')
      router.push('/terms')
    } catch (e) { /* 拦截器已提示 */ } finally {
      saving.value = false
    }
  }

  function audit() {
    ElMessageBox.confirm('审核操作：通过后才能发布（D-20）。确认为「已审核」？', '简单自审', { type: 'warning' })
      .then(async () => {
        await termApi.audit(termId, true)
        ElMessage.success('已更新审核状态')
      })
      .catch(() => {})
  }

  function publish() {
    publishTarget = null
    publishDialog.value = true
  }

  async function doPublish() {
    if (!publishSummary.value.trim()) {
      ElMessage.warning('修改内容说明必填（7.14）')
      return
    }
    publishing.value = true
    try {
      await termApi.publish(termId, publishSummary.value.trim())
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
  .pv-dim {
    min-height: 300px; padding: 30px 26px;
    background: linear-gradient(180deg, #efe7d9, #e5dccd);
  }
  .pv-chapter-hint { font-size: 10px; color: #a58b6e; margin-bottom: 12px; }
  .pv-sample { font-size: 13px; line-height: 2.2; color: #645b52; margin: 0; }
  .pv-term-inline { color: #762d25; border-bottom: 1px dashed #9c4939; font-weight: 600; }
  .pv-sheet {
    background: #fffdf8; border-radius: 18px 18px 0 0; padding: 18px 26px 26px;
    box-shadow: 0 -8px 30px rgba(50, 35, 25, .18);
  }
  .pv-sheet-handle { width: 40px; height: 4px; border-radius: 4px; background: #ded2c1; margin: 0 auto 14px; }
  .pv-sheet-kicker { font-size: 9px; color: #b58a4a; letter-spacing: 2px; display: block; }
  .pv-sheet-title { display: block; font-size: 20px; font-weight: 500; margin: 6px 0 12px; color: #28231f; }
  .pv-sheet-body { font-size: 12px; }
  .pv-sheet-src { font-size: 10px; color: #9b8c7c; margin-top: 12px; }
  .pv-sheet-close {
    margin-top: 18px; text-align: center; background: #eee6da; color: #695b4d;
    border-radius: 22px; padding: 9px 0; font-size: 12px;
  }
</style>
