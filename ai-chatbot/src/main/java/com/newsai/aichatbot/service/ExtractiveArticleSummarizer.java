package com.newsai.aichatbot.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.newsai.aichatbot.model.ArticleContext;

/**
 * Tóm tắt/tổng hợp nhiều bài báo bằng thuật toán extractive tự viết
 * (word-frequency scoring), không gọi LLM, không cần API key, chạy hoàn
 * toàn offline. Đây là bản mặc định để chatbot chạy được ngay; nhóm có thể
 * thay bằng {@link ArticleSummarizer} khác gọi LLM thật khi cần chất lượng
 * cao hơn (xem Javadoc của {@link ArticleSummarizer}).
 *
 * Ý tưởng thuật toán:
 *  1. Tách toàn bộ nội dung các bài báo liên quan thành câu.
 *  2. Đếm tần suất các từ có nghĩa (loại bỏ stopword) trên toàn bộ tập câu.
 *  3. Chấm điểm mỗi câu = tổng tần suất từ trong câu, chuẩn hoá theo độ dài câu.
 *  4. Chọn ra các câu điểm cao nhất, loại câu trùng lặp gần giống nhau,
 *     rồi ghép lại thành đoạn tổng hợp kèm trích dẫn nguồn.
 */
public class ExtractiveArticleSummarizer implements ArticleSummarizer {

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?…])\\s+");
    private static final Pattern WORD_SPLIT = Pattern.compile("[^\\p{L}0-9]+");

    private static final Set<String> STOPWORDS = Set.of(
            "là", "của", "và", "các", "những", "một", "có", "cho", "về",
            "này", "đó", "được", "với", "khi", "đã", "sẽ", "vào", "trên",
            "theo", "để", "như", "tại", "cũng", "nhưng", "thì", "còn", "từ",
            "the", "a", "an", "is", "are", "of", "in", "on", "for", "and", "to"
    );

    private final int maxSentences;

    public ExtractiveArticleSummarizer() {
        this(6);
    }

    public ExtractiveArticleSummarizer(int maxSentences) {
        this.maxSentences = Math.max(1, maxSentences);
    }

    @Override
    public String summarize(String question, List<ArticleContext> articles) {
        if (articles == null || articles.isEmpty()) {
            return "Không tìm thấy bài báo nào liên quan đến câu hỏi này trong dữ liệu đã crawl.";
        }

        List<ScoredSentence> allSentences = new ArrayList<>();
        Map<String, Integer> wordFreq = new LinkedHashMap<>();

        // Bước 1 + 2: tách câu và đếm tần suất từ trên toàn bộ ngữ cảnh.
        for (ArticleContext article : articles) {
            String text = article.text() == null ? "" : article.text();
            for (String rawSentence : SENTENCE_SPLIT.split(text)) {
                String sentence = rawSentence.strip();
                if (sentence.length() < 20) {
                    continue; // bỏ câu quá ngắn, thường là tiêu đề phụ/nhiễu
                }
                allSentences.add(new ScoredSentence(sentence, article, 0.0));
                for (String word : WORD_SPLIT.split(sentence.toLowerCase(Locale.ROOT))) {
                    if (word.length() < 3 || STOPWORDS.contains(word)) {
                        continue;
                    }
                    wordFreq.merge(word, 1, Integer::sum);
                }
            }
        }

        if (allSentences.isEmpty()) {
            return buildFallbackSummary(articles);
        }

        // Bước 3: chấm điểm từng câu.
        List<ScoredSentence> scored = allSentences.stream()
                .map(s -> new ScoredSentence(s.sentence(), s.article(), scoreSentence(s.sentence(), wordFreq)))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();

        // Bước 4: chọn câu điểm cao, loại trùng lặp gần giống.
        List<ScoredSentence> chosen = new ArrayList<>();
        for (ScoredSentence candidate : scored) {
            if (chosen.size() >= maxSentences) {
                break;
            }
            boolean tooSimilar = chosen.stream().anyMatch(c -> isNearDuplicate(c.sentence(), candidate.sentence()));
            if (!tooSimilar) {
                chosen.add(candidate);
            }
        }
        if (chosen.isEmpty()) {
            return buildFallbackSummary(articles);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Tổng hợp từ ").append(articles.size()).append(" bài báo liên quan:\n\n");
        for (ScoredSentence s : chosen) {
            sb.append("- ").append(s.sentence()).append(" (").append(s.article().sourceName()).append(")\n");
        }
        return sb.toString().stripTrailing();
    }

    private double scoreSentence(String sentence, Map<String, Integer> wordFreq) {
        String[] words = WORD_SPLIT.split(sentence.toLowerCase(Locale.ROOT));
        if (words.length == 0) {
            return 0.0;
        }
        double total = 0.0;
        for (String w : words) {
            total += wordFreq.getOrDefault(w, 0);
        }
        return total / Math.sqrt(words.length); // giảm thiên vị cho câu quá dài
    }

    private boolean isNearDuplicate(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.equals(nb)) {
            return true;
        }
        return na.contains(nb) || nb.contains(na);
    }

    private String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}0-9]+", "");
    }

    private String buildFallbackSummary(List<ArticleContext> articles) {
        // Nếu không tách được câu hợp lệ (nội dung quá ngắn), rơi về liệt kê description.
        StringBuilder sb = new StringBuilder("Các bài báo liên quan tìm thấy:\n\n");
        for (ArticleContext a : articles) {
            sb.append("- ").append(a.title()).append(" (").append(a.sourceName()).append(")\n");
        }
        return sb.toString().stripTrailing();
    }

    private record ScoredSentence(String sentence, ArticleContext article, double score) {
    }
}
