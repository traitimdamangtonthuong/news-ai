package com.newsai.aichatbot.service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newsai.aichatbot.client.dto.NamedItemDto;
import com.newsai.aichatbot.model.SearchPlan;
import com.newsai.aichatbot.text.TextUtils;

/**
 * Biến câu hỏi tự nhiên thành {@link SearchPlan}: từ khoá nội dung, khoảng
 * thời gian ("hôm nay", "tuần này", "ngày 5/11"...), tên báo và chuyên mục.
 *
 * Ràng buộc từ backend (xem README): {@code keyword} chỉ là MỘT chuỗi khớp
 * {@code LIKE '%...%'} theo từng ký tự (có dấu) trên title/description/content,
 * không có tìm kiếm đa từ khoá. Vì vậy lớp này sinh ra nhiều truy vấn ứng viên
 * (cả cụm, cặp từ liền kề, từ đơn); phần lọc/xếp hạng chính xác do
 * {@link RelevanceScorer} làm phía chatbot.
 */
public final class QueryAnalyzer {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_QUERIES = 6;

    // ===== Từ dừng (so khớp theo từ đơn) =====
    // Danh sách tiếng Việt GIỮ DẤU để không nhầm "bão" với "báo", "thế giới" với "thế"...
    private static final Set<String> STOP_VI = Set.of(
            "cho", "tôi", "mình", "bạn", "giúp", "xin", "hãy", "cần", "muốn", "biết", "nói",
            "về", "của", "là", "gì", "nào", "sao", "không", "có", "những", "các", "một",
            "nhiều", "nhất", "mới", "đây", "nay", "hôm", "qua", "trước", "vừa", "đang",
            "ở", "tại", "trong", "với", "và", "hoặc", "được", "bị", "sẽ", "đã", "vào",
            "lên", "xuống", "đến", "từ", "theo", "để", "như", "đó", "kia", "ấy", "thì",
            "mà", "nên", "vì", "bởi", "nếu", "xem", "đọc", "tìm", "hỏi", "ai", "em",
            "anh", "chị", "ạ", "nhé", "nha", "giùm", "dùm", "thử", "khi", "lúc", "còn",
            "cũng", "rất", "hơn", "nữa", "đều", "này", "đấy", "vậy", "thôi", "bài",
            "tuần", "tháng", "ngày", "năm", "ông", "bà", "trên", "dưới");
    private static final Set<String> STOP_EN = Set.of(
            "the", "a", "an", "is", "are", "of", "in", "on", "for", "and", "to", "about",
            "what", "how", "summarize", "summarise", "summary", "please", "tell", "me", "give",
            "show", "find", "news", "article", "articles", "any", "there", "some", "new", "today",
            "yesterday", "with", "from", "by", "at", "do", "does", "did", "can", "you", "i", "my",
            "it", "this", "that", "these", "those", "be", "was", "were", "has", "have", "had", "latest");
    // Chỉ dùng khi CẢ câu hỏi không có dấu (người dùng gõ không dấu): bản bỏ dấu của STOP_VI + STOP_EN.
    // Khi câu hỏi có dấu thì từ không dấu như "va" (va chạm), "vi" (vi phạm), "tai" (tai nạn) là từ thật.
    private static final Set<String> STOP_FOLDED = buildFoldedStops();

    private static final Set<String> GENERIC_CATEGORIES = Set.of("tin tuc", "tin moi nhat", "tin khac", "khac");

