package com.user.application.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.common.event.UserSignupCommand;
import com.user.application.adapter.vo.CommandType;
import com.user.application.port.out.ExternalEventPersistentPort;
import com.user.application.port.out.OutboundEventPublisher;
import com.user.domain.event.UserSignedUpEvent;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserSignedUpEventHandler {

	private static final Logger log = LoggerFactory.getLogger("API." + UserSignedUpEventHandler.class.getName());

	@Value("${custom.user-signup.topic.command}")
	private String userSignupCommandTopic;

	private final ExternalEventPersistentPort externalEventPersistentPort;
	private final OutboundEventPublisher kafkaEventPublisher;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void saveExternalEvent(UserSignedUpEvent event) {
		externalEventPersistentPort.save(
				event.userId(),
				event.eventType().name(),
				CommandType.USER_SIGNUP_COMMAND.name());

		log.info("이벤트 저장 완료: {}, {}, {}",
				event.userId(),
				event.eventType(),
				CommandType.USER_SIGNUP_COMMAND);
	}

	@Async("taskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publishUserSignupCommand(UserSignedUpEvent event) {
		UserSignupCommand userSignupCommand = new UserSignupCommand(event.userId());
		log.info("user signup command kafka 전송: {}", event.userId());
		try {
			kafkaEventPublisher.publish(userSignupCommandTopic, userSignupCommand);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		externalEventPersistentPort.completePublish(event.userId(), event.eventType().name(),
				CommandType.USER_SIGNUP_COMMAND.name());
		log.info("user signup command 상태 변경: {}", event.userId());
	}
}
