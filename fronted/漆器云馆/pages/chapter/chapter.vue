<template>
	<view class="page">
		<!-- 阅读进度（原型 progress-wrap：与全馆完成进度区分，14.5） -->
		<view class="progress-wrap">
			<text class="progress-label">{{ chapter ? chapter.title : '加载中' }}</text>
			<view class="progress-track"><view class="progress-fill" :style="{ width: readPercent + '%' }"></view></view>
			<text class="progress-num">{{ readPercent }}%</text>
		</view>

		<!-- 章节主视觉 -->
		<view class="hero">
			<image v-if="chapter && chapter.coverUrl" :src="full(chapter.coverUrl)" mode="aspectFill" class="hero-img" />
			<view v-else class="hero-fallback">
				<text class="hero-no">{{ chapterNo }}</text>
			</view>
			<view class="hero-copy">
				<text class="eyebrow">SHU QI YUN GUAN · DIGITAL EXHIBITION</text>
				<text class="chapter-no">CHAPTER {{ pad(chapter ? chapter.sortOrder : 0) }}</text>
				<text class="hero-title">{{ chapter ? chapter.title : '' }}</text>
				<text class="hero-sub">{{ chapter ? (chapter.subtitle || chapter.summary || '') : '' }}</text>
			</view>
		</view>

		<!-- 章节摘要（故事引入） -->
		<view class="intro" v-if="chapter && chapter.summary">
			<text class="section-kicker">THE STORY BEGINS</text>
			<text class="intro-text">{{ chapter.summary }}</text>
		</view>

		<!-- 节点快捷导航（原型 node-nav：点击定位，滚动高亮） -->
		<scroll-view scroll-x class="node-nav" v-if="nodes.length">
			<view class="node-nav-inner">
				<view class="chip" v-for="n in nodes" :key="n.id" :class="{ active: currentNodeId === n.id }"
					@click="scrollToNode(n.id)">{{ n.title }}</view>
			</view>
		</scroll-view>

		<!-- 节点内容区（节点按后台顺序渲染，不建独立路由，6.13） -->
		<view class="content">
			<view class="node-section" v-for="(n, idx) in nodes" :key="n.id" :id="'node-' + n.id">
				<view class="section-head">
					<text class="section-index">{{ pad(idx + 1) }}</text>
					<view class="sh-text">
						<text class="node-title">{{ n.title }}</text>
						<text class="node-sub" v-if="n.subtitle">{{ n.subtitle }}</text>
					</view>
				</view>

				<!-- 图文节点 -->
				<template v-if="n.nodeType === 'TEXT'">
					<view v-for="(m, mi) in n.mediaList" :key="mi" class="photo-card">
						<image :src="full(m.url)" mode="widthFix" class="photo-img" @load="measureSoon" />
						<text class="photo-caption" v-if="m.caption">{{ m.caption }}</text>
					</view>
					<view class="para" v-for="(para, pi) in n.paragraphs" :key="pi">
						<text v-for="(seg, si) in para" :key="si"
							:class="{ term: seg.type === 'term' }"
							@click="seg.type === 'term' && openTerm(seg.id)">{{ seg.text }}</text>
					</view>
				</template>

				<!-- 时间轴节点 -->
				<template v-else-if="n.nodeType === 'TIMELINE'">
					<view class="timeline">
						<view class="timeline-item" v-for="ev in n.events" :key="ev.id">
							<text class="tl-time">{{ ev.eventTime }}</text>
							<text class="tl-title">{{ ev.title }}</text>
							<text class="tl-desc" v-if="ev.description">{{ ev.description }}</text>
						</view>
					</view>
				</template>

				<!-- 人物口述节点 -->
				<template v-else-if="n.nodeType === 'QUOTE'">
					<view class="quote-card">
						<text class="quote-mark">“</text>
						<text class="quote-text">{{ n.quoteText }}</text>
						<view class="quote-person">
							<image v-if="n.inheritorPhotoUrl" :src="full(n.inheritorPhotoUrl)" class="avatar" mode="aspectFill" />
							<view v-else class="avatar avatar-fallback"><text class="avatar-char">匠</text></view>
							<view class="qp-text">
								<text class="qp-name">{{ n.inheritorName }}</text>
								<text class="qp-src" v-if="n.quoteSource">{{ n.quoteSource }}</text>
							</view>
						</view>
					</view>
				</template>

				<!-- 工序步骤节点 -->
				<template v-else-if="n.nodeType === 'PROCESS'">
					<view class="step-list">
						<view class="step-card" v-for="(s, si) in n.steps" :key="s.id">
							<view class="step-main" @click="toggleStep(n.id + '-' + s.id)">
								<view class="step-no"><text class="step-no-text">{{ stepLabel(si) }}</text></view>
								<view class="step-text"><text class="step-name">{{ s.name }}</text></view>
								<text class="step-toggle">{{ expanded[n.id + '-' + s.id] ? '－' : '＋' }}</text>
							</view>
							<view class="step-detail" v-if="expanded[n.id + '-' + s.id]">
								<image v-if="s.mediaUrl" :src="full(s.mediaUrl)" mode="widthFix" class="step-img" @load="measureSoon" />
								<text class="step-intro">{{ s.intro }}</text>
							</view>
						</view>
					</view>
					<view class="note" v-if="n.content">
						<text class="note-strong">适用范围：</text>
						<text>{{ n.content }}</text>
					</view>
				</template>
			</view>

			<!-- 关联内容卡（章节级关联，5.4 仅有效目标） -->
			<view class="node-section" v-if="relations.length">
				<view class="section-head">
					<text class="section-index">※</text>
					<view class="sh-text">
						<text class="node-title">相关内容</text>
						<text class="node-sub">点击进入详情，返回时保留本章阅读位置</text>
					</view>
				</view>
				<view class="relation-card" v-for="r in relations" :key="r.targetType + '-' + r.targetId"
					@click="openRelation(r)">
					<image v-if="r.image" :src="full(r.image)" class="relation-thumb" mode="aspectFill" />
					<view v-else class="relation-thumb relation-thumb-fallback"><text class="rt-char">{{ typeChar(r.targetType) }}</text></view>
					<view class="relation-info">
						<text class="r-kicker">{{ typeLabel(r.targetType) }}</text>
						<text class="r-name">{{ r.name }}</text>
					</view>
					<text class="relation-arrow">›</text>
				</view>
			</view>

			<!-- 章节结尾（完成本章） -->
			<view class="finish" id="finish">
				<view class="finish-mark"><text class="fm-char">✓</text></view>
				<text class="finish-title">{{ completed ? '本章参观完成' : '阅读到这里，为本章画上句号' }}</text>
				<text class="finish-desc">{{ finishDesc }}</text>
				<button class="complete-btn" :disabled="completed" @click="onComplete">
					{{ completed ? '本章已完成' : '完成本章' }}
				</button>
				<text class="next-hint" v-if="nextChapter" @click="goNext">下一章 · {{ nextChapter.title }}</text>
				<text class="next-hint" v-else>你已完成本次参观 · 可回到云展馆重读</text>
			</view>
		</view>
		<view class="bottom-space"></view>

		<!-- 底部固定栏（原型 bottom-bar） -->
		<view class="bottom-bar">
			<view class="bottom-progress">
				<text class="b-title">{{ chapter ? chapter.title : '' }}</text>
				<text class="b-sub">{{ currentNodeTitle }}</text>
				<view class="bar"><view class="bar-fill" :style="{ width: readPercent + '%' }"></view></view>
			</view>
			<view class="bottom-next" @click="scrollToEnd"><text class="bn-text">章节结尾 ↓</text></view>
		</view>

		<!-- 术语弹层（FR-06：关闭后停留在原阅读位置） -->
		<view class="overlay" v-if="termPopup" @click="termPopup = null">
			<view class="sheet" @click.stop>
				<view class="sheet-handle"></view>
				<text class="sheet-kicker">CULTURAL KNOWLEDGE</text>
				<text class="sheet-title">{{ termPopup.name }}</text>
				<text class="sheet-body">{{ termPopup.definition }}</text>
				<text class="sheet-src" v-if="termPopup.source">来源：{{ termPopup.source }}</text>
				<button class="sheet-close" @click="termPopup = null">返回章节</button>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { ref, computed } from 'vue';
	import { onLoad, onPageScroll } from '@dcloudio/uni-app';
	import { api, fullUrl } from '../../utils/api.js';
	import { anonymousId, report } from '../../utils/visitor.js';
	import { parseSegments, collectTermIds } from '../../utils/term.js';

	const chapterId = ref(null);
	const chapter = ref(null);
	const nodes = ref([]);
	const relations = ref([]);
	const termMap = {};
	const termPopup = ref(null);

	const currentNodeId = ref(null);
	const nodeTops = {};
	let contentHeight = 0;
	let windowHeight = 600;
	let resumeNodeId = null;
	let lastScrollAt = 0;

	const readPercent = ref(0);
	const expanded = ref({});
	const completed = ref(false);
	const nextChapter = ref(null);
	let loaded = false;

	function pad(n) {
		return String(n || 0).padStart(2, '0');
	}

	const chapterNo = computed(() => {
		if (!chapter.value) return '';
		return (chapter.value.chapterType === 0 ? '序章 · ' : '') + 'CHAPTER ' + pad(chapter.value.sortOrder);
	});

	const currentNodeTitle = computed(() => {
		const n = nodes.value.find((x) => x.id === currentNodeId.value);
		return n ? n.title : '正在阅读';
	});

	const finishDesc = computed(() => {
		if (completed.value) {
			return nextChapter.value
				? '下一步可以进入「' + nextChapter.value.title + '」，继续参观。'
				: '全部章节已参观完成，欢迎从序章再看一遍。';
		}
		return '完成后进度将计入全馆参观进度（序章不计入）。';
	});

	function stepLabel(i) {
		return String.fromCharCode(65 + (i % 26));
	}

	function typeLabel(t) {
		return { artifact: '关联藏品', craft: '关联工艺', inheritor: '关联人物' }[t] || '关联内容';
	}

	function typeChar(t) {
		return { artifact: '藏', craft: '工', inheritor: '匠' }[t] || '·';
	}

	async function load() {
		try {
			const detail = await api.chapter(chapterId.value);
			chapter.value = detail.chapter;

			// 先取术语释义，再构建节点（术语标记显示名优先用术语名称）
			const rawNodes = detail.nodes || [];
			const ids = collectTermIds(rawNodes);
			if (ids.length) {
				try {
					(await api.terms(ids) || []).forEach((t) => { termMap[t.id] = t; });
				} catch (e) { /* 释义缺失时标记仍显示，点击提示暂无释义 */ }
			}
			nodes.value = rawNodes.map((n) => {
				const node = Object.assign({}, n);
				if (n.nodeType === 'TEXT') {
					node.mediaList = n.media || [];
					node.paragraphs = (n.content || '').split(/\n+/)
						.map((p) => parseSegments(p))
						.map((segs) => segs.map((s) => s.type === 'term'
							? Object.assign({}, s, { label: termMap[s.id] ? termMap[s.id].name : s.label })
							: s))
						.filter((p) => p.length);
				}
				return node;
			});
			relations.value = detail.relations || [];

			// 完成状态与章节顺序（供完成反馈与下一章入口）
			const progress = await api.progressList(anonymousId());
			const self = (progress || []).find((p) => p.chapterId === Number(chapterId.value));
			completed.value = !!(self && self.completed);
			const idx = (progress || []).findIndex((p) => p.chapterId === Number(chapterId.value));
			nextChapter.value = idx >= 0 && idx < progress.length - 1 ? progress[idx + 1] : null;

			report('enter_chapter', { chapterId: Number(chapterId.value) });
			loaded = true;
			setTimeout(() => { measure(); tryResume(); }, 500);
		} catch (e) {
			uni.showToast({ title: '章节加载失败', icon: 'none' });
		}
	}

	function tryResume() {
		if (resumeNodeId && nodeTops[resumeNodeId] !== undefined) {
			scrollToNode(resumeNodeId);
			resumeNodeId = null;
		}
	}

	/** 测量各节点顶部位置与内容总高（阅读百分比 = 滚动 / (内容高 - 视口高)，14.5 阅读进度） */
	function measure() {
		const query = uni.createSelectorQuery();
		query.selectAll('.node-section').boundingClientRect();
		query.select('.page').boundingClientRect();
		query.selectViewport().scrollOffset();
		query.exec((res) => {
			const rects = res[0] || [];
			const scrollTop = res[2] ? res[2].scrollTop : 0;
			rects.forEach((r) => {
				if (r && r.id) nodeTops[r.id.replace('node-', '')] = r.top + scrollTop;
			});
			if (res[1]) contentHeight = res[1].height;
		});
	}

	let measureTimer = null;
	function measureSoon() {
		clearTimeout(measureTimer);
		measureTimer = setTimeout(measure, 200);
	}

	onPageScroll((e) => {
		if (!loaded) return;
		const max = Math.max(1, contentHeight - windowHeight);
		readPercent.value = Math.max(0, Math.min(100, Math.round((e.scrollTop / max) * 100)));
		const now = Date.now();
		if (now - lastScrollAt < 180) return;
		lastScrollAt = now;
		let current = null;
		for (const n of nodes.value) {
			const top = nodeTops[n.id];
			if (top !== undefined && e.scrollTop >= top - 150) current = n.id;
		}
		if (current && current !== currentNodeId.value) {
			currentNodeId.value = current;
			const cid = Number(chapterId.value);
			// 行为上报 + 断点续读位置（5.6：浏览到节点时更新）
			report('view_node', { chapterId: cid, nodeId: current });
			api.readPosition({ anonymousId: anonymousId(), chapterId: cid, nodeId: current }).catch(() => {});
		}
	});

	function scrollToNode(id) {
		const doScroll = () => {
			const top = nodeTops[id];
			if (top === undefined) return;
			uni.pageScrollTo({ scrollTop: Math.max(0, top - 130), duration: 300 });
		};
		if (nodeTops[id] === undefined) {
			measure();
			setTimeout(doScroll, 120);
		} else {
			doScroll();
		}
	}

	function scrollToEnd() {
		uni.pageScrollTo({ scrollTop: 999999, duration: 400 });
	}

	function toggleStep(key) {
		expanded.value = Object.assign({}, expanded.value, { [key]: !expanded.value[key] });
	}

	function openTerm(id) {
		termPopup.value = termMap[id] || { name: '术语', definition: '暂无释义数据' };
	}

	function openRelation(r) {
		report('click_relation', { chapterId: Number(chapterId.value), targetType: r.targetType, targetId: r.targetId });
		const map = {
			artifact: '/pages/artifact/artifact?id=',
			craft: '/pages/craft/craft?id=',
			inheritor: '/pages/inheritor/inheritor?id='
		};
		if (map[r.targetType]) {
			uni.navigateTo({ url: map[r.targetType] + r.targetId });
		}
	}

	async function onComplete() {
		if (completed.value) return;
		try {
			const res = await api.completeChapter({ anonymousId: anonymousId(), chapterId: Number(chapterId.value) });
			completed.value = true;
			uni.showToast({ title: res.firstTime ? `已完成 ${res.completedMain}/${res.totalMain} 章` : '本章已标记完成', icon: 'none' });
		} catch (e) { /* toast 已在 request 中提示 */ }
	}

	function goNext() {
		if (!nextChapter.value) return;
		uni.redirectTo({ url: '/pages/chapter/chapter?id=' + nextChapter.value.chapterId });
	}

	onLoad((options) => {
		windowHeight = (uni.getSystemInfoSync().windowHeight) || 600;
		chapterId.value = options.id;
		resumeNodeId = options.nodeId ? Number(options.nodeId) : null;
		load();
	});
