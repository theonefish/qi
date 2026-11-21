<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>藏品类别字典（D-27：受控字典，藏品表单从此选择）</span>
          <el-button type="primary" size="small" @click="dialogVisible = true">新增类别</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="类别名称" min-width="180" />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增类别" width="360px">
      <el-input v-model="name" placeholder="类别名称" maxlength="20" />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
  import { onMounted, ref } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { categoryApi } from '../api'

  const list = ref([])
  const loading = ref(false)
  const dialogVisible = ref(false)
  const name = ref('')
  const saving = ref(false)

  async function load() {
    loading.value = true
    try {
      list.value = (await categoryApi.list()) || []
    } finally {
      loading.value = false
    }
  }

  async function save() {
    if (!name.value.trim()) {
      ElMessage.warning('类别名称必填')
      return
    }
    saving.value = true
    try {
      await categoryApi.create(name.value.trim())
      ElMessage.success('已新增')
      name.value = ''
      dialogVisible.value = false
      load()
    } catch (e) {
      /* 拦截器已提示 */
    } finally {
      saving.value = false
    }
  }

  async function remove(row) {
    await ElMessageBox.confirm(`确认删除类别「${row.name}」？已被藏品使用的类别删除后藏品类别将显示为空。`, '删除', { type: 'warning' })
    try {
      await fetchDelete(row.id)
      ElMessage.success('已删除')
      load()
    } catch (e) {
      /* 拦截器已提示 */
    }
  }

  // 类别删除走通用 DELETE（后端 BaseMapper 逻辑删除，暂无独立 API 函数）
  async function fetchDelete(id) {
    const { default: http } = await import('../api/http')
    await http.delete('/admin/artifact-categories/' + id)
  }

  onMounted(() => { load() })
</script>
