package com.aiservice.domain.model;

import lombok.Builder;

@Builder
public record ProductVectorContent(
		String productId,
		String title,
		String categoryName,
		String status,
		int price,
		String description,
		String tags,
		String url) {
}
