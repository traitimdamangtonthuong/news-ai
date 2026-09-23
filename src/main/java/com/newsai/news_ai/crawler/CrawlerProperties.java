package com.newsai.news_ai.crawler;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties bound from the {@code crawler.*} prefix in application.properties.
 */
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    /** HTTP connect timeout when downloading a feed. */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /** HTTP read timeout when downloading a feed. */
    private Duration readTimeout = Duration.ofSeconds(15);

    /** User-Agent header sent to news sites. */
    private String userAgent = "news-ai-crawler/0.1";

    /** Maximum number of feed items considered per crawl. */
    private int maxItemsPerFeed = 200;

    /** Scheduling options (STEP 9, disabled by default). */
    private Scheduling scheduling = new Scheduling();

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getMaxItemsPerFeed() {
        return maxItemsPerFeed;
    }

    public void setMaxItemsPerFeed(int maxItemsPerFeed) {
        this.maxItemsPerFeed = maxItemsPerFeed;
    }

    public Scheduling getScheduling() {
        return scheduling;
    }

    public void setScheduling(Scheduling scheduling) {
        this.scheduling = scheduling;
    }

    public static class Scheduling {
        private boolean enabled = false;
        private Duration fixedDelay = Duration.ofHours(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getFixedDelay() {
            return fixedDelay;
        }

        public void setFixedDelay(Duration fixedDelay) {
            this.fixedDelay = fixedDelay;
        }
    }
}