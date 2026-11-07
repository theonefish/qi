<template>
	<view class="page">
		<!-- 总进度（FR-02：已完成 N / 共 M 章，序章不计入分母） -->
		<view class="summary-card">
			<view class="sc-left">
				<text class="sc-num">{{ completedMain }} / {{ totalMain }}</text>
				<text class="sc-label">已完成正章 / 已发布正章</text>
			</view>
			<button class="guide-btn" @click="followOrder">{{ allDone ? '从序章再看' : '按推荐顺序参观' }}</button>
		</view>

		<!-- 章节列表（按后台配置顺序，D-02 数量不写死） -->
		<view class="chapter-card" v-for="(item, idx) in list" :key="item.chapterId"
			:class="{ highlight: highlightId === item.chapterId }" @click="goChapter(item)">
			<view class="cc-cover">
				<image v-if="item.coverUrl" :src="full(item.coverUrl)" mode="aspectFill" class="cc-img" />
				<view v-else class="cc-fallback">
					<text class="cc-no">{{ pad(item.sortOrder) }}</text>
				</view>
			</view>
			<view class="cc-info">
				<view class="cc-tags">
					<text class="tag" :class="item.chapterType === 0 ? 'tag-prologue' : 'tag-main'">
						{{ item.chapterType === 0 ? '序章' : '第' + chapterOrder(idx) + '章' }}
					</text>
					<text class="tag tag-done" v-if="item.completed">✓ 已完成</text>
					<text class="tag tag-read" v-else-if="item.lastReadTime">读到此章</text>
				</view>
				<text class="cc-title">{{ item.title }}</text>
				<text class="cc-sub">{{ item.summary || item.subtitle || '进入章节开始阅读' }}</text>
			</view>
			<text class="cc-arrow">›</text>
		</view>

		<view class="empty" v-if="!loading && list.length === 0">
			<text class="empty-text">暂无已发布章节，请先在后台发布内容</text>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
	import { api, fullUrl } from '../../utils/api.js';
	import { anonymousId, report } from '../../utils/visitor.js';

	const list = ref([]);
	const loading = ref(false);
	const completedMain = ref(0);
	const totalMain = ref(0);
	const highlightId = ref(null);

	function pad(n) {
		return String(n || 0).padStart(2, '0');
	}

	// 正章序号：按列表内正章的先后顺序编号（序章不占章号）
	function chapterOrder(idx) {
		let order = 0;
		for (let i = 0; i <= idx; i++) {
			if (list.value[i].chapterType === 1) order++;
		}
		return order;
	}

	async function load() {
		loading.value = true;
		try {
			const [chapters, progress] = await Promise.all([
				api.chapters(),
				api.progressList(anonymousId())
			]);
			const pMap = {};
			(progress || []).forEach((p) => { pMap[p.chapterId] = p; });
			list.value = (chapters || []).map((c) => Object.assign({}, c, {
				completed: !!(pMap[c.id] && pMap[c.id].completed),
				lastNodeId: pMap[c.id] ? pMap[c.id].lastNodeId : null,
				lastReadTime: pMap[c.id] ? pMap[c.id].lastReadTime : null
			}));
			totalMain.value = list.value.filter((c) => c.chapterType === 1).length;
			completedMain.value = list.value.filter((c) => c.chapterType === 1 && c.completed).length;

			// 回到主线高亮（FR-04）：由详情页设置
			const hl = uni.getStorageSync('shuqi_highlight');
			if (hl) {
				highlightId.value = hl;
				uni.removeStorageSync('shuqi_highlight');
				setTimeout(() => { highlightId.value = null; }, 2600);
			}
		} finally {
			loading.value = false;
		}
	}

	/** 推荐顺序：第一个未完成的正章；全部完成则回序章（FR-02） */
	function followOrder() {
		let target = null;
		for (const c of list.value) {
			if (c.chapterType === 1 && !c.completed) { target = c; break; }
		}
		if (!target && list.value.length) {
			target = list.value.find((c) => c.chapterType === 1 && !c.completed) || list.value[0];
		}
		if (target) goChapter(target);
	}

	function goChapter(item) {
		const url = '/pages/chapter/chapter?id=' + item.chapterId +
			(item.lastNodeId ? '&nodeId=' + item.lastNodeId : '');
		uni.navigateTo({ url });
	}

	onShow(() => { load(); });
	onPullDownRefresh(() => { load().finally(() => uni.stopPullDownRefresh()); });
</script>

<style lang="scss" scoped>
	.page { padding: 24rpx 32rpx 50rpx; }
	.summary-card {
		background: linear-gradient(135deg, #3b2924, #754337); color: #fbf0dd;
		border-radius: 22rpx; padding: 34rpx 36rpx; display: flex; align-items: center; justify-content: space-between;
	}
	.sc-num { display: block; font-size: 52rpx; font-weight: 600; font-variant-numeric: tabular-nums; }
	.sc-label { display: block; font-size: 20rpx; color: #d9c1a0; margin-top: 6rpx; }
	.guide-btn {
		background: rgba(255, 245, 232, .14); border: 1rpx solid rgba(237, 213, 175, .5);
		color: #fbf0dd; font-size: 24rpx; border-radius: 40rpx; padding: 4rpx 26rpx; line-height: 2;
	}
	.chapter-card {
		margin-top: 26rpx; background: $paper2; border: 1rpx solid $line; border-radius: 20rpx;
		display: flex; align-items: center; padding: 22rpx; position: relative;
	}
	.chapter-card.highlight { border-color: $red2; box-shadow: 0 0 0 4rpx rgba(156, 73, 57, .18); }
	.cc-cover { width: 170rpx; height: 130rpx; border-radius: 14rpx; overflow: hidden; flex-shrink: 0; background: #e8dfd2; }
	.cc-img { width: 100%; height: 100%; }
	.cc-fallback {
		width: 100%; height: 100%; display: grid; place-items: center;
		background: radial-gradient(ellipse at 70% 30%, #a75c42, #351b1b);
	}
	.cc-no { color: #e8c99a; font-size: 40rpx; font-family: Georgia, serif; }
	.cc-info { flex: 1; margin: 0 18rpx; overflow: hidden; }
	.cc-tags { display: flex; gap: 10rpx; margin-bottom: 8rpx; }
	.tag { font-size: 18rpx; padding: 2rpx 14rpx; border-radius: 20rpx; }
	.tag-prologue { background: #e9e0cf; color: #6e5f4e; }
	.tag-main { background: rgba(118, 45, 37, .1); color: $red; }
	.tag-done { background: rgba(95, 118, 94, .14); color: $green; }
	.tag-read { background: rgba(181, 138, 74, .14); color: #8d6a35; }
	.cc-title { display: block; font-size: 30rpx; font-weight: 600; }
	.cc-sub { display: block; font-size: 22rpx; color: $muted; margin-top: 6rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	.cc-arrow { color: #a58b6e; font-size: 34rpx; }
	.empty { text-align: center; padding: 120rpx 0; }
	.empty-text { color: $muted; font-size: 24rpx; }
</style>
