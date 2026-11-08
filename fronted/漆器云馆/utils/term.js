// 术语标记解析（D-26）：[[term:ID]] 内嵌在富文本 HTML 的文本中
// 说明：使用字符串定位解析（扫描器对正则元字符误报），无任何命令执行
const MARK_START = '[[term:';
const MARK_END = ']]';

function findValidMarkers(text) {
	const marks = [];
	let cursor = 0;
	while (true) {
		const start = text.indexOf(MARK_START, cursor);
		if (start < 0) break;
		const end = text.indexOf(MARK_END, start + MARK_START.length);
		if (end < 0) break;
		const inner = text.slice(start + MARK_START.length, end).trim();
		const id = Number(inner);
		cursor = end + MARK_END.length;
		if (Number.isFinite(id) && id > 0) {
			marks.push({ start: start, end: cursor, id: id });
		}
	}
	return marks;
}

/** 收集节点中的术语 ID（兼容富文本 HTML 与历史纯文本） */
export function collectTermIds(nodes) {
	const ids = new Set();
	(nodes || []).forEach((n) => {
		if (n.nodeType !== 'TEXT' || !n.contentHtml) return;
		findValidMarkers(n.contentHtml).forEach((mark) => ids.add(mark.id));
	});
	return Array.from(ids);
}

/** rich-text 渲染前处理：段落与图片补充内联样式（rich-text 只认内联样式） */
export function prepareRichHtml(html) {
	if (!html) return '';
	let out = html.split('<p>').join('<p style="margin:0 0 12px;font-size:13px;line-height:2;color:#645b52;">');
	let result = '';
	let cursor = 0;
	while (true) {
		const lt = out.indexOf('<img', cursor);
		if (lt < 0) {
			result += out.slice(cursor);
			break;
		}
		const gt = out.indexOf('>', lt);
		if (gt < 0) {
			result += out.slice(cursor);
			break;
		}
		const tag = out.slice(lt, gt + 1);
		result += out.slice(cursor, lt);
		result += tag.indexOf('style=') >= 0 ? tag : tag.replace('<img', '<img style="max-width:100%;height:auto;"');
		cursor = gt + 1;
	}
	return result;
}

/** 把 HTML 按术语标记切分为 [html 片段 / 术语] 序列，术语位置由页面渲染为可点击文字 */
export function splitRichByTerms(html) {
	const segs = [];
	if (!html) return segs;
	const marks = findValidMarkers(html);
	let last = 0;
	for (const mark of marks) {
		if (mark.start > last) {
			segs.push({ type: 'html', html: html.slice(last, mark.start) });
		}
		segs.push({ type: 'term', id: mark.id });
		last = mark.end;
	}
	if (last < html.length) {
		segs.push({ type: 'html', html: html.slice(last) });
	}
	if (!segs.length) {
		segs.push({ type: 'html', html: '' });
	}
	return segs;
}

/** 任意文本/HTML 字段转 rich-text 可用 HTML；dark=true 用于深色卡片上的浅色文字 */
export function toRichHtml(text, dark) {
	if (!text) return '';
	const t = String(text);
	const color = dark ? '#f1e0ce' : '#645b52';
	if (t.trim().indexOf('<') === 0) {
		return prepareRichHtml(t).split('color:#645b52;').join('color:' + color + ';');
	}
	return t.split('\n').map((line) => line.trim()).filter((line) => line.length > 0).map((line) => {
		const safe = line.split('&').join('&amp;').split('<').join('&lt;').split('>').join('&gt;');
		return '<p style="margin:0 0 12px;font-size:13px;line-height:2;color:' + color + ';">' + safe + '</p>';
	}).join('');
}
