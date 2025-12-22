package com.aiservice.infrastructure.qdrant;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.aiservice.application.HybridSearchProcessor;
import com.aiservice.application.dto.SearchParams;
import com.aiservice.controller.dto.DocumentSearchResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@ActiveProfiles({ "local", "secret" })
public class ScoreTest {

	@Autowired
	private QdrantVectorRepository qdrantVectorRepository;
	@Autowired
	private HybridSearchProcessor hybridSearchProcessor;

	/**
	 * 2025-12-21T23:45:36.331+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.i.qdrant.QdrantVectorRepository : 유사도 검색 시작 query = 아이폰, 최대 결과 = 20
	 * 2025-12-21T23:45:37.983+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38876450061798096
	 * 2025-12-21T23:45:37.983+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3883203864097595
	 * 2025-12-21T23:45:37.983+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3877905607223511
	 * 2025-12-21T23:45:37.983+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38769638538360596
	 * 2025-12-21T23:45:37.983+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38766592741012573
	 * 2025-12-21T23:45:37.983+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38739216327667236
	 * 2025-12-21T23:45:37.983+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3869849741458893
	 * 2025-12-21T23:45:37.983+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38694244623184204
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3869393467903137
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38693124055862427
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38685116171836853
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3868078589439392
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3867114186286926
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3866436779499054
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3864450752735138
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38642987608909607
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3864297866821289
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38636642694473267
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38634729385375977
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3863157033920288
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38628822565078735
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38627171516418457
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3861909806728363
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3861285448074341
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3861227035522461
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38610291481018066
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38600701093673706
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38600602746009827
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38597506284713745
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3859138488769531
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38590821623802185
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3858809173107147
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3858639895915985
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38580918312072754
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.385774701833725
	 * 2025-12-21T23:45:37.984+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3857128620147705
	 * 2025-12-21T23:45:37.985+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38568562269210815
	 * 2025-12-21T23:45:37.985+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.38566097617149353
	 * 2025-12-21T23:45:37.985+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3856337070465088
	 * 2025-12-21T23:45:37.985+09:00 INFO 1943 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : 0.3856332004070282
	 */
	@DisplayName("카테고리 없는 경우")
	@Test
	void test1() throws Exception {

		SearchParams searchParams = SearchParams.builder()
				.q("갤럭시")
				.build();

		List<DocumentSearchResponse> lists = qdrantVectorRepository.similaritySearch(searchParams, 20);

		lists.forEach(t -> log.info(String.valueOf(t.score())));
	}

	/**
	 * 2025-12-21T23:51:43.199+09:00 INFO 2135 --- [ai-service] [ Test worker]
	 * c.a.i.qdrant.QdrantVectorRepository : 유사도 검색 시작 query = 아이폰, 최대 결과 = 20, 카테고리
	 * = 아이폰 16 프로
	 * 2025-12-21T23:51:44.612+09:00 INFO 2135 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : score = 0.38876450061798096, 카테고리 = 아이폰
	 * 16 프로
	 * 2025-12-21T23:51:44.612+09:00 INFO 2135 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : score = 0.3883203864097595, 카테고리 = 아이폰
	 * 16 프로
	 * 2025-12-21T23:51:44.612+09:00 INFO 2135 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : score = 0.3877905607223511, 카테고리 = 아이폰
	 * 16 프로
	 * 2025-12-21T23:51:44.612+09:00 INFO 2135 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : score = 0.38769638538360596, 카테고리 = 아이폰
	 * 16 프로
	 * 2025-12-21T23:51:44.612+09:00 INFO 2135 --- [ai-service] [ Test worker]
	 * c.a.infrastructure.qdrant.ScoreTest : score = 0.38766592741012573, 카테고리 = 아이폰
	 * 16 프로
	 * .4932805895805359,
	 */
	@DisplayName("카테고리 있는 경우")
	@Test
	void test2() throws Exception {
		SearchParams searchParams = SearchParams.builder()
				.q("갤럭시 S24 울트라")
				.category("모바일/태블릿")
				.build();
		List<DocumentSearchResponse> lists = qdrantVectorRepository.similaritySearch(searchParams, 20);

		lists.forEach(t -> log.info("score = {}, 카테고리 = {}, content = {}", t.score(), t.metadata().get("categoryName"),
				t.content()));
	}

	@DisplayName("rrf merger")
	@Test
	void test3() throws Exception {
		SearchParams searchParams = SearchParams.builder()
				.q("갤럭시 S24 울트라")
				.category("모바일/태블릿")
				.build();
		List<DocumentSearchResponse> lists = hybridSearchProcessor.search(searchParams, 20);
		lists.forEach(t -> log.info("RRF Score: {}, Category: {}, Title: {}, Content: {}",
				String.format("%.6f", t.score()),
				t.metadata().get("categoryName"),
				t.metadata().get("title"),
				t.content()));
	}
}
