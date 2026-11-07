// 匿名标识（D-05：本地生成、长期保存）与访问会话标识（7.11：每次冷启动重新生成）
import { api } from './api.js';

const ANON_KEY = 'shuqi_anonymous_id';
let visitId = '';
let behaviorQueue = [];
let flushTimer = null;

function uuid() {
	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
		const r = (Math.random() * 16) | 0;
		return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
	});
}

export function initVisitor() {
	if (!uni.getStorageSync(ANON_KEY)) {
		uni.setStorageSync(ANON_KEY, uuid());
	}
	visitId = uuid();
}

export function anonymousId() {
	let id = uni.getStorageSync(ANON_KEY);
	if (!id) {
		id = uuid();
		uni.setStorageSync(ANON_KEY, id);
	}
	return id;
}

export function currentVisitId() {
	return visitId || (visitId = uuid());
}

/**
 * 行为上报（7.11 口径）：攒批 2 秒合并发送，失败静默丢弃，不阻塞阅读。
 * complete_chapter 由后端“完成本章”接口记录，前端不上报。
 */
export function report(eventType, payload = {}) {
	if (eventType === 'complete_chapter') return;
	behaviorQueue.push(Object.assign({ eventType }, payload));
	if (flushTimer) return;
	flushTimer = setTimeout(() => {
		const events = behaviorQueue;
		behaviorQueue = [];
		flushTimer = null;
		if (!events.length) return;
		api.reportBehaviors({ anonymousId: anonymousId(), visitId: currentVisitId(), events }).catch(() => {});
	}, 2000);
}
