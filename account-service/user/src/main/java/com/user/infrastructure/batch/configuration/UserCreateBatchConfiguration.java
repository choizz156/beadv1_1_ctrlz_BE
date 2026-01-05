package com.user.infrastructure.batch.configuration;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import com.common.event.UserSignupCommand;
import com.user.infrastructure.batch.UserSignupWriter;
import com.user.infrastructure.jpa.entity.ExternalEventEntity;

import io.netty.channel.ConnectTimeoutException;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserCreateBatchConfiguration {

	private final EntityManagerFactory entityManagerFactory;
	private final UserSignupWriter userSignupWriter;

	@Bean
	public Job userSignupRetryJob(Step userSignupRetryStep, JobRepository jobRepository) {
		return new JobBuilder("userSignupRetryJob", jobRepository)
				.start(userSignupRetryStep)
				.build();
	}

	@Bean
	public Step userSignupRetryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("userSignupRetryStep", jobRepository)
				.<ExternalEventEntity, UserSignupCommand>chunk(1000, transactionManager)
				.reader(userSignupItemReader())
				.processor(userSignupItemProcessor())
				.writer(userSignupItemWriter())
				.faultTolerant()
				.retryLimit(3)
				.retry(ConnectException.class) // 연결 안 됐을 경우
				.retry(UnknownHostException.class) // host명 모를경우
				.skipLimit(100)
				.skip(ConnectTimeoutException.class) // read time out
				.skip(DataAccessException.class) // db 예외
				.build();
	}

	@Bean
	public JpaCursorItemReader<ExternalEventEntity> userSignupItemReader() {
		log.info("User signup retry batch reader 생성");
		return new JpaCursorItemReaderBuilder<ExternalEventEntity>()
				.name("ExternalEventEntityItemReader")
				.entityManagerFactory(entityManagerFactory)
				.queryString("select e from ExternalEventEntity e where published =: status order by e.id asc")
				.parameterValues(Map.of("status", false))
				.build();
	}

	@Bean
	public ItemProcessor<ExternalEventEntity, UserSignupCommand> userSignupItemProcessor() {
		return item -> new UserSignupCommand(item.getUserId());
	}

	@Bean
	public ItemWriter<UserSignupCommand> userSignupItemWriter() {
		return userSignupWriter;
	}

}
