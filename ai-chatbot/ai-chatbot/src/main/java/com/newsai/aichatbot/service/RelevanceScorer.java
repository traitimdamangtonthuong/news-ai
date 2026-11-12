package com.newsai.aichatbot.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.newsai.aichatbot.text.TextUtils;

/**
 * Chấm điểm mức liên quan của một bài báo với các từ khoá của câu hỏi, phía chatbot.
 *
 * Cần thiết vì backend chỉ tìm theo chuỗi con ({@code LIKE '%ai%'} khớp cả "hai",
 * "bài", "đài"...). Ở đây so khớp theo NGUYÊN TỪ (đã bỏ dấu, không phân biệt hoa/thường)
 * và ưu tiên: khớp tiêu đề &gt; khớp mô tả &gt; khớp trong nội dung; bài chứa đủ nhiều
 * từ khoá và chứa nguyên cụm liền nhau được cộng điểm.
 */
public final class RelevanceScorer {

    /** @param value điểm tổng; @param coverage tỉ lệ từ khoá xuất hiện ít nhất một lần (0..1) */
    public record Score(double value, double coverage) {
        public static final Score NONE = new Score(0.0, 0.0);
    }

    private final List<String> terms;
    private final boolean foldText;
    private final String phrase;

    /**
     * @param terms    từ khoá (chữ thường); giữ dấu hay đã bỏ dấu tuỳ {@code foldText}
     * @param foldText true: bỏ dấu văn bản trước khi so khớp (câu hỏi gõ không dấu);
     *                 false: so khớp đúng dấu, để "bão" không khớp "bảo"/"báo"
     */
    public RelevanceScorer(List<String> terms, boolean foldText) {
        this.terms = terms;
        this.foldText = foldText;
        this.phrase = " " + String.join(" ", terms).toLowerCase(java.util.Locale.ROOT) + " ";
    }

    public boolean isEmpty() {
        return terms.isEmpty();
    }

    /** Chấm theo tiêu đề + mô tả (dữ liệu có sẵn trong kết quả search, chưa cần tải nội dung). */
    public Score scoreHeader(String title, String description) {
        return score(title, description, null);
    }

    /** Chấm đầy đủ; {@code body} có thể null. */
    public Score score(String title, String description, String body) {
        if (terms.isEmpty()) {
            return Score.NONE;
        }
        Bag t = new Bag(title, foldText);
        Bag d = new Bag(description, foldText);
        Bag b = body == null ? null : new Bag(body, foldText);

        double sum = 0.0;
        double matchedWeight = 0.0;
        double totalWeight = 0.0;
        for (String term : terms) {
            double weight = isNumber(term) ? 0.4 : 1.0; // số đứng riêng ("5") ít phân biệt hơn từ thường
            totalWeight += weight;
            double v = 0.0;
            if (t.count(term) > 0) {
                v += 3.0;
            }
            if (d.count(term) > 0) {
                v += 2.0;
            }
            if (b != null) {
                v += 0.5 * Math.min(b.count(term), 6);
            }
            if (v > 0) {
                matchedWeight += weight;
            }
            sum += v * weight;
        }
        double coverage = matchedWeight / totalWeight;
        double value = sum * (0.5 + coverage);
        if (terms.size() >= 2) {
            // Cụm từ liền nhau ("giá xăng") quan trọng hơn việc hai từ xuất hiện rời rạc ("giá trị ... xăng dầu").
            if (t.joined.contains(phrase)) {
                value += 12.0;
            }
            if (d.joined.contains(phrase)) {
                value += 8.0;
            }
            if (b != null && b.joined.contains(phrase)) {
                value += 4.0;
            }
        }
        return new Score(value, coverage);
    }

    private static boolean isNumber(String term) {
        return !term.isEmpty() && term.chars().allMatch(Character::isDigit);
    }

    /** Từ khoá có chữ hoa (vd. "AI") phải khớp đúng chữ hoa; các từ khoá khác không phân biệt hoa/thường. */
    private static boolean isCaseSensitive(String term) {
        return !term.equals(term.toLowerCase(java.util.Locale.ROOT));
    }

    private static final class Bag {
        final Map<String, Integer> counts = new HashMap<>();
        final Map<String, Integer> rawCounts = new HashMap<>();
        final String joined;

        Bag(String text, boolean fold) {
            List<String> words = TextUtils.words(fold ? TextUtils.fold(text) : TextUtils.lowerNfc(text));
            for (String w : words) {
                counts.merge(w, 1, Integer::sum);
            }
            for (String w : TextUtils.words(java.text.Normalizer.normalize(
                    text == null ? "" : text, java.text.Normalizer.Form.NFC))) {
                rawCounts.merge(w, 1, Integer::sum);
            }
            joined = " " + String.join(" ", words) + " ";
        }

        int count(String term) {
            return (isCaseSensitive(term) ? rawCounts : counts).getOrDefault(term, 0);
        }
    }
}
