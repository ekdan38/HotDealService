package com.hong.orderservice.repository.outbox;

import com.hong.orderservice.domain.outbox.UserCartOutbox;
import com.hong.orderservice.domain.status.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCartOutboxEventRepository extends JpaRepository<UserCartOutbox, Long> {

    List<UserCartOutbox> findByOutboxStatusInOrderByIdAsc(List<OutboxStatus> statuses);
}
