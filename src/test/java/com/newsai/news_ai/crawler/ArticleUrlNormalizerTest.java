package com.newsai.news_ai.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArticleUrlNormalizerTest {

    @Test
    void stripsTrackingParamsLowercasesHostAndRemovesTrailingSlash() {
        // Only the host is lowercased; the path keeps its original case.
        assertEquals(
                "https://example.com/Path",
                ArticleUrlNormalizer.normalize("https://Example.com/Path/?UTM_Medium=x&fbclid=abc&utm_source=t"));
    }

    @Test
    void removesFragment() {
        assertEquals(
                "https://example.com/a",
                ArticleUrlNormalizer.normalize("https://example.com/a#some-fragment"));
    }

    @Test
    void handlesProtocolRelativeLinks() {
        assertEquals("https://example.com/a", ArticleUrlNormalizer.normalize("//example.com/a"));
    }

    @Test
    void sortsRemainingQueryParameters() {
        assertEquals(
                "https://example.com/p?a=1&b=3&z=2",
                ArticleUrlNormalizer.normalize("https://example.com/p?z=2&a=1&b=3"));
    }

    @Test
    void leavesUnparseableInputUnchanged() {
        String weird = "not a url";
        assertEquals(weird, ArticleUrlNormalizer.normalize(weird));
    }
}