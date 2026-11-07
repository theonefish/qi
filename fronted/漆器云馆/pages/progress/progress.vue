<template>
	<view class="page">
		<view class="summary">
			<text class="sum-num">{{ completedMain }} / {{ totalMain }} 章</text>
			<text class="sum-label">参观进度（序章不计入）</text>
		</view>

		<view class="ch-item" v-for="(item, idx) in list" :key="item.chapterId" @click="goRead(item, idx)">
			<view class="ch-badge" :class="item.completed ? 'done' : ''">
				<text>{{ item.completed ? '✓' : (item.chapterType === 0 ? '序' : idx) }}</text>
			</view>
			<view class="ch-info">
				<text class="ch-title">{{ item.title }}</text>
				<text class="ch-sub">
					{{ item.completed ? '已完成' : (item.lastNodeId ? '读到此章 · 点击继续' : '未开始') }}
				</text>
			</view>
			<text class="ch-arrow">›</text>
		</view>

		<view class="empty" v-if="loaded && list.length === 0">
			<text class="empty-text">暂无已发布章节</text>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { api } from '../../utils/api.js';
	import { anonymousId } from '../../utils/visitor.js';

	const list = ref([]);
	const loaded = ref(false);
	const completedMain = ref(0);
	const totalMain = ref(0);

	async function load() {
		try {
			list.value = (await api.progressList(anonymousId())) || [];
			totalMain.value = list.value.filter((c) => c.chapterType === 1).length;
			completedMain.value = list.value.filter((c) => c.chapterType === 1 && c.completed).length;
		} catch (e) { /* request 已提示 */ }
		loaded.value = true;
	}

	/** 从上次位置继续（5.6）：带 lastNodeId 定位到节点 */
	function goRead(item, idx) {
		const url = '/pages/chapter/chapter?id=' + item.chapterId +
			(item.lastNodeId ? '&nodeId=' + item.lastNodeId : '');
		uni.navigateTo({ url });
	}

	onShow(() => { load(); });
</script>

<style lang="scss" scoped>
	.page { padding: 24rpx 32rpx 50rpx; }
	.summary {
		background: linear-gradient(135deg, #3b2924, #754337); color: #fbf0dd;
		border-radius: 22rpx; padding: 36rpx; text-align: center;
	}
	.sum-num { display: block; font-size: 56rpx; font-weight: 600; }
	.sum-label { display: block; font-size: 22rpx; color: #d9c1a0; margin-top: 8rpx; }
	.ch-item {
		display: flex; align-items: center; gap: 22rpx; background: $paper2;
		border: 1rpx solid $line; border-radius: 18rpx; padding: 24rpx; margin-top: 20rpx;
	}
	.ch-badge {
		width: 70rpx; height: 70rpx; border-radius: 50%; display: grid; place-items: center;
		background: #f1e6d7; color: $red; font-size: 26rpx; flex-shrink: 0;
	}
	.ch-badge.done { background: rgba(95, 118, 94, .15); color: $green; }
	.ch-info { flex: 1; }
	.ch-title { display: block; font-size: 28rpx; font-weight: 600; }
	.ch-sub { display: block; font-size: 21rpx; color: $muted; margin-top: 6rpx; }
	.ch-arrow { color: #a58b6e; font-size: 34rpx; }
	.empty { text-align: center; padding: 140rpx 0; }
	.empty-text { color: $muted; font-size: 24rpx; }
</style>
