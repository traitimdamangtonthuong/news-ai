package com.newsai.news_ai.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.newsai.news_ai.source.Source;

class FeedItemMapperTest {

    private final FeedItemMapper mapper = new FeedItemMapper();

    private static final String RSS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/">
              <channel>
                <title>Test Feed</title>
                <link>https://example.com</link>
                <description>desc</description>
                <item>
                  <title>   First Item  </title>
                  <link>https://example.com/1?utm_source=x&amp;fbclid=abc</link>
                  <category>Technology</category>
                  <description>A short description</description>
                  <content:encoded><![CDATA[<p>Full content here</p>]]></content:encoded>
                  <pubDate>Tue, 02 Jan 2026 03:04:05 GMT</pubDate>
                  <enclosure url="https://example.com/img1.jpg" type="image/jpeg" length="1"/>
                </item>
                <item>
                  <title>Protocol relative</title>
                  <link>//example.com/2</link>
                  <description>B</description>
                </item>
                <item>
                  <link>https://example.com/no-title</link>
                </item>
              </channel>
            </rss>
            """;

    @Test
    void mapsRssFieldsProperly() throws Exception {
        FeedParseResult result = mapper.parse(
                new ByteArrayInputStream(RSS.getBytes(StandardCharsets.UTF_8)),
                source());

        assertEquals(3, result.skippedMalformed() + result.items().size());
        assertEquals(1, result.skippedMalformed());
        assertEquals(2, result.items().size());

        RssFeedItem first = result.items().get(0);
        assertEquals("First Item", first.title());
        assertEquals("https://example.com/1?utm_source=x&fbclid=abc", first.url());
        assertEquals("A short description", first.description());
        assertEquals("<p>Full content here</p>", first.content());
        assertEquals("https://example.com/img1.jpg", first.imageUrl());
        assertEquals(LocalDateTime.of(2026, 1, 2, 3, 4, 5), first.publishedAt());
        assertEquals(java.util.List.of("Technology"), first.categories());

        // Protocol-relative link resolved against the source base URL.
        assertEquals("https://example.com/2", result.items().get(1).url());
        // Item without <category> tags gets an empty category list.
        assertEquals(java.util.List.of(), result.items().get(1).categories());
    }

    @Test
    void atomCategoryTermIsUsedAndLabelIsFallback() throws Exception {
        String atom = """
                <?xml version="1.0" encoding="utf-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <title>Atom Feed</title>
                  <id>urn:uuid:feed</id>
                  <updated>2026-01-01T00:00:00Z</updated>
                  <entry>
                    <title>Term item</title>
                    <id>urn:uuid:1</id>
                    <updated>2026-01-01T00:00:00Z</updated>
                    <link href="https://example.com/atom1"/>
                    <category term="Finance" label="Money"/>
                  </entry>
                  <entry>
                    <title>Label-only item</title>
                    <id>urn:uuid:2</id>
                    <updated>2026-01-01T00:00:00Z</updated>
                    <link href="https://example.com/atom2"/>
                    <category label="Sports"/>
                  </entry>
                </feed>
                """;

        FeedParseResult result = mapper.parse(
                new ByteArrayInputStream(atom.getBytes(StandardCharsets.UTF_8)), source());

        assertEquals(List.of("Finance"), result.items().get(0).categories());
        assertEquals(List.of("Sports"), result.items().get(1).categories());
    }

    @Test
    void dublinCoreSubjectsAreExtractedFromEntry() throws Exception {
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <channel>
                    <title>Feed</title>
                    <link>https://example.com</link>
                    <description>d</description>
                    <category>ChannelFallback</category>
                    <dc:subject>FeedLevel</dc:subject>
                    <item>
                      <title>Item with dc:subject</title>
                      <link>https://example.com/5</link>
                      <dc:subject>Politics</dc:subject>
                    </item>
                  </channel>
                </rss>
                """;

        FeedParseResult result = mapper.parse(
                new ByteArrayInputStream(rss.getBytes(StandardCharsets.UTF_8)), source());

        assertEquals(List.of("Politics"), result.items().get(0).categories());
    }

    @Test
    void channelLevelMetadataIsUsedAsFallback() throws Exception {
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <channel>
                    <title>Feed</title>
                    <link>https://example.com</link>
                    <description>d</description>
                    <category>ChannelFallback</category>
                    <dc:subject>FeedLevel</dc:subject>
                    <item>
                      <title>Item with no entry metadata</title>
                      <link>https://example.com/6</link>
                    </item>
                  </channel>
                </rss>
                """;

        FeedParseResult result = mapper.parse(
                new ByteArrayInputStream(rss.getBytes(StandardCharsets.UTF_8)), source());

        assertEquals(List.of("ChannelFallback", "FeedLevel"), result.items().get(0).categories());
    }

    @Test
    void entryMetadataWinsOverChannelMetadata() throws Exception {
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <title>Feed</title>
                    <link>https://example.com</link>
                    <description>d</description>
                    <category>ChannelCategory</category>
                    <item>
                      <title>Item with own category</title>
                      <link>https://example.com/7</link>
                      <category>EntryCategory</category>
                    </item>
                  </channel>
                </rss>
                """;

        FeedParseResult result = mapper.parse(
                new ByteArrayInputStream(rss.getBytes(StandardCharsets.UTF_8)), source());

        assertEquals(List.of("EntryCategory"), result.items().get(0).categories());
    }

    @Test
    void htmlDescriptionIsStrippedToPlainTextWithEntitiesDecoded() throws Exception {
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <title>Feed</title>
                    <link>https://example.com</link>
                    <description>d</description>
                    <item>
                      <title>VnExpress-style description</title>
                      <link>https://example.com/html-desc</link>
                      <description><![CDATA[<a href="https://example.com/story"><img src="https://example.com/i.jpg"></a></br>Người dân có thể tự đăng ký <b>không nhận</b> cuộc gọi quảng cáo&hellip; theo đề xuất&#39; mới.]]></description>
                    </item>
                    <item>
                      <title>Image-only description</title>
                      <link>https://example.com/img-only</link>
                      <description><![CDATA[<a href="https://example.com/story2"><img src="https://example.com/i2.jpg"></a>]]></description>
                    </item>
                    <item>
                      <title>Plain description untouched</title>
                      <link>https://example.com/plain</link>
                      <description>Một mô tả thuần văn bản.</description>
                    </item>
                  </channel>
                </rss>
                """;

        FeedParseResult result = mapper.parse(
                new ByteArrayInputStream(rss.getBytes(StandardCharsets.UTF_8)), source());

        assertEquals(3, result.items().size());
        // Tags removed, whitespace collapsed, entities (&hellip;, &#39;) decoded.
        assertEquals(
                "Người dân có thể tự đăng ký không nhận cuộc gọi quảng cáo… theo đề xuất' mới.",
                result.items().get(0).description());
        // Markup-only description has no textual lead -> null, not an empty string.
        assertNull(result.items().get(1).description());
        // Descriptions without HTML pass through unchanged.
        assertEquals("Một mô tả thuần văn bản.", result.items().get(2).description());
    }

    private Source source() {
        return new Source(1L, "Test Source", "https://example.com", "https://example.com/feed.rss");
    }
}