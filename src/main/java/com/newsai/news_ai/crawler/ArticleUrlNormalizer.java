package com.newsai.news_ai.crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Produces a canonical key for duplicate detection.
 *
 * <p>Phase-A scope: scheme+host lowercased, fragment removed, common tracking
 * parameters dropped, query parameters sorted, trailing slash removed. The
 * original URL is always what gets stored — normalization is used only for
 * comparison inside a crawl run.
 */
public final class ArticleUrlNormalizer {

    private static final List<String> TRACKING_PARAMS = List.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid");

    private ArticleUrlNormalizer() {
    }

    public static String normalize(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        String trimmed = url.trim();

        // Protocol-relative links get an https scheme so they can be parsed.
        if (trimmed.startsWith("//")) {
            trimmed = "https:" + trimmed;
        }

        try {
            URI uri = new URI(trimmed);

            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase();
            String host = uri.getHost();
            if (host == null) {
                return trimmed;
            }
            host = host.toLowerCase();

            String path = uri.getPath() == null ? "" : uri.getPath();
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            List<String> keptParams = new ArrayList<>();
            if (uri.getRawQuery() != null) {
                for (String pair : uri.getRawQuery().split("&")) {
                    int eq = pair.indexOf('=');
                    String name = eq >= 0 ? pair.substring(0, eq) : pair;
                    if (!TRACKING_PARAMS.contains(name.toLowerCase())) {
                        keptParams.add(pair);
                    }
                }
            }
            keptParams.sort(Comparator.naturalOrder());

            StringBuilder normalized = new StringBuilder(scheme).append("://").append(host).append(path);
            if (!keptParams.isEmpty()) {
                normalized.append('?').append(String.join("&", keptParams));
            }
            return normalized.toString();
        } catch (URISyntaxException e) {
            return trimmed;
        }
    }
}