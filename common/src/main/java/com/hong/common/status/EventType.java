package com.hong.common.status;

import lombok.Getter;

@Getter
public enum EventType {
    PAYMENT_RESULT("payment-result-topic");

    private final String topic;

    EventType(String topic) {
        this.topic = topic;
    }

}