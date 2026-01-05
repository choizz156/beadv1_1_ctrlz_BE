package com.aiservice.infrastructure.qdrant;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import com.aiservice.domain.model.ProductVectorContent;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@ActiveProfiles({ "local", "secret" })
class VectorDbBatchInsertionTest {

	private static final int BATCH_SIZE = 512;
	private static final int THREAD_POOL_SIZE = 8;

	@Autowired
	private VectorStore qdrantVectorStore;
	@Autowired
	private TokenTextSplitter tokenSplitter;

	@Test
	@DisplayName("CSV 데이터를 batch 단위로 VectorDB에 적재한다")
	void test1() throws Exception {
		log.info("=== VectorDB batch insert 시작 ===");
		long start = System.nanoTime();

		List<ProductData> products = parseCsv();
		log.info("총 {}개 상품 로드 완료", products.size());

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		AtomicInteger successBatch = new AtomicInteger();
		AtomicInteger failBatch = new AtomicInteger();

		List<Document> batch = new ArrayList<>(BATCH_SIZE);

		for (ProductData product : products) {
			batch.add(buildDocument(product));

			if (batch.size() == BATCH_SIZE) {
				List<Document> insertBatch = new ArrayList<>(batch);
				List<Document> applied = tokenSplitter.apply(insertBatch);
				batch.clear();

				executor.submit(() -> {
					try {
						qdrantVectorStore.accept(applied);
						int count = successBatch.incrementAndGet();
						log.info("Batch insert 성공 (#{} / size={})", count, insertBatch.size());
					} catch (Exception e) {
						failBatch.incrementAndGet();
						log.error("Batch insert 실패", e);
					}
				});
			}
		}

		// 남은 데이터
		if (!batch.isEmpty()) {
			List<Document> insertBatch = new ArrayList<>(batch);
			List<Document> applied = tokenSplitter.apply(insertBatch);
			executor.submit(() -> {
				try {
					qdrantVectorStore.accept(applied);
					successBatch.incrementAndGet();
				} catch (Exception e) {
					failBatch.incrementAndGet();
					log.error("마지막 batch 실패", e);
				}
			});
		}

		executor.shutdown();
		executor.awaitTermination(30, TimeUnit.MINUTES);

		long end = System.nanoTime();

		log.info("=== VectorDB batch insert 완료 ===");
		log.info("성공 batch: {}, 실패 batch: {}", successBatch.get(), failBatch.get());
		log.info("총 소요 시간: {}초", Duration.ofNanos(end - start).toSeconds());
	}

	/**
	 * CSV 단일 스레드 파싱
	 */
	private List<ProductData> parseCsv() throws Exception {
		List<ProductData> products = new ArrayList<>();

		Resource resource = new ClassPathResource("data/test.csv");
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty())
					continue;

				String[] allColumns = line.split(",");
				if (allColumns.length < 14)
					continue;

