<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="15">
        <el-card shadow="never" class="block">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>{{ motifId ? '编辑纹饰' : '新增纹饰' }}</span>
              <div style="display: flex; gap: 10px">
                <el-button size="small" @click="$router.push('/motifs')">返回列表</el-button>
                <el-button size="small" type="warning" @click="publishDialog = true" v-if="motifId">发布</el-button>
              </div>
            </div>
          </template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" maxlength="30" />
            </el-form-item>
            <el-form-item label="图片">
              <el-upload :action="uploadConfig.action" :headers="uploadConfig.headers()" :show-file-list="false"
                accept="image/*" :on-success="onUploadSuccess">
                <el-button type="primary" plain>上传图片</el-button>
              </el-upload>
              <el-image v-if="form.imageUrl" :src="fullUrl(form.imageUrl)" fit="cover"
                style="width: 120px; height: 90px; margin-left: 12px; border-radius: 8px" />
            </el-form-item>
            <el-form-item label="文化解释" prop="culturalExplain">
              <RichEditor v-model="form.culturalExplain" height="200px" />
            </el-form-item>
            <el-form-item label="资料来源" prop="source">
              <el-input v-model="form.source" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="save">
              {{ motifId ? '保存纹饰' : '创建纹饰' }}
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="9">
        <PhonePreview>
          <div class="pv-card">
            <img v-if="form.imageUrl" :src="fullUrl(form.imageUrl)" class="pv-img" />
            <div v-else class="pv-img pv-img-fallback"><span>饰</span></div>
            <div class="pv-card-body">
              <span class="pv-kicker">MOTIF · 纹饰资料</span>
              <span class="pv-title">{{ form.name || '纹饰名称' }}</span>
              <div class="pv-rich" v-if="form.culturalExplain" v-html="previewTextHtml(form.culturalExplain)"></div>
            </div>
          </div>
          <div class="pv-note-hint">
            P0 在章节与藏品中引用展示纹饰内容；独立纹饰主题页为 P1
          </div>
        </PhonePreview>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { ElMessage } from 'element-plus'
  import { motifApi, uploadConfig } from '../api'
  import { fullUrl } from '../api/http'
  import RichEditor from '../components/RichEditor.vue'
  import PhonePreview from '../components/PhonePreview.vue'
  import { previewTextHtml } from '../utils/preview-shared'

  const route = useRoute()
  const router = useRouter()
  const motifId = route.params.id ? Number(route.params.id) : null

  const form = reactive({ id: null, name: '', imageUrl: '', culturalExplain: '', source: '' })
  const formRef = ref()
  const saving = ref(false)

  const publishDialog = ref(false)
  const publishSummary = ref('')

  const rules = {
    name: [{ required: true, message: '名称必填', trigger: 'blur' }],
    culturalExplain: [{ required: true, message: '文化解释必填（发布条件）', trigger: 'blur' }],
    source: [{ required: true, message: '资料来源必填', trigger: 'blur' }]
  }

  async function load() {
    if (!motifId) return
    try {
      const page = await motifApi.page({ pageNum: 1, pageSize: 100 })
      const self = (page.list || []).find((m) => m.id === motifId)
      if (self) Object.assign(form, self)
    } catch (e) { /* 拦截器已提示 */ }
  }

  function onUploadSuccess(resp) {
    if (resp && resp.code === 0 && resp.data) form.imageUrl = resp.data.url
  }

  async function save() {
    await formRef.value.validate()
    saving.value = true
    try {
      if (motifId) await motifApi.update(motifId, { ...form })
      else await motifApi.create({ ...form })
      ElMessage.success('已保存')
      router.push('/motifs')
    } catch (e) { /* 拦截器已提示 */ } finally {
      saving.value = false
    }
  }

  onMounted(() => { load() })
</script>

<style scoped>
  /* .block 统一在 styles/admin.scss */
  .pv-card {
    margin: 14px; border-radius: 14px; overflow: hidden;
    background: #fffdf8; border: 1px solid #e6dccd;
  }
  .pv-img { width: 100%; height: 200px; object-fit: cover; display: block; }
  .pv-img-fallback {
    display: flex; align-items: center; justify-content: center;
    background: #eee0cb; color: #a07b4d; font-size: 48px;
  }
  .pv-card-body { padding: 20px 24px; }
  .pv-kicker { font-size: 9px; color: #b58a4a; letter-spacing: 2px; display: block; margin-bottom: 6px; }
  .pv-title { display: block; font-size: 22px; font-weight: 600; margin-bottom: 12px; color: #28231f; }
  .pv-rich { font-size: 12px; }
  .pv-note-hint { text-align: center; font-size: 11px; color: #a58b6e; margin-top: 14px; }
  .el-dialog { }
</style>
