/**
 * 从节点详情（nodeApi.detail 返回的 { node, quote, eventRefs, craftRefs, mediaRefs }）
 * 构建 NodeSaveRequest 全量负载（分支叙事设计稿 §11.2）。
 *
 * 用途：画布上不改内容的结构变更（增删选项、覆盖边）必须走「detail → 改字段 → 全量 update」，
 * 因为后端 update 对 nextNodeId/flowPos 是显式覆盖写入，不带会清空。
 *
 * overrides 覆盖任意字段（如 { choices: [...] }）。
 * 口径与 ChapterEdit.saveNode 一致：TEXT 的独立 media 以正文内嵌图为准（清空 media 引用）。
 */
export function buildPayloadFromDetail(d, overrides = {}) {
  const node = (d && d.node) || {}
  return {
    nodeType: node.nodeType,
    title: node.title,
    subtitle: node.subtitle,
    content: node.content,
    source: node.source,
    media: node.nodeType === 'TEXT'
      ? []
      : (d.mediaRefs || []).map((r) => ({ mediaId: r.mediaId, caption: r.caption })),
    eventIds: (d.eventRefs || []).map((r) => r.eventId),
    inheritorId: d.quote ? d.quote.inheritorId : null,
    quoteText: d.quote ? d.quote.quoteText : null,
    quoteSource: d.quote ? d.quote.source : null,
    authorizationNote: d.quote ? d.quote.authorizationNote : null,
    craftId: d.craftRefs && d.craftRefs.length ? d.craftRefs[0].craftId : null,
    craftStepIds: (d.craftRefs || []).map((r) => r.craftStepId).filter((x) => x),
    // 结构字段必须回传，防止显式 set 清空
    nextNodeId: node.nextNodeId ?? null,
    flowPos: node.flowPos ?? null,
    ...overrides
  }
}
