<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="15">
        <el-card shadow="never" class="block">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>{{ eventId ? '编辑历史事件' : '新增历史事件' }}</span>
              <div style="display: flex; gap: 10px">
                <el-button size="small" @click="$router.push('/events')">返回列表</el-button>
                <el-button size="small" type="warning" @click="publishDialog = true" v-if="eventId">发布</el-button>
              </div>
            </div>
          </template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
            <el-form-item label="时间/时期" prop="eventTime">
              <el-input v-model="form.eventTime" placeholder="如：汉代；年代不明用时期表述，不得编造年份（C.3）" />
            </el-form-item>
            <el-form-item label="事件名称" prop="title">
              <el-input v-model="form.title" />
            </el-form-item>
            <el-form-item label="事件介绍" prop="description">
              <RichEditor v-model="form.description" height="220px" />
            </el-form-item>
            <el-form-item label="资料来源" prop="source">
              <el-input v-model="form.source" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="save">
              {{ eventId ? '保存事件' : '创建事件' }}
            </el-button>
            <span class="hint">发布后可在章节的“时间轴节点”中引用（引用时校验已发布，5.4）</span>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="9">
        <PhonePreview>
          <div class="pv-dim">
            <div class="pv-chapter-hint">第一章 · 漆从蜀地来（时间轴节点示意）</div>
            <div class="pv-timeline">
              <div class="pv-tl-item">
                <span class="pv-tl-time">{{ form.eventTime || '时间 / 时期' }}</span>
                <span class="pv-tl-title">{{ form.title || '事件名称' }}</span>
                <div class="pv-tl-desc" v-if="form.description" v-html="previewTextHtml(form.description)"></div>
                <div class="pv-tl-desc" v-else>事件介绍将在此处展示……</div>
              </div>
              <div class="pv-tl-item pv-tl-ghost">
                <span class="pv-tl-time">下一事件</span>
                <span class="pv-tl-title">……</span>
              </div>
            </div>
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
  import { eventApi } from '../api'
  import RichEditor from '../components/RichEditor.vue'
  import PhonePreview from '../components/PhonePreview.vue'
  import { previewTextHtml } from '../utils/preview-shared'

  const route = useRoute()
  const router = useRouter()
  const eventId = route.params.id ? Number(route.params.id) : null

  const form = reactive({ id: null, eventTime: '', title: '', description: '', source: '' })
  const formRef = ref()
  const saving = ref(false)

  const publishDialog = ref(false)
  const publishSummary = ref('')

  const rules = {
    eventTime: [{ required: true, message: '时间/时期必填', trigger: 'blur' }],
    title: [{ required: true, message: '事件名称必填', trigger: 'blur' }],
    description: [{ required: true, message: '事件介绍必填（发布条件）', trigger: 'blur' }],
    source: [{ required: true, message: '资料来源必填', trigger: 'blur' }]
  }

  async function load() {
    if (!eventId) return
    try {
      const page = await eventApi.page({ pageNum: 1, pageSize: 100 })
      const self = (page.list || []).find((e) => e.id === eventId)
      if (self) Object.assign(form, self)
    } catch (e) { /* 拦截器已提示 */ }
  }

  async function save() {
    await formRef.value.validate()
    saving.value = true
    try {
      if (eventId) await eventApi.update(eventId, { ...form })
      else await eventApi.create({ ...form })
      ElMessage.success('已保存')
      router.push('/events')
    } catch (e) { /* 拦截器已提示 */ } finally {
      saving.value = false
    }
  }

  onMounted(() => { load() })
</script>

<style scoped>
  /* .block / .hint 统一在 styles/admin.scss */
  .hint { margin-left: 14px; }
  .pv-dim {
    min-height: 420px; padding: 30px 26px;
    background: linear-gradient(180deg, #efe7d9, #e5dccd);
  }
  .pv-chapter-hint { font-size: 10px; color: #a58b6e; margin-bottom: 20px; }
  .pv-timeline { margin-left: 10px; border-left: 1px solid #c9ad83; padding-left: 20px; }
  .pv-tl-item { position: relative; padding-bottom: 24px; }
  .pv-tl-item::before {
    content: ''; position: absolute; left: -25px; top: 5px; width: 8px; height: 8px;
    border-radius: 50%; background: #f6f1e8; border: 2px solid #a77c48;
  }
  .pv-tl-time { display: block; font-size: 11px; color: #762d25; font-weight: 600; }
  .pv-tl-title { display: block; font-size: 14px; color: #28231f; margin-top: 3px; }
  .pv-tl-desc { margin: 6px 0 0; font-size: 12px; color: #6c6258; line-height: 1.8; }
  .pv-tl-ghost { opacity: .45; }
</style>
