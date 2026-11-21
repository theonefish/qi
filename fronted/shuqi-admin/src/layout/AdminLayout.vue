<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">蜀漆云馆</div>
      <div class="logo-sub">内容管理后台</div>
      <el-menu :default-active="$route.path" router class="menu">
        <el-menu-item index="/overview">数据总览</el-menu-item>
        <el-menu-item index="/chapters">章节管理</el-menu-item>
        <el-menu-item index="/artifacts">藏品管理</el-menu-item>
        <el-menu-item index="/categories">类别字典</el-menu-item>
        <el-menu-item index="/crafts">工艺管理</el-menu-item>
        <el-menu-item index="/inheritors">传承人管理</el-menu-item>
        <el-sub-menu index="culture">
          <template #title>文化资料</template>
          <el-menu-item index="/terms">专业术语</el-menu-item>
          <el-menu-item index="/events">历史事件</el-menu-item>
          <el-menu-item index="/motifs">纹饰资料</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/media">媒体资源</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title }}</span>
        <div class="header-right">
          <span class="admin-name">{{ adminName }}</span>
          <el-button link type="danger" @click="onLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
  import { computed } from 'vue'
  import { useRouter } from 'vue-router'
  import { getAdminName, clearSession } from '../api/http'

  const router = useRouter()
  const adminName = computed(() => getAdminName())

  function onLogout() {
    clearSession()
    router.push('/login')
  }
</script>

<style scoped>
  .layout { height: 100vh; }
  .aside {
    background: #351b1b;
    color: #fbf0dd;
  }
  .logo {
    text-align: center;
    padding: 26px 0 4px;
    font-size: 20px;
    letter-spacing: 6px;
    color: #f1e0ce;
    font-weight: 600;
  }
  .logo-sub {
    text-align: center;
    font-size: 11px;
    color: #d9c1a0;
    padding-bottom: 18px;
    border-bottom: 1px solid rgba(237, 213, 175, .18);
  }
  .menu {
    border-right: none;
    background: transparent;
    margin-top: 10px;
  }
  .menu :deep(.el-menu-item) {
    color: #e9d2b3;
    background: transparent;
  }
  .menu :deep(.el-menu-item.is-active) {
    color: #fff5e8;
    background: rgba(156, 73, 57, .55);
  }
  .menu :deep(.el-menu-item:hover) {
    background: rgba(255, 245, 232, .08);
  }
  .menu :deep(.el-sub-menu__title) {
    color: #e9d2b3;
    background: transparent;
  }
  .menu :deep(.el-sub-menu__title:hover) {
    background: rgba(255, 245, 232, .08);
  }
  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #e6dccd;
    background: #fffdf8;
  }
  .page-title { font-size: 16px; font-weight: 600; color: #28231f; }
  .header-right { display: flex; align-items: center; gap: 16px; }
  .admin-name { font-size: 13px; color: #81776d; }
  .main { background: #f6f1e8; }
</style>
