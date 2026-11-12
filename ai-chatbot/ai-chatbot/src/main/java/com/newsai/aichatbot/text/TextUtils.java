package com.newsai.aichatbot.text;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiện ích xử lý văn bản tiếng Việt dùng chung (Java thuần, không phụ thuộc
 * thư viện ngoài): bỏ dấu, tách từ, giải mã HTML entity, tách câu.
 */
public final class TextUtils {

    private static final Pattern WORD_SPLIT = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern ENTITY = Pattern.compile("&(#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);");

    // Tách câu sau . ! ? … (có thể kèm dấu đóng ngoặc/nháy) khi câu sau bắt đầu bằng chữ hoa/số/mở nháy.
    // Không tách sau các từ viết tắt phổ biến (TP., PGS., GS., TS., ThS., BS., TT.).
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
            "(?<=[.!?…]|[.!?…][\"”’)])"
                    + "(?<!\\b(?:TP|PGS|GS|TS|ThS|BS|TT|Mr|Mrs|Dr)\\.)"
                    + "\\s+(?=[\"“‘(\\[]?[\\p{Lu}\\p{N}])");

    private static final Map<String, String> NAMED_ENTITIES = Map.ofEntries(
            Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"),
            Map.entry("quot", "\""), Map.entry("apos", "'"), Map.entry("nbsp", " "),
            Map.entry("ndash", "–"), Map.entry("mdash", "—"), Map.entry("hellip", "…"),
            Map.entry("lsquo", "‘"), Map.entry("rsquo", "’"),
            Map.entry("ldquo", "“"), Map.entry("rdquo", "”"),
            Map.entry("laquo", "«"), Map.entry("raquo", "»"), Map.entry("copy", "©"));

    private TextUtils() {
    }

    /**
     * Chữ thường + bỏ dấu tiếng Việt, GIỮ NGUYÊN độ dài chuỗi (mỗi ký tự NFC
     * ánh xạ đúng 1 ký tự) để vị trí khớp regex trên chuỗi đã fold dùng lại
     * được cho chuỗi gốc. Đầu vào phải ở dạng NFC.
     */
    public static String foldAligned(String nfc) {
        StringBuilder sb = new StringBuilder(nfc.length());
        for (int i = 0; i < nfc.length(); i++) {
            char c = Character.toLowerCase(nfc.charAt(i));
            if (c == 'đ') {
                sb.append('d');
            } else if (c < 128) {
                sb.append(c);
            } else {
                String decomposed = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
                char base = decomposed.charAt(0);
                sb.append(Character.getType(base) == Character.NON_SPACING_MARK ? ' ' : base);
            }
        }
        return sb.toString();
    }

    /** Chuẩn hoá NFC + chữ thường, GIỮ dấu (để phân biệt "bão" với "bảo", "báo"). */
    public static String lowerNfc(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(java.util.Locale.ROOT);
    }

    /** Chuẩn hoá NFC rồi fold (bỏ dấu, chữ thường). */
    public static String fold(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return foldAligned(Normalizer.normalize(s, Normalizer.Form.NFC));
    }

    /** Tách thành các "từ" (âm tiết) theo ký tự không phải chữ/số; bỏ chuỗi rỗng. */
    public static List<String> words(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) {
            return out;
        }
        for (String w : WORD_SPLIT.split(s)) {
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        return out;
    }

    /** Giải mã HTML entity (&amp; &#225; &#xE1; ...), xử lý cả trường hợp bị mã hoá 2 lần. */
    public static String unescapeHtml(String s) {
        if (s == null) {
            return "";
        }
        String cur = s;
        for (int pass = 0; pass < 2 && cur.indexOf('&') >= 0; pass++) {
            Matcher m = ENTITY.matcher(cur);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String decoded = decodeEntity(m.group(1));
                m.appendReplacement(sb, Matcher.quoteReplacement(decoded != null ? decoded : m.group()));
            }
            m.appendTail(sb);
            String next = sb.toString();
            if (next.equals(cur)) {
                break;
            }
            cur = next;
        }
        return cur;
    }

    private static String decodeEntity(String body) {
        if (body.charAt(0) == '#') {
            try {
                int cp = (body.length() > 1 && (body.charAt(1) == 'x' || body.charAt(1) == 'X'))
                        ? Integer.parseInt(body.substring(2), 16)
                        : Integer.parseInt(body.substring(1));
                return Character.isValidCodePoint(cp) ? new String(Character.toChars(cp)) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return NAMED_ENTITIES.get(body);
    }

    /** Tách một đoạn văn thành các câu. */
    public static List<String> splitSentences(String paragraph) {
        List<String> out = new ArrayList<>();
        if (paragraph == null || paragraph.isBlank()) {
            return out;
        }
        for (String part : SENTENCE_BOUNDARY.split(paragraph.strip())) {
            String sentence = part.strip();
            if (!sentence.isEmpty()) {
                out.add(sentence);
            }
        }
        return out;
    }

    /** Câu có kết thúc bằng dấu câu (loại tiêu đề phụ/chú thích không có dấu chấm). */
    public static boolean endsWithTerminal(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        char last = s.charAt(s.length() - 1);
        if (last == '"' || last == '”' || last == '’' || last == ')') {
            if (s.length() < 2) {
                return false;
            }
            last = s.charAt(s.length() - 2);
        }
        return last == '.' || last == '!' || last == '?' || last == '…';
    }

    /** Cắt chuỗi ở ranh giới từ gần nhất không vượt quá {@code max} ký tự, thêm "…". */
    public static String clip(String s, int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "" : s;
        }
        int cut = s.lastIndexOf(' ', max);
        if (cut < max / 2) {
            cut = max;
        }
        return s.substring(0, cut).stripTrailing() + "…";
    }
}
