package com.denisbondd111.searchservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {

    private List<SearchResult> results;
    private long totalHits;
    private int page;
    private int size;
}
