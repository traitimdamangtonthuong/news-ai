package com.newsai.news_ai.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.newsai.news_ai.error.ResourceNotFoundException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CrawlController.class)
class CrawlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RssCrawlerService rssCrawlerService;

    @Test
    void crawlAllReturnsPerSourceResults() throws Exception {
        when(rssCrawlerService.crawlAll()).thenReturn(List.of(
                new CrawlResult(1L, "Test Source", 3, 1, 1, 1, 0, List.of())));

        String body = mockMvc.perform(post("/api/crawl"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        assertEquals(1, root.size());
        assertEquals("Test Source", root.get(0).get("sourceName").asText());
        assertEquals(3, root.get(0).get("fetched").asInt());
        assertEquals(1, root.get(0).get("created").asInt());
        assertEquals(1, root.get(0).get("updated").asInt());
        assertEquals(1, root.get(0).get("unchanged").asInt());
        assertEquals(0, root.get(0).get("failed").asInt());
    }

    @Test
    void crawlSourceReturnsResult() throws Exception {
        when(rssCrawlerService.crawlSource(1L))
                .thenReturn(new CrawlResult(1L, "Test Source", 5, 2, 2, 1, 0, List.of()));

        String body = mockMvc.perform(post("/api/crawl/sources/1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        assertEquals(1L, root.get("sourceId").asLong());
        assertEquals(5, root.get("fetched").asInt());
        assertEquals(2, root.get("created").asInt());
        assertEquals(2, root.get("updated").asInt());
        assertEquals(1, root.get("unchanged").asInt());
        assertEquals(0, root.get("failed").asInt());
    }

    @Test
    void unknownSourceReturns404JsonError() throws Exception {
        when(rssCrawlerService.crawlSource(999L))
                .thenThrow(new ResourceNotFoundException("Source", 999L));

        String body = mockMvc.perform(post("/api/crawl/sources/999"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        assertEquals(404, root.get("status").asInt());
        assertEquals("/api/crawl/sources/999", root.get("path").asText());
        assertEquals("Source not found with id: 999", root.get("message").asText());
    }
}