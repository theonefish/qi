// 后端接口封装：统一 BASE_URL、Result 解包、/files 文件地址补全
// 接口清单见后端工程 qi/docs/API.md
export const BASE_URL = 'http://localhost:8080';

/** 相对文件地址（/files/...）转完整地址 */
export function fullUrl(path) {
	if (!path) return '';
	return path.indexOf('http') === 0 ? path : BASE_URL + path;
}

/** 页面模板通用短名：存量页面模板多写 full(...)，此别名统一指向 fullUrl（勿删） */
export const full = fullUrl;

function qs(params) {
	if (!params) return '';
	const parts = [];
	for (const k in params) {
		if (params[k] !== undefined && params[k] !== null && params[k] !== '') {
			parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k]));
		}
	}
	return parts.length ? '?' + parts.join('&') : '';
}

function request(path, options = {}) {
	return new Promise((resolve, reject) => {
		uni.request({
			url: BASE_URL + path,
			method: options.method || 'GET',
			data: options.data,
			success: (res) => {
				const body = res.data;
				if (body && body.code === 0) {
					resolve(body.data);
				} else {
					const msg = (body && body.msg) || '请求失败';
					uni.showToast({ title: msg, icon: 'none' });
					reject(new Error(msg));
				}
			},
			fail: () => {
				uni.showToast({ title: '网络连接失败，请确认后端已启动', icon: 'none' });
				reject(new Error('network error'));
			}
		});
	});
}

export const api = {
	// 章节（FR-02 / FR-03）
	chapters: () => request('/app/chapters'),
	chapter: (id) => request('/app/chapters/' + id),
	// 藏品（FR-07 / FR-08）
	artifacts: (params) => request('/app/artifacts' + qs(params)),
	artifact: (id) => request('/app/artifacts/' + id),
	artifactCategories: () => request('/app/artifacts/categories'),
	// 工艺（FR-10）与传承人（FR-11）
	crafts: () => request('/app/crafts'),
	craft: (id) => request('/app/crafts/' + id),
	inheritors: () => request('/app/inheritors'),
	inheritor: (id) => request('/app/inheritors/' + id),
	// 纹饰资料（FR 补充：纹饰主题页）
	motifs: () => request('/app/motifs'),
	// 术语批量释义（FR-06，正文 [[term:ID]] 标记解析后调用）
	terms: (ids) => request('/app/terms' + qs({ ids: ids.join(',') })),
	// 访客：进度 / 断点续读 / 收藏 / 行为上报
	summary: (anonymousId) => request('/app/visitor/summary' + qs({ anonymousId })),
	progressList: (anonymousId) => request('/app/visitor/progress' + qs({ anonymousId })),
	readPosition: (data) => request('/app/visitor/progress/read', { method: 'POST', data }),
	completeChapter: (data) => request('/app/visitor/progress/complete', { method: 'POST', data }),
	favorites: (anonymousId) => request('/app/visitor/favorites' + qs({ anonymousId })),
	addFavorite: (data) => request('/app/visitor/favorites', { method: 'POST', data }),
	removeFavorite: (params) => request('/app/visitor/favorites' + qs(params), { method: 'DELETE' }),
	isFavorite: (anonymousId, targetType, targetId) =>
		request('/app/visitor/favorites/exists' + qs({ anonymousId, targetType, targetId })),
	reportBehaviors: (data) => request('/app/visitor/behaviors', { method: 'POST', data })
};
