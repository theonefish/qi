<template>
  <div>
    <!-- 总览卡片（ADM-02） -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 章节维度行为统计（ADM-11） -->
    <el-card shadow="never" class="block">
      <template #header>章节参观行为（进入 / 节点浏览 / 完成 / 回到主线）</template>
      <el-table :data="chapterStats" size="small" stripe>
        <el-table-column prop="chapterId" label="ID" width="64" />
        <el-table-column prop="title" label="章节" min-width="180" />
        <el-table-column prop="enterCount" label="进入次数" width="110" />
        <el-table-column prop="viewNodeCount" label="节点浏览" width="110" />
        <el-table-column prop="completeCount" label="完成次数" width="110" />
        <el-table-column prop="backMainCount" label="回到主线" width="110" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
  import { computed, ref } from 'vue'
  import { statsApi } from '../api'

  const overview = ref({})
  const chapterStats = ref([])

  const cards = computed(() => {
    const o = overview.value
    return [
      { label: '已发布章节', value: o.publishedChapters ?? '-' },
      { label: '已发布藏品', value: o.publishedArtifacts ?? '-' },
      { label: '草稿藏品', value: o.draftArtifacts ?? '-' },
      { label: '已发布工艺', value: o.publishedCrafts ?? '-' },
      { label: '已发布传承人', value: o.publishedInheritors ?? '-' },
      { label: '到访访客数', value: o.visitorCount ?? '-' },
      { label: '今日行为事件', value: o.todayEventCount ?? '-' }
    ]
  })

  async function load() {
    try {
      overview.value = (await statsApi.overview()) || {}
      chapterStats.value = (await statsApi.chapters()) || []
    } catch (e) {
      /* 拦截器已提示 */
    }
  }

  load()
</script>

<style scoped>
  /* 统计卡样式统一在 styles/admin.scss */
  .block { margin-top: 8px; }
</style>
