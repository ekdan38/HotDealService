package com.hong.paymentservice.repository;

import com.hong.paymentservice.domain.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
}
