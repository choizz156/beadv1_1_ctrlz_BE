package com.user.infrastructure.batch.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreateBatchScheduler {

	private final JobLauncher jobLauncher;
	private final Job userSignupRetryJob;

	@Scheduled(cron = "0 */5 * * * *")
	public void runUserSignupRetryBatch() {
		try {
			JobParameters jobParameters = new JobParametersBuilder()
					.addLong("time", System.currentTimeMillis())
					.toJobParameters();

			JobExecution jobExecution = jobLauncher.run(userSignupRetryJob, jobParameters);

			if (jobExecution.getStatus().isUnsuccessful()) {
				log.error("UserSignupRetry batch job 실패: {}", jobExecution.getStatus());
			}

			log.info("UserSignupRetry batch job 완료");

		} catch (Exception e) {
			log.error("UserSignupRetry batch job 실행 중 에러", e);
		}
	}
}