    // ===== Biểu thức nhận diện (chạy trên chuỗi đã bỏ dấu, chữ thường) =====
    private static final Pattern ISO_DATE = Pattern.compile("(?<!\\d)(\\d{4})-(\\d{1,2})-(\\d{1,2})(?!\\d)");
    private static final Pattern DAY_MONTH_WORDS = Pattern.compile(
            "ngay\\s+(\\d{1,2})\\s*(?:[/\\-.]|\\s+thang\\s+)\\s*(\\d{1,2})"
                    + "(?:\\s*(?:[/\\-.]|\\s+nam\\s+)\\s*(\\d{4}|\\d{2}))?(?!\\d)");
    private static final Pattern DAY_MONTH_SLASH = Pattern.compile(
            "(?<![\\d/])(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}|\\d{2}))?(?![\\d/])");
    private static final Pattern MONTH_NUM = Pattern.compile(
            "(?<![a-z0-9])thang\\s+(\\d{1,2})(?:\\s*(?:/|nam)\\s*(\\d{4}))?(?!\\d)");
    private static final Pattern LAST_N_DAYS = Pattern.compile(
            "(?<!\\d)(\\d{1,2})\\s+ngay\\s+(?:vua qua|gan day|truoc|qua)(?![a-z0-9])");
    private static final Pattern RELATIVE = Pattern.compile(
            "(?<![a-z0-9])(hom nay|hom qua|hom kia|tuan nay|tuan truoc|tuan roi|tuan qua|"
                    + "thang nay|thang truoc|thang roi|thang qua|nam nay|nam ngoai|nam truoc|"
                    + "vai ngay qua|may ngay qua|ngay gan day|ngay qua)(?![a-z0-9])");
    private static final Pattern LATEST = Pattern.compile(
            "(?<![a-z0-9])(?:moi nhat|gan day|moi day|vua xay ra|nong nhat|co gi moi|gi moi|latest|newest|recent)(?![a-z0-9])");
    private static final Pattern REQUEST_PHRASES = Pattern.compile(
            "(?<![a-z0-9])(?:tom tat|tong hop|bai bao|bai viet|thong tin|tin tuc|noi dung|lien quan|"
                    + "cho toi biet|cho minh biet|ban co biet|vui long|nhu the nao|the nao|co gi|nhung gi|"
                    + "bao nhieu|bao lau|bao gio|khi nao|o dau)(?![a-z0-9])");
    // "tin" (= news) đứng riêng, trừ các cụm như tin giả, tin đồn, tin nhắn...
    private static final Pattern NEWS_WORD = Pattern.compile(
            "(?<![a-z0-9])tin(?![a-z0-9])(?!\\s+(?:gia|don|nhan|tuong|cay|hoc|dung|vui|buon|\\d))");
    // "mới" đứng riêng = muốn tin mới (không nhầm với "mỗi", "môi").
    private static final Pattern MOI_WORD = Pattern.compile("(?<![a-z0-9])moi(?![a-z0-9])");
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final char SENTINEL = '\u0001';

    private final Clock clock;

    public QueryAnalyzer() {
        this(Clock.systemDefaultZone());
    }

    public QueryAnalyzer(Clock clock) {
        this.clock = clock;
    }

    /** Từ khoá đã bỏ dấu của câu hỏi (không cần danh sách nguồn/chuyên mục). Dùng cho bước tóm tắt. */
    public static List<String> extractTerms(String question) {
        List<String> folded = new ArrayList<>();
        for (String k : new QueryAnalyzer().analyze(question, List.of(), List.of()).keywords()) {
            folded.add(TextUtils.fold(k).strip());
        }
        return folded;
    }

    /** Có phải từ dừng (đã bỏ dấu) hay không; dùng chung cho bước tóm tắt. */
    public static boolean isStopWord(String foldedWord) {
        return STOP_FOLDED.contains(foldedWord);
    }

