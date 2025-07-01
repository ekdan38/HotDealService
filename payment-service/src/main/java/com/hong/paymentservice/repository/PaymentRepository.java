package com.hong.paymentservice.repository;

import com.hong.paymentservice.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderIdAndUserId(String orderId, Long userId);

    Optional<Payment> findByOrderId(String orderId);
}
