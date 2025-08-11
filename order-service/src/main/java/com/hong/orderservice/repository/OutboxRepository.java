package com.hong.orderservice.repository;

import com.hong.common.status.OutboxStatus;
import com.hong.orderservice.domain.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findByOutboxStatusInAndCreatedAtBefore(List<OutboxStatus> outboxStatuses, LocalDateTime createdAt);

}
