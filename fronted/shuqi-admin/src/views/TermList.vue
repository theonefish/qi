<template>
  <div>
    <el-card shadow="never" class="filter-bar">
      <el-input v-model="keyword" placeholder="术语名称" clearable style="width: 200px" @keyup.enter="load(true)" />
      <el-button type="primary" @click="load(true)">查询</el-button>
      <el-button type="danger" plain @click="$router.push('/terms/new')">新增术语</el-button>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column prop="name" label="术语" width="140" />
        <el-table-column label="释义" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ plain(row.definition) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" min-width="140" show-overflow-tooltip />
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
            <el-button link type="primary" @click="$router.push('/terms/' + row.id + '/edit')">编辑</el-button>
            <el-button link type="success" @click="audit(row)">审核</el-button>
            <el-button link type="warning" @click="publish(row)">发布</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="pageSize"
        v-model:current-page="pageNum" @current-change="load(false)" style="margin-top: 16px" />
    </el-card>

    <el-dialog v-model="publishVisible" title="发布术语" width="480px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px"
        title="发布条件：名称、释义、审核通过（D-20）。发布后正文标记才可点击查看释义。" />
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
  import { termApi } from '../api'

  const router = useRouter()
  const list = ref([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = 10
  const loading = ref(false)
  const publishing = ref(false)
  const keyword = ref('')

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

  async function load(reset) {
    if (reset) pageNum.value = 1
    loading.value = true
    try {
      const page = await termApi.page({
        pageNum: pageNum.value, pageSize,
        keyword: keyword.value || undefined
      })
      list.value = page.list || []
      total.value = page.total || 0
    } finally {
      loading.value = false
    }
  }

  function audit(row) {
    const action = row.auditStatus === 1 ? '撤销审核' : '审核通过'
    ElMessageBox.confirm(`确认对术语「${row.name}」执行：${action}？`, '简单自审', { type: 'warning' })
      .then(async () => {
        await termApi.audit(row.id, row.auditStatus !== 1)
        ElMessage.success('已更新审核状态')
        load(false)
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
      await termApi.publish(publishTarget.id, publishSummary.value.trim())
      ElMessage.success('发布成功')
      publishVisible.value = false
      load(false)
    } catch (e) { /* 拦截器已提示 */ } finally {
      publishing.value = false
    }
  }

  async function remove(row) {
    await ElMessageBox.confirm(`删除术语「${row.name}」？正文中已插入的该标记将失去释义数据。`, '删除', { type: 'warning' })
    await termApi.remove(row.id)
    ElMessage.success('已删除')
    load(false)
  }

  onMounted(() => { load(true) })
</script>

<style scoped>
  /* 筛选条样式统一在 styles/admin.scss */
</style>
