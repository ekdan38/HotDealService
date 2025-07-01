package com.hong.paymentservice.repository;

import com.hong.paymentservice.domain.outbox.OrderStatusOutbox;
import com.hong.paymentservice.domain.status.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusOutboxRepository extends JpaRepository<OrderStatusOutbox, Long> {

    List<OrderStatusOutbox> findByOutboxStatusInOrderByIdAsc(List<OutboxStatus> statuses);

}
