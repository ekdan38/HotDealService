package com.hong.orderservice.repository.outbox;

import com.hong.orderservice.domain.outbox.CancelPaymentOutbox;
import com.hong.orderservice.domain.status.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CancelPaymentOutboxRepository extends JpaRepository<CancelPaymentOutbox, Long> {
    List<CancelPaymentOutbox> findByOutboxStatusInOrderByIdAsc(List<OutboxStatus> statuses);

}
