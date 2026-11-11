<template>
	<view class="page" v-if="artifact">
		<!-- 主图（多角度图片 P2，本期以高清主图 + 局部观察为准） -->
		<view class="main-img-wrap">
			<image v-if="artifact.mainImageUrl" :src="full(artifact.mainImageUrl)" mode="aspectFill" class="main-img" />
			<view v-else class="main-img-fallback"><text>藏</text></view>
		</view>

		<view class="head">
			<text class="name">{{ artifact.name }}</text>
			<view class="meta-chips">
				<text class="chip" v-if="categoryName">{{ categoryName }}</text>
				<text class="chip" v-if="artifact.era">{{ artifact.era }}</text>
				<text class="chip" v-if="artifact.material">{{ artifact.material }}</text>
			</view>
			<!-- 收藏（FR-09：仅藏品，状态与我的收藏一致） -->
			<view class="fav-btn" :class="{ active: favorited }" @click="toggleFavorite">
				<text>{{ favorited ? '♥ 已收藏' : '♡ 收藏' }}</text>
			</view>
		</view>

		<!-- 档案模块：无数据整块隐藏（FR-08）；富文本图文混排 -->
		<view class="section" v-if="artifact.craftDesc">
			<text class="s-title">工艺说明</text>
			<rich-text class="rich" :nodes="toRichHtml(artifact.craftDesc)"></rich-text>
		</view>
		<view class="section" v-if="artifact.story">
			<text class="s-title">文化故事</text>
			<rich-text class="rich" :nodes="toRichHtml(artifact.story)"></rich-text>
		</view>
		<view class="section" v-if="artifact.motifMeaning">
			<text class="s-title">纹饰寓意</text>
			<rich-text class="rich" :nodes="toRichHtml(artifact.motifMeaning)"></rich-text>
		</view>

		<!-- 关联内容（5.4：仅有效目标） -->
		<view class="section" v-if="relations.length">
			<text class="s-title">相关内容</text>
			<view class="relation-card" v-for="r in relations" :key="r.targetType + '-' + r.targetId"
				@click="openRelation(r)">
				<image v-if="r.image" :src="full(r.image)" class="rel-thumb" mode="aspectFill" />
				<view v-else class="rel-thumb rel-fallback"><text>{{ typeChar(r.targetType) }}</text></view>
				<view class="rel-info">
					<text class="rel-kicker">{{ typeLabel(r.targetType) }}</text>
					<text class="rel-name">{{ r.name }}</text>
				</view>
				<text class="rel-arrow">›</text>
			</view>
		</view>

		<!-- 回到主线（FR-04：定位关联章节并高亮） -->
		<view class="back-main" v-if="chapterRelation" @click="backToMain">
			<text>回到主线 · {{ chapterRelation.name }}</text>
		</view>

		<view class="source" v-if="artifact.source">
			<text>资料来源：{{ artifact.source }}<template v-if="artifact.authorizationNote">｜{{ artifact.authorizationNote }}</template></text>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';
	import { api, fullUrl, full } from '../../utils/api.js';
	import { anonymousId, report } from '../../utils/visitor.js';
	import { toRichHtml } from '../../utils/term.js';

	const artifact = ref(null);
	const categoryName = ref('');
	const relations = ref([]);
	const favorited = ref(false);
	const chapterRelation = ref(null);

	function typeLabel(t) {
		return { chapter: '关联章节', craft: '相关工艺', inheritor: '相关人物' }[t] || '关联内容';
	}

	function typeChar(t) {
		return { chapter: '章', craft: '工', inheritor: '匠' }[t] || '·';
	}

	async function load(id) {
		try {
			const detail = await api.artifact(id);
			artifact.value = detail.artifact;
			categoryName.value = detail.categoryName || '';
			relations.value = detail.relations || [];
			chapterRelation.value = relations.value.find((r) => r.targetType === 'chapter') || null;
			// 浏览记录（FR-12 P1）：进入详情即记录，重复浏览只刷新时间
			api.recordHistory({ anonymousId: anonymousId(), targetType: 'artifact', targetId: id }).catch(() => {});
			favorited.value = await api.isFavorite(anonymousId(), 'artifact', id);
		} catch (e) { /* request 已提示 */ }
	}

	async function toggleFavorite() {
		const id = artifact.value.id;
		try {
			if (favorited.value) {
				await api.removeFavorite({ anonymousId: anonymousId(), targetType: 'artifact', targetId: id });
				favorited.value = false;
				uni.showToast({ title: '已取消收藏', icon: 'none' });
			} else {
				await api.addFavorite({ anonymousId: anonymousId(), targetType: 'artifact', targetId: id });
				favorited.value = true;
				uni.showToast({ title: '已加入收藏', icon: 'none' });
			}
		} catch (e) { /* request 已提示 */ }
	}

	function openRelation(r) {
		// 从藏品发起的跳转不携带章节上下文，7.11 的 click_relation 统计以章节内跳转为准
		const map = {
			chapter: '/pages/chapter/chapter?id=',
			craft: '/pages/craft/craft?id=',
			inheritor: '/pages/inheritor/inheritor?id='
		};
		if (map[r.targetType]) uni.navigateTo({ url: map[r.targetType] + r.targetId });
	}

	/** 回到主线（FR-04）：进入云展馆并高亮关联章节 */
	function backToMain() {
		if (!chapterRelation.value) return;
		report('back_main', { chapterId: chapterRelation.value.targetId });
		uni.setStorageSync('shuqi_highlight', chapterRelation.value.targetId);
		uni.switchTab({ url: '/pages/exhibition/exhibition' });
	}

	onLoad((options) => {
		if (options.id) load(Number(options.id));
	});
