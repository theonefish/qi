<template>
	<view class="page" v-if="craft">
		<view class="head">
			<text class="kicker">CRAFT · 工艺档案</text>
			<text class="name">{{ craft.name }}</text>
			<text class="overview" v-if="craft.overview">{{ craft.overview }}</text>
			<text class="feature" v-if="craft.feature">{{ craft.feature }}</text>
		</view>

		<!-- 适用范围说明（FR-10：工序因器而异，不宣称通用） -->
		<view class="note" v-if="craft.scope">
			<text class="note-strong">适用范围：</text>
			<text>{{ craft.scope }}</text>
		</view>

		<!-- 工序步骤（后台配置顺序，FR-10） -->
		<view class="section" v-if="steps.length">
			<text class="s-title">工序流程</text>
			<view class="step-item" v-for="(s, idx) in steps" :key="s.id">
				<view class="st-no"><text>{{ idx + 1 }}</text></view>
				<view class="st-body">
					<text class="st-name">{{ s.name }}</text>
					<image v-if="s.mediaUrl" :src="full(s.mediaUrl)" mode="widthFix" class="st-img" />
					<text class="st-intro" v-if="s.intro">{{ s.intro }}</text>
				</view>
			</view>
		</view>

		<!-- 关联代表性藏品（仅有效目标，FR-10） -->
		<view class="section" v-if="relatedArtifacts.length">
			<text class="s-title">关联藏品</text>
			<view class="rel-card" v-for="a in relatedArtifacts" :key="a.id" @click="goArtifact(a.id)">
				<image v-if="a.mainImageUrl" :src="full(a.mainImageUrl)" class="rel-img" mode="aspectFill" />
				<view v-else class="rel-img rel-fallback"><text>藏</text></view>
				<view class="rel-info">
					<text class="rel-name">{{ a.name }}</text>
					<text class="rel-kicker">查看器物档案</text>
				</view>
				<text class="rel-arrow">›</text>
			</view>
		</view>

		<view class="source" v-if="craft.source"><text>资料来源：{{ craft.source }}</text></view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';
	import { api, fullUrl } from '../../utils/api.js';

	const craft = ref(null);
	const steps = ref([]);
	const relatedArtifacts = ref([]);

	async function load(id) {
		try {
			const detail = await api.craft(id);
			craft.value = detail.craft;
			steps.value = detail.steps || [];
			relatedArtifacts.value = detail.relatedArtifacts || [];
		} catch (e) { /* request 已提示 */ }
	}

	function goArtifact(id) {
		uni.navigateTo({ url: '/pages/artifact/artifact?id=' + id });
	}

	onLoad((options) => {
		if (options.id) load(Number(options.id));
	});
</script>

<style lang="scss" scoped>
	.page { padding-bottom: 60rpx; }
	.head {
		padding: 40rpx; color: #fbf0dd;
		background: radial-gradient(ellipse at 75% 20%, #a75c42 0%, #7e352d 45%, #351b1b 100%);
	}
	.kicker { font-size: 18rpx; letter-spacing: 4rpx; color: #e8c99a; display: block; }
	.name { display: block; font-size: 46rpx; font-weight: 500; letter-spacing: 6rpx; margin: 14rpx 0 16rpx; }
	.overview { display: block; font-size: 25rpx; color: #f1e0ce; line-height: 1.9; }
	.feature { display: block; font-size: 23rpx; color: #d9c1a0; line-height: 1.9; margin-top: 12rpx; }
	.note {
		margin: 26rpx 40rpx 0; background: #eee6da; border-radius: 16rpx;
		padding: 22rpx 26rpx; font-size: 24rpx; color: #685e52; line-height: 1.9;
	}
	.note-strong { color: #5a4030; font-weight: 600; }
	.section { padding: 34rpx 40rpx 0; }
	.s-title {
		display: block; font-size: 30rpx; font-weight: 600; margin-bottom: 20rpx;
		padding-left: 18rpx; border-left: 6rpx solid $gold; line-height: 1.3;
	}
	.step-item { display: flex; gap: 22rpx; padding: 20rpx 0; border-bottom: 1rpx solid $line; }
	.step-item:last-child { border-bottom: none; }
	.st-no {
		width: 56rpx; height: 56rpx; border-radius: 50%; display: grid; place-items: center;
		background: #f1e6d7; color: $red; font-size: 24rpx; flex-shrink: 0;
	}
	.st-body { flex: 1; }
	.st-name { display: block; font-size: 28rpx; font-weight: 600; }
	.st-img { width: 100%; border-radius: 12rpx; margin: 14rpx 0; }
	.st-intro { display: block; font-size: 24rpx; color: #6b6258; line-height: 1.9; margin-top: 8rpx; }
	.rel-card {
		display: flex; align-items: center; gap: 20rpx; background: $paper2;
		border: 1rpx solid #e5d8c7; border-radius: 18rpx; padding: 20rpx; margin-bottom: 18rpx;
	}
	.rel-img { width: 120rpx; height: 110rpx; border-radius: 12rpx; background: #e8dfd2; flex-shrink: 0; }
	.rel-fallback { display: grid; place-items: center; color: #a07b4d; font-size: 36rpx; }
	.rel-info { flex: 1; }
	.rel-name { display: block; font-size: 27rpx; font-weight: 600; }
	.rel-kicker { display: block; font-size: 20rpx; color: #a07b4d; margin-top: 4rpx; }
	.rel-arrow { color: #a58b6e; font-size: 34rpx; }
	.source { margin: 36rpx 40rpx 0; font-size: 20rpx; color: #9b8c7c; }
</style>
