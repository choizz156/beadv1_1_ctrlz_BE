package com.aiservice.infrastructure.qdrant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import com.aiservice.application.dto.SearchParams;
import com.aiservice.controller.dto.DocumentSearchResponse;
import com.aiservice.domain.model.ProductVectorContent;
import com.aiservice.domain.repository.VectorRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
public class QdrantVectorRepository implements VectorRepository {

	private final VectorStore qdrantVectorStore;
	private final TokenTextSplitter tokenSplitter;

	@Override
	public String addDocument(ProductVectorContent data) {
		String documentId = UUID.randomUUID().toString();
		String content = buildNaturalLanguageContent(data);

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("documentId", documentId);
		metadata.put("productId", data.productId());
		metadata.put("categoryName", data.categoryName());
		metadata.put("tags", data.tags());
		metadata.put("price", data.price());

		Document document = new Document(content, metadata);
		List<Document> appliedDocument = tokenSplitter.apply(List.of(document));

		qdrantVectorStore.accept(appliedDocument);

		return documentId;
	}

	@Override
	public Optional<Document> findDocumentByProductId(String productId) {
		FilterExpressionBuilder filter = new FilterExpressionBuilder();
		SearchRequest request = SearchRequest.builder()
				.query("product") // OpenAI 에러 방지용 dummy query
				.topK(3)
				.filterExpression(filter.eq("productId", productId).build())
				.build();

		List<Document> documents = qdrantVectorStore.similaritySearch(request);
		if (!documents.isEmpty()) {
			return Optional.of(documents.getFirst());
		}
		return Optional.empty();
	}

	@Override
	public List<DocumentSearchResponse> similaritySearch(SearchParams searchParams, int maxResults) {
		log.info("유사도 검색 시작 searchParams = {}, 최대 결과 = {}", searchParams, maxResults);

		FilterExpressionBuilder filter = new FilterExpressionBuilder();
		Filter.Expression expression = null;
		boolean hasCategory = searchParams.category() != null && !searchParams.category().isBlank();
		boolean hasTags = searchParams.tags() != null && !searchParams.tags().isBlank();

		if (hasCategory && hasTags) {
			expression = filter.and(
					filter.eq("categoryName", searchParams.category()),
					filter.eq("tags", searchParams.tags())).build();
		} else if (hasCategory) {
			expression = filter.eq("categoryName", searchParams.category()).build();
		} else if (hasTags) {
			expression = filter.eq("tags", searchParams.tags()).build();
		}

		SearchRequest.Builder requestBuilder = SearchRequest.builder()
				.query(searchParams.q())
				.topK(maxResults);

		if (expression != null) {
			requestBuilder.filterExpression(expression);
		}

		SearchRequest request = requestBuilder.build();

		List<Document> documents = qdrantVectorStore.similaritySearch(request);

		if (documents.isEmpty()) {
			return List.of();
		}

		return documents.stream()
				.map(document -> DocumentSearchResponse.builder()
						.id(document.getId())
						.content(document.getText() == null ? "" : document.getText())
						.metadata(document.getMetadata())
						.score(document.getScore() == null ? 0 : document.getScore())
						.build())
				.toList();
	}

	@Override
	public void deleteDocument(String productId) {
		FilterExpressionBuilder filter = new FilterExpressionBuilder();
		List<Document> documents = qdrantVectorStore.similaritySearch(
				SearchRequest.builder()
						.query("product") // OpenAI 에러 방지용 dummy query
						.topK(5) // 혹시 중복된게 있을 수 있으니 여유있게
						.filterExpression(filter.eq("productId", productId).build())
						.build());

		if (!documents.isEmpty()) {
			List<String> ids = documents.stream().map(Document::getId).toList();
			qdrantVectorStore.delete(ids);
			log.info("documentID 삭제: {}, count: {}", productId, ids.size());
		}

		log.info("삭제할 document 존재하지 않음: {}", productId);

	}

	private String buildNaturalLanguageContent(ProductVectorContent data) {

		// 글 제목
		String content = "제품명: " + data.title() + ". "
		// 상세 설명
				+ "설명: " + data.description() + ".";

		return content.trim();
	}
}
