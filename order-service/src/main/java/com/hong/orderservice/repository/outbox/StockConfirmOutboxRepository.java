package com.hong.orderservice.repository.outbox;

import com.hong.orderservice.domain.outbox.StockConfirmOutbox;
import com.hong.orderservice.domain.status.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockConfirmOutboxRepository extends JpaRepository<StockConfirmOutbox, Long> {

    List<StockConfirmOutbox> findByOutboxStatusInOrderByIdAsc(List<OutboxStatus> statuses);
}
