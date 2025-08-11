package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.domain.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
}
