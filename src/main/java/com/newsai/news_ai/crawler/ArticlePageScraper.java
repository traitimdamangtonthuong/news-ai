package com.newsai.news_ai.crawler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import org.jsoup.nodes.Attribute;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Scrapes an article's HTML page to extract the breadcrumb hierarchy
 * (parent category + subcategory).
 *
 * <p>Supports multiple news sites:
 * <ul>
 *   <li><b>VnExpress</b> – {@code ul.breadcrumb li a} hoặc meta tag
 *   <li><b>Thanh Niên</b> – {@code ol.breadcrumb li a} hoặc {@code .path-nav a}
 *   <li><b>Nhân Dân</b> – {@code .breadcrumb li a}
 * </ul>
 *
 * <p>Tất cả lỗi (network timeout, parsing, site block) đều được bắt và
 * trả về {@link BreadcrumbResult#EMPTY} — scraper không bao giờ ném ngoại lệ
 * làm gián đoạn crawler.
 */
@Component
public class ArticlePageScraper {

    private static final Logger log = LoggerFactory.getLogger(ArticlePageScraper.class);

    /** Timeout kết nối tới trang bài viết (ms). */
    private static final int CONNECT_TIMEOUT_MS = 7_000;

    /**
     * User-Agent giả trình duyệt — VnExpress block Java default UA
     * ("Java/xx.x") và trả về 403.
     */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36";

    /** Trích slug từ path URL. Ví dụ: "/khoa-hoc-cong-nghe/ai" → ["khoa-hoc-cong-nghe", "ai"] */
    private static final Pattern PATH_SLUG_PATTERN = Pattern.compile("/([a-z0-9][a-z0-9\\-]*)");

    /**
     * CSS selectors cho breadcrumb, theo thứ tự ưu tiên.
     * Mỗi phần tử là một selector trả về danh sách các link a.
     */
    private static final List<String> CONTENT_SELECTORS = List.of(
            "article.fck_detail:has(p)", 
            ".detail-content:has(p)"
    );

    private static final List<String> BREADCRUMB_SELECTORS = List.of(
            "ul.breadcrumb li a",   // VnExpress standard
            "ol.breadcrumb li a",   // Thanh Niên
            ".breadcrumb li a",     // generic
            ".path-nav a",          // Thanh Niên alternative
            ".bread-crumb a",       // một số site khác
            "nav[aria-label=breadcrumb] a"
    );

    /** Các phần tử rác cần xóa khỏi nội dung bài viết. */
    private static final String NOISE_SELECTORS = String.join(", ",
            ".box-tinlienquan", ".banner", "script", "iframe", "table", "style", "meta", "noscript",
            "svg", "[data-widget]", "[data-component-type]", "[data-token]",
            ".social-box", ".btn-share", ".footer-content",
            "a[href^=javascript]",          // nút share fb/tw, in bài
            "a[href^=#box_comment]",        // nút bình luận
            "[data-type=comment]",
            "a[href^=/gui-bai-viet]",       // link "gửi bài viết"
            "div:containsOwn(Gửi câu hỏi)"  // hộp "Gửi câu hỏi, ý kiến... tại đây"
    );

    /** Chỉ giữ lại các thuộc tính cần thiết cho việc hiển thị. */
    private static final Set<String> KEEP_ATTRS =
            Set.of("href", "src", "alt", "poster", "controls", "width");
    /**
     * Scrape breadcrumb từ trang bài viết.
     *
     * @param articleUrl URL đầy đủ của bài viết (https://...)
     * @return {@link BreadcrumbResult} chứa parent + subcategory names/slugs,
     *         hoặc {@link BreadcrumbResult#EMPTY} nếu không đọc được.
     */
    public ArticleScrapeResult scrape(String articleUrl) {
        if (articleUrl == null || articleUrl.isBlank()) {
            return ArticleScrapeResult.EMPTY; // ĐÃ SỬA
        }

        try {
            Document doc = Jsoup.connect(articleUrl)
                    .userAgent(USER_AGENT)
                    .timeout(CONNECT_TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            // Trích xuất cả Breadcrumb và Content
            BreadcrumbResult breadcrumb = extractBreadcrumb(doc, articleUrl);
            String content = extractContent(doc, articleUrl);

            return new ArticleScrapeResult(breadcrumb, content);

        } catch (IOException e) {
            log.debug("Could not fetch article page [{}]: {}", articleUrl, e.getMessage());
            return ArticleScrapeResult.EMPTY; // ĐÃ SỬA
        } catch (Exception e) {
            log.warn("Unexpected error scraping [{}]: {}", articleUrl, e.getMessage());
            return ArticleScrapeResult.EMPTY; // ĐÃ SỬA
        }
    }

    private String extractContent(Document doc, String articleUrl) {
        log.debug("Fetched HTML length={} for [{}]\n{}", 
            doc.html().length(), articleUrl, doc.html().substring(0, Math.min(2000, doc.html().length())));

        for (String selector : CONTENT_SELECTORS) {
            Element contentEl = doc.selectFirst(selector);
            if (contentEl == null) continue;

            // 1. Xóa rác (share, comment, svg, widget, hộp gửi bài...)
            contentEl.select(NOISE_SELECTORS).remove();

            // 2. Video -> thẻ <video> HTML5 sạch
            for (Element video : contentEl.select("video")) {
                Element figure = video.closest("figure");
                Element img = figure != null ? figure.selectFirst("img") : null;
                String src = video.hasAttr("src") ? video.attr("src") : video.select("source").attr("src");
                String poster = img != null ? img.attr("src") : video.attr("poster");
                if (src.isBlank()) { video.remove(); continue; }

                Element clean = new Element("video")
                        .attr("controls", "").attr("width", "100%")
                        .attr("poster", poster).attr("src", src);
                Element container = video.closest("div[id^=video_parent]");
                (container != null ? container : video).replaceWith(clean);
            }

            // 3. <picture> -> <img> đơn giản, sửa lazy-load (data-src -> src)
            for (Element picture : contentEl.select("picture")) {
                Element img = picture.selectFirst("img");
                if (img != null) picture.replaceWith(img.clone()); else picture.remove();
            }
            for (Element img : contentEl.select("img")) {
                String realSrc = img.attr("data-src");
                if (!realSrc.isBlank()) img.attr("src", realSrc);
            }

            // 4. Khử trùng lặp nội dung (Loại bỏ các đoạn <p> hoặc khối bị lặp đôi do layout mobile/desktop)
            Set<String> seenParagraphs = new java.util.HashSet<>();
            for (Element p : contentEl.select("p")) {
                String text = p.text().trim();
                // Nếu đoạn văn dài hơn 15 ký tự và đã xuất hiện trước đó -> xóa bản sao
                if (text.length() > 15) {
                    if (seenParagraphs.contains(text)) {
                        p.remove();
                    } else {
                        seenParagraphs.add(text);
                    }
                }
            }

            // 5. Chỉ giữ whitelist thuộc tính (xóa class, style, id, data-*, loading...)
            for (Element el : contentEl.getAllElements()) {
                el.attributes().asList().stream()
                        .map(Attribute::getKey)
                        .filter(k -> !KEEP_ATTRS.contains(k))
                        .toList()
                        .forEach(el::removeAttr);
            }

            // 6. Dọn thẻ rỗng lặp đến khi hết (xử lý được div lồng div, khoảng trắng/xuống dòng)
            boolean removed;
            do {
                removed = false;
                for (Element el : contentEl.select("p, div, span, a, figure, figcaption, strong, em, b, i, section")) {
                    if (el == contentEl || el.parent() == null) continue;
                    if (!el.hasText() && el.select("img, video, audio, iframe").isEmpty()) {
                        el.remove();
                        removed = true;
                    }
                }
            } while (removed);

            return contentEl.html();
        }
        log.debug("Không tìm thấy nội dung bài viết bằng các selector hiện có cho [{}]", articleUrl);
        return null;
    }

    /**
     * Trích xuất breadcrumb từ Document đã parse.
     * Tách thành method riêng để dễ test với HTML tĩnh (không cần network).
     */
    BreadcrumbResult extractBreadcrumb(Document doc, String articleUrl) {
        // --- Thử CSS selectors theo thứ tự ưu tiên ---
        for (String selector : BREADCRUMB_SELECTORS) {
            Elements links = doc.select(selector);
            BreadcrumbResult result = parseBreadcrumbLinks(links);
            if (result.hasData()) {
                log.debug("Breadcrumb found via selector '{}' for [{}] with {} levels",
                        selector, articleUrl, result.chain().size());
                return result;
            }
        }

        // --- Fallback: đọc meta tag article:section ---
        String metaSection = doc.select("meta[property=article:section]").attr("content").trim();
        if (!metaSection.isBlank()) {
            String slug = slugFromText(metaSection);
            return new BreadcrumbResult(List.of(new BreadcrumbResult.BreadcrumbNode(metaSection, slug)));
        }

        // --- Fallback cuối: thử đọc slug từ URL path ---
        BreadcrumbResult urlResult = extractFromUrl(articleUrl);
        if (urlResult.hasData()) {
            return urlResult;
        }

        return BreadcrumbResult.EMPTY;
    }

    /**
     * Parse danh sách link a từ breadcrumb selector.
     * Bỏ qua link đầu tiên nếu là "Trang chủ" / "Home".
     * Phần tử cuối cùng thường là tiêu đề bài viết (không phải category) → bỏ.
     */
    private BreadcrumbResult parseBreadcrumbLinks(Elements links) {
        if (links.isEmpty()) {
            return BreadcrumbResult.EMPTY;
        }

        // Lọc bỏ "Trang chủ", "Home", link trống và map thẳng thành BreadcrumbNode
        List<BreadcrumbResult.BreadcrumbNode> chain = links.stream()
                .filter(el -> {
                    String text = el.text().trim();
                    String href = el.attr("href").trim();
                    return !text.isBlank()
                            && !text.equalsIgnoreCase("Trang chủ")
                            && !text.equalsIgnoreCase("Home")
                            && !href.equals("/")
                            && !href.isBlank();
                })
                .map(el -> new BreadcrumbResult.BreadcrumbNode(
                        el.text().trim(), 
                        slugFromHref(el.attr("href"))
                ))
                .toList();

        return chain.isEmpty() ? BreadcrumbResult.EMPTY : new BreadcrumbResult(chain);
    }

    /**
     * Fallback: đoán category từ URL path.
     * Ví dụ: "https://vnexpress.net/khoa-hoc-cong-nghe/ai/ten-bai-viet-1234.html"
     * → parentSlug = "khoa-hoc-cong-nghe"
     */
    private BreadcrumbResult extractFromUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            Matcher m = PATH_SLUG_PATTERN.matcher(path);
            if (m.find()) {
                String firstSlug = m.group(1);
                if (firstSlug != null && !firstSlug.isBlank()) {
                    // URL path không có name tiếng Việt, chỉ có slug
                    return new BreadcrumbResult(List.of(new BreadcrumbResult.BreadcrumbNode(null, firstSlug)));
                }
            }
        } catch (Exception ignored) {}
        return BreadcrumbResult.EMPTY;
    }

    /**
     * Trích slug từ href.
     * "/khoa-hoc-cong-nghe/ai" → "ai" (phần cuối cùng có nghĩa)
     * "/khoa-hoc-cong-nghe"    → "khoa-hoc-cong-nghe"
     */
    static String slugFromHref(String href) {
        if (href == null || href.isBlank()) return null;
        // Lấy path, bỏ query string
        String path = href.split("\\?")[0].trim();
        // Tách theo "/"
        String[] parts = path.split("/");
        // Lấy phần cuối không rỗng
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].trim();
            if (!part.isBlank()) {
                return part;
            }
        }
        return null;
    }

    /**
     * Tạo slug xấp xỉ từ tên tiếng Việt.
     * Chỉ dùng cho fallback meta tag (không có href để lấy slug).
     * Ví dụ: "Sức khỏe" → "suc-khoe" (simplified, không diacritic)
     */
    static String slugFromText(String text) {
        if (text == null) return null;
        return text.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9\\-]", "");
    }
}
