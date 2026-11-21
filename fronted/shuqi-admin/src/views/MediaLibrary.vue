<template>
  <div>
    <el-card shadow="never" class="filter-bar">
      <el-upload :action="uploadConfig.action" :headers="uploadConfig.headers()" :show-file-list="false"
        accept="image/*,video/*,audio/*" :on-success="onUploadSuccess" :on-error="() => ElMessage.error('上传失败')">
        <el-button type="primary">上传媒体文件</el-button>
      </el-upload>
      <el-select v-model="mediaType" placeholder="类型" clearable style="width: 130px" @change="load(true)">
        <el-option label="图片" value="image" />
        <el-option label="音频" value="audio" />
        <el-option label="视频" value="video" />
        <el-option label="文件" value="file" />
      </el-select>
      <el-input v-model="keyword" placeholder="资源名称" clearable style="width: 200px" @keyup.enter="load(true)" />
      <el-button type="primary" @click="load(true)">查询</el-button>
      <span class="tip">上传白名单：jpg/png/gif/webp/bmp/mp3/mp4/wav/m4a/pdf，单文件 ≤ 20MB</span>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="预览" width="110">
          <template #default="{ row }">
            <el-image v-if="row.mediaType === 'image'" :src="fullUrl(row.url)" fit="cover"
              style="width: 72px; height: 56px; border-radius: 6px"
              :preview-src-list="[fullUrl(row.url)]" preview-teleported />
            <span v-else>{{ typeLabel(row.mediaType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="mediaType" label="类型" width="90" />
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ sizeText(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" min-width="120" show-overflow-tooltip />
        <el-table-column label="地址" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" :href="fullUrl(row.url)" target="_blank" style="font-size: 12px">
              {{ row.url }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="pageSize"
        v-model:current-page="pageNum" @current-change="load(false)" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup>
  import { onMounted, ref } from 'vue'
  import { ElMessage } from 'element-plus'
  import { mediaApi, uploadConfig } from '../api'
  import { fullUrl } from '../api/http'

  const list = ref([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = 10
  const loading = ref(false)
  const mediaType = ref(null)
  const keyword = ref('')

  function typeLabel(t) {
    return { audio: '[音频]', video: '[视频]', file: '[文件]' }[t] || t
  }

  function sizeText(bytes) {
    if (!bytes) return '-'
    return bytes > 1024 * 1024 ? (bytes / 1024 / 1024).toFixed(1) + ' MB' : Math.round(bytes / 1024) + ' KB'
  }

  async function load(reset) {
    if (reset) pageNum.value = 1
    loading.value = true
    try {
      const page = await mediaApi.page({
        pageNum: pageNum.value,
        pageSize,
        mediaType: mediaType.value || undefined,
        keyword: keyword.value || undefined
      })
      list.value = page.list || []
      total.value = page.total || 0
    } finally {
      loading.value = false
    }
  }

  function onUploadSuccess(resp) {
    if (resp && resp.code === 0) {
      ElMessage.success('上传成功')
      load(true)
    } else {
      ElMessage.error((resp && resp.msg) || '上传失败')
    }
  }

  onMounted(() => { load(true) })
</script>

<style scoped>
  /* 筛选条样式统一在 styles/admin.scss */
  .tip { font-size: 12px; color: #81776d; }
</style>
