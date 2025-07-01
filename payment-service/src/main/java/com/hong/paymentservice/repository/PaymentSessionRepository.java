package com.hong.paymentservice.repository;

import com.hong.paymentservice.domain.PaymentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentSessionRepository extends JpaRepository<PaymentSession, String> {

    Optional<PaymentSession> findByOrderIdAndUserId(String orderId, Long userId);
    Optional<PaymentSession> findByIdAndUserId(String id, Long userId);

}
