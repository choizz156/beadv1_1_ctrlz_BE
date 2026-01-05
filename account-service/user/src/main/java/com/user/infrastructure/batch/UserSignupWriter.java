package com.user.infrastructure.batch;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.common.event.UserSignupCommand;
import com.user.application.port.out.OutboundEventPublisher;
import com.user.infrastructure.jpa.repository.ExternalEventJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSignupWriter implements ItemWriter<UserSignupCommand> {

    @Value("${custom.user-signup.topic.command}")
    private String userSignupCommandTopic;

    private final OutboundEventPublisher outboundEventPublisher;
    private final ExternalEventJpaRepository externalEventJpaRepository;

    @Override
    public void write(Chunk<? extends UserSignupCommand> chunk) {
        List<? extends UserSignupCommand> items = chunk.getItems();
        log.info("User signup retry batch write 시작: {} items", items.size());

        Set<String> successUserId = sendBatchToKafka(items);
        updatePublishedStatus(successUserId);

        log.info("User signup retry batch write 완료: {} items", items.size());
    }

    private Set<String> sendBatchToKafka(List<? extends UserSignupCommand> items) {
        Set<String> successfulUserIds = ConcurrentHashMap.newKeySet();
        for (UserSignupCommand command : items) {
            publishByKafka(command, successfulUserIds);
        }
        return successfulUserIds;
    }

    private void publishByKafka(UserSignupCommand command, Set<String> successfulUserIds) {
        try {
            outboundEventPublisher.publish(userSignupCommandTopic, command);
            successfulUserIds.add(command.userId());
            log.debug("User signup command (retry) 전송 성공: userId={}", command.userId());
        } catch (Exception e) {
            log.warn("카프카 전송 실패 userId: {}", command.userId(), e);
        }
    }

    private void updatePublishedStatus(Set<String> successUserId) {
        if (successUserId.isEmpty()) {
            log.info("전송할 userId가 없습니다.");
            return;
        }

        try {
            int count = externalEventJpaRepository.updatePublished(successUserId);
            log.info("이벤트 상태 업데이트 개수: {}", count);
        } catch (Exception e) {
            log.error("Error updating published status", e);
            throw new RuntimeException("Failed to update published status", e);
        }
    }
}
