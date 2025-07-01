package com.hong.orderservice.repository.outbox;

import com.hong.orderservice.domain.outbox.ExpirePaymentOutbox;
import com.hong.orderservice.domain.status.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpirePaymentOutboxRepository extends JpaRepository<ExpirePaymentOutbox, Long> {
    List<ExpirePaymentOutbox> findByOutboxStatusInOrderByIdAsc(List<OutboxStatus> statuses);

}
