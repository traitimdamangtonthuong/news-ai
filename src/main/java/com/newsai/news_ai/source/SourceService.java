package com.newsai.news_ai.source;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newsai.news_ai.error.ResourceNotFoundException;

@Service 
public class SourceService {
    private final SourceRepository sourceRepository;

    public SourceService(
        SourceRepository sourceRepository
    ) {
        this.sourceRepository = sourceRepository;
    }

    public List<Source> getAllSources() {
        return sourceRepository.findAll();
    };

    public Source getSourceById(Long id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Source", id));
    }

    public Source addSource(Source source) {
        source.setId(null);
        return sourceRepository.save(source);
    }

    @Transactional
    public Source updateSource(Long id, Source updatedSource) {
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Source", id));

        source.setName(updatedSource.getName());
        source.setBaseUrl(updatedSource.getBaseUrl());
        source.setRssUrl(updatedSource.getRssUrl());

        return sourceRepository.save(source);
    }

    public void deleteSource(Long id) {
        if (!sourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Source", id);
        }

        sourceRepository.deleteById(id);
    }
}

