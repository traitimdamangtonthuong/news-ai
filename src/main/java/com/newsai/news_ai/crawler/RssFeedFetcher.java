package com.newsai.news_ai.crawler;

import java.io.InputStream;
import java.net.http.HttpClient;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Downloads an RSS/Atom feed as a stream using JDK HttpClient via RestClient.
 * Redirects are followed; connect/read timeouts and a User-Agent come from
 * {@link CrawlerProperties}.
 */
@Component
public class RssFeedFetcher {

    private final RestClient restClient;

    public RssFeedFetcher(CrawlerProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", properties.getUserAgent())
                .defaultHeader("Accept",
                        "application/rss+xml, application/atom+xml, application/xml;q=0.9, */*;q=0.8")
                .build();
    }

    /**
     * @return open input stream with the feed body; caller must close it
     * @throws RssCrawlException if the feed cannot be fetched
     */
    public InputStream fetch(String feedUrl) {
        try {
            return restClient.get()
                    .uri(feedUrl)
                    .retrieve()
                    .body(InputStream.class);
        } catch (RestClientResponseException e) {
            throw new RssCrawlException(
                    "Failed to fetch feed '" + feedUrl + "': HTTP " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new RssCrawlException("Failed to access feed '" + feedUrl + "': " + e.getMessage());
        }
    }
}