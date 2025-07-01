package com.hong.orderservice.domain.status;

public enum OutboxStatus {
    PENDING, SENT, FAILED, SKIP, IN_PROGRESS
}
