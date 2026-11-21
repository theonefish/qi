<template>
	<view class="page">
		<view class="page-head">
			<text class="page-title">纹饰资料</text>
			<text class="page-sub">漆器纹样背后的文化与寓意</text>
		</view>

		<view class="motif-card" v-for="m in list" :key="m.id">
			<image v-if="m.imageUrl" :src="full(m.imageUrl)" mode="aspectFill" class="motif-img" />
			<view v-else class="motif-img motif-img-fallback"><text>纹</text></view>
			<view class="motif-body">
				<text class="motif-name">{{ m.name }}</text>
				<text class="motif-explain">{{ m.culturalExplain || '暂无文化解释' }}</text>
				<text class="motif-src" v-if="m.source">来源：{{ m.source }}</text>
			</view>
		</view>

		<view class="empty" v-if="!loading && list.length === 0">
			<text class="empty-text">暂无已发布纹饰资料</text>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { api, full } from '../../utils/api.js';

	const list = ref([]);
	const loading = ref(false);

	async function load() {
		loading.value = true;
		try {
			list.value = (await api.motifs()) || [];
		} catch (e) { /* request 已提示 */ } finally {
			loading.value = false;
		}
	}

	onShow(() => {
		load();
	});
</script>

<style lang="scss" scoped>
	.page { padding-bottom: 40rpx; }
	.page-head { padding: 34rpx 40rpx 6rpx; }
	.page-title { display: block; font-size: 40rpx; font-weight: 600; }
	.page-sub { display: block; font-size: 23rpx; color: $muted; margin-top: 8rpx; }
	.motif-card {
		margin: 24rpx 32rpx 0; background: $paper2; border: 1rpx solid $line;
		border-radius: 20rpx; overflow: hidden;
	}
	.motif-img { width: 100%; height: 340rpx; background: #e8dfd2; display: block; }
	.motif-img-fallback {
		display: grid; place-items: center; color: #e8c99a; font-size: 44rpx;
		background: radial-gradient(ellipse at 70% 30%, #a75c42, #351b1b);
	}
	.motif-body { padding: 22rpx 26rpx 26rpx; }
	.motif-name { display: block; font-size: 30rpx; font-weight: 600; }
	.motif-explain { display: block; font-size: 24rpx; color: #655d54; line-height: 1.9; margin-top: 12rpx; }
	.motif-src { display: block; font-size: 20rpx; color: #9b8c7c; margin-top: 12rpx; }
	.empty { text-align: center; padding: 140rpx 0; }
	.empty-text { color: $muted; font-size: 24rpx; }
</style>
