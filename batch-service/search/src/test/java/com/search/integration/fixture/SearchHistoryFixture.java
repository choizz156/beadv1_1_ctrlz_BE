package com.search.integration.fixture;

/**
 * SearchHistory 테스트 데이터 생성 유틸리티
 */
public class SearchHistoryFixture {

	public static String createLog(String userId, String searchTerm, String timestamp) {
		return String.format(
				"{\"@timestamp\":\"%s\",\"data\":\"query = %s, userId = %s\"}",
				timestamp, searchTerm, userId);
	}

	public static String createInvalidLog() {
		return "{invalid json}";
	}

	public static String createLogWithMissingField(String timestamp) {
		return String.format("{\"@timestamp\":\"%s\"}", timestamp);
	}

	public static String createItemViewLog(String userId, String title, String timestamp) {
		return String.format(
				"{\"@timestamp\":\"%s\",\"data\":\"title = %s, userId = %s\"}",
				timestamp, title, userId);
	}
}
