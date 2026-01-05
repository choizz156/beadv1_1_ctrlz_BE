package com.aiservice.application;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.aiservice.domain.model.UserContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonalizedQueryRefiner {

	private final ChatClient chatClient;

	public String refineQuery(String originalQuery, UserContext context) {
		String prompt = """
				Expand the user’s search intent into a personalized context.
				User information: [Gender: %s, Age: %d, Recent interests: %s, Recent cart items: %s, Recent view history: %s]
			     Original search query: %s
			
			      Respond with only the expanded search query in a single line. Do not include any other explanation.
			""".formatted(
			context.gender(),
			context.age(),
			String.join(", ", context.searchKeywords()),
			String.join(", ", context.cartProductNames()),
			String.join(", ", context.viewedTitle()),
			originalQuery);

		try {
			String refinedQuery = chatClient.prompt()
				.user(prompt)
				.call()
				.content();

			log.info("검색어 개인화: '{}' -> '{}'", originalQuery, refinedQuery);
			return refinedQuery != null ? refinedQuery : originalQuery;
		} catch (Exception e) {
			log.warn("검색어 개인화 실패, 원본 사용: {}", e.getMessage());
			return originalQuery;
		}
	}
}
