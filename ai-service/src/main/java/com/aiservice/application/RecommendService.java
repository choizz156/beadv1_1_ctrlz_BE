package com.aiservice.application;

import com.aiservice.application.dto.SearchParams;
import com.aiservice.domain.model.RecommendationResult;

public interface RecommendService {
	RecommendationResult recommendProductsByQuery(String userId, SearchParams searchParams);
}
