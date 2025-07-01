package com.hong.orderservice.domain.status;

public enum OrderStatus {
    PENDING_PAYMENT, PAID, PAYMENT_FAILED, CANCEL, RETURN_REQUESTED, RETURNED, EXPIRED;
}