    public SearchPlan analyze(String question, List<NamedItemDto> sources, List<NamedItemDto> categories) {
        String nfc = Normalizer.normalize(question == null ? "" : question.strip(), Normalizer.Form.NFC);
        char[] orig = nfc.toCharArray();
        char[] fold = TextUtils.foldAligned(nfc).toCharArray();
        LocalDate today = LocalDate.now(clock);

        // 1) Khoảng thời gian
        DateRange range = detectDate(orig, fold, today);

        // 2) "mới nhất", cụm từ yêu cầu ("tóm tắt", "bài báo"...), từ "tin" đứng riêng
        boolean latest = blankAll(LATEST, orig, fold);
        latest |= blankWordIfTyped(MOI_WORD, Set.of("mới", "moi"), orig, fold);
        blankAll(REQUEST_PHRASES, orig, fold);
        blankWordIfTyped(NEWS_WORD, Set.of("tin"), orig, fold); // "tín" (tín hiệu, tín dụng) vẫn là từ khoá

        // 3) Tên báo (vd. VnExpress): lọc ở phía chatbot vì mỗi feed có sourceId riêng
        List<String> outlets = new ArrayList<>();
        String outletLabel = null;
        for (Map.Entry<String, String> e : outletIndex(sources).entrySet()) {
            Pattern p = Pattern.compile("(?<![a-z0-9])(?:bao\\s+)?" + Pattern.quote(e.getKey()) + "(?![a-z0-9])");
            if (blankAll(p, orig, fold)) {
                outlets.add(e.getKey());
                if (outletLabel == null) {
                    outletLabel = e.getValue();
                }
            }
        }

        // 4) Từ khoá nội dung
        boolean accentless = nfc.chars().noneMatch(c -> c >= 128 && Character.isLetter(c));
        List<Token> tokens = keptTokens(orig, accentless);

        // 5) Chuyên mục: chỉ lọc theo chuyên mục khi ngoài tên chuyên mục không còn từ khoá nào
        List<Long> categoryIds = List.of();
        String categoryLabel = null;
        CategoryMatch cm = matchCategory(fold, categories);
        if (cm != null) {
            char[] orig2 = orig.clone();
            char[] fold2 = fold.clone();
            blank(orig2, fold2, cm.start, cm.end);
            if (keptTokens(orig2, accentless).isEmpty()) {
                tokens = List.of();
                categoryIds = cm.ids;
                categoryLabel = cm.label;
            }
        }

        List<String> keywords = new ArrayList<>();
        List<String> terms = new ArrayList<>();
        List<Boolean> adjacentToNext = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<Token> unique = new ArrayList<>();
        for (Token t : tokens) {
            if (seen.add(t.folded)) {
                unique.add(t);
                keywords.add(t.lower);
                // Từ viết tắt HOA ("AI") giữ nguyên chữ hoa để không khớp nhầm "ai" (= who) trong bài.
                terms.add(t.acronym ? t.raw : (accentless ? t.folded : t.lower));
            }
        }
        for (int i = 0; i < unique.size(); i++) {
            adjacentToNext.add(i + 1 < unique.size() && unique.get(i + 1).adjacentToPrev
                    && unique.get(i + 1).index == unique.get(i).index + 1);
        }

        List<String> queries = buildQueries(unique, adjacentToNext);
        boolean browsingOnly = keywords.isEmpty();
        return new SearchPlan(
                List.copyOf(keywords), List.copyOf(terms), accentless, List.copyOf(queries),
                range == null ? null : range.from, range == null ? null : range.to,
                range == null ? null : range.label(),
                categoryIds, categoryLabel, List.copyOf(outlets), outletLabel,
                latest || browsingOnly);
    }

    // ------------------------------------------------------------------ queries

    private static List<String> buildQueries(List<Token> toks, List<Boolean> adjacentToNext) {
        Set<String> queries = new LinkedHashSet<>();
        int n = toks.size();
        if (n == 0) {
            return List.of();
        }
        if (n == 1) {
            queries.add(toks.get(0).lower);
            return List.copyOf(queries);
        }
        String all = joinTokens(toks);
        if (n <= 4) {
            queries.add(all);
        }
        int bigrams = 0;
        for (int i = 0; i + 1 < n && bigrams < 3; i++) {
            if (adjacentToNext.get(i)) {
                String phrase = toks.get(i).lower + " " + toks.get(i + 1).lower;
                if (queries.add(phrase)) {
                    bigrams++;
                }
            }
        }
        List<Token> bySpecificity = new ArrayList<>(toks);
        bySpecificity.sort(Comparator.comparingInt(Token::weight).reversed());
        int singles = 0;
        for (Token t : bySpecificity) {
            if (singles >= 3) {
                break;
            }
            if (t.folded.length() <= 2 && t.folded.chars().allMatch(Character::isDigit)) {
                continue; // số 1-2 chữ số đứng riêng khớp gần như mọi bài
            }
            if (queries.add(t.lower)) {
                singles++;
            }
        }
        List<String> out = new ArrayList<>(queries);
        return out.size() > MAX_QUERIES ? out.subList(0, MAX_QUERIES) : out;
    }

