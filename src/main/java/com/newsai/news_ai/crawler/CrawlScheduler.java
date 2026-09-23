package com.newsai.news_ai.crawler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * STEP 9 scaffold: periodic crawling. Disabled unless
 * {@code crawler.scheduling.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "crawler.scheduling.enabled", havingValue = "true")
public class CrawlScheduler {

    private final RssCrawlerService rssCrawlerService;

    public CrawlScheduler(RssCrawlerService rssCrawlerService) {
        this.rssCrawlerService = rssCrawlerService;
    }

    @Scheduled(fixedDelayString = "${crawler.scheduling.fixed-delay:PT1H}")
    public void crawlAllScheduled() {
        rssCrawlerService.crawlAll();
    }
}