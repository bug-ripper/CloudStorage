package com.denisbondd111.searchservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResult {

    private String fileId;
    private String originalFilename;
    private double score;
    private List<String> snippets;   // выделенные фрагменты (highlights)
}