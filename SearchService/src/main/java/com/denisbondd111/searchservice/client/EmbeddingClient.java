package com.denisbondd111.searchservice.client;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final RestTemplate restTemplate;

    @Value("${embedding.service.url}")
    private String embeddingUrl;

    @SuppressWarnings("unchecked")
    public List<Double> getQueryEmbedding(String queryText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "texts", List.of(queryText)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    embeddingUrl,
                    request,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Embedding service returned status: " + response.getStatusCode());
            }

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("embeddings")) {
                throw new RuntimeException("No embeddings in response");
            }

            List<List<Double>> embeddings = (List<List<Double>>) body.get("embeddings");
            if (embeddings == null || embeddings.isEmpty()) {
                throw new RuntimeException("Empty embeddings list");
            }

            return embeddings.get(0);

        } catch (RestClientException e) {
            throw new RuntimeException("Failed to call EmbeddingService", e);
        }
    }
}