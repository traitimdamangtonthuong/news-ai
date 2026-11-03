package com.newsai.news_ai.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ArticlePageScraperTest {

    private final ArticlePageScraper scraper = new ArticlePageScraper();

    @Test
    void extractBreadcrumb_VnExpressStandard() {
        String html = "<ul class=\"breadcrumb\">" +
                "<li><a href=\"/\">Trang chủ</a></li>" +
                "<li><a href=\"/khoa-hoc\">Khoa học</a></li>" +
                "<li><a href=\"/khoa-hoc/ai\">AI</a></li>" +
                "</ul>";
        Document doc = Jsoup.parse(html);

        BreadcrumbResult result = scraper.extractBreadcrumb(doc, "https://vnexpress.net/test.html");

        assertThat(result.hasData()).isTrue();
        assertThat(result.chain()).hasSize(2);
        
        // Kiểm tra cấp 1 (Parent)
        assertThat(result.chain().get(0).name()).isEqualTo("Khoa học");
        assertThat(result.chain().get(0).slug()).isEqualTo("khoa-hoc");
        
        // Kiểm tra cấp 2 (Sub)
        assertThat(result.chain().get(1).name()).isEqualTo("AI");
        assertThat(result.chain().get(1).slug()).isEqualTo("ai");
    }

    @Test
    void extractBreadcrumb_VnExpress3Levels() {
        String html = "<ul class=\"breadcrumb\">" +
                "<li><a href=\"/\">Trang chủ</a></li>" +
                "<li><a href=\"/xe\">Xe</a></li>" +
                "<li><a href=\"/xe/thi-truong\">Thị trường</a></li>" +
                "<li><a href=\"/xe/thi-truong/the-gioi\">Thế giới</a></li>" +
                "</ul>";
        Document doc = Jsoup.parse(html);

        BreadcrumbResult result = scraper.extractBreadcrumb(doc, "https://vnexpress.net/test.html");

        // Kiểm tra bài toán 3 cấp độ
        assertThat(result.hasData()).isTrue();
        assertThat(result.chain()).hasSize(3);
        assertThat(result.chain().get(0).name()).isEqualTo("Xe");
        assertThat(result.chain().get(1).name()).isEqualTo("Thị trường");
        assertThat(result.chain().get(2).name()).isEqualTo("Thế giới");
    }

    @Test
    void extractBreadcrumb_FallbackMetaTag() {
        String html = "<meta property=\"article:section\" content=\"Sức khỏe\">";
        Document doc = Jsoup.parse(html);

        BreadcrumbResult result = scraper.extractBreadcrumb(doc, "https://example.com/test.html");

        assertThat(result.hasData()).isTrue();
        assertThat(result.chain()).hasSize(1);
        assertThat(result.chain().get(0).name()).isEqualTo("Sức khỏe");
        assertThat(result.chain().get(0).slug()).isEqualTo("suc-khoe");
    }

    @Test
    void extractBreadcrumb_FallbackUrlSlug() {
        Document doc = Jsoup.parse("<html></html>");
        BreadcrumbResult result = scraper.extractBreadcrumb(doc, "https://vnexpress.net/khoa-hoc-cong-nghe/ai/123.html");

        assertThat(result.hasData()).isTrue();
        assertThat(result.chain()).hasSize(1);
        assertThat(result.chain().get(0).name()).isNull(); // Name bị null do chỉ đoán được qua URL
        assertThat(result.chain().get(0).slug()).isEqualTo("khoa-hoc-cong-nghe");
    }

    @Test
    void extractBreadcrumb_Empty() {
        Document doc = Jsoup.parse("<html></html>");
        BreadcrumbResult result = scraper.extractBreadcrumb(doc, "https://vnexpress.net/");

        assertThat(result.hasData()).isFalse();
        assertThat(result.chain()).isEmpty();
    }
}