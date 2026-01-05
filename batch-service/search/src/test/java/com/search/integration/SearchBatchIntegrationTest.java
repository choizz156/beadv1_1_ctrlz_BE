package com.search.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.search.integration.fixture.SearchHistoryFixture;

@SpringBatchTest
@SpringBootTest(classes = { SearchBatchTestConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "batch.search.log-directory=${java.io.tmpdir}/search-batch-test",
        "batch.search.log-pattern=*.log.gz",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.batch.job.enabled=false"
})
class SearchBatchIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SearchBatchTestConfig testConfig;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // DB 데이터 정리
        jdbcTemplate.execute("DELETE FROM user_behavior");

        // Job 실행 이력 정리 (재실행 시 깔끔하게)
        jobRepositoryTestUtils.removeJobExecutions();

        // 테스트 디렉토리 설정
        testConfig.setLogDirectory(tempDir.toAbsolutePath().toString());
        testConfig.setLogPattern("*.log.gz");
    }

    @Test
    @DisplayName("정상 로그 파일을 읽어 DB에 저장한다 (ITEM-VIEW)")
    void test1() throws Exception {
        // given
        createGzipLogFile(tempDir.resolve("item-view-001.log.gz"),
                SearchHistoryFixture.createItemViewLog("userA", "apple", "2024-12-07T10:00:00"),
                SearchHistoryFixture.createItemViewLog("userB", "banana", "2024-12-07T10:05:00"));

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(createJobParameters());

        // then - JobExecution 검증
        assertThat(jobExecution)
                .satisfies(execution -> {
                    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
                    assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
                });

        // StepExecution 검증 (순차 처리: 단일 Step)
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution)
                .satisfies(step -> {
                    assertThat(step.getReadCount()).isEqualTo(2);
                    assertThat(step.getWriteCount()).isEqualTo(2);
                    assertThat(step.getSkipCount()).isZero();
                });

        // DB 결과 검증
        List<UserBehaviorEntity> results = findAllUserBehavior();
        assertThat(results)
                .hasSize(2)
                .satisfiesExactly(
                        first -> assertUserBehavior(first, "userA", "apple", "VIEW"),
                        second -> assertUserBehavior(second, "userB", "banana", "VIEW"));
    }

    @Test
    @DisplayName("서치 로그 파일을 읽어 SEARCH 타입으로 저장한다")
    void test5() throws Exception {
        // given
        createGzipLogFile(tempDir.resolve("search-view-001.log.gz"),
                SearchHistoryFixture.createLog("userC", "cherry", "2024-12-07T12:00:00"));

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(createJobParameters());

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // DB 결과 검증
        List<UserBehaviorEntity> results = findAllUserBehavior();
        assertThat(results)
                .hasSize(1)
                .satisfiesExactly(
                        first -> assertUserBehavior(first, "userC", "cherry", "SEARCH"));
    }

    @Test
    @DisplayName("여러 .gz 파일을 순차적으로 처리한다")
    void test2() throws Exception {
        // given - 2개의 .gz 파일 생성
        createGzipLogFile(tempDir.resolve("item-view-001.log.gz"),
                SearchHistoryFixture.createItemViewLog("userA", "apple", "2024-12-07T10:00:00"));
        createGzipLogFile(tempDir.resolve("item-view-002.log.gz"),
                SearchHistoryFixture.createItemViewLog("userB", "banana", "2024-12-07T11:00:00"));

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(createJobParameters());

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 순차 처리 검증: 모든 파일의 레코드가 합산됨
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(2);

        List<UserBehaviorEntity> results = findAllUserBehavior();
        assertThat(results)
                .hasSize(2)
                .extracting(UserBehaviorEntity::userId)
                .containsExactlyInAnyOrder("userA", "userB");
    }

    @Test
    @DisplayName("잘못된 형식의 로그는 skip하고 정상 처리를 계속한다")
    void test3() throws Exception {
        // given
        String validLog1 = SearchHistoryFixture.createLog("userA", "apple", "2024-12-07T10:00:00");
        String invalidLog = "invalid json line";
        String validLog2 = SearchHistoryFixture.createLog("userB", "banana", "2024-12-07T10:05:00");

        createGzipLogFile(tempDir.resolve("test.log.gz"), validLog1, invalidLog, validLog2);

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(createJobParameters());

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Skip 카운트 검증
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getSkipCount()).isGreaterThanOrEqualTo(1);

        List<UserBehaviorEntity> results = findAllUserBehavior();
        assertThat(results)
                .hasSize(2)
                .extracting(UserBehaviorEntity::userId)
                .containsExactly("userA", "userB");
    }

    @Test
    @DisplayName("빈 디렉토리에서도 정상 완료된다")
    void test4() throws Exception {
        // given - 빈 디렉토리 (파일 없음)

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(createJobParameters());

        // then
        assertThat(jobExecution)
                .satisfies(execution -> {
                    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
                    assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
                });

        assertThat(findAllUserBehavior()).isEmpty();
    }

    // === Helper Methods ===

    private JobParameters createJobParameters() {
        return new JobParametersBuilder()
                .addString("executedAt", LocalDateTime.now().toString())
                .toJobParameters();
    }

    private void createGzipLogFile(Path path, String... logLines) throws IOException {
        String content = String.join("\n", logLines);
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(path))) {
            gzipOut.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private List<UserBehaviorEntity> findAllUserBehavior() {
        return jdbcTemplate.query(
                "SELECT user_id, behavior_value, behavior_type, created_at FROM user_behavior ORDER BY user_id",
                (rs, rowNum) -> new UserBehaviorEntity(
                        rs.getString("user_id"),
                        rs.getString("behavior_value"),
                        rs.getString("behavior_type"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
    }

    private void assertUserBehavior(UserBehaviorEntity entity, String expectedUserId, String expectedValue,
            String expectedType) {
        assertThat(entity.userId()).isEqualTo(expectedUserId);
        assertThat(entity.value()).isEqualTo(expectedValue);
        assertThat(entity.type()).isEqualTo(expectedType);
        assertThat(entity.createdAt()).isNotNull();
    }

    // === Inner Classes ===

    record UserBehaviorEntity(String userId, String value, String type, LocalDateTime createdAt) {
    }
}
