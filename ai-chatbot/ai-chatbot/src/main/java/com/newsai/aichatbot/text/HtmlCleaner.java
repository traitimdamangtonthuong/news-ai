package com.newsai.aichatbot.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Làm sạch nội dung HTML của bài báo (cột {@code content} do crawler lưu
 * nguyên HTML: thẻ p/h1/strong/a, figure + figcaption, img, comment...) thành
 * các đoạn văn thuần để tóm tắt.
 */
public final class HtmlCleaner {

    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    // Khối không chứa nội dung bài viết: ảnh + chú thích ảnh, video, script...
    private static final Pattern DROP_BLOCKS = Pattern.compile(
            "<(script|style|figure|figcaption|video|audio|iframe|noscript|svg|picture|form|button)\\b[^>]*>.*?</\\1\\s*>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCK_TAG = Pattern.compile(
            "</?(?:p|div|br|li|ul|ol|h[1-6]|tr|table|section|article|blockquote)\\b[^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern INVISIBLE = Pattern.compile("[\\u200B-\\u200D\\u2060\\uFEFF]");
    private static final Pattern SPACES = Pattern.compile("[\\s\\u00A0\\u2007\\u202F]+");
    // "Ảnh: ...", "Video: ...", "Nguồn: ..." (có dấu hai chấm) hoặc "Xem thêm", "Đọc thêm"...
    private static final Pattern BOILERPLATE = Pattern.compile(
            "^(?:anh|video|clip|nguon|do hoa|infographic)\\s*:.*|^anh minh hoa.*|^(?:xem them|doc them|bai lien quan).*");

    private HtmlCleaner() {
    }

    /** HTML (hoặc văn bản thường) → danh sách đoạn văn sạch, theo thứ tự xuất hiện. */
    public static List<String> toParagraphs(String html, String title) {
        List<String> out = new ArrayList<>();
        if (html == null || html.isBlank()) {
            return out;
        }
        String s = COMMENT.matcher(html).replaceAll(" ");
        s = DROP_BLOCKS.matcher(s).replaceAll(" ");
        s = BLOCK_TAG.matcher(s).replaceAll("\n");
        s = ANY_TAG.matcher(s).replaceAll("");
        s = TextUtils.unescapeHtml(s);
        s = INVISIBLE.matcher(s).replaceAll("");

        String foldedTitle = TextUtils.fold(title == null ? "" : SPACES.matcher(title).replaceAll(" ").strip());
        for (String line : s.split("\n")) {
            String p = SPACES.matcher(line).replaceAll(" ").strip();
            if (p.isEmpty()) {
                continue;
            }
            String folded = TextUtils.fold(p);
            if (BOILERPLATE.matcher(folded).matches()) {
                continue;
            }
            if (out.isEmpty() && !foldedTitle.isEmpty() && folded.equals(foldedTitle)) {
                continue; // thẻ <h1> lặp lại tiêu đề bài
            }
            out.add(p);
        }
        // Dòng cuối ngắn, không có dấu câu thường là chữ ký tác giả, vd. "Lê Nguyễn (Tổng hợp)".
        if (out.size() >= 3) {
            String last = out.get(out.size() - 1);
            if (last.length() < 60 && !TextUtils.endsWithTerminal(last)) {
                out.remove(out.size() - 1);
            }
        }
        return out;
    }

    /** HTML → một chuỗi văn bản thuần (các đoạn nối bằng khoảng trắng). Dùng cho title/description. */
    public static String toPlainText(String html) {
        return String.join(" ", toParagraphs(html, null));
    }
}
