// 管理端接口清单（与 qi/docs/API.md 对应）
import http from './http'

export const authApi = {
  login: (data) => http.post('/admin/auth/login', data),
  logout: () => http.post('/admin/auth/logout')
}

export const statsApi = {
  overview: () => http.get('/admin/stats/overview'),
  chapters: () => http.get('/admin/stats/chapters')
}

export const categoryApi = {
  list: () => http.get('/admin/artifact-categories'),
  create: (name) => http.post('/admin/artifact-categories', { name })
}

export const artifactApi = {
  page: (params) => http.get('/admin/artifacts', { params }),
  detail: (id) => http.get(`/admin/artifacts/${id}`),
  create: (data) => http.post('/admin/artifacts', data),
  update: (id, data) => http.put(`/admin/artifacts/${id}`, data),
  audit: (id, pass) => http.post(`/admin/artifacts/${id}/audit`, { pass }),
  publish: (id, modifySummary) => http.post(`/admin/artifacts/${id}/publish`, { modifySummary }),
  offShelf: (id) => http.post(`/admin/artifacts/${id}/off-shelf`),
  setRelations: (id, targets) => http.put(`/admin/artifacts/${id}/relations`, { targets }),
  publishRecords: (id) => http.get(`/admin/artifacts/${id}/publish-records`),
  contentRecords: (params) => http.get('/admin/artifacts/publish-records', { params })
}

export const mediaApi = {
  page: (params) => http.get('/admin/media', { params })
}

export const chapterApi = {
  page: (params) => http.get('/admin/chapters', { params }),
  detail: (id) => http.get(`/admin/chapters/${id}`),
  create: (data) => http.post('/admin/chapters', data),
  update: (id, data) => http.put(`/admin/chapters/${id}`, data),
  remove: (id) => http.delete(`/admin/chapters/${id}`),
  publish: (id, modifySummary) => http.post(`/admin/chapters/${id}/publish`, { modifySummary }),
  offShelf: (id) => http.post(`/admin/chapters/${id}/off-shelf`),
  setRelations: (id, targets) => http.put(`/admin/chapters/${id}/relations`, { targets }),
  publishRecords: (id) => http.get(`/admin/chapters/${id}/publish-records`)
}

export const nodeApi = {
  list: (chapterId) => http.get(`/admin/chapters/${chapterId}/nodes`),
  detail: (chapterId, nodeId) => http.get(`/admin/chapters/${chapterId}/nodes/${nodeId}`),
  create: (chapterId, data) => http.post(`/admin/chapters/${chapterId}/nodes`, data),
  update: (chapterId, nodeId, data) => http.put(`/admin/chapters/${chapterId}/nodes/${nodeId}`, data),
  remove: (chapterId, nodeId) => http.delete(`/admin/chapters/${chapterId}/nodes/${nodeId}`),
  move: (chapterId, nodeId, direction) =>
    http.post(`/admin/chapters/${chapterId}/nodes/${nodeId}/move?direction=${direction}`)
}

// 分支编排（分支叙事设计稿 §7.4）：画布数据、坐标批量保存、流向覆盖连线
export const flowApi = {
  get: (chapterId) => http.get(`/admin/chapters/${chapterId}/flow`),
  saveLayout: (chapterId, items) => http.put(`/admin/chapters/${chapterId}/flow-layout`, { items }),
  setNext: (chapterId, nodeId, targetNodeId) =>
    http.put(`/admin/chapters/${chapterId}/nodes/${nodeId}/next`, { targetNodeId })
}

export const craftApi = {
  list: () => http.get('/admin/crafts'),
  create: (data) => http.post('/admin/crafts', data),
  update: (id, data) => http.put(`/admin/crafts/${id}`, data),
  remove: (id) => http.delete(`/admin/crafts/${id}`),
  publish: (id, modifySummary) => http.post(`/admin/crafts/${id}/publish`, { modifySummary }),
  offShelf: (id) => http.post(`/admin/crafts/${id}/off-shelf`),
  setRelations: (id, targets) => http.put(`/admin/crafts/${id}/relations`, { targets }),
  steps: (id) => http.get(`/admin/crafts/${id}/steps`),
  createStep: (id, data) => http.post(`/admin/crafts/${id}/steps`, data),
  updateStep: (id, stepId, data) => http.put(`/admin/crafts/${id}/steps/${stepId}`, data),
  deleteStep: (id, stepId) => http.delete(`/admin/crafts/${id}/steps/${stepId}`),
  moveStep: (id, stepId, direction) =>
    http.post(`/admin/crafts/${id}/steps/${stepId}/move?direction=${direction}`)
}

export const relationApi = {
  listBySource: (params) => http.get('/admin/relations', { params }),
  listByTarget: (params) => http.get('/admin/relations/targets', { params })
}

export const inheritorApi = {
  list: () => http.get('/admin/inheritors'),
  create: (data) => http.post('/admin/inheritors', data),
  update: (id, data) => http.put(`/admin/inheritors/${id}`, data),
  remove: (id) => http.delete(`/admin/inheritors/${id}`),
  audit: (id, pass) => http.post(`/admin/inheritors/${id}/audit`, { pass }),
  publish: (id, modifySummary) => http.post(`/admin/inheritors/${id}/publish`, { modifySummary }),
  offShelf: (id) => http.post(`/admin/inheritors/${id}/off-shelf`),
  setRelations: (id, targets) => http.put(`/admin/inheritors/${id}/relations`, { targets })
}

export const eventApi = {
  page: (params) => http.get('/admin/timeline-events', { params }),
  create: (data) => http.post('/admin/timeline-events', data),
  update: (id, data) => http.put(`/admin/timeline-events/${id}`, data),
  remove: (id) => http.delete(`/admin/timeline-events/${id}`),
  publish: (id, modifySummary) => http.post(`/admin/timeline-events/${id}/publish`, { modifySummary }),
  offShelf: (id) => http.post(`/admin/timeline-events/${id}/off-shelf`)
}

export const termApi = {
  page: (params) => http.get('/admin/terms', { params }),
  create: (data) => http.post('/admin/terms', data),
  update: (id, data) => http.put(`/admin/terms/${id}`, data),
  remove: (id) => http.delete(`/admin/terms/${id}`),
  audit: (id, pass) => http.post(`/admin/terms/${id}/audit`, { pass }),
  publish: (id, modifySummary) => http.post(`/admin/terms/${id}/publish`, { modifySummary }),
  offShelf: (id) => http.post(`/admin/terms/${id}/off-shelf`)
}

export const motifApi = {
  page: (params) => http.get('/admin/motifs', { params }),
  create: (data) => http.post('/admin/motifs', data),
  update: (id, data) => http.put(`/admin/motifs/${id}`, data),
  remove: (id) => http.delete(`/admin/motifs/${id}`),
  publish: (id, modifySummary) => http.post(`/admin/motifs/${id}/publish`, { modifySummary }),
  offShelf: (id) => http.post(`/admin/motifs/${id}/off-shelf`)
}

// 上传接口由 el-upload 直接调用，这里提供地址与请求头
export const uploadConfig = {
  action: 'http://localhost:8080/admin/media/upload',
  headers: () => ({ Authorization: localStorage.getItem('shuqi_admin_token') || '' })
}
