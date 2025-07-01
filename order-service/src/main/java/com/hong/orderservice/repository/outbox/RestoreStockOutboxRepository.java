package com.hong.orderservice.repository.outbox;

import com.hong.orderservice.domain.outbox.RestoreStockOutbox;
import com.hong.orderservice.domain.status.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestoreStockOutboxRepository extends JpaRepository<RestoreStockOutbox, Long> {
    List<RestoreStockOutbox> findByOutboxStatusInOrderByIdAsc(List<OutboxStatus> statuses);
}
