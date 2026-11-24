package com.newsai.aichatbot.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.newsai.aichatbot.model.ArticleContext;
import com.newsai.aichatbot.text.TextUtils;

/**
 * Tóm tắt nhiều bài báo bằng thuật toán extractive tự viết (chấm điểm câu),
 * không gọi LLM, không cần API key, chạy hoàn toàn offline. Là bản mặc định để
 * chatbot chạy được ngay và là phương án dự phòng khi LLM lỗi.
 *
 * Với MỖI bài báo (đã được làm sạch HTML):
 *  1. Dùng mô tả (description) của toà soạn làm câu mở đầu nếu có độ dài hợp lý.
 *  2. Tách nội dung thành câu, loại tiêu đề phụ/chú thích/câu quá ngắn.
 *  3. Chấm điểm = tần suất từ nổi bật trong bài + số từ khoá của câu hỏi có mặt
 *     + ưu tiên câu ở đoạn đầu.
 *  4. Chọn vài câu điểm cao nhất, bỏ câu trùng ý, giữ đúng thứ tự gốc.
 * Kết quả gom theo số thứ tự [1], [2]... khớp với danh sách nguồn trích dẫn.
 */
public class ExtractiveArticleSummarizer implements ArticleSummarizer {

    private static final int MIN_SENTENCE = 30;
    private static final int MAX_SENTENCE_SCAN = 600;
    private static final int MAX_SENTENCE_OUT = 360;
    private static final int LEAD_MIN = 40;
    private static final int LEAD_MAX = 350;
    private static final double DUPLICATE_SIMILARITY = 0.6;
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Từ chức năng phổ biến (đã bỏ dấu) ngoài danh sách của QueryAnalyzer, để không tính vào độ nổi bật.
    private static final Set<String> EXTRA_STOP = Set.of(
            "nguoi", "nhieu", "trong", "tren", "duoi", "sau", "hai", "ba", "nhat", "cung", "con",
            "mot", "cac", "nhung", "duoc", "khong", "cho", "voi", "nay", "khi", "nhu", "tai", "vao");

    private final int sentencesPerArticle;

    public ExtractiveArticleSummarizer() {
        this(2);
    }

    /** @param sentencesPerArticle số câu tối đa cho mỗi bài khi có từ 3 bài trở lên (1-2 bài: nhiều hơn một câu) */
    public ExtractiveArticleSummarizer(int sentencesPerArticle) {
        this.sentencesPerArticle = Math.max(1, sentencesPerArticle);
    }

    @Override
    public String summarize(String question, List<ArticleContext> articles) {
        if (articles == null || articles.isEmpty()) {
            return "Không tìm thấy bài báo nào liên quan đến câu hỏi này trong dữ liệu đã crawl.";
        }
        List<String> terms = QueryAnalyzer.extractTerms(question);
        int target = articles.size() <= 2 ? sentencesPerArticle + 1 : sentencesPerArticle;

        StringBuilder sb = new StringBuilder();
        sb.append("Tổng hợp từ ").append(articles.size()).append(" bài báo liên quan:");
        int n = 1;
        for (ArticleContext a : articles) {
            sb.append("\n\n[").append(n++).append("] ").append(a.title());
            sb.append(" — ").append(a.sourceName());
            if (a.publishedAt() != null) {
                sb.append(" • ").append(a.publishedAt().format(DMY));
            }
            List<String> picked = pick(a, terms, target);
            if (picked.isEmpty()) {
                sb.append("\n• (Bài viết chưa có nội dung chi tiết để tóm tắt.)");
            } else {
                for (String s : picked) {
                    sb.append("\n• ").append(TextUtils.clip(s, MAX_SENTENCE_OUT));
                }
            }
        }
        return sb.toString();
    }

    // ----------------------------------------------------------------- selection

    private record Sentence(String text, int paragraph, int position, List<String> words, Set<String> wordSet) {
    }

