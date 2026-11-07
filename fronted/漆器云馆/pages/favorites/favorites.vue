<template>
	<view class="page">
		<view class="fav-item" v-for="f in list" :key="f.targetId" @click="goDetail(f.targetId)">
			<image v-if="f.mainImageUrl" :src="full(f.mainImageUrl)" class="fi-img" mode="aspectFill" />
			<view v-else class="fi-img fi-fallback"><text>藏</text></view>
			<view class="fi-info">
				<text class="fi-name">{{ f.name }}</text>
				<text class="fi-time">收藏于 {{ formatTime(f.favoriteTime) }}</text>
			</view>
			<text class="fi-arrow">›</text>
		</view>

		<view class="empty" v-if="loaded && list.length === 0">
			<text class="empty-text">还没有收藏藏品，去藏品页看看吧</text>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { api, fullUrl } from '../../utils/api.js';
	import { anonymousId } from '../../utils/visitor.js';

	const list = ref([]);
	const loaded = ref(false);

	function formatTime(t) {
		return t ? String(t).replace('T', ' ').slice(0, 16) : '';
	}

	async function load() {
		try {
			list.value = (await api.favorites(anonymousId())) || [];
		} catch (e) { /* request 已提示 */ }
		loaded.value = true;
	}

	function goDetail(id) {
		uni.navigateTo({ url: '/pages/artifact/artifact?id=' + id });
	}

	onShow(() => { load(); });
</script>

<style lang="scss" scoped>
	.page { padding: 24rpx 32rpx 50rpx; }
	.fav-item {
		display: flex; align-items: center; gap: 22rpx; background: $paper2;
		border: 1rpx solid $line; border-radius: 18rpx; padding: 20rpx; margin-bottom: 20rpx;
	}
	.fi-img { width: 140rpx; height: 120rpx; border-radius: 14rpx; flex-shrink: 0; background: #e8dfd2; }
	.fi-fallback { display: grid; place-items: center; color: #a07b4d; font-size: 40rpx; }
	.fi-info { flex: 1; }
	.fi-name { display: block; font-size: 28rpx; font-weight: 600; }
	.fi-time { display: block; font-size: 20rpx; color: $muted; margin-top: 8rpx; }
	.fi-arrow { color: #a58b6e; font-size: 34rpx; }
	.empty { text-align: center; padding: 140rpx 0; }
	.empty-text { color: $muted; font-size: 24rpx; }
</style>
