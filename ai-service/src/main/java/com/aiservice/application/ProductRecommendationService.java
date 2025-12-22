
package com.aiservice.application;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aiservice.application.dto.SearchParams;
import com.aiservice.controller.dto.DocumentSearchResponse;
import com.aiservice.domain.model.RecommendationResult;
import com.aiservice.domain.vo.RecommendationStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductRecommendationService implements RecommendService {

	@Value("${custom.recommendation.limit:10}")
	private int recommendationLimit;

	private final SessionService sessionService;
	private final HybridSearchProcessor hybridSearchProcessor;
	private final RecommendationMessageGenerator recommendationMessageGenerator;
	private final UserContextService userContextService;
	private final PersonalizedQueryRefiner personalizedQueryRefiner;

	@Override
	public RecommendationResult recommendProductsByQuery(String userId, SearchParams searchParams) {
		log.info("추천 생성 시작 - 사용자: {}, 쿼리: {}", userId, searchParams);

		// 추천 제한 체크
		if (isLimitReached(userId)) {
			log.info("사용자 추천 제한 ({}) 도달: {}", recommendationLimit, userId);
			RecommendationResult limitResult = RecommendationResult.limitReached();
			sessionService.publishRecommendationData(userId, limitResult);
			return limitResult;
		}

		// 1. 유저 컨텍스트 조회
		var userContext = userContextService.getUserContext(userId);

		// 2. 쿼리 개인화
		String refinedQuery = personalizedQueryRefiner.refineQuery(searchParams.q(), userContext);
		SearchParams refinedParams = SearchParams.builder()
				.q(refinedQuery)
				.category(searchParams.category())
				.minPrice(searchParams.minPrice())
				.maxPrice(searchParams.maxPrice())
				.tags(searchParams.tags())
				.status(searchParams.status())
				.tradeStatus(searchParams.tradeStatus())
				.sort(searchParams.sort())
				.build();

		// 하이브리드 검색
		List<DocumentSearchResponse> searchResults = hybridSearchProcessor.search(refinedParams, 20);

		// 메시지 생성 및 결과 구성
		RecommendationResult result = buildResult(userId, refinedParams.q(), searchResults);

		// 세션에 발행
		sessionService.incrementRecommendationCount(userId);

		log.info("{} 개의 추천 결과 저장 완료 - 사용자: {} (쿼리: {} -> {})",
				searchResults.size(), userId, refinedParams.q(), refinedQuery);

		return result;
	}

	private boolean isLimitReached(String userId) {
		return sessionService.getRecommendationCount(userId) >= recommendationLimit;
	}

	private RecommendationResult buildResult(String userId, String query,
			List<DocumentSearchResponse> searchResults) {
		return Optional.ofNullable(recommendationMessageGenerator.toPrompt(userId, query, searchResults))
				.map(msg -> RecommendationResult.builder()
						.status(RecommendationStatus.OK)
						.message(msg)
						.build())
				.orElseGet(RecommendationResult::noResults);
	}
}
