<script>
	import { initVisitor } from './utils/visitor.js';

	export default {
		onLaunch: function() {
			// 初始化匿名标识（D-05）与访问会话标识（7.11 口径）
			initVisitor();
		},
		onShow: function() {},
		onHide: function() {}
	}
</script>

<style>
	/* 全局基础样式 */
	page {
		background: #f6f1e8;
		color: #28231f;
		font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
		line-height: 1.65;
	}
	button { background: transparent; }
	button::after { border: none; }
</style>