    private static String joinTokens(List<Token> toks) {
        List<String> parts = new ArrayList<>();
        for (Token t : toks) {
            parts.add(t.lower);
        }
        return String.join(" ", parts);
    }

    // ------------------------------------------------------------------- tokens

    private static final class Token {
        final String raw;
        final String lower;
        final String folded;
        final boolean acronym;
        final boolean hasDigit;
        final int index;              // thứ tự token (kể cả token bị bỏ) trong câu
        final boolean adjacentToPrev; // giữa nó và token ngay trước chỉ có khoảng trắng/gạch nối

        Token(String raw, int index, boolean adjacentToPrev) {
            this.raw = raw;
            this.lower = raw.toLowerCase(Locale.ROOT);
            this.folded = TextUtils.fold(raw).strip();
            this.acronym = isAcronym(raw);
            this.hasDigit = raw.chars().anyMatch(Character::isDigit);
            this.index = index;
            this.adjacentToPrev = adjacentToPrev;
        }

        int weight() {
            return folded.length() + (acronym ? 6 : 0) + (hasDigit ? 2 : 0);
        }
    }

    private static List<Token> keptTokens(char[] orig, boolean accentless) {
        String s = new String(orig);
        List<Token> out = new ArrayList<>();
        Matcher m = TOKEN.matcher(s);
        int index = 0;
        int prevEnd = -1;
        while (m.find()) {
            String raw = m.group();
            boolean adjacent = prevEnd >= 0 && s.substring(prevEnd, m.start()).matches("[\\s\\-]*");
            if (keep(raw, accentless)) {
                out.add(new Token(raw, index, adjacent));
            }
            index++;
            prevEnd = m.end();
        }
        return out;
    }

