<template>
	<view class="page" v-if="person">
		<view class="head">
			<image v-if="person.photoUrl" :src="full(person.photoUrl)" class="photo" mode="aspectFill" />
			<view v-else class="photo photo-fallback"><text>匠</text></view>
			<view class="head-info">
				<text class="kicker">INHERITOR · 传承人</text>
				<text class="name">{{ person.name }}</text>
				<rich-text class="intro" v-if="person.intro" :nodes="toRichHtml(person.intro, true)"></rich-text>
			</view>
		</view>

		<view class="section" v-if="person.career">
			<text class="s-title">从艺经历</text>
			<rich-text class="rich" :nodes="toRichHtml(person.career)"></rich-text>
		</view>
		<view class="section" v-if="person.story">
			<text class="s-title">传承故事</text>
			<rich-text class="rich" :nodes="toRichHtml(person.story)"></rich-text>
		</view>

		<!-- 代表作品（FR-11：进入藏品详情） -->
		<view class="section" v-if="works.length">
			<text class="s-title">代表作品</text>
			<view class="rel-card" v-for="w in works" :key="w.id" @click="goArtifact(w.id)">
				<image v-if="w.mainImageUrl" :src="full(w.mainImageUrl)" class="rel-img" mode="aspectFill" />
				<view v-else class="rel-img rel-fallback"><text>藏</text></view>
				<view class="rel-info">
					<text class="rel-name">{{ w.name }}</text>
					<text class="rel-kicker">查看器物档案</text>
				</view>
				<text class="rel-arrow">›</text>
			</view>
		</view>

		<!-- 擅长技艺（FR-11：进入工艺详情） -->
		<view class="section" v-if="crafts.length">
			<text class="s-title">擅长技艺</text>
			<view class="craft-chips">
				<view class="craft-chip" v-for="c in crafts" :key="c.id" @click="goCraft(c.id)">
					<text>{{ c.name }}</text>
				</view>
			</view>
		</view>

		<view class="source" v-if="person.source"><text>资料来源：{{ person.source }}</text></view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';
	import { api, fullUrl, full } from '../../utils/api.js';
	import { toRichHtml } from '../../utils/term.js';

	const person = ref(null);
	const works = ref([]);
	const crafts = ref([]);

	async function load(id) {
		try {
			const detail = await api.inheritor(id);
			person.value = detail.inheritor;
			works.value = detail.works || [];
			crafts.value = detail.crafts || [];
		} catch (e) { /* request 已提示 */ }
	}

	function goArtifact(id) {
		uni.navigateTo({ url: '/pages/artifact/artifact?id=' + id });
	}

	function goCraft(id) {
		uni.navigateTo({ url: '/pages/craft/craft?id=' + id });
	}

	onLoad((options) => {
		if (options.id) load(Number(options.id));
	});
</script>

<style lang="scss" scoped>
	.page { padding-bottom: 60rpx; }
	.head {
		padding: 44rpx 40rpx; display: flex; gap: 30rpx; align-items: center; color: #fbf0dd;
		background: linear-gradient(135deg, #3b2924, #754337);
	}
	.photo { width: 150rpx; height: 150rpx; border-radius: 50%; flex-shrink: 0; }
	.photo-fallback { display: grid; place-items: center; background: #bd9360; color: #3a2924; font-size: 52rpx; }
	.head-info { flex: 1; }
	.kicker { font-size: 18rpx; letter-spacing: 3rpx; color: #d9c1a0; display: block; }
	.name { display: block; font-size: 44rpx; font-weight: 500; letter-spacing: 4rpx; margin: 10rpx 0 12rpx; }
	.intro { display: block; font-size: 23rpx; color: #f1e0ce; line-height: 1.8; }
	.section { padding: 32rpx 40rpx 0; }
	.s-title {
		display: block; font-size: 30rpx; font-weight: 600; margin-bottom: 14rpx;
		padding-left: 18rpx; border-left: 6rpx solid $gold; line-height: 1.3;
	}
	.s-text { font-size: 26rpx; color: #645b52; line-height: 2; }
	.rich { display: block; }
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
	.craft-chips { display: flex; flex-wrap: wrap; gap: 16rpx; }
	.craft-chip {
		border: 1rpx solid $red2; color: $red; border-radius: 40rpx;
		padding: 12rpx 30rpx; font-size: 24rpx; background: rgba(156, 73, 57, .05);
	}
	.source { margin: 36rpx 40rpx 0; font-size: 20rpx; color: #9b8c7c; }
</style>
