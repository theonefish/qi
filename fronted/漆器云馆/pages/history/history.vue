<template>
	<view class="page">
		<!-- 概览与清空（FR-12 P1） -->
		<view class="top" v-if="list.length">
			<text class="top-count">共 {{ list.length }} 条足迹</text>
			<text class="top-clear" @click="clearAll">清空</text>
		</view>

		<!-- 按日期分组，组内按最近浏览时间倒序 -->
		<view class="group" v-for="g in groups" :key="g.key">
			<text class="group-label">{{ g.label }}</text>
			<view class="h-item" v-for="h in g.items" :key="h.targetType + '-' + h.targetId" @click="open(h)">
				<image v-if="h.imageUrl" :src="full(h.imageUrl)" class="h-img" mode="aspectFill" />
				<view v-else class="h-img h-fallback"><text>{{ typeChar(h.targetType) }}</text></view>
				<view class="h-info">
					<view class="h-top">
						<text class="h-type">{{ typeLabel(h.targetType) }}</text>
						<text class="h-time">{{ timeText(h.visitTime) }}</text>
					</view>
					<text class="h-title">{{ h.title }}</text>
					<text class="h-sub" v-if="h.subtitle">{{ h.subtitle }}</text>
				</view>
				<text class="h-arrow">›</text>
			</view>
		</view>

		<view class="empty" v-if="loaded && list.length === 0">
			<text class="empty-icon">◷</text>
			<text class="empty-text">还没有浏览记录</text>
			<text class="empty-hint">看过的章节、藏品、工艺和传承人会出现在这里</text>
		</view>
	</view>
</template>

<script setup>
	import { ref, computed } from 'vue';
	import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
	import { api, full } from '../../utils/api.js';
	import { anonymousId } from '../../utils/visitor.js';

	const list = ref([]);
	const loaded = ref(false);

	const groups = computed(() => {
		const result = [];
		const index = {};
		for (const item of list.value) {
			const key = dateKey(item.visitTime);
			if (!index[key]) {
				index[key] = { key, label: dayLabel(key), items: [] };
				result.push(index[key]);
			}
			index[key].items.push(item);
		}
		return result;
	});

	function typeLabel(t) {
		return { chapter: '章节', artifact: '藏品', craft: '工艺', inheritor: '传承人' }[t] || '内容';
	}

	function typeChar(t) {
		return { chapter: '章', artifact: '藏', craft: '工', inheritor: '匠' }[t] || '·';
	}

	/** 后端时间为 ISO 字符串，统一取 YYYY-MM-DD 分组 */
	function dateKey(t) {
		return String(t || '').replace('T', ' ').slice(0, 10);
	}

	function dayKey(offset) {
		const d = new Date();
		d.setDate(d.getDate() - offset);
		const m = String(d.getMonth() + 1).padStart(2, '0');
		const day = String(d.getDate()).padStart(2, '0');
		return d.getFullYear() + '-' + m + '-' + day;
	}

	function dayLabel(key) {
		if (key === dayKey(0)) return '今天';
		if (key === dayKey(1)) return '昨天';
		const parts = key.split('-');
		return parts.length === 3 ? Number(parts[1]) + '月' + Number(parts[2]) + '日' : key;
	}

	function timeText(t) {
		const text = String(t || '').replace('T', ' ');
		return text.length >= 16 ? text.slice(11, 16) : text;
	}

	async function load() {
		try {
			list.value = (await api.history(anonymousId(), 50)) || [];
		} catch (e) { /* request 已提示 */ }
		loaded.value = true;
	}

	function open(h) {
		const map = {
			chapter: '/pages/chapter/chapter?id=',
			artifact: '/pages/artifact/artifact?id=',
			craft: '/pages/craft/craft?id=',
			inheritor: '/pages/inheritor/inheritor?id='
		};
		if (map[h.targetType]) {
			uni.navigateTo({ url: map[h.targetType] + h.targetId });
		}
	}

	function clearAll() {
		uni.showModal({
			title: '清空浏览记录',
			content: '清空后不可恢复，是否继续？',
			confirmColor: '#762d25',
			success: async (res) => {
				if (!res.confirm) return;
				try {
					await api.clearHistory(anonymousId());
					list.value = [];
					uni.showToast({ title: '已清空', icon: 'none' });
				} catch (e) { /* request 已提示 */ }
			}
		});
	}

	onShow(() => { load(); });

	onPullDownRefresh(async () => {
		await load();
		uni.stopPullDownRefresh();
	});
</script>

<style lang="scss" scoped>
	.page { padding: 24rpx 32rpx 50rpx; }
	.top {
		display: flex; justify-content: space-between; align-items: center;
		padding: 8rpx 6rpx 18rpx;
	}
	.top-count { font-size: 22rpx; color: $muted; }
	.top-clear { font-size: 24rpx; color: $red; padding: 6rpx 10rpx; }
	.group { margin-bottom: 10rpx; }
	.group-label {
		display: block; font-size: 22rpx; color: $muted;
		margin: 18rpx 4rpx 14rpx; letter-spacing: 2rpx;
	}
	.h-item {
		display: flex; align-items: center; gap: 22rpx; background: $paper2;
		border: 1rpx solid $line; border-radius: 18rpx; padding: 20rpx; margin-bottom: 16rpx;
	}
	.h-img { width: 132rpx; height: 116rpx; border-radius: 14rpx; flex-shrink: 0; background: #e8dfd2; }
	.h-fallback { display: grid; place-items: center; color: #a07b4d; font-size: 40rpx; }
	.h-info { flex: 1; overflow: hidden; }
	.h-top { display: flex; align-items: center; justify-content: space-between; }
	.h-type {
		font-size: 18rpx; color: #8d6a35; background: rgba(181, 138, 74, .14);
		border-radius: 18rpx; padding: 2rpx 14rpx;
	}
	.h-time { font-size: 20rpx; color: #b3a795; }
	.h-title {
		display: block; font-size: 28rpx; font-weight: 600; margin-top: 12rpx;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
	}
	.h-sub {
		display: block; font-size: 21rpx; color: $muted; margin-top: 8rpx;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
	}
	.h-arrow { color: #a58b6e; font-size: 34rpx; }
	.empty { text-align: center; padding: 150rpx 30rpx 0; }
	.empty-icon { display: block; font-size: 72rpx; color: #c9b9a4; }
	.empty-text { display: block; font-size: 26rpx; color: #6b6258; margin-top: 18rpx; }
	.empty-hint { display: block; font-size: 22rpx; color: $muted; margin-top: 12rpx; line-height: 1.8; }
</style>
