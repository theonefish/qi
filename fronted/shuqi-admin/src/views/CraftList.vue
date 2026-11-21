<template>
  <div>
    <el-card shadow="never" class="filter-bar">
      <el-button type="danger" plain @click="$router.push('/crafts/new')">新增工艺</el-button>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column prop="name" label="工艺名称" width="160" />
        <el-table-column label="概述" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ plain(row.overview) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goEdit(row)">编辑与工序</el-button>
            <el-button link type="warning" @click="publish(row)">发布</el-button>
            <el-button link type="danger" v-if="row.status === 1" @click="offShelf(row)">下架</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="publishVisible" title="发布工艺" width="480px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px"
        title="发布校验：名称/概述/适用范围 + 至少一个有效工序（每道工序需名称与简介）" />
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
  import { craftApi } from '../api'

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
      list.value = (await craftApi.list()) || []
    } finally {
      loading.value = false
    }
  }

  function goEdit(row) {
    router.push('/crafts/' + row.id + '/edit')
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
      await craftApi.publish(publishTarget.id, publishSummary.value.trim())
      ElMessage.success('发布成功')
      publishVisible.value = false
      load()
    } catch (e) { /* 拦截器已提示 */ } finally {
      publishing.value = false
    }
  }

  async function offShelf(row) {
    await ElMessageBox.confirm(`下架「${row.name}」？`, '下架', { type: 'warning' })
    await craftApi.offShelf(row.id)
    ElMessage.success('已下架')
    load()
  }

  async function remove(row) {
    await ElMessageBox.confirm(`删除工艺「${row.name}」？其下工序将一并删除；引用它的工序节点将校验失败。`, '删除', { type: 'warning' })
    await craftApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  }

  onMounted(() => { load() })
</script>

<style scoped>
  /* 筛选条样式统一在 styles/admin.scss */
</style>
