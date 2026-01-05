package com.search.configuration;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.search.dto.UserBehaviorDto;
import com.search.listener.SearchBatchSkipListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 검색 이력 배치 작업 설정
 * 파티셔닝 사용
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SearchBatchConfiguration {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final DataSource dataSource;

	private final SearchLogResourceProvider resourceProvider;
	private final SearchBatchSkipListener skipListener;
	private final SearchFilePartitioner partitioner;

	@Value("${batch.search.log-directory:logs}")
	private String logDirectory;

	@Value("${batch.search.log-pattern:item-view.log*,search-view.log*}")
	private String logPattern;

	@Value("${batch.search.chunk-size:1000}")
	private int chunkSize;

	@Value("${batch.search.skip-limit:100}")
	private int skipLimit;

	@Value("${batch.search.pool-size:4}")
	private int poolSize;

	private static final String UPSERT_SQL =
		"INSERT INTO user_behavior (id, user_id, behavior_value, behavior_type, created_at) "
			+
			"VALUES (:id, :userId, :behaviorValue, :behaviorType, :createdAt) " +
			"ON DUPLICATE KEY UPDATE " +
			"user_id = :userId, " +
			"behavior_value = :behaviorValue, " +
			"behavior_type = :behaviorType, " +
			"created_at = :createdAt";

	@Qualifier("searchHistoryJob")
	@Bean
	public Job searchHistoryJob() {
		log.info("검색 이력 배치 작업 초기화 (파티셔닝 모드) - chunkSize: {}, skipLimit: {}, poolSize: {}, directory: {}, pattern: {}",
			chunkSize, skipLimit, poolSize, logDirectory, logPattern);

		return new JobBuilder("searchHistoryJob", jobRepository)
			.start(masterStep())
			.build();
	}

	@Bean
	public Step masterStep() {
		return new StepBuilder("masterStep", jobRepository)
			.partitioner("slaveStep", partitioner)
			.step(slaveStep())
			.gridSize(poolSize)
			.taskExecutor(batchTaskExecutor())
			.build();
	}

	@Bean
	public Step slaveStep() {
		return new StepBuilder("slaveStep", jobRepository)
			.<UserBehaviorDto, UserBehaviorDto>chunk(chunkSize, transactionManager)
			.reader(partitionedReader(null))
			.writer(searchHistoryWriter())
			.faultTolerant()
			.skip(Exception.class)
			.skipLimit(skipLimit)
			.listener(skipListener)
			.build();
	}

	@Bean
	public TaskExecutor batchTaskExecutor() {
		return new TaskExecutorAdapter(
			new VirtualThreadTaskExecutor("batch-async-")
		);
	}

	/**
	 * 파티션별 여러 파일을 처리하는 MultiResourceItemReader
	 * @param filePaths StepExecutionContext에서 주입받는 쉼표로 구분된 파일 경로들
	 */
	@Bean
	@StepScope
	public MultiResourceItemReader<UserBehaviorDto> partitionedReader(
		@Value("#{stepExecutionContext['filePaths']}") String filePaths) {

		if (filePaths == null || filePaths.isEmpty()) {
			log.warn("파티션에 할당된 파일이 없습니다.");
			return createEmptyMultiReader();
		}

		// 쉼표로 구분된 파일 경로들을 배열로 변환
		String[] filePathArray = filePaths.split(",");
		Resource[] resources = new Resource[filePathArray.length];

		log.info("파티션 Reader 초기화 - 할당된 파일 수: {}", filePathArray.length);

		for (int i = 0; i < filePathArray.length; i++) {
			String filePath = filePathArray[i].trim();
			resources[i] = resourceProvider.createResource(filePath);
			log.info("  [{}] {}", i + 1, filePath);
		}

		return new MultiResourceItemReaderBuilder<UserBehaviorDto>()
			.name("partitionedMultiReader")
			.resources(resources)
			.delegate(createDynamicReader())
			.build();
	}

	/**
	 * 파일명에 따라 동적으로 LineMapper를 결정하는 Reader
	 */
	private FlatFileItemReader<UserBehaviorDto> createDynamicReader() {
		return new FlatFileItemReader<UserBehaviorDto>() {
			@Override
			public void setResource(Resource resource) {
				super.setResource(resource);
				// 파일명으로 behaviorType 판단
				String fileName = resource.getFilename();
				String behaviorType = determineBehaviorTypeFromFileName(fileName);

				log.info("Reader 설정 변경 - 파일: {}, 타입: {}", fileName, behaviorType);
				this.setLineMapper(new SearchLogLineMapper(behaviorType));
			}
		};
	}

	/**
	 * 파일명에서 동작 타입 결정
	 */
	private String determineBehaviorTypeFromFileName(String fileName) {
		if (fileName == null) {
			return "VIEW";
		}
		return fileName.contains("item-view") ? "VIEW" : "SEARCH";
	}

	/**
	 * 빈 MultiResourceItemReader 생성 (파일이 없을 때)
	 */
	private MultiResourceItemReader<UserBehaviorDto> createEmptyMultiReader() {
		Resource[] emptyResources = new Resource[] {
			new org.springframework.core.io.ByteArrayResource(new byte[0])
		};

		FlatFileItemReader<UserBehaviorDto> emptyDelegate = new FlatFileItemReaderBuilder<UserBehaviorDto>()
			.name("emptyReader")
			.lineMapper(new SearchLogLineMapper("VIEW"))
			.build();

		return new MultiResourceItemReaderBuilder<UserBehaviorDto>()
			.name("emptyMultiReader")
			.resources(emptyResources)
			.delegate(emptyDelegate)
			.build();
	}

	@Bean
	public JdbcBatchItemWriter<UserBehaviorDto> searchHistoryWriter() {
		return new JdbcBatchItemWriterBuilder<UserBehaviorDto>()
			.dataSource(dataSource)
			.sql(UPSERT_SQL)
			.beanMapped()
			.build();
	}
}
