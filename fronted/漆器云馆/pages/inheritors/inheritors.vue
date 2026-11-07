<template>
	<view class="page">
		<view class="inh-item" v-for="p in list" :key="p.id" @click="goDetail(p.id)">
			<image v-if="p.photoUrl" :src="full(p.photoUrl)" class="ii-photo" mode="aspectFill" />
			<view v-else class="ii-photo ii-fallback"><text>匠</text></view>
			<view class="ii-info">
				<text class="ii-name">{{ p.name }}</text>
				<text class="ii-intro">{{ p.intro || '查看人物介绍' }}</text>
			</view>
			<text class="ii-arrow">›</text>
		</view>

		<view class="empty" v-if="loaded && list.length === 0">
			<text class="empty-text">暂无已发布的传承人</text>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { api, fullUrl } from '../../utils/api.js';

	const list = ref([]);
	const loaded = ref(false);

	async function load() {
		try {
			list.value = (await api.inheritors()) || [];
		} catch (e) { /* request 已提示 */ }
		loaded.value = true;
	}

	function goDetail(id) {
		uni.navigateTo({ url: '/pages/inheritor/inheritor?id=' + id });
	}

	onShow(() => { load(); });
</script>

<style lang="scss" scoped>
	.page { padding: 24rpx 32rpx 50rpx; }
	.inh-item {
		display: flex; align-items: center; gap: 22rpx; background: $paper2;
		border: 1rpx solid $line; border-radius: 18rpx; padding: 22rpx; margin-bottom: 20rpx;
	}
	.ii-photo { width: 120rpx; height: 120rpx; border-radius: 50%; flex-shrink: 0; background: #e8dfd2; }
	.ii-fallback { display: grid; place-items: center; background: #bd9360; color: #3a2924; font-size: 40rpx; }
	.ii-info { flex: 1; overflow: hidden; }
	.ii-name { display: block; font-size: 29rpx; font-weight: 600; }
	.ii-intro {
		display: block; font-size: 22rpx; color: $muted; margin-top: 8rpx;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
	}
	.ii-arrow { color: #a58b6e; font-size: 34rpx; }
	.empty { text-align: center; padding: 140rpx 0; }
	.empty-text { color: $muted; font-size: 24rpx; }
</style>
