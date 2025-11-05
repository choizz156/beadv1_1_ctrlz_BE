package com.userservice.infrastructure.kafka.config;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Profile("test")
@Component
public class TestKafkaProducer {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public <T> void send(String topic, T event) {
		kafkaTemplate.send(topic, event);
		log.info(String.format("Sending message to %s", topic));
	}
}