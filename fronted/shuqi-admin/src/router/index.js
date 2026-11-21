import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '../api/http'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/overview',
    children: [
      { path: 'overview', component: () => import('../views/Overview.vue'), meta: { title: '数据总览' } },
      { path: 'chapters', component: () => import('../views/ChapterList.vue'), meta: { title: '章节管理' } },
      { path: 'chapters/:id/edit', component: () => import('../views/ChapterEdit.vue'), meta: { title: '章节编辑' } },
      { path: 'chapters/:id/flow', component: () => import('../views/FlowEdit.vue'), meta: { title: '分支编排' } },
      { path: 'artifacts', component: () => import('../views/ArtifactList.vue'), meta: { title: '藏品管理' } },
      { path: 'artifacts/new', component: () => import('../views/ArtifactEdit.vue'), meta: { title: '新增藏品' } },
      { path: 'artifacts/:id/edit', component: () => import('../views/ArtifactEdit.vue'), meta: { title: '藏品编辑' } },
      { path: 'categories', component: () => import('../views/CategoryDict.vue'), meta: { title: '类别字典' } },
      { path: 'crafts', component: () => import('../views/CraftList.vue'), meta: { title: '工艺管理' } },
      { path: 'crafts/new', component: () => import('../views/CraftEdit.vue'), meta: { title: '新增工艺' } },
      { path: 'crafts/:id/edit', component: () => import('../views/CraftEdit.vue'), meta: { title: '工艺编辑' } },
      { path: 'inheritors', component: () => import('../views/InheritorList.vue'), meta: { title: '传承人管理' } },
      { path: 'inheritors/new', component: () => import('../views/InheritorEdit.vue'), meta: { title: '新增传承人' } },
      { path: 'inheritors/:id/edit', component: () => import('../views/InheritorEdit.vue'), meta: { title: '传承人编辑' } },
      { path: 'terms', component: () => import('../views/TermList.vue'), meta: { title: '术语管理' } },
      { path: 'terms/new', component: () => import('../views/TermEdit.vue'), meta: { title: '新增术语' } },
      { path: 'terms/:id/edit', component: () => import('../views/TermEdit.vue'), meta: { title: '术语编辑' } },
      { path: 'events', component: () => import('../views/EventList.vue'), meta: { title: '历史事件' } },
      { path: 'events/new', component: () => import('../views/EventEdit.vue'), meta: { title: '新增事件' } },
      { path: 'events/:id/edit', component: () => import('../views/EventEdit.vue'), meta: { title: '事件编辑' } },
      { path: 'motifs', component: () => import('../views/MotifList.vue'), meta: { title: '纹饰资料' } },
      { path: 'motifs/new', component: () => import('../views/MotifEdit.vue'), meta: { title: '新增纹饰' } },
      { path: 'motifs/:id/edit', component: () => import('../views/MotifEdit.vue'), meta: { title: '纹饰编辑' } },
      { path: 'media', component: () => import('../views/MediaLibrary.vue'), meta: { title: '媒体资源' } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 登录守卫（AC-09：未登录不能访问后台管理功能）
router.beforeEach((to) => {
  if (to.path !== '/login' && !getToken()) {
    return '/login'
  }
})

export default router
