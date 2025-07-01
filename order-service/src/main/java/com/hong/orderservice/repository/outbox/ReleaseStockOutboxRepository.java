package com.hong.orderservice.repository.outbox;

import com.hong.orderservice.domain.outbox.ReleaseStockOutbox;
import com.hong.orderservice.domain.status.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseStockOutboxRepository extends JpaRepository<ReleaseStockOutbox, Long> {
    List<ReleaseStockOutbox> findByOutboxStatusInOrderByIdAsc(List<OutboxStatus> statuses);

}
