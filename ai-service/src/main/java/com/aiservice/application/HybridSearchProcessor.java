package com.aiservice.application;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import com.aiservice.application.dto.SearchParams;
import com.aiservice.controller.dto.DocumentSearchResponse;
import com.aiservice.domain.repository.VectorRepository;
import com.aiservice.infrastructure.feign.DomainServiceClient;
import com.aiservice.infrastructure.feign.dto.PageResponse;
import com.aiservice.infrastructure.feign.dto.ProductPostEsSearchResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchProcessor {

	private final DomainServiceClient domainServiceClient;
	private final VectorRepository vectorRepository;
	private final RRFMerger rrfMerger;
	private final ChatModel chatModel;

	public List<DocumentSearchResponse> search(SearchParams searchParams, int limit) {
		log.info("하이브리드 검색 시작(searchParams): {}", searchParams);

		WeightResult weightResult = getQueryIntent(searchParams.q());

		CompletableFuture<List<DocumentSearchResponse>> vectorFuture = CompletableFuture.supplyAsync(() -> {
			List<DocumentSearchResponse> result = vectorRepository.similaritySearch(searchParams, limit);
			log.info("벡터 검색 결과: {} 건", result.size());
			return result;
		});

		CompletableFuture<List<ProductPostEsSearchResponse>> esFuture = CompletableFuture
				.supplyAsync(() -> searchFromElasticsearch(searchParams.q(), limit));

		List<DocumentSearchResponse> vectorResults = vectorFuture.join();
		List<ProductPostEsSearchResponse> esResults = esFuture.join();

		if (esResults.isEmpty()) {
			log.warn("ES 검색 실패 또는 결과 없음, 벡터 검색 결과만 사용");
			return vectorResults;
		}

		log.info("ES 검색 결과: {} 건", esResults.size());

		List<DocumentSearchResponse> mergedResults = rrfMerger.mergeWithRRF(
				esResults,
				vectorResults,
				limit,
				weightResult.esWeight(),
				weightResult.vectorWeight());
		log.info("병합된 결과: {} 건", mergedResults.size());

		return mergedResults;
	}

	private WeightResult getQueryIntent(String q) {
		double esWeight = 1.0;
		double vectorWeight = 1.0;
		try {
			String intent = classifyQueryIntent(q);
			log.info("검색어 의도 분류 결과: {}", intent);
			if (intent.contains("EXACT")) {
				esWeight = 3.0; // 키워드 매칭 우선 (강화)
				vectorWeight = 1.0;
			} else if (intent.contains("SEMANTIC")) {
				esWeight = 1.0;
				vectorWeight = 3.0; // 의미 기반 검색 우선 (강화)
			}
		} catch (Exception e) {
			log.warn("의도 분류 실패, 기본 가중치 사용", e);
		}
		return new WeightResult(esWeight, vectorWeight);
	}

	private String classifyQueryIntent(String query) {
		// 브랜드나 구체적인 모델명이 포함되면 EXACT로 유도
		String prompt = """
				Classify the search query "%s" into one of two categories:
				1. EXACT: Query contains specific brand names (e.g., Samsung, Apple), model names (e.g., S24, iPhone), or product codes.
				2. SEMANTIC: Query describes features, usage, or abstract concepts (e.g., good camera, cheap laptop) without specific brands/models.

				Respond ONLY with the word EXACT or SEMANTIC.
				"""
				.formatted(query);
		return chatModel.call(prompt);
	}

	private List<ProductPostEsSearchResponse> searchFromElasticsearch(String query, int limit) {
		try {
			PageResponse<List<ProductPostEsSearchResponse>> response = domainServiceClient.search(query, limit);
			return response.contents() != null ? response.contents() : List.of();
		} catch (Exception e) {
			log.error("domain-service를 통한 Elasticsearch 검색 실패", e);
			return List.of();
		}
	}

	private record WeightResult(double esWeight, double vectorWeight) {

	}
}
