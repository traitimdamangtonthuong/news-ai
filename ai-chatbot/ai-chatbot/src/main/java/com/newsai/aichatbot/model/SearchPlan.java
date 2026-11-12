package com.newsai.aichatbot.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Kế hoạch tìm kiếm rút ra từ câu hỏi của người dùng.
 *
 * @param keywords      từ khoá nội dung (chữ thường, giữ dấu) theo thứ tự xuất hiện
 * @param terms         các từ khoá dùng để chấm điểm khớp nguyên từ: chữ thường GIỮ dấu nếu câu hỏi có dấu
 *                      (phân biệt "bão" với "bảo"), hoặc đã bỏ dấu nếu câu hỏi gõ không dấu
 * @param matchFolded   true nếu khi chấm điểm phải bỏ dấu cả văn bản bài báo (câu hỏi gõ không dấu)
 * @param queries       các chuỗi gửi lên backend ({@code keyword=}), ưu tiên từ cụ thể đến rộng
 * @param from          lọc ngày đăng từ (bao gồm), null nếu không lọc
 * @param to            lọc ngày đăng đến (bao gồm), null nếu không lọc
 * @param dateLabel     mô tả khoảng thời gian cho người dùng, vd. "hôm nay (08/11/2026)"
 * @param categoryIds   chuyên mục cần lọc (chỉ khi câu hỏi thuần là xem một chuyên mục)
 * @param categoryLabel tên chuyên mục để hiển thị
 * @param outlets       tên báo (đã bỏ dấu) cần giữ lại, vd. "vnexpress"
 * @param outletLabel   tên báo để hiển thị
 * @param latest        người dùng muốn tin mới nhất
 */
public record SearchPlan(
        List<String> keywords,
        List<String> terms,
        boolean matchFolded,
        List<String> queries,
        LocalDate from,
        LocalDate to,
        String dateLabel,
        List<Long> categoryIds,
        String categoryLabel,
        List<String> outlets,
        String outletLabel,
        boolean latest
) {
    public boolean hasKeywords() {
        return !terms.isEmpty();
    }

    public boolean hasDate() {
        return from != null || to != null;
    }

    public SearchPlan withoutDate() {
        return new SearchPlan(keywords, terms, matchFolded, queries, null, null, null,
                categoryIds, categoryLabel, outlets, outletLabel, latest);
    }

    /**
     * Khi chuyên mục không có bài nào (chuyên mục tồn tại nhưng chưa crawl bài nào gắn với nó),
     * chuyển sang tìm tên chuyên mục như một từ khoá thông thường.
     */
    public SearchPlan withCategoryAsKeyword() {
        if (categoryLabel == null) {
            return this;
        }
        String lower = categoryLabel.toLowerCase(java.util.Locale.ROOT).strip();
        List<String> words = com.newsai.aichatbot.text.TextUtils.words(lower);
        return new SearchPlan(words, words, false, List.of(lower), from, to, dateLabel,
                List.of(), null, outlets, outletLabel, latest);
    }

    /** Mô tả ngắn gọn điều kiện tìm kiếm để báo lại cho người dùng. */
    public String describe() {
        List<String> parts = new ArrayList<>();
        if (!keywords.isEmpty()) {
            parts.add("từ khoá \"" + String.join(" ", keywords) + "\"");
        }
        if (outletLabel != null) {
            parts.add("nguồn " + outletLabel);
        }
        if (categoryLabel != null) {
            parts.add("chuyên mục " + categoryLabel);
        }
        if (dateLabel != null) {
            parts.add(dateLabel);
        }
        return parts.isEmpty() ? "tin mới nhất" : String.join(", ", parts);
    }
}