</script>

<style lang="scss" scoped>
	.page { padding-bottom: 40rpx; }
	.progress-wrap {
		display: flex; align-items: center; gap: 16rpx;
		padding: 16rpx 34rpx 12rpx; background: $paper;
	}
	.progress-label { font-size: 20rpx; color: $muted; white-space: nowrap; max-width: 220rpx; overflow: hidden; text-overflow: ellipsis; }
	.progress-track { height: 6rpx; background: #e5d9c8; border-radius: 6rpx; overflow: hidden; flex: 1; }
	.progress-fill { height: 100%; background: $red2; transition: width .2s; }
	.progress-num { font-size: 20rpx; color: $red; }

	.hero {
		margin: 8rpx 32rpx 0; border-radius: 24rpx; overflow: hidden; position: relative;
		min-height: 420rpx; background: radial-gradient(ellipse at 75% 28%, #a75c42 0%, #7e352d 39%, #351b1b 100%);
	}
	.hero-img { position: absolute; width: 100%; height: 100%; }
	.hero-fallback { position: absolute; width: 100%; height: 100%; display: grid; place-items: center; }
	.hero-no { color: rgba(232, 201, 154, .5); font-size: 120rpx; font-family: Georgia, serif; }
	.hero-copy { position: absolute; left: 44rpx; right: 44rpx; bottom: 44rpx; color: #fff8ea; }
	.eyebrow { display: block; font-size: 16rpx; letter-spacing: 4rpx; color: #e8c99a; }
	.chapter-no { display: block; font-size: 22rpx; letter-spacing: 4rpx; color: #eacb9c; margin-top: 12rpx; font-family: Georgia, serif; }
	.hero-title { display: block; font-size: 60rpx; font-weight: 500; letter-spacing: 10rpx; margin: 8rpx 0 10rpx; }
	.hero-sub { display: block; font-size: 24rpx; color: #f1e0ce; }

	.intro { padding: 44rpx 44rpx 20rpx; }
	.section-kicker { font-size: 18rpx; color: $gold; letter-spacing: 4rpx; display: block; margin-bottom: 12rpx; }
	.intro-text { font-size: 26rpx; color: #655d54; line-height: 2; }

	.node-nav {
		position: sticky; top: 0; z-index: 15; background: rgba(246, 241, 232, .97);
		padding: 18rpx 30rpx; border-top: 1rpx solid #ece3d6; border-bottom: 1rpx solid #e7ddcf;
		white-space: nowrap;
	}
	.node-nav-inner { display: flex; gap: 14rpx; }
	.chip {
		display: inline-block; border: 1rpx solid #ddd0be; color: #786b5f; border-radius: 40rpx;
		padding: 10rpx 24rpx; font-size: 22rpx; background: transparent;
	}
	.chip.active { background: $red; border-color: $red; color: #fff5e8; }

	.content { padding: 0 40rpx 30rpx; }
	.node-section { padding: 46rpx 4rpx 40rpx; border-bottom: 1rpx solid $line; }
	.section-head { display: flex; gap: 20rpx; margin-bottom: 22rpx; }
	.section-index { font-family: Georgia, serif; font-size: 40rpx; color: #b18c5c; min-width: 56rpx; }
	.sh-text { flex: 1; }
	.node-title { display: block; font-size: 36rpx; font-weight: 600; }
	.node-sub { display: block; font-size: 22rpx; color: $muted; margin-top: 6rpx; }

	.photo-card { border-radius: 16rpx; overflow: hidden; background: #e8dfd2; margin: 24rpx 0; }
	.photo-img { display: block; width: 100%; }
	.photo-caption { padding: 14rpx 20rpx; font-size: 20rpx; color: #85796b; background: #eee6da; display: block; }

	.para { margin: 16rpx 0; }
	.para text { font-size: 26rpx; line-height: 2; color: #645b52; }
	.term { border-bottom: 2rpx dashed $red2; color: $red; }

	.timeline { margin: 20rpx 0 0 18rpx; border-left: 2rpx solid #c9ad83; padding-left: 36rpx; }
	.timeline-item { position: relative; padding-bottom: 32rpx; }
	.timeline-item::before {
		content: ''; position: absolute; left: -46rpx; top: 10rpx; width: 14rpx; height: 14rpx;
		border-radius: 50%; background: $paper; border: 3rpx solid #a77c48;
	}
	.tl-time { display: block; font-size: 22rpx; color: $red; font-weight: 600; }
	.tl-title { display: block; font-size: 28rpx; margin-top: 4rpx; }
	.tl-desc { display: block; font-size: 24rpx; color: #6c6258; margin-top: 8rpx; line-height: 1.9; }

	.quote-card {
		border-radius: 22rpx; background: linear-gradient(135deg, #3b2924, #754337);
		padding: 36rpx; color: #fbf0dd; margin: 24rpx 0;
	}
	.quote-mark { font-family: Georgia, serif; font-size: 64rpx; color: #d0ad77; line-height: 1; }
	.quote-text { display: block; font-size: 30rpx; line-height: 1.9; letter-spacing: 1rpx; margin-top: 6rpx; }
	.quote-person { display: flex; align-items: center; gap: 18rpx; margin-top: 26rpx; padding-top: 22rpx; border-top: 1rpx solid rgba(237, 213, 175, .25); }
	.avatar { width: 66rpx; height: 66rpx; border-radius: 50%; }
	.avatar-fallback { background: #bd9360; display: grid; place-items: center; }
	.avatar-char { color: #3a2924; font-size: 28rpx; }
	.qp-text { flex: 1; }
	.qp-name { display: block; font-size: 24rpx; font-weight: 600; }
	.qp-src { display: block; font-size: 20rpx; color: #d9c1a0; margin-top: 4rpx; }

	.step-list { display: grid; gap: 18rpx; margin: 24rpx 0; }
	.step-card { border: 1rpx solid #e4d8c8; border-radius: 18rpx; background: $paper2; overflow: hidden; }
	.step-main { display: flex; align-items: center; gap: 22rpx; padding: 24rpx; }
	.step-no {
		width: 64rpx; height: 64rpx; border-radius: 50%; display: grid; place-items: center;
		background: #f1e6d7; flex-shrink: 0;
	}
	.step-no-text { color: $red; font-family: Georgia, serif; font-size: 26rpx; }
	.step-text { flex: 1; }
	.step-name { font-size: 28rpx; font-weight: 600; }
	.step-toggle { color: #9b866c; font-size: 34rpx; }
	.step-detail { padding: 0 30rpx 28rpx 110rpx; }
	.step-img { width: 100%; border-radius: 12rpx; margin-bottom: 14rpx; }
	.step-intro { font-size: 24rpx; color: #6b6258; line-height: 1.9; }

	.note {
		background: #eee6da; border-radius: 16rpx; padding: 22rpx 26rpx;
		font-size: 24rpx; color: #685e52; line-height: 1.9; margin: 20rpx 0 0;
	}
	.note-strong { color: #5a4030; font-weight: 600; }

	.relation-card {
		display: flex; align-items: center; gap: 22rpx; padding: 22rpx;
		background: $paper2; border: 1rpx solid #e5d8c7; border-radius: 18rpx; margin: 22rpx 0;
	}
	.relation-thumb { width: 150rpx; height: 130rpx; border-radius: 12rpx; background: #492622; flex-shrink: 0; }
	.relation-thumb-fallback { display: grid; place-items: center; background: #eee0cb; }
	.rt-char { color: #a07b4d; font-size: 40rpx; }
	.relation-info { flex: 1; }
	.r-kicker { font-size: 20rpx; color: #a07b4d; display: block; }
	.r-name { font-size: 28rpx; font-weight: 600; display: block; margin-top: 6rpx; }
	.relation-arrow { color: #a58b6e; font-size: 36rpx; }

	.finish { margin: 30rpx 0 0; padding: 40rpx 30rpx; text-align: center; background: #eee5d8; border-radius: 22rpx; }
	.finish-mark {
		width: 80rpx; height: 80rpx; border-radius: 50%; border: 1rpx solid $gold; color: #94703e;
		display: grid; place-items: center; margin: 0 auto 16rpx;
	}
	.fm-char { font-size: 36rpx; }
	.finish-title { display: block; font-size: 36rpx; font-weight: 500; }
	.finish-desc { display: block; font-size: 22rpx; color: #776b5f; margin: 12rpx 0 26rpx; }
	.complete-btn {
		background: $red; color: #fff5e9; border-radius: 56rpx; font-size: 28rpx;
		letter-spacing: 4rpx; padding: 6rpx 48rpx;
	}
	.complete-btn[disabled] { opacity: .6; color: #fff5e9; background: $red; }
	.next-hint { display: block; font-size: 22rpx; color: #8d7860; margin-top: 22rpx; }

	.bottom-space { height: 140rpx; }
	.bottom-bar {
		position: fixed; bottom: 0; left: 0; right: 0; z-index: 30;
		background: rgba(255, 253, 248, .97); border-top: 1rpx solid #e7ddcf;
		padding: 18rpx 30rpx calc(18rpx + env(safe-area-inset-bottom));
		display: flex; align-items: center; gap: 22rpx;
	}
	.bottom-progress { flex: 1; }
	.b-title { display: block; font-size: 24rpx; font-weight: 600; }
	.b-sub { display: block; font-size: 20rpx; color: $muted; margin-top: 2rpx; }
	.bar { height: 6rpx; background: #e9dfd1; border-radius: 6rpx; margin-top: 10rpx; overflow: hidden; }
	.bar-fill { height: 100%; background: $red2; display: block; }
	.bottom-next { border: 1rpx solid $red; border-radius: 44rpx; padding: 12rpx 26rpx; }
	.bn-text { color: $red; font-size: 22rpx; }

	.overlay {
		position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 60;
		background: rgba(28, 20, 17, .5); display: flex; align-items: flex-end;
	}
	.sheet {
		width: 100%; background: $paper2; border-radius: 32rpx 32rpx 0 0;
		padding: 20rpx 40rpx calc(40rpx + env(safe-area-inset-bottom));
	}
	.sheet-handle { width: 70rpx; height: 8rpx; border-radius: 6rpx; background: #ded2c1; margin: 0 auto 26rpx; }
	.sheet-kicker { font-size: 18rpx; color: $gold; letter-spacing: 3rpx; display: block; }
	.sheet-title { display: block; font-size: 40rpx; font-weight: 500; margin: 8rpx 0 16rpx; }
	.sheet-body { display: block; font-size: 26rpx; line-height: 1.9; color: #6b6157; }
	.sheet-src { display: block; font-size: 20rpx; color: #9b8c7c; margin-top: 16rpx; }
	.sheet-close { margin-top: 30rpx; background: #eee6da; color: #695b4d; border-radius: 44rpx; font-size: 24rpx; }
</style>
