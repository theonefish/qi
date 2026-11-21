<template>
  <div>
    <el-card shadow="never" class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="章节标题" clearable style="width: 200px" @keyup.enter="load(true)" />
      <el-select v-model="filters.status" placeholder="状态" clearable style="width: 130px">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已下架" :value="2" />
      </el-select>
      <el-button type="primary" @click="load(true)">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
      <el-button type="danger" plain @click="openCreate">新增章节</el-button>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="sortOrder" label="顺序" width="70" />
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.chapterType === 0 ? 'info' : 'warning'">
              {{ row.chapterType === 0 ? '序章' : '正章' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="summary" label="摘要" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goEdit(row)">编辑与节点</el-button>
            <el-button link type="warning" @click="$router.push(`/chapters/${row.id}/flow`)">分支编排</el-button>
            <el-button link type="warning" @click="publish(row)">发布</el-button>
            <el-button link type="info" @click="openRecords(row)">记录</el-button>
            <el-button link type="danger" v-if="row.status === 1" @click="offShelf(row)">下架</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="pageSize"
        v-model:current-page="pageNum" @current-change="load(false)" style="margin-top: 16px" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑章节' : '新增章节'" width="560px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="60" />
        </el-form-item>
        <el-form-item label="章节类型" prop="chapterType">
          <el-radio-group v-model="form.chapterType">
            <el-radio :value="0">序章（不计入进度）</el-radio>
            <el-radio :value="1">正章（计入进度）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" />
        </el-form-item>
        <el-form-item label="封面图" prop="coverUrl">
          <el-upload :action="uploadConfig.action" :headers="uploadConfig.headers()" :show-file-list="false"
            accept="image/*" :on-success="onUploadSuccess" :on-error="() => ElMessage.error('上传失败')">
            <el-button type="primary" plain>上传封面</el-button>
          </el-upload>
          <el-image v-if="form.coverUrl" :src="fullUrl(form.coverUrl)" fit="cover"
            style="width: 120px; height: 72px; margin-left: 12px; border-radius: 8px" />
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sortOrder" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存（草稿）</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishVisible" title="发布章节" width="480px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px"
        title="发布校验：标题/封面/摘要/类型/排序 + 至少一个有效节点（引用的工艺、传承人、事件须已发布）" />
      <el-input v-model="publishSummary" type="textarea" :rows="3" placeholder="修改内容说明（必填，将记入发布记录）" />
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="doPublish">确认发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recordsVisible" title="发布记录（仅查看）" width="720px">
      <el-table :data="records" size="small" max-height="360">
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="modifySummary" label="修改内容" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ recordStatus(row.publishStatus) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作管理员" width="110" />
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ (row.publishTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { useRouter } from 'vue-router'
  import { chapterApi, uploadConfig } from '../api'
  import { fullUrl } from '../api/http'

  const router = useRouter()
  const list = ref([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = 10
  const loading = ref(false)
  const saving = ref(false)
  const publishing = ref(false)

  const filters = reactive({ keyword: '', status: null })

  const formVisible = ref(false)
  const formRef = ref()
  const form = reactive({
    id: null, title: '', chapterType: 1, subtitle: '', coverUrl: '', summary: '', sortOrder: null
  })
  const rules = {
    title: [{ required: true, message: '标题必填', trigger: 'blur' }],
    chapterType: [{ required: true, message: '类型必选', trigger: 'change' }],
    summary: [{ required: true, message: '摘要必填（发布条件）', trigger: 'blur' }]
  }

  const publishVisible = ref(false)
  const publishSummary = ref('')
  let publishTarget = null

  const recordsVisible = ref(false)
  const records = ref([])

  function statusLabel(s) {
    return { 0: '草稿', 1: '已发布', 2: '已下架' }[s] || s
  }
  function statusType(s) {
    return { 0: 'info', 1: 'success', 2: 'danger' }[s] || 'info'
  }
  function recordStatus(s) {
    return { 0: '发布中', 1: '发布成功', 2: '发布失败', 3: '回滚成功', 4: '回滚失败' }[s] || s
  }

  async function load(reset) {
    if (reset) pageNum.value = 1
    loading.value = true
    try {
      const page = await chapterApi.page({
        pageNum: pageNum.value,
        pageSize,
        keyword: filters.keyword || undefined,
        status: filters.status ?? undefined
      })
      list.value = page.list || []
      total.value = page.total || 0
    } finally {
      loading.value = false
    }
  }

  function resetFilters() {
    filters.keyword = ''
    filters.status = null
    load(true)
  }

  function openCreate() {
    Object.assign(form, { id: null, title: '', chapterType: 1, subtitle: '', coverUrl: '', summary: '', sortOrder: null })
    formVisible.value = true
  }

  function onUploadSuccess(resp) {
    if (resp && resp.code === 0 && resp.data && resp.data.url) {
      form.coverUrl = resp.data.url
      formRef.value?.validateField('coverUrl')
    } else {
      ElMessage.error((resp && resp.msg) || '上传失败')
    }
  }

  async function save() {
    await formRef.value.validate()
    saving.value = true
    try {
      if (form.id) {
        await chapterApi.update(form.id, { ...form })
      } else {
        await chapterApi.create({ ...form })
      }
      ElMessage.success('已保存，可进入“编辑与节点”维护叙事节点')
      formVisible.value = false
      load(false)
    } catch (e) {
      /* 拦截器已提示 */
    } finally {
      saving.value = false
    }
  }

  function goEdit(row) {
    router.push('/chapters/' + row.id + '/edit')
  }

  function openPublish(row) {
    publishTarget = row
    publishSummary.value = ''
    publishVisible.value = true
  }

  async function doPublish() {
    if (!publishSummary.value.trim()) {
      ElMessage.warning('修改内容说明必填（7.14）')
      return
    }
    publishing.value = true
    try {
      await chapterApi.publish(publishTarget.id, publishSummary.value.trim())
      ElMessage.success('发布成功')
      publishVisible.value = false
      load(false)
    } catch (e) {
      /* 校验失败原因已由拦截器提示 */
    } finally {
      publishing.value = false
    }
  }

  async function offShelf(row) {
    await ElMessageBox.confirm(`下架后小程序立即不可见，确认下架「${row.title}」？`, '下架', { type: 'warning' })
    await chapterApi.offShelf(row.id)
    ElMessage.success('已下架')
    load(false)
  }

  async function remove(row) {
    await ElMessageBox.confirm(`删除章节「${row.title}」？其下叙事节点将一并删除。`, '删除章节', { type: 'warning' })
    await chapterApi.remove(row.id)
    ElMessage.success('已删除')
    load(false)
  }

  async function openRecords(row) {
    try {
      records.value = (await chapterApi.publishRecords(row.id)) || []
      recordsVisible.value = true
    } catch (e) { /* 拦截器已提示 */ }
  }

  onMounted(() => { load(true) })
</script>

<style scoped>
  .filter-bar { margin-bottom: 16px; }
  .filter-bar :deep(.el-card__body) { display: flex; gap: 12px; flex-wrap: wrap; }
</style>
