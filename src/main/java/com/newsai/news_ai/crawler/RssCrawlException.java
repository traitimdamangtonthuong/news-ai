package com.newsai.news_ai.crawler;

/**
 * Unchecked exception for feed download / parse failures.
 * Caught by {@link RssCrawlerService} and reported per source.
 */
public class RssCrawlException extends RuntimeException {

    public RssCrawlException(String message) {
        super(message);
    }

    public RssCrawlException(String message, Throwable cause) {
        super(message, cause);
    }
}