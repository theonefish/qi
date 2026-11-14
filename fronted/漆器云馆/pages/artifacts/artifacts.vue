<template>
	<view class="page">
		<!-- 分类筛选（FR-07：筛选项来自类别字典中实际有已发布藏品的类别） -->
		<scroll-view scroll-x class="cat-bar">
			<view class="cat-inner">
				<view class="cat-chip" :class="{ active: activeCategory === null }" @click="switchCategory(null)">全部</view>
				<view class="cat-chip" v-for="c in categories" :key="c.id"
					:class="{ active: activeCategory === c.id }" @click="switchCategory(c.id)">{{ c.name }}</view>
			</view>
		</scroll-view>

		<!-- 藏品列表（最新添加排序，分批加载，FR-07） -->
		<view class="grid">
			<view class="card" v-for="a in list" :key="a.id" @click="goDetail(a.id)">
				<view class="card-img-wrap">
					<image v-if="a.mainImageUrl" :src="full(a.mainImageUrl)" mode="aspectFill" class="card-img" />
					<view v-else class="card-img-fallback"><text>藏</text></view>
				</view>
				<view class="card-info">
					<text class="card-name">{{ a.name }}</text>
					<text class="card-cat">{{ catName(a.categoryId) }}<template v-if="a.era"> · {{ a.era }}</template></text>
				</view>
			</view>
		</view>

		<view class="list-end" v-if="list.length && finished"><text>已加载全部藏品</text></view>
		<view class="empty" v-if="!loading && list.length === 0">
			<text class="empty-text">{{ activeCategory ? '该分类下暂无藏品' : '暂无已发布藏品' }}</text>
			<view class="empty-action" v-if="activeCategory" @click="switchCategory(null)"><text>清除筛选</text></view>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow, onReachBottom } from '@dcloudio/uni-app';
	import { api, fullUrl, full } from '../../utils/api.js';

	const categories = ref([]);
	const activeCategory = ref(null);
	const list = ref([]);
	const pageNum = ref(1);
	const pageSize = 10;
	const finished = ref(false);
	const loading = ref(false);
	const catMap = {};

	function catName(id) {
		return catMap[id] || '';
	}

	async function loadCategories() {
		try {
			categories.value = (await api.artifactCategories()) || [];
			categories.value.forEach((c) => { catMap[c.id] = c.name; });
		} catch (e) { /* 忽略 */ }
	}

	async function load(reset) {
		if (loading.value) return;
		if (reset) {
			pageNum.value = 1;
			finished.value = false;
			list.value = [];
		}
		if (finished.value) return;
		loading.value = true;
		try {
			const page = await api.artifacts({
				pageNum: pageNum.value,
				pageSize: pageSize,
				categoryId: activeCategory.value
			});
			list.value = list.value.concat(page.list || []);
			if (list.value.length >= (page.total || 0)) finished.value = true;
			pageNum.value += 1;
		} finally {
			loading.value = false;
		}
	}

	function switchCategory(id) {
		if (activeCategory.value === id) return;
		activeCategory.value = id;
		load(true);
	}

	function goDetail(id) {
		uni.navigateTo({ url: '/pages/artifact/artifact?id=' + id });
	}

	/** 切回 tab 时静默刷新已加载的页，避免直接 load(true) 把列表重置到第一页 */
	async function refresh() {
		if (loading.value || !list.value.length) return;
		const categoryId = activeCategory.value;
		const pages = Math.max(1, pageNum.value - 1);
		const keep = list.value.length;
		let stale = false;
		loading.value = true;
		try {
			const next = [];
			let total = 0;
			for (let p = 1; p <= pages; p++) {
				const page = await api.artifacts({ pageNum: p, pageSize, categoryId });
				total = page.total || 0;
				next.push(...(page.list || []));
				if (next.length >= total) break;
			}
			if (activeCategory.value !== categoryId) {
				stale = true;
				return;
			}
			list.value = next.slice(0, Math.min(keep, total));
			finished.value = list.value.length >= total;
		} catch (e) {
			// 刷新失败时保留原列表
		} finally {
			loading.value = false;
			if (stale) load(true);
		}
	}

	onShow(() => {
		if (!list.value.length) {
			loadCategories();
			load(true);
		} else {
			refresh();
		}
	});
	onReachBottom(() => { load(false); });
</script>

<style lang="scss" scoped>
	.page { padding-bottom: 40rpx; }
	.cat-bar {
		position: sticky; top: 0; z-index: 10; background: rgba(246, 241, 232, .97);
		padding: 18rpx 30rpx; border-bottom: 1rpx solid $line; white-space: nowrap;
	}
	.cat-inner { display: flex; gap: 14rpx; }
	.cat-chip {
		display: inline-block; border: 1rpx solid #ddd0be; color: #786b5f; border-radius: 40rpx;
		padding: 10rpx 28rpx; font-size: 24rpx;
	}
	.cat-chip.active { background: $red; border-color: $red; color: #fff5e8; }
	.grid { display: flex; flex-wrap: wrap; justify-content: space-between; padding: 24rpx; }
	.card {
		/* 两列：750rpx 设计宽 - 左右内边距 48 = 702，单卡 340 + 列间 22（固定 rpx，
		   避免 calc 混用 % 与 rpx 在部分微信基础库上失效导致通栏） */
		width: 340rpx; background: $paper2; border: 1rpx solid $line;
		border-radius: 18rpx; overflow: hidden; margin-bottom: 22rpx;
	}
	.card-img-wrap { width: 100%; height: 300rpx; background: #e8dfd2; }
	.card-img { width: 100%; height: 100%; }
	.card-img-fallback {
		width: 100%; height: 100%; display: grid; place-items: center;
		background: radial-gradient(ellipse at 70% 30%, #a75c42, #351b1b); color: #e8c99a; font-size: 44rpx;
	}
	.card-info { padding: 18rpx 22rpx 22rpx; }
	.card-name { display: block; font-size: 27rpx; font-weight: 600; }
	.card-cat { display: block; font-size: 21rpx; color: $muted; margin-top: 6rpx; }
	.list-end { text-align: center; padding: 30rpx 0 10rpx; font-size: 22rpx; color: $muted; }
	.empty { text-align: center; padding: 140rpx 0; }
	.empty-text { color: $muted; font-size: 24rpx; }
	.empty-action {
		display: inline-block; margin-top: 24rpx; border: 1rpx solid $red2; color: $red;
		border-radius: 40rpx; padding: 8rpx 30rpx; font-size: 24rpx;
	}
</style>
