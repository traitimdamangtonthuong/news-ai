package com.newsai.news_ai.crawler;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual crawl triggers:
 *
 * <pre>
 * POST /api/crawl                -> crawl every source with an RSS URL
 * POST /api/crawl/sources/{id}   -> crawl one source (404 if unknown)
 * </pre>
 */
@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    private final RssCrawlerService rssCrawlerService;

    public CrawlController(RssCrawlerService rssCrawlerService) {
        this.rssCrawlerService = rssCrawlerService;
    }

    @PostMapping
    public List<CrawlResult> crawlAll() {
        return rssCrawlerService.crawlAll();
    }

    @PostMapping("/sources/{sourceId}")
    public CrawlResult crawlSource(@PathVariable Long sourceId) {
        return rssCrawlerService.crawlSource(sourceId);
    }
}