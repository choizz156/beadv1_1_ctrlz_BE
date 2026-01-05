package com.user.configuration;

import org.springframework.core.task.TaskDecorator;

import io.micrometer.context.ContextSnapshot;

public class CustomTaskDecorator implements TaskDecorator {

	@Override
	public Runnable decorate(Runnable task) {
		// 현재 스레드의 컨텍스트(MDC, Observation 등)를 캡처합니다.
		ContextSnapshot snapshot = ContextSnapshot.captureAll();
		return () -> {
			// 비동기 스레드에서 캡처한 컨텍스트를 복구합니다.
			try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
				task.run();
			}
		};
	}
}
