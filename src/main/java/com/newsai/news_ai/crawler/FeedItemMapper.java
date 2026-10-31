package com.newsai.news_ai.crawler;

import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.newsai.news_ai.source.Source;
import com.rometools.rome.feed.module.DCModule;
import com.rometools.rome.feed.module.DCSubject;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

/**
 * Converts a fetched feed input stream into usable {@link RssFeedItem}s.
 * ROME parses both RSS 2.0 and Atom; {@link XmlReader} handles feed encoding.
 */
@Component
public class FeedItemMapper {

    private static final List<String> IMAGE_EXTENSIONS =
            List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private static final Pattern HEX_ENTITY = Pattern.compile("&#x([0-9a-fA-F]+);");
    private static final Pattern DEC_ENTITY = Pattern.compile("&#(\\d+);");

    public FeedParseResult parse(InputStream input, Source source) {
        SyndFeed feed;
        try {
            feed = new SyndFeedInput().build(new XmlReader(input));
        } catch (Exception e) {
            throw new RssCrawlException("Unsupported or malformed feed: " + e.getMessage(), e);
        }

        List<SyndEntry> entries = feed.getEntries();
        if (entries == null || entries.isEmpty()) {
            return new FeedParseResult(List.of(), 0);
        }

        String origin = extractOrigin(source.getBaseUrl());

        List<RssFeedItem> items = new ArrayList<>();
        int skippedMalformed = 0;

        for (SyndEntry entry : entries) {
            String title = clean(entry.getTitle());
            String link = entry.getLink();

            if (title == null || link == null || link.isBlank()) {
                skippedMalformed++;
                continue;
            }

            String absoluteUrl = resolve(link, origin);
            if (absoluteUrl == null) {
                skippedMalformed++;
                continue;
            }

            String description = entry.getDescription() == null
                    ? null
                    : stripHtml(entry.getDescription().getValue());

            String content = extractContent(entry);

            String imageUrl = resolve(extractImageUrl(entry), origin);

            LocalDateTime publishedAt = toLocalDateTime(entry.getPublishedDate() != null
                    ? entry.getPublishedDate()
                    : entry.getUpdatedDate());

            List<String> categories = extractCategories(entry, feed);

            items.add(new RssFeedItem(
                    title, absoluteUrl, description, content, imageUrl, publishedAt, categories));
        }

        return new FeedParseResult(items, skippedMalformed);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String collapsed = value.replaceAll("\\s+", " ").trim();
        return collapsed.isEmpty() ? null : collapsed;
    }

    /**
     * Cleans a feed-provided HTML description (VnExpress-style
     * {@code <a href=...><img .../></a></br>Lead text...}) into plain text, so
     * the stored description can be shown directly without rendering raw
     * markup. Returns null when nothing but tags/whitespace remains (an
     * image-only lead has no textual description).
     */
    private String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        String withoutTags = html.replaceAll("<[^>]*>", " ");
        String text = decodeEntities(withoutTags).replaceAll("\\s+", " ").trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Decodes the HTML entities commonly found in feed HTML. Numeric
     * references are handled generically; {@code &amp;} is decoded last so
     * double-escaped sequences are not collapsed twice.
     */
    private String decodeEntities(String text) {
        String decoded = decodeNumericEntities(text, HEX_ENTITY, 16);
        decoded = decodeNumericEntities(decoded, DEC_ENTITY, 10);
        return decoded
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&hellip;", "…")
                .replace("&ndash;", "-")
                .replace("&mdash;", "-")
                .replace("&amp;", "&");
    }

    private String decodeNumericEntities(String text, Pattern pattern, int radix) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement;
            try {
                replacement = new String(Character.toChars(
                        Integer.parseInt(matcher.group(1), radix)));
            } catch (IllegalArgumentException e) {
                replacement = matcher.group();
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Category names from the entry's {@code <category>} tags (Atom uses
     * {@code term}; when that is blank the {@code label} is used) plus any
     * Dublin Core {@code <dc:subject>} values. Entry-level metadata wins;
     * channel/feed-level metadata is only consulted when the entry declares
     * nothing, preserving the historic channel fallback.
     */
    private List<String> extractCategories(SyndEntry entry, SyndFeed feed) {
        List<String> names = new ArrayList<>();
        addCategoryNames(names, entry.getCategories());
        addDcSubjects(names, (DCModule) entry.getModule(DCModule.URI));

        if (names.isEmpty()) {
            addCategoryNames(names, feed.getCategories());
            addDcSubjects(names, (DCModule) feed.getModule(DCModule.URI));
        }
        return names;
    }

    private void addCategoryNames(List<String> names, List<SyndCategory> categories) {
        if (categories == null) {
            return;
        }
        for (SyndCategory category : categories) {
            String raw = category.getName();
            if (raw == null || raw.isBlank()) {
                raw = category.getLabel();
            }
            addUnique(names, clean(raw));
        }
    }

    private void addDcSubjects(List<String> names, DCModule module) {
        if (module == null) {
            return;
        }
        List<? extends DCSubject> subjects = module.getSubjects();
        if (subjects == null) {
            return;
        }
        for (DCSubject subject : subjects) {
            addUnique(names, clean(subject.getValue()));
        }
    }

    private void addUnique(List<String> names, String name) {
        if (name != null && !names.contains(name)) {
            names.add(name);
        }
    }

    /** Full text lives in {@code <content:encoded>} when present. */
    private String extractContent(SyndEntry entry) {
        List<SyndContent> contents = entry.getContents();
        if (contents != null) {
            for (SyndContent content : contents) {
                if (content.getValue() != null && !content.getValue().isBlank()) {
                    return content.getValue();
                }
            }
        }
        return null;
    }

    /** First image-looking enclosure (Media RSS / RSS {@code <enclosure>}). */
    private String extractImageUrl(SyndEntry entry) {
        List<SyndEnclosure> enclosures = entry.getEnclosures();
        if (enclosures == null) {
            return null;
        }
        for (SyndEnclosure enclosure : enclosures) {
            String url = enclosure.getUrl();
            String type = enclosure.getType();
            if (url == null || url.isBlank()) {
                continue;
            }
            if (type != null && type.startsWith("image/")) {
                return url;
            }
            String lower = url.toLowerCase();
            if (type == null && IMAGE_EXTENSIONS.stream().anyMatch(lower::endsWith)) {
                return url;
            }
        }
        return null;
    }

    /** Feed timestamps are normalized to UTC wall-clock time. */
    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * Resolves protocol-relative ({@code //...}) and root-relative ({@code /...})
     * links against the source's base URL; otherwise returns the link as-is.
     */
    private String resolve(String link, String origin) {
        if (link == null || link.isBlank()) {
            return null;
        }

        if (link.startsWith("//")) {
            if (origin != null) {
                int schemeEnd = origin.indexOf("://");
                if (schemeEnd > 0) {
                    return origin.substring(0, schemeEnd) + ":" + link;
                }
            }
            return "https:" + link;
        }

        if (link.startsWith("/")) {
            if (origin == null) {
                return null;
            }
            return origin + link;
        }

        return link;
    }

    private String extractOrigin(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(baseUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            return uri.getScheme().toLowerCase() + "://" + uri.getHost().toLowerCase();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}