</script>

<style lang="scss" scoped>
	.page { padding-bottom: 60rpx; }
	.main-img-wrap { width: 100%; height: 560rpx; background: #e8dfd2; }
	.main-img { width: 100%; height: 100%; }
	.main-img-fallback {
		width: 100%; height: 100%; display: grid; place-items: center;
		background: radial-gradient(ellipse at 70% 30%, #a75c42, #351b1b); color: #e8c99a; font-size: 80rpx;
	}
	.head { padding: 30rpx 40rpx 6rpx; position: relative; }
	.name { font-size: 40rpx; font-weight: 600; display: block; padding-right: 160rpx; }
	.meta-chips { display: flex; gap: 12rpx; margin-top: 16rpx; flex-wrap: wrap; }
	.chip { font-size: 20rpx; background: rgba(118, 45, 37, .08); color: $red; border-radius: 20rpx; padding: 4rpx 18rpx; }
	.fav-btn {
		position: absolute; right: 40rpx; top: 30rpx; border: 1rpx solid #ddd0be; border-radius: 40rpx;
		padding: 10rpx 24rpx; font-size: 24rpx; color: $muted;
	}
	.fav-btn.active { border-color: $red2; color: $red; background: rgba(156, 73, 57, .07); }
	.section { padding: 30rpx 40rpx 0; }
	.s-title {
		display: block; font-size: 30rpx; font-weight: 600; margin-bottom: 14rpx;
		padding-left: 18rpx; border-left: 6rpx solid $gold; line-height: 1.3;
	}
	.s-text { font-size: 26rpx; color: #645b52; line-height: 2; }
	.rich { display: block; }
	.relation-card {
		display: flex; align-items: center; gap: 20rpx; background: $paper2;
		border: 1rpx solid #e5d8c7; border-radius: 18rpx; padding: 20rpx; margin-top: 18rpx;
	}
	.rel-thumb { width: 120rpx; height: 110rpx; border-radius: 12rpx; background: #eee0cb; flex-shrink: 0; }
	.rel-fallback { display: grid; place-items: center; color: #a07b4d; font-size: 36rpx; }
	.rel-info { flex: 1; }
	.rel-kicker { font-size: 20rpx; color: #a07b4d; display: block; }
	.rel-name { font-size: 27rpx; font-weight: 600; display: block; margin-top: 4rpx; }
	.rel-arrow { color: #a58b6e; font-size: 34rpx; }
	.back-main {
		margin: 36rpx 40rpx 0; text-align: center; border: 1rpx solid $red2; color: $red;
		border-radius: 44rpx; padding: 16rpx 0; font-size: 26rpx;
	}
	.source { margin: 36rpx 40rpx 0; font-size: 20rpx; color: #9b8c7c; line-height: 1.8; }
</style>