    private List<String> pick(ArticleContext a, List<String> terms, int target) {
        List<String> result = new ArrayList<>();
        String lead = a.description() == null ? "" : a.description().strip();
        boolean useLead = lead.length() >= LEAD_MIN && lead.length() <= LEAD_MAX;
        Set<String> leadWords = Set.of();
        if (useLead) {
            result.add(lead);
            leadWords = new HashSet<>(TextUtils.words(TextUtils.fold(lead)));
        }
        int need = target - result.size();
        if (need <= 0) {
            return result;
        }

        List<Sentence> candidates = candidates(a, leadWords);
        if (candidates.isEmpty()) {
            return result;
        }

        Map<String, Integer> tf = new HashMap<>();
        for (Sentence s : candidates) {
            for (String w : s.wordSet()) {
                if (isContentWord(w)) {
                    tf.merge(w, 1, Integer::sum);
                }
            }
        }

        record Scored(Sentence sentence, double score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (Sentence s : candidates) {
            double tfScore = 0.0;
            int contentWords = 0;
            for (String w : s.wordSet()) {
                if (isContentWord(w)) {
                    tfScore += tf.getOrDefault(w, 0);
                    contentWords++;
                }
            }
            tfScore /= Math.sqrt(contentWords + 1.0);
            int hits = 0;
            for (String t : terms) {
                if (s.wordSet().contains(t)) {
                    hits++;
                }
            }
            double position = 3.0 / (1.0 + s.paragraph());
            scored.add(new Scored(s, tfScore + 4.0 * hits + position));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());

        List<Sentence> chosen = new ArrayList<>();
        for (Scored sc : scored) {
            if (chosen.size() >= need) {
                break;
            }
            boolean duplicate = false;
            for (Sentence c : chosen) {
                if (similar(c.wordSet(), sc.sentence().wordSet())) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                chosen.add(sc.sentence());
            }
        }
        chosen.sort(Comparator.comparingInt(Sentence::paragraph).thenComparingInt(Sentence::position));
        for (Sentence s : chosen) {
            result.add(s.text());
        }
        return result;
    }

    private List<Sentence> candidates(ArticleContext a, Set<String> leadWords) {
        List<Sentence> out = new ArrayList<>();
        List<String> paragraphs = a.paragraphs() == null ? List.of() : a.paragraphs();
        for (int p = 0; p < paragraphs.size(); p++) {
            List<String> sentences = TextUtils.splitSentences(paragraphs.get(p));
            for (int i = 0; i < sentences.size(); i++) {
                String s = sentences.get(i);
                if (s.length() < MIN_SENTENCE || s.length() > MAX_SENTENCE_SCAN || !TextUtils.endsWithTerminal(s)) {
                    continue; // tiêu đề phụ, chú thích, câu cụt
                }
                List<String> words = TextUtils.words(TextUtils.fold(s));
                Set<String> wordSet = new LinkedHashSet<>(words);
                if (!leadWords.isEmpty() && similar(leadWords, wordSet)) {
                    continue; // lặp lại ý của phần mô tả
                }
                out.add(new Sentence(s, p, i, words, wordSet));
            }
        }
        return out;
    }

    private static boolean isContentWord(String foldedWord) {
        return foldedWord.length() >= 3 && !QueryAnalyzer.isStopWord(foldedWord) && !EXTRA_STOP.contains(foldedWord);
    }

    /** Hai câu "trùng ý" khi tập từ giao nhau đủ lớn so với câu ngắn hơn. */
    private static boolean similar(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        int common = 0;
        for (String w : a) {
            if (b.contains(w)) {
                common++;
            }
        }
        double overlap = (double) common / Math.min(a.size(), b.size());
        return overlap >= DUPLICATE_SIMILARITY + 0.2 || (double) common / (a.size() + b.size() - common) >= DUPLICATE_SIMILARITY;
    }
}