				try {
					int len = allColumns.length;
					String id = allColumns[0].trim();
					String title = allColumns[6].trim();
					String name = allColumns[7].trim();
					long price = Long.parseLong(allColumns[8].trim());

					// 끝에서부터 역순으로 고정 필드 추출
					String status = allColumns[len - 4].trim();

					// 중간 필드들은 모두 설명(description)으로 합침
					StringBuilder descBuilder = new StringBuilder();
					for (int i = 9; i < len - 4; i++) {
						if (i > 9)
							descBuilder.append(",");
						descBuilder.append(allColumns[i]);
					}
					String description = descBuilder.toString().trim();

					// CSV의 카테고리(index 5) 사용, 없으면 categorize 로직 사용
					String csvCategory = allColumns[5].trim();
					String categoryName = csvCategory.isEmpty() ? categorize(name, title, price) : csvCategory;

					products.add(ProductData.builder()
							.id(id)
							.title(title)
							.categoryName(categoryName)
							.price(price)
							.description(description)
							.status(status)
							.build());
				} catch (Exception e) {
					log.warn("CSV 파싱 실패: {}", line);
				}
			}
		}
		return products;
	}

	private String categorize(String name, String title, long price) {
		String text = (name + " " + title).toLowerCase();

		// 1. 수입명품 (비싼 브랜드 우선)
		if (text.contains("샤넬") || text.contains("로렉스") || text.contains("에르메스") ||
				text.contains("루이비통") || text.contains("구찌") || text.contains("프라다") ||
				text.contains("발렌시아가") || text.contains("생로랑") || text.contains("디올") ||
				text.contains("명품")
				|| (price > 2000000 && (text.contains("백") || text.contains("가방") || text.contains("시계")))) {
			return "수입명품";
		}

		// 6. 모바일/태블릿
		if (text.contains("아이폰") || text.contains("갤럭시") || text.contains("아이패드") ||
				text.contains("태블릿") || text.contains("워치") || text.contains("스마트폰") || text.contains("버즈")
				|| text.contains("에어팟")) {
			return "모바일/태블릿";
		}

		// 8. 노트북/PC
		if (text.contains("맥북") || text.contains("노트북") || text.contains("그램") ||
				text.contains("pc") || text.contains("컴퓨터") || text.contains("모니터") || text.contains("키보드")
				|| text.contains("마우스")) {
			return "노트북/PC";
		}

		// 9. 카메라/캠코더
		if (text.contains("니콘") || text.contains("캐논") || text.contains("소니") ||
				text.contains("카메라") || text.contains("캠코더") || text.contains("dslr") || text.contains("미러리스")
				|| text.contains("렌즈")) {
			return "카메라/캠코더";
		}

		// 12. 게임
		if (text.contains("닌텐도") || text.contains("스위치") || text.contains("ps5") ||
				text.contains("플레이스테이션") || text.contains("게임") || text.contains("타이틀") || text.contains("엑스박스")
				|| text.contains("스팀덱")) {
			return "게임";
		}

		// 7. 가전제품
		if (text.contains("냉장고") || text.contains("세탁기") || text.contains("tv") ||
				text.contains("에어컨") || text.contains("건조기") || text.contains("전자레인지") || text.contains("청소기")
				|| text.contains("공기청정기")) {
			return "가전제품";
		}

		// 2. 패션의류
		if (text.contains("패딩") || text.contains("코트") || text.contains("티셔츠") ||
				text.contains("바지") || text.contains("원피스") || text.contains("셔츠") || text.contains("자켓")
				|| text.contains("의류")) {
			return "패션의류";
		}

		// 3. 패션잡화
		if (text.contains("운동화") || text.contains("스니커즈") || text.contains("구두") ||
				text.contains("가방") || text.contains("지갑") || text.contains("벨트") || text.contains("안경")
				|| text.contains("선글라스") || text.contains("잡화")) {
			return "패션잡화";
		}

		// 4. 뷰티
		if (text.contains("화장품") || text.contains("향수") || text.contains("스킨") || text.contains("로션")
				|| text.contains("뷰티")) {
			return "뷰티";
		}

		// 5. 출산/유아동
		if (text.contains("장난감") || text.contains("유모차") || text.contains("카시트") || text.contains("아기")
				|| text.contains("기저귀") || text.contains("유아")) {
			return "출산/유아동";
		}

		// 10. 가구/인테리어
		if (text.contains("책상") || text.contains("침대") || text.contains("소파") || text.contains("테이블")
				|| text.contains("의자") || text.contains("조명") || text.contains("인테리어")) {
			return "가구/인테리어";
		}

		// 11. 리빙/생활
		if (text.contains("주방") || text.contains("식기") || text.contains("컵") || text.contains("냄비")
				|| text.contains("리빙") || text.contains("수건")) {
			return "리빙/생활";
		}

		// 13. 반려동물/취미
		if (text.contains("사료") || text.contains("간식") || text.contains("강아지") || text.contains("고양이")
				|| text.contains("반려동물") || text.contains("피규어") || text.contains("레고")) {
			return "반려동물/취미";
		}

		// 14. 도서/음반/문구
		if (text.contains("도서") || text.contains("책") || text.contains("cd") || text.contains("앨범")
				|| text.contains("문구") || text.contains("다이어리")) {
			return "도서/음반/문구";
		}

		// 16. 스포츠
		if (text.contains("골프") || text.contains("축구") || text.contains("야구") || text.contains("농구")
				|| text.contains("스포츠") || text.contains("테니스")) {
			return "스포츠";
		}

		// 17. 레저/여행
		if (text.contains("캠핑") || text.contains("텐트") || text.contains("여행") || text.contains("캐리어")
				|| text.contains("등산")) {
			return "레저/여행";
		}

		// 18. 오토바이
		if (text.contains("오토바이") || text.contains("바이크") || text.contains("헬멧")) {
			return "오토바이";
		}

		// 20. 무료나눔
		if (text.contains("나눔") || text.contains("무료")) {
			return "무료나눔";
		}

		// 21. 중고차
		if (text.contains("차") || text.contains("자동차") || text.contains("중고차")) {
			return "중고차";
		}

		// 15. 티켓/쿠폰
		if (text.contains("티켓") || text.contains("쿠폰") || text.contains("기프티콘") || text.contains("상품권")) {
			return "티켓/쿠폰";
		}

		// 19. 공구/산업용품
		if (text.contains("공구") || text.contains("드릴")) {
			return "공구/산업용품";
		}

		// 기본값
		String[] categories = {
				"수입명품", "패션의류", "패션잡화", "뷰티", "출산/유아동", "모바일/태블릿", "가전제품",
				"노트북/PC", "카메라/캠코더", "가구/인테리어", "리빙/생활", "게임", "반려동물/취미",
				"도서/음반/문구", "티켓/쿠폰", "스포츠", "레저/여행", "오토바이", "공구/산업용품", "무료나눔", "중고차"
		};
		int index = Math.abs((name + title).hashCode()) % categories.length;
		return categories[index];
	}

	/**
	 * Document 생성 (ID 고정 → upsert 가능)
	 */
	private Document buildDocument(ProductData product) {
		String join = String.join(",", extractTags(product));
		System.out.println("join = " + join);
		ProductVectorContent data = ProductVectorContent.builder()
				.productId(product.id)
				.title(product.title)
				.description(product.description)
				.categoryName(product.categoryName)
				.tags(join)
				.price(product.price.intValue())
				.status(product.status)
				.build();

		String content = buildNaturalLanguageContent(data);

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("documentId", product.id);
		metadata.put("productId", data.productId());
		metadata.put("categoryName", data.categoryName());
		metadata.put("tags", data.tags());
		metadata.put("price", data.price());

		// 🔑 productId 기반 고정 ID → 재적재 시 upsert
		String documentId = UUID.nameUUIDFromBytes(product.id.getBytes(StandardCharsets.UTF_8)).toString();

		return new Document(documentId, content, metadata);
	}

	private List<String> extractTags(ProductData product) {
		List<String> tags = new ArrayList<>();
		String title = product.title.toLowerCase();

		if (title.contains("갤럭시") || title.contains("삼성"))
			tags.add("삼성");
		if (title.contains("아이폰") || title.contains("애플") || title.contains("맥북"))
			tags.add("애플");
		if (title.contains("소니"))
			tags.add("소니");
		if (title.contains("lg"))
			tags.add("LG");
		if (title.contains("닌텐도"))
			tags.add("닌텐도");
		if (title.contains("다이슨"))
			tags.add("다이슨");
		if (title.contains("나이키") || title.contains("조던"))
			tags.add("나이키");

		tags.add(product.categoryName);
		return tags;
	}

	private String buildNaturalLanguageContent(ProductVectorContent data) {
		return """
				제품명: %s.
				설명: %s.
				""".formatted(
				data.title(),
				data.description()).trim();
	}

	@Builder
	private static class ProductData {
		String id;
		String title;
		String categoryName;
		Long price;
		String description;
		String status;
	}
}
