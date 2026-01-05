package com.aiservice.infrastructure.qdrant;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.aiservice.application.HybridSearchProcessor;
import com.aiservice.application.RRFMerger;
import com.aiservice.application.dto.SearchParams;
import com.aiservice.controller.dto.DocumentSearchResponse;
import com.aiservice.domain.repository.VectorRepository;
import com.aiservice.infrastructure.feign.DomainServiceClient;
import com.aiservice.infrastructure.feign.dto.PageResponse;
import com.aiservice.infrastructure.feign.dto.ProductPostEsSearchResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@ActiveProfiles({ "local", "secret" })
public class RrfComparisonTest {

    @Autowired
    private DomainServiceClient domainServiceClient;
    @Autowired
    private VectorRepository vectorRepository;
    @Autowired
    private RRFMerger rrfMerger;
    @Autowired
    private HybridSearchProcessor hybridSearchProcessor;

    // 비교 쿼리 목록
    private static final List<String> QUERIES = List.of(
            "갤럭시 S24", // 1. 정확한 모델명 (Keyword/ES 유리)
            "사진 잘 나오는 핸드폰", // 2. 의미 기반 (Vector 유리)
            "아이폰 15 프로 상태 좋은거" // 3. 복합 (RRF 유리)
    );

    @Test
    @DisplayName("Vector vs ES vs RRF 비교 결과 출력")
    void test1() {
        StringBuilder markdownTable = new StringBuilder();
        markdownTable.append("\n### 검색 방식 별 결과 비교\n\n");
        markdownTable.append("| Query | Method | Top 1 Result | Score |\n");
        markdownTable.append("| :--- | :--- | :--- | :--- |\n");

        for (String query : QUERIES) {
            SearchParams params = SearchParams.builder().q(query).build();
            int limit = 5;

            // 1. Vector Search
            List<DocumentSearchResponse> vectorResults = vectorRepository.similaritySearch(params, limit);
            String vectorTop1 = vectorResults.isEmpty() ? "No Result" : formatResult(vectorResults.getFirst());
            String vectorScore = vectorResults.isEmpty() ? "-" : String.format("%.4f", vectorResults.getFirst().score());

            // 2. ES Search
            List<ProductPostEsSearchResponse> esResults = List.of();
            try {
                PageResponse<List<ProductPostEsSearchResponse>> response = domainServiceClient.search(query, limit);
                if (response.contents() != null) {
                    esResults = response.contents();
                }
            } catch (Exception e) {
                log.warn("ES Search Failed: {}", e.getMessage());
            }
            String esTop1 = esResults.isEmpty() ? "No Result" : formatEsResult(esResults.getFirst());
            String esScore = esResults.isEmpty() ? "-" : String.format("%.4f", esResults.getFirst().score());

            // 3. RRF (Merge)
            List<DocumentSearchResponse> rrfResults = rrfMerger.mergeWithRRF(esResults, vectorResults, limit, 1.0, 1.0);
            String rrfTop1 = rrfResults.isEmpty() ? "No Result" : formatResult(rrfResults.getFirst());
            String rrfScore = rrfResults.isEmpty() ? "-" : String.format("%.4f", rrfResults.getFirst().score());

            //4.hybrid
            List<DocumentSearchResponse> search = hybridSearchProcessor.search(params, limit);
            String searchTop1 = search.isEmpty() ? "No Result" : formatResult(search.getFirst());
            String searchScore = search.isEmpty() ? "-" : String.format("%.4f", search.getFirst().score());


            // Add to Table
            markdownTable.append(String.format("| **%s** | Vector | %s | %s |\n", query, vectorTop1, vectorScore));
            markdownTable.append(String.format("| | ES | %s | %s |\n", esTop1, esScore));
            markdownTable.append(String.format("| | **RRF** | **%s** | **%s** |\n", rrfTop1, rrfScore));
            markdownTable.append(String.format("| | **search** | **%s** | **%s** |\n", searchTop1, searchScore));
        }

        System.out.println(markdownTable.toString());
    }

    @Test
    @DisplayName("하이브리드 서치")
    void test2() {
        StringBuilder markdownTable = new StringBuilder();
        markdownTable.append("\n### 검색 방식 별 결과 비교\n\n");
        markdownTable.append("| Query | Method | Top 1 Result | Score |\n");
        markdownTable.append("| :--- | :--- | :--- | :--- |\n");

        for (String query : QUERIES) {
            SearchParams params = SearchParams.builder().q(query).build();
            int limit = 5;

            // 1. Vector Search
            List<DocumentSearchResponse> vectorResults = vectorRepository.similaritySearch(params, limit);
            String vectorTop1 = vectorResults.isEmpty() ? "No Result" : formatResult(vectorResults.getFirst());
            String vectorScore = vectorResults.isEmpty() ? "-" : String.format("%.4f", vectorResults.getFirst().score());

            // 2. ES Search
            List<ProductPostEsSearchResponse> esResults = List.of();
            try {
                PageResponse<List<ProductPostEsSearchResponse>> response = domainServiceClient.search(query, limit);
                if (response.contents() != null) {
                    esResults = response.contents();
                }
            } catch (Exception e) {
                log.warn("ES Search Failed: {}", e.getMessage());
            }
            String esTop1 = esResults.isEmpty() ? "No Result" : formatEsResult(esResults.getFirst());
            String esScore = esResults.isEmpty() ? "-" : String.format("%.4f", esResults.getFirst().score());

            // 3. RRF (Merge)
            List<DocumentSearchResponse> rrfResults = rrfMerger.mergeWithRRF(esResults, vectorResults, limit, 1.0, 1.0);
            String rrfTop1 = rrfResults.isEmpty() ? "No Result" : formatResult(rrfResults.getFirst());
            String rrfScore = rrfResults.isEmpty() ? "-" : String.format("%.4f", rrfResults.getFirst().score());

            // Add to Table
            markdownTable.append(String.format("| **%s** | Vector | %s | %s |\n", query, vectorTop1, vectorScore));
            markdownTable.append(String.format("| | ES | %s | %s |\n", esTop1, esScore));
            markdownTable.append(String.format("| | **RRF** | **%s** | **%s** |\n", rrfTop1, rrfScore));
        }

        System.out.println(markdownTable.toString());
    }

    private String formatResult(DocumentSearchResponse doc) {
        String title = (String) doc.metadata().get("title");
        // title이 메타데이터에 없으면 content 앞부분 사용 (일단 title이 null이면)
        if (title == null || title.isBlank()) {
            // content에서 "제품명: " 파싱 시도
            String content = doc.content();
            if (content != null && content.contains("제품명: ")) {
                int start = content.indexOf("제품명: ") + 5;
                int end = content.indexOf(".", start);
                if (end > start) {
                    title = content.substring(start, end);
                } else {
                    title = content;
                }
            } else {
                title = "Unknown Title";
            }
        }
        return title.length() > 30 ? title.substring(0, 27) + "..." : title;
    }

    private String formatEsResult(ProductPostEsSearchResponse doc) {
        String title = doc.title();
        return title.length() > 30 ? title.substring(0, 27) + "..." : title;
    }
}
