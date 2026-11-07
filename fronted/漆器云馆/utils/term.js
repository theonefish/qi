// 术语轻量标记解析（D-26）：正文中的 [[term:ID]] 或 [[term:ID|显示名]]
// 使用字符串定位解析，避免正则元字符歧义
const MARK_START = '[[term:';

export function parseSegments(text) {
	const segs = [];
	if (!text) return segs;
	let cursor = 0;
	while (true) {
		const start = text.indexOf(MARK_START, cursor);
		if (start < 0) break;
		const end = text.indexOf(']]', start);
		if (end < 0) break;
		if (start > cursor) {
			segs.push({ type: 'text', text: text.slice(cursor, start) });
		}
		const inner = text.slice(start + MARK_START.length, end).trim();
		const sep = inner.indexOf('|');
		const idPart = sep >= 0 ? inner.slice(0, sep).trim() : inner;
		const labelPart = sep >= 0 ? inner.slice(sep + 1).trim() : '';
		const id = Number(idPart);
		if (!Number.isFinite(id) || id <= 0) {
			// 非法标记按普通文本保留，不影响阅读
			segs.push({ type: 'text', text: text.slice(start, end + 2) });
		} else {
			segs.push({ type: 'term', id: id, label: labelPart || '术语' });
		}
		cursor = end + 2;
	}
	if (cursor < text.length) {
		segs.push({ type: 'text', text: text.slice(cursor) });
	}
	return segs;
}

/** 收集节点中出现的全部术语 ID，供批量释义查询 */
export function collectTermIds(nodes) {
	const ids = new Set();
	(nodes || []).forEach((n) => {
		if (n.nodeType === 'TEXT' && n.content) {
			parseSegments(n.content).forEach((s) => {
				if (s.type === 'term') ids.add(s.id);
			});
		}
	});
	return Array.from(ids);
}
