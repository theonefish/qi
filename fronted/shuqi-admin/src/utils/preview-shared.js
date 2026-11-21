// 预览/正文中的术语标记解析（D-26）：字符串定位实现，术语以 [[term:ID]] 内嵌于富文本
const MARK_START = '[[term:'
const MARK_END = ']]'

export function splitRichByTerms(html) {
  const segs = []
  if (!html) return segs
  const marks = []
  let cursor = 0
  while (true) {
    const start = html.indexOf(MARK_START, cursor)
    if (start < 0) break
    const end = html.indexOf(MARK_END, start + MARK_START.length)
    if (end < 0) break
    const inner = html.slice(start + MARK_START.length, end).trim()
    const id = Number(inner)
    cursor = end + MARK_END.length
    if (Number.isFinite(id) && id > 0) marks.push({ start: start, end: cursor, id: id })
  }
  let last = 0
  for (const mark of marks) {
    if (mark.start > last) segs.push({ type: 'html', html: html.slice(last, mark.start) })
    segs.push({ type: 'term', id: mark.id })
    last = mark.end
  }
  if (last < html.length) segs.push({ type: 'html', html: html.slice(last) })
  if (!segs.length) segs.push({ type: 'html', html: '' })
  return segs
}

/** 管理端字段（纯文本或 HTML）转预览用 HTML；dark 用于深色底 */
export function previewTextHtml(text, dark) {
  if (!text) return ''
  const t = String(text).trim()
  const color = dark ? '#f1e0ce' : '#645b52'
  if (t.startsWith('<')) {
    return t.split('color:#645b52;').join('color:' + color + ';')
  }
  return t.split('\n').map((line) => line.trim()).filter((line) => line.length > 0).map((line) => {
    const safe = line.split('&').join('&amp;').split('<').join('&lt;').split('>').join('&gt;')
    return '<p style="margin:0 0 10px;font-size:12px;line-height:2;color:' + color + ';">' + safe + '</p>'
  }).join('')
}
