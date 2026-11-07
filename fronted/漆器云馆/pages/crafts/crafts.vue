<template>
	<view class="page">
		<view class="craft-item" v-for="c in list" :key="c.id" @click="goDetail(c.id)">
			<view class="ci-badge"><text>工</text></view>
			<view class="ci-info">
				<text class="ci-name">{{ c.name }}</text>
				<text class="ci-overview">{{ c.overview || '查看该项工艺的介绍与工序' }}</text>
			</view>
			<text class="ci-arrow">›</text>
		</view>

		<view class="empty" v-if="loaded && list.length === 0">
			<text class="empty-text">暂无已发布工艺</text>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { api } from '../../utils/api.js';

	const list = ref([]);
	const loaded = ref(false);

	async function load() {
		try {
			list.value = (await api.crafts()) || [];
		} catch (e) { /* request 已提示 */ }
		loaded.value = true;
	}

	function goDetail(id) {
		uni.navigateTo({ url: '/pages/craft/craft?id=' + id });
	}

	onShow(() => { load(); });
</script>

<style lang="scss" scoped>
	.page { padding: 24rpx 32rpx 50rpx; }
	.craft-item {
		display: flex; align-items: center; gap: 22rpx; background: $paper2;
		border: 1rpx solid $line; border-radius: 18rpx; padding: 26rpx; margin-bottom: 20rpx;
	}
	.ci-badge {
		width: 80rpx; height: 80rpx; border-radius: 22rpx; display: grid; place-items: center;
		background: radial-gradient(ellipse at 70% 30%, #a75c42, #351b1b); color: #e8c99a; font-size: 34rpx;
		flex-shrink: 0;
	}
	.ci-info { flex: 1; overflow: hidden; }
	.ci-name { display: block; font-size: 29rpx; font-weight: 600; }
	.ci-overview {
		display: block; font-size: 22rpx; color: $muted; margin-top: 8rpx;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
	}
	.ci-arrow { color: #a58b6e; font-size: 34rpx; }
	.empty { text-align: center; padding: 140rpx 0; }
	.empty-text { color: $muted; font-size: 24rpx; }
</style>
