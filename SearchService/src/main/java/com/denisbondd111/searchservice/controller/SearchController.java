package com.denisbondd111.searchservice.controller;

import com.denisbondd111.searchservice.dto.SearchRequest;
import com.denisbondd111.searchservice.dto.SearchResponse;
import com.denisbondd111.searchservice.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    public ResponseEntity<SearchResponse> search(
            @RequestBody @Valid SearchRequest request,
            @RequestHeader("X-User-Id") String userId) {

        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SearchResponse response = searchService.search(request, userId);
        return ResponseEntity.ok(response);
    }
}