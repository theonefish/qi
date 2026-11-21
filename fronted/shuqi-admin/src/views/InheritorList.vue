<template>
  <div>
    <el-card shadow="never" class="filter-bar">
      <el-button type="danger" plain @click="$router.push('/inheritors/new')">新增传承人</el-button>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column label="照片" width="90">
          <template #default="{ row }">
            <el-image v-if="row.photoUrl" :src="fullUrl(row.photoUrl)" fit="cover"
              style="width: 48px; height: 48px; border-radius: 50%"
              :preview-src-list="[fullUrl(row.photoUrl)]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column label="简介" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ plain(row.intro) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="资料来源" min-width="140" show-overflow-tooltip />
        <el-table-column label="审核" width="90">
          <template #default="{ row }">
            <el-tag :type="row.auditStatus === 1 ? 'success' : 'warning'" size="small">
              {{ row.auditStatus === 1 ? '已审核' : '未审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/inheritors/' + row.id + '/edit')">编辑</el-button>
            <el-button link type="success" @click="audit(row)">审核</el-button>
            <el-button link type="warning" @click="publish(row)">发布</el-button>
            <el-button link type="danger" v-if="row.status === 1" @click="offShelf(row)">下架</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="publishVisible" title="发布传承人" width="480px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px"
        title="发布校验：姓名/简介/照片/资料来源 + 审核通过（5.2 / D-20）" />
      <el-input v-model="publishSummary" type="textarea" :rows="3" placeholder="修改内容说明（必填）" />
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="doPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
  import { onMounted, ref } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { useRouter } from 'vue-router'
  import { inheritorApi } from '../api'
  import { fullUrl } from '../api/http'

  const router = useRouter()
  const list = ref([])
  const loading = ref(false)
  const publishing = ref(false)

  const publishVisible = ref(false)
  const publishSummary = ref('')
  let publishTarget = null

  function statusLabel(s) {
    return { 0: '草稿', 1: '已发布', 2: '已下架' }[s] || s
  }
  function statusType(s) {
    return { 0: 'info', 1: 'success', 2: 'danger' }[s] || 'info'
  }
  function plain(html) {
    return String(html || '').replace(/<[^>]+>/g, ' ').trim()
  }

  async function load() {
    loading.value = true
    try {
      list.value = (await inheritorApi.list()) || []
    } finally {
      loading.value = false
    }
  }

  function audit(row) {
    const action = row.auditStatus === 1 ? '撤销审核' : '审核通过'
    ElMessageBox.confirm(`确认对传承人「${row.name}」执行：${action}？`, '简单自审', { type: 'warning' })
      .then(async () => {
        await inheritorApi.audit(row.id, row.auditStatus !== 1)
        ElMessage.success('已更新审核状态')
        load()
      })
      .catch(() => {})
  }

  function publish(row) {
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
      await inheritorApi.publish(publishTarget.id, publishSummary.value.trim())
      ElMessage.success('发布成功')
      publishVisible.value = false
      load()
    } catch (e) { /* 拦截器已提示 */ } finally {
      publishing.value = false
    }
  }

  async function offShelf(row) {
    await ElMessageBox.confirm(`下架「${row.name}」？`, '下架', { type: 'warning' })
    await inheritorApi.offShelf(row.id)
    ElMessage.success('已下架')
    load()
  }

  async function remove(row) {
    await ElMessageBox.confirm(`删除传承人「${row.name}」？`, '删除', { type: 'warning' })
    await inheritorApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  }

  onMounted(() => { load() })
</script>

<style scoped>
  /* 筛选条样式统一在 styles/admin.scss */
</style>
