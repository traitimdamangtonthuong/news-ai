package com.newsai.news_ai.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RssCategoryResolverTest {

    private final RssCategoryResolver resolver = new RssCategoryResolver();

    @Test
    void extractsSlugProperly() {
        assertEquals("the-gioi", resolver.extractSlug("https://vnexpress.net/rss/the-gioi.rss"));
        assertEquals("suc-khoe", resolver.extractSlug("https://vnexpress.net/rss/suc-khoe.rss?utm_source=rss"));
        assertEquals("khoa-hoc", resolver.extractSlug("https://thanhnien.vn/rss/khoa-hoc.xml"));
        assertEquals("thegioi-1231", resolver.extractSlug("https://nhandan.vn/rss/thegioi-1231.rss"));
        assertNull(resolver.extractSlug(null));
        assertNull(resolver.extractSlug(""));
    }

    @Test
    void resolvesKnownCategories() {
        assertEquals("Thế giới", resolver.resolveCategoryName("https://vnexpress.net/rss/the-gioi.rss"));
        assertEquals("Thế giới", resolver.resolveCategoryName("https://nhandan.vn/rss/thegioi-1231.rss"));
        assertEquals("Sức khỏe", resolver.resolveCategoryName("https://vnexpress.net/rss/suc-khoe.rss"));
        assertEquals("Khoa học công nghệ", resolver.resolveCategoryName("https://vnexpress.net/rss/khoa-hoc.rss"));
        assertEquals("Thể thao", resolver.resolveCategoryName("https://thanhnien.vn/rss/the-thao.rss"));
        assertEquals("Xe", resolver.resolveCategoryName("https://vnexpress.net/rss/oto-xe-may.rss"));
        assertEquals("Tin mới nhất", resolver.resolveCategoryName("https://vnexpress.net/rss/tin-moi-nhat.rss"));
    }

    @Test
    void generalFeedsResolveToNull() {
        assertNull(resolver.resolveCategoryName("https://vnexpress.net/rss/trang-chu.rss"));
        assertNull(resolver.resolveCategoryName("https://vnexpress.net/rss/tin-nong.rss"));
    }
}
