package com.aiservice.infrastructure.qdrant;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.aiservice.application.HybridSearchProcessor;
import com.aiservice.application.RRFMerger;
import com.aiservice.application.dto.SearchParams;
import com.aiservice.controller.dto.DocumentSearchResponse;
import com.aiservice.domain.repository.VectorRepository;
import com.aiservice.infrastructure.feign.DomainServiceClient;
import com.aiservice.infrastructure.feign.dto.ProductPostEsSearchResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@ActiveProfiles({"local", "secret"})
public class RrfPerformanceEvaluationTest {

	@Autowired
	private DomainServiceClient domainServiceClient;
	@Autowired
	private VectorRepository vectorRepository;

	@Autowired
	private RRFMerger rrfMerger;
	@Autowired
	private HybridSearchProcessor hybridSearchProcessor;

	// Test Data Structure
	record TestCase(String query, String intentType, String expectedKeyword) {
	}

	private final List<TestCase> TEST_CASES = List.of(
		// 1. EXACT
		new TestCase("갤럭시 S24", "EXACT", "울트라"),

		// 2. SEMANTIC
		new TestCase("사진 잘 나오는 폰", "SEMANTIC", "샤오미")

		);

	@Test
	@DisplayName("Evaluate Retrieval Performance: Dynamic RRF vs Simple RRF (Real Data)")
	void evaluateSearchPerformance() {
		System.out.println("\n========== 검색 성능 평가 (MRR) - Real Data ==========\n");
		double totalMrrSimple = 0;
		double totalMrrDynamic = 0;

		for (TestCase testCase : TEST_CASES) {
			String query = testCase.query();
			String expected = testCase.expectedKeyword();
			SearchParams params = SearchParams.builder().q(query).build();

			//search
			List<ProductPostEsSearchResponse> esResults = fetchEsResults(query);
			List<DocumentSearchResponse> vectorResults = vectorRepository.similaritySearch(params, 20);

			// 2. Simple RRF
			var simpleResults = rrfMerger.mergeWithRRF(esResults, vectorResults, 20, 1.0, 1.0);
			double mrrSimple = calculateReciprocalRankDoc(simpleResults, expected);
			totalMrrSimple += mrrSimple;

			// 3. Dynamic RRF
			var dynamicResults = hybridSearchProcessor.search(params, 20);
			double mrrDynamic = calculateReciprocalRankDoc(dynamicResults, expected);
			totalMrrDynamic += mrrDynamic;

			System.out.printf("[Query: %-15s] Expected: %-5s | Simple MRR: %.2f | Dynamic MRR: %.2f\n",
				query, expected, mrrSimple, mrrDynamic);
		}

		printReport(totalMrrSimple, totalMrrDynamic, TEST_CASES.size());
	}

	private List<ProductPostEsSearchResponse> fetchEsResults(String query) {
		try {
			// contents() can be null
			var response = domainServiceClient.search(query, 10);
			return response.contents() != null ? response.contents() : List.of();
		} catch (Exception e) {
			log.warn("ES Search Failed for query: {}", query);
			return List.of();
		}
	}

	private double calculateReciprocalRankDoc(List<DocumentSearchResponse> results, String expectedKeyword) {
		for (int i = 0; i < results.size(); i++) {

			String title = (String)results.get(i).metadata().get("title");

			if (title == null) {

				title = results.get(i).content();
			}

			if (title != null && title.contains(expectedKeyword)) {
				return 1.0 / (i + 1);
			}
		}
		return 0.0;
	}

	private void printReport(double simpleTotal, double dynamicTotal, int count) {
		double mrrSimple = simpleTotal / count;
		double mrrDynamic = dynamicTotal / count;

		System.out.println("\n=======================================================");
		System.out.println("                   최종 성능 결과 (MRR)                 ");
		System.out.println("=======================================================");
		System.out.printf("| Method        | MRR Score | Improvement (vs Simple) |\n");
		System.out.printf("|:-------------:|:---------:|:-----------------------:|\n");
		System.out.printf("| Static RRF    | %.4f    | -                       |\n", mrrSimple);
		double improvement = (mrrSimple == 0) ? 100.0 : ((mrrDynamic - mrrSimple) / mrrSimple) * 100.0;
		System.out.printf("| Dynamic RRF   | %.4f    | %+2.2f%%                 |\n", mrrDynamic, improvement);
		System.out.println("=======================================================");
	}
}
