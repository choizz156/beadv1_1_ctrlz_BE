package com.domainservice.domain.cart.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.common.event.UserSignupCommand;
import com.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@KafkaListener(topics = {
		"${custom.user-signup.topic.command}" }, containerFactory = "userSignupKafkaListenerContainerFactory")
@Component
public class CartCommandListener {

	private static final Logger log = LoggerFactory.getLogger("API." + CartCommandListener.class.getSimpleName());

	private final CartService cartService;

	@KafkaHandler
	public void handler(@Payload UserSignupCommand userSignupCommand, Acknowledgment ack) {
		try {
			cartService.addCart(userSignupCommand.userId());
			log.info("Cart created for user: {}", userSignupCommand.userId());
			ack.acknowledge();
		} catch (CustomException e) {
			log.info("이미 처리된 이벤트입니다: {}", userSignupCommand.userId());
			ack.acknowledge();
		} catch (Exception e) {
			log.error("카프카 event handler error: {}", e.getMessage(), e);
			ack.acknowledge();
			throw e;
		}
	}
}