    private static boolean keep(String token, boolean accentless) {
        boolean hasDigit = token.chars().anyMatch(Character::isDigit);
        if (token.length() == 1) {
            return hasDigit; // bỏ chữ cái lẻ, giữ chữ số lẻ (vd. "bão số 5")
        }
        if (isAcronym(token) || hasDigit) {
            return true;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        if (STOP_VI.contains(lower) || STOP_EN.contains(lower)) {
            return false;
        }
        return !(accentless && STOP_FOLDED.contains(lower));
    }

    /** "AI", "EU", "VN"... viết HOA đúng 2 chữ cái A-Z: luôn là từ khoá (khác với "ai" = who). */
    private static boolean isAcronym(String t) {
        return t.length() == 2 && t.chars().allMatch(c -> c >= 'A' && c <= 'Z');
    }

    private static Set<String> buildFoldedStops() {
        Set<String> s = new LinkedHashSet<>();
        for (String w : STOP_VI) {
            s.add(TextUtils.fold(w).strip());
        }
        s.addAll(STOP_EN);
        return Set.copyOf(s);
    }

    // ---------------------------------------------------------- blanking helpers

    private static void blank(char[] orig, char[] fold, int start, int end) {
        for (int i = start; i < end; i++) {
            orig[i] = SENTINEL;
            fold[i] = ' ';
        }
    }

    private static boolean blankAll(Pattern p, char[] orig, char[] fold) {
        boolean any = false;
        Matcher m = p.matcher(new String(fold));
        while (m.find()) {
            blank(orig, fold, m.start(), m.end());
            any = true;
        }
        return any;
    }

    /** Xoá các chỗ khớp {@code p} (trên chuỗi đã bỏ dấu) nhưng chỉ khi chữ gốc đúng là một trong {@code typed}. */
    private static boolean blankWordIfTyped(Pattern p, Set<String> typed, char[] orig, char[] fold) {
        Matcher m = p.matcher(new String(fold));
        List<int[]> spans = new ArrayList<>();
        while (m.find()) {
            String original = new String(orig, m.start(), m.end() - m.start()).toLowerCase(Locale.ROOT);
            if (typed.contains(original)) {
                spans.add(new int[] {m.start(), m.end()});
            }
        }
        for (int[] sp : spans) {
            blank(orig, fold, sp[0], sp[1]);
        }
        return !spans.isEmpty();
    }

    // --------------------------------------------------------------- outlets / categories

    /** folded outlet -> tên hiển thị. Outlet = phần trước " - " của tên source (vd. "VnExpress - Xe"). */
    private static Map<String, String> outletIndex(List<NamedItemDto> sources) {
        Map<String, String> index = new LinkedHashMap<>();
        if (sources == null) {
            return index;
        }
        for (NamedItemDto s : sources) {
            if (s == null || s.name() == null || s.name().isBlank()) {
                continue;
            }
            String name = s.name().strip();
            int dash = name.indexOf(" - ");
            String outlet = dash > 0 ? name.substring(0, dash).strip() : name;
            String folded = TextUtils.fold(outlet).strip();
            if (folded.length() >= 3 && !GENERIC_CATEGORIES.contains(folded)) {
                index.putIfAbsent(folded, outlet);
            }
        }
        List<Map.Entry<String, String>> entries = new ArrayList<>(index.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        Map<String, String> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : entries) {
            sorted.put(e.getKey(), e.getValue());
        }
        return sorted;
    }

    private record CategoryMatch(int start, int end, List<Long> ids, String label) {
    }

    private static CategoryMatch matchCategory(char[] fold, List<NamedItemDto> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        Map<String, List<NamedItemDto>> byName = new LinkedHashMap<>();
        for (NamedItemDto c : categories) {
            if (c == null || c.name() == null || c.id() == null) {
                continue;
            }
            String folded = TextUtils.fold(c.name()).strip();
            if (folded.length() >= 3 && !GENERIC_CATEGORIES.contains(folded)) {
                byName.computeIfAbsent(folded, k -> new ArrayList<>()).add(c);
            }
        }
        String text = new String(fold);
        CategoryMatch best = null;
        for (Map.Entry<String, List<NamedItemDto>> e : byName.entrySet()) {
            Matcher m = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(e.getKey()) + "(?![a-z0-9])").matcher(text);
            if (m.find() && (best == null || e.getKey().length() > (best.end - best.start))) {
                List<Long> ids = new ArrayList<>();
                for (NamedItemDto c : e.getValue()) {
                    if (ids.size() < 4) {
                        ids.add(c.id());
                    }
                }
                best = new CategoryMatch(m.start(), m.end(), ids, e.getValue().get(0).name());
            }
        }
        return best;
    }

    // -------------------------------------------------------------------- dates

    private record DateRange(LocalDate from, LocalDate to, String phrase) {
        String label() {
            if (phrase.startsWith("ngày ")) {
                return phrase; // đã chứa ngày cụ thể
            }
            if (from.equals(to)) {
                return phrase + " (" + from.format(DMY) + ")";
            }
            return phrase + " (" + from.format(DMY) + " – " + to.format(DMY) + ")";
        }
    }

