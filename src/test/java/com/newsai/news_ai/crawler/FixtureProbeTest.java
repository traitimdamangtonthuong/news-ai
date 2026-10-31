package com.newsai.news_ai.crawler;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.newsai.news_ai.source.Source;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// Temporary diagnostic: parses the 3 fixture feeds and prints mapped fields.
class FixtureProbeTest {

    private final FeedItemMapper mapper = new FeedItemMapper();

    @Test
    void probe() throws Exception {
        for (String name : List.of(
                "fixtures/feed-vnexpress.xml",
                "fixtures/feed-thanhnien.xml",
                "fixtures/feed-nhandan.xml")) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
                assertNotNull(in, "missing " + name);
                FeedParseResult result = mapper.parse(in,
                        new Source(1L, "probe", "https://example.com", null));
                System.out.println("=== " + name
                        + "  items=" + result.items().size()
                        + "  skipped=" + result.skippedMalformed() + " ===");
                for (RssFeedItem item : result.items().subList(0, Math.min(2, result.items().size()))) {
                    System.out.println("  title         : " + item.title());
                    System.out.println("  publishedAt   : " + item.publishedAt());
                    System.out.println("  imageUrl      : " + item.imageUrl());
                    System.out.println("  categories    : " + item.categories());
                    System.out.println("  description   : " + item.description());
                    System.out.println("  ---");
                }
            }
        }
    }
}