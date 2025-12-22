package com.search.configuration;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.search.listener.SearchBatchSkipListener;

@SpringBatchTest
@SpringBootTest(classes = {
        SearchBatchConfiguration.class,
        SearchFilePartitioner.class,
        SearchLogResourceProvider.class,
        SearchBatchSkipListener.class,
        SearchBatchConfigurationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "batch.search.log-directory=${java.io.tmpdir}",
        "batch.search.log-pattern=test-*.log.gz"
})
class SearchBatchConfigurationTest {

    @Autowired
    private SearchBatchConfiguration searchBatchConfiguration;

    @Autowired
    private SearchLogResourceProvider resourceProvider;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private PlatformTransactionManager transactionManager;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.batch.core.scope.StepScope stepScope() {
            org.springframework.batch.core.scope.StepScope stepScope = new org.springframework.batch.core.scope.StepScope();
            stepScope.setAutoProxy(false);
            return stepScope;
        }

        @org.springframework.context.annotation.Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    @DisplayName("test1: 다중 파일 리소스 생성 확인")
    void test1_MultiResourceCreation(@TempDir Path tempDir) throws Exception {
        // Given
        String logContent = "{\"@timestamp\":\"2024-12-07T10:00:00\",\"data\":\"query = lego, userId = user1\"}";

        // 2개의 .gz 파일 생성
        createGzipFile(tempDir.resolve("test-001.log.gz"), logContent);
        createGzipFile(tempDir.resolve("test-002.log.gz"), logContent);

        // When
        Resource[] resources = resourceProvider.createResources(
                tempDir.toAbsolutePath().toString(),
                "test-*.log.gz");

        // Then
        assertThat(resources).hasSize(2);
    }

    @Test
    @DisplayName("test2: 여러 .gz 파일에서 Resource 파싱 가능 확인")
    void test2_GzipResourceParsing(@TempDir Path tempDir) throws Exception {
        // Given
        String logContent1 = "{\"@timestamp\":\"2024-12-07T10:00:00\",\"data\":\"query = apple, userId = user1\"}";
        String logContent2 = "{\"@timestamp\":\"2024-12-07T11:00:00\",\"data\":\"query = banana, userId = user2\"}";

        createGzipFile(tempDir.resolve("test-001.log.gz"), logContent1);
        createGzipFile(tempDir.resolve("test-002.log.gz"), logContent2);

        // When
        Resource[] resources = resourceProvider.createResources(
                tempDir.toAbsolutePath().toString(),
                "test-*.log.gz");

        // Then
        assertThat(resources).hasSize(2);

        // 각 리소스가 읽을 수 있는지 확인
        for (Resource resource : resources) {
            assertThat(resource.exists()).isTrue();
            assertThat(resource.isReadable()).isTrue();
        }
    }

    @Test
    @DisplayName("test3: 빈 디렉토리에서 빈 리소스 배열 반환")
    void test3_EmptyDirectory(@TempDir Path tempDir) throws Exception {
        // When
        Resource[] resources = resourceProvider.createResources(
                tempDir.toAbsolutePath().toString(),
                "test-*.log.gz");

        // Then
        assertThat(resources).isEmpty();
    }

    @Test
    @DisplayName("test4: 파일명 기준 정렬 확인")
    void test4_FileSorting(@TempDir Path tempDir) throws Exception {
        // Given - 순서를 섞어서 생성
        String logContent = "{\"@timestamp\":\"2024-12-07T10:00:00\",\"data\":\"query = test, userId = user1\"}";
        createGzipFile(tempDir.resolve("test-003.log.gz"), logContent);
        createGzipFile(tempDir.resolve("test-001.log.gz"), logContent);
        createGzipFile(tempDir.resolve("test-002.log.gz"), logContent);

        // When
        Resource[] resources = resourceProvider.createResources(
                tempDir.toAbsolutePath().toString(),
                "test-*.log.gz");

        // Then - 정렬되어 있어야 함
        assertThat(resources).hasSize(3);
        assertThat(resources[0].getDescription()).contains("test-001");
        assertThat(resources[1].getDescription()).contains("test-002");
        assertThat(resources[2].getDescription()).contains("test-003");
    }

    private void createGzipFile(Path path, String content) throws Exception {
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(path))) {
            gzipOut.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
