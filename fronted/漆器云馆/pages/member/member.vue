<template>
	<view class="page">
		<!-- 进度与收藏概览（FR-12）：点击进入参观进度页 -->
		<view class="head-card" @click="go('/pages/progress/progress')">
			<text class="head-title">参观进度</text>
			<view class="pc-bar"><view class="pc-fill" :style="{ width: progressPercent + '%' }"></view></view>
			<view class="head-row">
				<text class="head-num">{{ totalMain === 0 ? '内容准备中' : completedMain + ' / ' + totalMain + ' 章' }}</text>
				<text class="head-fav">收藏 {{ favoriteCount }} 件 ›</text>
			</view>
		</view>

		<view class="menu">
			<view class="menu-item" @click="go('/pages/favorites/favorites')">
				<text class="mi-name">我的收藏</text><text class="mi-arrow">›</text>
			</view>
			<view class="menu-item" @click="go('/pages/progress/progress')">
				<text class="mi-name">参观进度</text><text class="mi-arrow">›</text>
			</view>
			<view class="menu-item" @click="go('/pages/history/history')">
				<text class="mi-name">浏览记录</text><text class="mi-arrow">›</text>
			</view>
			<!-- P1 功能按“控制显示”原则灰显（FR-12） -->
			<view class="menu-item disabled" @click="comingSoon('意见反馈')">
				<view class="mi-left"><text class="mi-name">意见反馈</text><text class="mi-tag">即将上线</text></view>
				<text class="mi-arrow">›</text>
			</view>
		</view>

		<!-- 关于展馆（FR-12：展馆介绍、资料来源和版权信息） -->
		<view class="about">
			<text class="about-title">关于展馆</text>
			<text class="about-text">“蜀漆云馆”是以叙事性信息架构为核心的成都漆艺数字展馆。展品、工艺与人物资料均登记来源并经审核后发布；参观顺序按章节主线组织，序章不计入进度。</text>

			<text class="about-title" style="margin-top: 26rpx;">资料来源与版权</text>
			<text class="about-text">本馆展示的藏品档案、工艺说明、历史事件与人物口述，均在后台登记资料来源与审核记录后发布；图片等媒体素材的使用依据与授权信息随内容一并管理，未经核实或授权的资料不对外展示。如对内容或素材使用有疑问，可通过后续上线的意见反馈渠道与我们联系。</text>

			<text class="about-title" style="margin-top: 26rpx;">数据与隐私说明</text>
			<text class="about-text">为支持断点续读、收藏、浏览记录与参观行为统计，系统在本设备生成了一个匿名标识并在服务端保存您的阅读位置、浏览记录与收藏（不涉及手机号等个人信息）。清除小程序数据后，原进度、浏览记录与收藏将无法找回。</text>
		</view>

		<text class="version">蜀漆云馆 v1.0.0</text>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { api } from '../../utils/api.js';
	import { anonymousId } from '../../utils/visitor.js';

	const completedMain = ref(0);
	const totalMain = ref(0);
	const progressPercent = ref(0);
	const favoriteCount = ref(0);

	async function load() {
		try {
			const s = await api.summary(anonymousId());
			completedMain.value = s.completedMain;
			totalMain.value = s.totalMain;
			progressPercent.value = s.progressPercent;
			favoriteCount.value = s.favoriteCount;
		} catch (e) { /* 忽略 */ }
	}

	function go(url) {
		uni.navigateTo({ url });
	}

	function comingSoon(name) {
		uni.showToast({ title: name + '功能规划中（P1）', icon: 'none' });
	}

	onShow(() => { load(); });
</script>

<style lang="scss" scoped>
	.page { padding: 24rpx 32rpx 50rpx; }
	.head-card {
		background: linear-gradient(135deg, #3b2924, #754337); color: #fbf0dd;
		border-radius: 22rpx; padding: 38rpx 36rpx;
	}
	.head-title { font-size: 26rpx; color: #d9c1a0; }
	.pc-bar { height: 10rpx; background: rgba(255, 245, 232, .18); border-radius: 8rpx; margin: 22rpx 0; overflow: hidden; }
	.pc-fill { height: 100%; background: #d0a36b; }
	.head-row { display: flex; justify-content: space-between; }
	.head-num { font-size: 32rpx; font-weight: 600; }
	.head-fav { font-size: 24rpx; color: #d9c1a0; }
	.menu {
		margin-top: 26rpx; background: $paper2; border: 1rpx solid $line; border-radius: 20rpx; overflow: hidden;
	}
	.menu-item {
		display: flex; justify-content: space-between; align-items: center;
		padding: 30rpx 32rpx; border-bottom: 1rpx solid $line;
	}
	.menu-item:last-child { border-bottom: none; }
	.menu-item.disabled { opacity: .55; }
	.mi-left { display: flex; align-items: center; gap: 14rpx; }
	.mi-name { font-size: 28rpx; }
	.mi-tag {
		font-size: 18rpx; color: #8d6a35; background: rgba(181, 138, 74, .14);
		border-radius: 18rpx; padding: 2rpx 14rpx;
	}
	.mi-arrow { color: #a58b6e; font-size: 32rpx; }
	.about { margin-top: 26rpx; padding: 28rpx 30rpx; background: #eee6da; border-radius: 18rpx; }
	.about-title { display: block; font-size: 26rpx; font-weight: 600; margin-bottom: 10rpx; }
	.about-text { font-size: 22rpx; color: #685e52; line-height: 1.9; }
	.version { display: block; text-align: center; margin-top: 30rpx; font-size: 20rpx; color: #b3a795; }
</style>
