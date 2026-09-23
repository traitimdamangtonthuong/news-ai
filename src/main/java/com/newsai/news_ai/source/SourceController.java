package com.newsai.news_ai.source;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping("/api/sources")
public class SourceController {
    private final SourceService sourceService;

    public SourceController(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @GetMapping
    public List<Source> getAllSources() {
        return sourceService.getAllSources();
    }

    @GetMapping("/{id}")
    public Source getSourceById(@PathVariable Long id) {
        return sourceService.getSourceById(id);
    }

    @PostMapping
    public Source addSource(@Valid @RequestBody Source source) {
        return sourceService.addSource(source);
    }

    @PutMapping("/{id}")
    public Source updateSource(
            @PathVariable Long id,
            @Valid @RequestBody Source source) {

        return sourceService.updateSource(id, source);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable Long id) {
        sourceService.deleteSource(id);
        return ResponseEntity.noContent().build();
    }
}
