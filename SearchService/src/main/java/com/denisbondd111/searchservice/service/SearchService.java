package com.denisbondd111.searchservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.denisbondd111.searchservice.client.EmbeddingClient;
import com.denisbondd111.searchservice.dto.SearchRequest;
import com.denisbondd111.searchservice.dto.SearchResponse;
import com.denisbondd111.searchservice.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchClient esClient;
    private final EmbeddingClient embeddingClient;

    @Value("${elasticsearch.index}")
    private String indexName;

    @Value("${search.page.default-size}")
    private int defaultPageSize;

    @Value("${search.page.max-size}")
    private int maxPageSize;

    @Value("${search.rrf.window-size}")
    private long rrfWindowSize;

    @Value("${search.rrf.rank-constant}")
    private int rrfRankConstant;

    @Value("${search.knn.candidates}")
    private int knnCandidates;

    @Value("${search.knn.k}")
    private int knnK;

    public SearchResponse search(SearchRequest request, String userId) {
        if (request.getSize() <= 0 || request.getSize() > maxPageSize) {
            request.setSize(defaultPageSize);
        }
        if (request.getPage() < 1) {
            request.setPage(1);
        }

        int from = (request.getPage() - 1) * request.getSize();
        int fetchSize = (int) Math.max(rrfWindowSize, request.getSize() * 3);  // берём запас для rerank

        List<Double> queryVector;
        try {
            queryVector = embeddingClient.getQueryEmbedding(request.getQuery());
        } catch (Exception e) {
            log.error("Failed to get embedding", e);
            return emptyResponse(request);
        }

        // 1. BM25 поиск (text)
        co.elastic.clients.elasticsearch.core.SearchRequest bm25Req = co.elastic.clients.elasticsearch.core.SearchRequest.of(sr -> sr
                .index(indexName)
                .size(fetchSize)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m.match(ma -> ma.field("text").query(request.getQuery())))
                                .filter(f -> f.term(t -> t.field("user_id").value(FieldValue.of(userId))))
                        )
                )
                .highlight(h -> h.fields("text", hf -> hf.preTags("<b>").postTags("</b>").fragmentSize(150).numberOfFragments(3)))
        );

        // 2. KNN поиск (vector)
        co.elastic.clients.elasticsearch.core.SearchRequest knnReq = co.elastic.clients.elasticsearch.core.SearchRequest.of(sr -> sr
                .index(indexName)
                .size(fetchSize)
                .knn(k -> k
                        .field("embedding")
                        .queryVector(queryVector.stream().map(Double::floatValue).toList())
                        .k(fetchSize)
                        .numCandidates(knnCandidates)
                        .filter(f -> f.term(t -> t.field("user_id").value(FieldValue.of(userId))))
                )
                .highlight(h -> h.fields("text", hf -> hf.preTags("<b>").postTags("</b>").fragmentSize(150).numberOfFragments(3)))
        );

        try {
            co.elastic.clients.elasticsearch.core.SearchResponse<Map> bm25Resp = esClient.search(bm25Req, Map.class);
            co.elastic.clients.elasticsearch.core.SearchResponse<Map> knnResp = esClient.search(knnReq, Map.class);

            // Собираем все уникальные документы из двух результатов
            Map<String, Hit<Map>> docMap = new HashMap<>();

            // BM25 hits
            bm25Resp.hits().hits().forEach(hit -> {
                String id = hit.id();
                docMap.put(id, hit);
            });

            // KNN hits
            knnResp.hits().hits().forEach(hit -> {
                String id = hit.id();
                docMap.putIfAbsent(id, hit);  // если уже есть — оставляем BM25 версию или можно мержить
            });

            // Применяем RRF в коде
            List<ScoredDoc> scoredDocs = new ArrayList<>();
            int k = rrfRankConstant;

            for (Hit<Map> hit : docMap.values()) {
                double bm25Rank = getRank(bm25Resp.hits().hits(), hit);
                double knnRank = getRank(knnResp.hits().hits(), hit);

                double bm25Score = bm25Rank > 0 ? 1.0 / (k + bm25Rank) : 0.0;
                double knnScore = knnRank > 0 ? 1.0 / (k + knnRank) : 0.0;

                double rrfScore = bm25Score + knnScore;

                scoredDocs.add(new ScoredDoc(hit, rrfScore));
            }

            // Сортируем по RRF score descending
            scoredDocs.sort((a, b) -> Double.compare(b.score, a.score));

            // Берём нужную страницу
            int end = Math.min(from + request.getSize(), scoredDocs.size());
            List<SearchResult> results = new ArrayList<>();
            for (int i = from; i < end; i++) {
                ScoredDoc sd = scoredDocs.get(i);
                Hit<Map> hit = sd.hit;

                Map<String, Object> source = hit.source();
                if (source == null) continue;

                String fileId = (String) source.get("file_id");
                String filename = (String) source.get("original_filename");

                List<String> snippets = new ArrayList<>();
                var hl = hit.highlight();
                if (hl != null && hl.containsKey("text")) {
                    snippets.addAll(hl.get("text"));
                }

                results.add(SearchResult.builder()
                        .fileId(fileId)
                        .originalFilename(filename)
                        .score(sd.score)
                        .snippets(snippets)
                        .build());
            }

            return SearchResponse.builder()
                    .results(results)
                    .totalHits(scoredDocs.size())
                    .page(request.getPage())
                    .size(request.getSize())
                    .build();

        } catch (IOException e) {
            log.error("ES search failed", e);
            return emptyResponse(request);
        }
    }

    // Вспомогательный класс
    private static class ScoredDoc {
        Hit<Map> hit;
        double score;

        ScoredDoc(Hit<Map> hit, double score) {
            this.hit = hit;
            this.score = score;
        }
    }

    // Получить ранг документа в списке хитов (1-based, 0 если не найден)
    private double getRank(List<Hit<Map>> hits, Hit<Map> target) {
        for (int i = 0; i < hits.size(); i++) {
            if (hits.get(i).id().equals(target.id())) {
                return i + 1;
            }
        }
        return 0;
    }

    private SearchResponse emptyResponse(SearchRequest request) {
        return SearchResponse.builder()
                .results(Collections.emptyList())
                .totalHits(0)
                .page(request.getPage())
                .size(request.getSize())
                .build();
    }
}
