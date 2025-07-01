package com.hong.orderservice.repository.outbox;

import com.hong.orderservice.domain.outbox.CreatePaymentOutbox;
import com.hong.orderservice.domain.status.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreatePaymentOutboxRepository extends JpaRepository<CreatePaymentOutbox, Long> {

    List<CreatePaymentOutbox> findByOutboxStatusInOrderByIdAsc(List<OutboxStatus> statuses);
}
