<template>
	<view class="page">
		<!-- 品牌主视觉（参考原型视觉语言：纸质底 + 漆红主色） -->
		<view class="hero">
			<text class="hero-eyebrow">SHU QI YUN GUAN · DIGITAL EXHIBITION</text>
			<text class="hero-title">蜀 漆 云 馆</text>
			<text class="hero-sub">从历史、工艺、器物与传承人四个角度，认识成都漆艺</text>
			<view class="hero-deco"></view>
		</view>

		<!-- 参观入口（FR-01） -->
		<view class="actions">
			<button class="btn-primary" @click="startVisit">{{ lastRead ? '继续参观' : '开始参观' }}</button>
			<text class="action-hint" v-if="lastRead">
				上次读到「{{ lastRead.chapterTitle }}」{{ lastRead.nodeTitle ? ' · ' + lastRead.nodeTitle : '' }}
			</text>
			<text class="action-hint" v-else>按推荐顺序完成全部章节的参观</text>
		</view>

		<!-- 进度卡（FR-05：已完成正章 / 已发布正章） -->
		<view class="progress-card" v-if="totalMain > 0">
			<view class="pc-head">
				<text class="pc-title">参观进度</text>
				<text class="pc-num">{{ completedMain }} / {{ totalMain }} 章</text>
			</view>
			<view class="pc-bar"><view class="pc-fill" :style="{ width: progressPercent + '%' }"></view></view>
		</view>

		<!-- 快捷入口（工艺/传承人为非底部导航页面，首页快捷入口是主要到达路径） -->
		<view class="entry-grid">
			<view class="entry" @click="go('/pages/exhibition/exhibition', true)">
				<text class="entry-icon">馆</text><text class="entry-name">云展馆</text>
			</view>
			<view class="entry" @click="go('/pages/artifacts/artifacts', true)">
				<text class="entry-icon">藏</text><text class="entry-name">藏品</text>
			</view>
			<view class="entry" @click="go('/pages/crafts/crafts')">
				<text class="entry-icon">工</text><text class="entry-name">漆艺工艺</text>
			</view>
			<view class="entry" @click="go('/pages/inheritors/inheritors')">
				<text class="entry-icon">匠</text><text class="entry-name">传承人</text>
			</view>
			<view class="entry" @click="go('/pages/favorites/favorites')">
				<text class="entry-icon">收</text><text class="entry-name">我的收藏</text>
			</view>
			<view class="entry" @click="go('/pages/progress/progress')">
				<text class="entry-icon">程</text><text class="entry-name">参观进度</text>
			</view>
			<view class="entry disabled" @click="comingSoon">
				<text class="entry-icon">饰</text><text class="entry-name">纹饰主题</text>
			</view>
			<view class="entry disabled" @click="comingSoon">
				<text class="entry-icon">声</text><text class="entry-name">语音讲解</text>
			</view>
		</view>

		<!-- 关于展馆（FR-12） -->
		<view class="about">
			<text class="about-title">关于展馆</text>
			<text class="about-text">蜀漆云馆是以叙事性信息架构为核心的成都漆艺数字展馆。所有展品、工艺与人物资料均登记来源并经审核，参观顺序以章节主线组织，序章不计入参观进度。</text>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { api } from '../../utils/api.js';
	import { anonymousId } from '../../utils/visitor.js';

	const lastRead = ref(null);
	const completedMain = ref(0);
	const totalMain = ref(0);
	const progressPercent = ref(0);

	async function load() {
		try {
			const s = await api.summary(anonymousId());
			lastRead.value = s.lastRead;
			completedMain.value = s.completedMain;
			totalMain.value = s.totalMain;
			progressPercent.value = s.progressPercent;
		} catch (e) { /* 首页数据加载失败不阻塞展示 */ }
	}

	function startVisit() {
		if (lastRead.value) {
			const url = '/pages/chapter/chapter?id=' + lastRead.value.chapterId +
				(lastRead.value.nodeId ? '&nodeId=' + lastRead.value.nodeId : '');
			uni.navigateTo({ url });
		} else {
			uni.switchTab({ url: '/pages/exhibition/exhibition' });
		}
	}

	function go(url, isTab) {
		isTab ? uni.switchTab({ url }) : uni.navigateTo({ url });
	}

	function comingSoon() {
		uni.showToast({ title: '该功能规划中（P1）', icon: 'none' });
	}

	onShow(() => {
		load();
	});
</script>

<style lang="scss" scoped>
	.page { padding-bottom: 40rpx; }
	.hero {
		margin: 20rpx 32rpx 0; border-radius: 26rpx; overflow: hidden; position: relative;
		padding: 90rpx 50rpx; color: #fff8ea;
		background: radial-gradient(ellipse at 75% 28%, #a75c42 0%, #7e352d 39%, #351b1b 100%);
	}
	.hero-eyebrow { font-size: 18rpx; letter-spacing: 5rpx; color: #e8c99a; display: block; }
	.hero-title { display: block; font-size: 64rpx; letter-spacing: 14rpx; margin: 14rpx 0 10rpx; font-weight: 500; }
	.hero-sub { display: block; font-size: 24rpx; color: #f1e0ce; }
	.hero-deco {
		position: absolute; right: -60rpx; top: -60rpx; width: 320rpx; height: 320rpx; border-radius: 50%;
		background: radial-gradient(circle, rgba(212, 154, 97, .35), transparent 70%);
	}
	.actions { padding: 44rpx 40rpx 10rpx; text-align: center; }
	.btn-primary {
		background: $red; color: #fff5e9; border-radius: 60rpx; font-size: 30rpx;
		letter-spacing: 6rpx; padding: 10rpx 0; width: 70%;
	}
	.action-hint { display: block; margin-top: 18rpx; font-size: 22rpx; color: $muted; }
	.progress-card {
		margin: 30rpx 32rpx 0; background: $paper2; border: 1rpx solid $line; border-radius: 20rpx; padding: 28rpx 30rpx;
	}
	.pc-head { display: flex; justify-content: space-between; align-items: center; }
	.pc-title { font-size: 28rpx; font-weight: 600; }
	.pc-num { font-size: 26rpx; color: $red; font-weight: 600; }
	.pc-bar { height: 10rpx; background: #e5d9c8; border-radius: 8rpx; margin-top: 20rpx; overflow: hidden; }
	.pc-fill { height: 100%; background: $red2; }
	.entry-grid {
		margin: 30rpx 32rpx 0; background: $paper2; border: 1rpx solid $line; border-radius: 20rpx;
		display: flex; flex-wrap: wrap; padding: 16rpx 0;
	}
	.entry { width: 25%; text-align: center; padding: 24rpx 0; }
	.entry-icon {
		display: inline-grid; place-items: center; width: 84rpx; height: 84rpx; border-radius: 24rpx;
		background: #f1e6d7; color: $red; font-size: 32rpx; line-height: 84rpx;
	}
	.entry-name { display: block; font-size: 22rpx; color: $muted; margin-top: 10rpx; }
	.entry.disabled { opacity: .5; }
	.about { margin: 30rpx 32rpx 0; padding: 26rpx 30rpx; background: #eee6da; border-radius: 18rpx; }
	.about-title { display: block; font-size: 26rpx; font-weight: 600; margin-bottom: 10rpx; }
	.about-text { font-size: 22rpx; color: #685e52; line-height: 1.9; }
</style>
