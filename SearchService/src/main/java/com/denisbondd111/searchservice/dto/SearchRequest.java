package com.denisbondd111.searchservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchRequest {

    @NotBlank(message = "Query must not be empty")
    @Size(min = 1, max = 500, message = "Query length must be between 1 and 500 characters")
    private String query;

    private int page = 1;

    private int size = 10;
}
