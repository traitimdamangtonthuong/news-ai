package com.newsai.news_ai.source;

import jakarta.persistence.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Source name is required")
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @NotBlank(message = "Base URL is required")
    @Size(max = 2048)
    @Column(nullable = false, unique = true, length = 2048)
    private String baseUrl;

    @Column(length = 2048)
    private String rssUrl;

    public Source() {
    }

    public Source(String name, String baseUrl, String rssUrl) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.rssUrl = rssUrl;
    }

    public Source(Long id, String name, String baseUrl, String rssUrl) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.rssUrl = rssUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRssUrl() {
        return rssUrl;
    }

    public void setRssUrl(String rssUrl) {
        this.rssUrl = rssUrl;
    }
}