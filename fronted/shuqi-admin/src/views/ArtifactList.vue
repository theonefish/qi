<template>
  <div>
    <el-card shadow="never" class="filter-bar">
      <el-input v-model="filters.name" placeholder="藏品名称" clearable style="width: 200px" @keyup.enter="load(true)" />
      <el-select v-model="filters.categoryId" placeholder="类别" clearable style="width: 150px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable style="width: 130px">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已下架" :value="2" />
      </el-select>
      <el-button type="primary" @click="load(true)">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
      <el-button type="danger" plain @click="$router.push('/artifacts/new')">新增藏品</el-button>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column label="主图" width="88">
          <template #default="{ row }">
            <el-image v-if="row.mainImageUrl" :src="fullUrl(row.mainImageUrl)" fit="cover"
              style="width: 56px; height: 48px; border-radius: 6px" :preview-src-list="[fullUrl(row.mainImageUrl)]"
              preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="类别" width="100">
          <template #default="{ row }">{{ catMap[row.categoryId] || row.categoryId }}</template>
        </el-table-column>
        <el-table-column prop="era" label="年代" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核" width="90">
          <template #default="{ row }">
            <el-tag :type="row.auditStatus === 1 ? 'success' : 'warning'" size="small">
              {{ row.auditStatus === 1 ? '已审核' : '未审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/artifacts/' + row.id + '/edit')">编辑</el-button>
            <el-button link type="success" @click="audit(row)">审核</el-button>
            <el-button link type="warning" @click="openPublish(row)">发布</el-button>
            <el-button link type="danger" v-if="row.status === 1" @click="offShelf(row)">下架</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="pageSize"
        v-model:current-page="pageNum" @current-change="load(false)" style="margin-top: 16px" />
    </el-card>

    <el-dialog v-model="publishVisible" title="发布藏品" width="480px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px"
        title="发布校验：必填字段、资料来源、审核状态、有效关联（目标须已发布）" />
      <el-input v-model="publishSummary" type="textarea" :rows="3" placeholder="修改内容说明（必填，将记入发布记录）" />
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="doPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { useRouter } from 'vue-router'
  import { artifactApi, categoryApi } from '../api'
  import { fullUrl } from '../api/http'

  const router = useRouter()
  const list = ref([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = 10
  const loading = ref(false)
  const publishing = ref(false)
  const categories = ref([])
  const catMap = {}

  const filters = reactive({ name: '', categoryId: null, status: null })

  const publishVisible = ref(false)
  const publishSummary = ref('')
  let publishTarget = null

  function statusLabel(s) {
    return { 0: '草稿', 1: '已发布', 2: '已下架' }[s] || s
  }
  function statusType(s) {
    return { 0: 'info', 1: 'success', 2: 'danger' }[s] || 'info'
  }

  async function loadCategories() {
    try {
      categories.value = (await categoryApi.list()) || []
      categories.value.forEach((c) => { catMap[c.id] = c.name })
    } catch (e) { /* 拦截器已提示 */ }
  }

  async function load(reset) {
    if (reset) pageNum.value = 1
    loading.value = true
    try {
      const page = await artifactApi.page({
        pageNum: pageNum.value,
        pageSize,
        name: filters.name || undefined,
        categoryId: filters.categoryId ?? undefined,
        status: filters.status ?? undefined
      })
      list.value = page.list || []
      total.value = page.total || 0
    } finally {
      loading.value = false
    }
  }

  function resetFilters() {
    filters.name = ''
    filters.categoryId = null
    filters.status = null
    load(true)
  }

  function audit(row) {
    const action = row.auditStatus === 1 ? '撤销审核（改为未审核）' : '审核通过'
    ElMessageBox.confirm(`确认对「${row.name}」执行：${action}？`, '简单自审', { type: 'warning' })
      .then(async () => {
        await artifactApi.audit(row.id, row.auditStatus !== 1)
        ElMessage.success('已更新审核状态')
        load(false)
      })
      .catch(() => {})
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
      await artifactApi.publish(publishTarget.id, publishSummary.value.trim())
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
    await ElMessageBox.confirm(`下架后小程序立即不可见，确认下架「${row.name}」？`, '下架', { type: 'warning' })
    await artifactApi.offShelf(row.id)
    ElMessage.success('已下架')
    load(false)
  }

  async function remove(row) {
    await ElMessageBox.confirm(`删除草稿「${row.name}」？`, '删除', { type: 'warning' })
    await artifactApi.remove(row.id)
    ElMessage.success('已删除')
    load(false)
  }

  onMounted(() => {
    loadCategories()
    load(true)
  })
</script>

<style scoped>
  /* 筛选条样式统一在 styles/admin.scss */
</style>
