package com.accountapplication.user.infrastructure.jpa.adapter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.common.event.UserSignupCommand;
import com.user.application.adapter.UserSignedUpEventHandler;
import com.user.application.adapter.vo.CommandType;
import com.user.application.port.out.ExternalEventPersistentPort;
import com.user.application.port.out.OutboundEventPublisher;
import com.user.domain.event.UserSignedUpEvent;
import com.user.application.adapter.vo.EventType;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSignedUpEventHandler 테스트")
class UserSignedUpEventHandlerTest {

	@Mock
	private ExternalEventPersistentPort externalEventPersistentPort;

	@Mock
	private OutboundEventPublisher kafkaEventPublisher;

	@InjectMocks
	private UserSignedUpEventHandler userSignedUpEventHandler;

	private static final String USER_SIGNUP_COMMAND_TOPIC = "user-signup-command";
	private static final String TEST_USER_ID = "userId";

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(userSignedUpEventHandler, "userSignupCommandTopic", USER_SIGNUP_COMMAND_TOPIC);
	}

	@DisplayName("UserSignedUpEvent 발생 시 외부 이벤트를 저장한다")
	@Test
	void test1() {
		// given
		UserSignedUpEvent event = UserSignedUpEvent.from(TEST_USER_ID, EventType.CREATED);

		// when
		userSignedUpEventHandler.saveExternalEvent(event);

		// then
		verify(externalEventPersistentPort, times(1))
				.save(eq(TEST_USER_ID), eq(EventType.CREATED.name()), eq(CommandType.USER_SIGNUP_COMMAND.name()));
	}

	@DisplayName("UserSignedUpEvent 발생 시 Kafka에 이벤트를 발행한다")
	@Test
	void test2() {
		// given
		UserSignedUpEvent event = UserSignedUpEvent.from(TEST_USER_ID, EventType.CREATED);

		// when
		userSignedUpEventHandler.publishUserSignupCommand(event);

		// then
		verify(kafkaEventPublisher, times(1))
				.publish(eq(USER_SIGNUP_COMMAND_TOPIC), eq(new UserSignupCommand(event.userId())));
	}

	@DisplayName("Kafka 발행 후 이벤트 발행 완료 처리를 한다")
	@Test
	void test3() {
		// given
		UserSignedUpEvent event = UserSignedUpEvent.from(TEST_USER_ID, EventType.CREATED);

		// when
		userSignedUpEventHandler.publishUserSignupCommand(event);

		// then
		verify(externalEventPersistentPort, times(1))
				.completePublish(eq(TEST_USER_ID), eq(EventType.CREATED.name()),
						eq(CommandType.USER_SIGNUP_COMMAND.name()));
	}
}
