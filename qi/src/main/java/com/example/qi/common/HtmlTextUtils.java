package com.example.qi.common;

/**
 * 富文本工具：长文本字段改为富文本 HTML 后，
 * 发布校验需剔除标签判断是否真的填写了文字（"<p><br></p>" 视为空）。
 */
public final class HtmlTextUtils {

    private HtmlTextUtils() {
    }

    /** 剔除 HTML 标签后的纯文本（不含首尾空白） */
    public static String plainText(String html) {
        return html == null ? "" : html.replaceAll("<[^>]+>", " ").trim();
    }

    /** 剔除标签后是否仍有正文文字 */
    public static boolean hasText(String html) {
        return !plainText(html).isEmpty();
    }
}
