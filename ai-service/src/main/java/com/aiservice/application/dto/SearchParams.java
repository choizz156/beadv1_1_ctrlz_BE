package com.aiservice.application.dto;

import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record SearchParams(
	String q,
	String category,
	@Min(value = 0, message = "최소 가격은 0원 이상이어야 합니다.")
	Long minPrice,
	@Min(value = 0, message = "최대 가격은 0원 이상이어야 합니다.")
	Long maxPrice,
	String tags,
	String status,
	String tradeStatus,
	String sort
) {
}