    private static DateRange detectDate(char[] orig, char[] fold, LocalDate today) {
        Matcher m = ISO_DATE.matcher(new String(fold));
        while (m.find()) {
            LocalDate d = ymd(m.group(1), m.group(2), m.group(3));
            if (d != null) {
                blank(orig, fold, m.start(), m.end());
                return new DateRange(d, d, "ngày " + d.format(DMY));
            }
        }
        for (Pattern p : List.of(DAY_MONTH_WORDS, DAY_MONTH_SLASH)) {
            m = p.matcher(new String(fold));
            while (m.find()) {
                LocalDate d = dayMonth(m.group(1), m.group(2), m.group(3), today);
                if (d != null) {
                    blank(orig, fold, m.start(), m.end());
                    return new DateRange(d, d, "ngày " + d.format(DMY));
                }
            }
        }
        m = MONTH_NUM.matcher(new String(fold));
        while (m.find()) {
            int month = Integer.parseInt(m.group(1));
            if (month < 1 || month > 12) {
                continue;
            }
            int year = m.group(2) != null ? Integer.parseInt(m.group(2)) : today.getYear();
            if (m.group(2) == null && month > today.getMonthValue()) {
                year--;
            }
            YearMonth ym = YearMonth.of(year, month);
            blank(orig, fold, m.start(), m.end());
            return new DateRange(ym.atDay(1), ym.atEndOfMonth(), "tháng " + month + "/" + year);
        }
        m = LAST_N_DAYS.matcher(new String(fold));
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 1) {
                blank(orig, fold, m.start(), m.end());
                return new DateRange(today.minusDays(n - 1L), today, n + " ngày gần đây");
            }
        }
        m = RELATIVE.matcher(new String(fold));
        if (m.find()) {
            DateRange r = relative(m.group(1), today);
            blank(orig, fold, m.start(), m.end());
            return r;
        }
        return null;
    }

    private static DateRange relative(String phrase, LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return switch (phrase) {
            case "hom nay" -> new DateRange(today, today, "hôm nay");
            case "hom qua" -> new DateRange(today.minusDays(1), today.minusDays(1), "hôm qua");
            case "hom kia" -> new DateRange(today.minusDays(2), today.minusDays(2), "hôm kia");
            case "tuan nay" -> new DateRange(monday, today, "tuần này");
            case "tuan truoc", "tuan roi" -> new DateRange(monday.minusDays(7), monday.minusDays(1), "tuần trước");
            case "tuan qua" -> new DateRange(today.minusDays(6), today, "7 ngày qua");
            case "thang nay" -> new DateRange(today.withDayOfMonth(1), today, "tháng này");
            case "thang truoc", "thang roi" -> {
                YearMonth prev = YearMonth.from(today).minusMonths(1);
                yield new DateRange(prev.atDay(1), prev.atEndOfMonth(), "tháng trước");
            }
            case "thang qua" -> new DateRange(today.minusDays(29), today, "30 ngày qua");
            case "nam nay" -> new DateRange(today.withDayOfYear(1), today, "năm nay");
            case "nam ngoai", "nam truoc" -> {
                LocalDate start = today.minusYears(1).withDayOfYear(1);
                yield new DateRange(start, start.withDayOfYear(start.lengthOfYear()), "năm ngoái");
            }
            default -> new DateRange(today.minusDays(2), today, "vài ngày gần đây");
        };
    }

    private static LocalDate ymd(String y, String mo, String d) {
        try {
            return LocalDate.of(Integer.parseInt(y), Integer.parseInt(mo), Integer.parseInt(d));
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate dayMonth(String d, String mo, String yearStr, LocalDate today) {
        if (yearStr == null && d.equals("24") && mo.equals("7")) {
            return null; // "24/7" = cả ngày đêm, không phải ngày 24 tháng 7
        }
        try {
            int year = today.getYear();
            if (yearStr != null) {
                year = Integer.parseInt(yearStr);
                if (year < 100) {
                    year += 2000;
                }
            }
            LocalDate date = LocalDate.of(year, Integer.parseInt(mo), Integer.parseInt(d));
            if (yearStr == null && date.isAfter(today)) {
                date = date.minusYears(1);
            }
            return date;
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
    }
}
