package com.hong.hotdealservice.event;

import com.hong.common.status.EventType;
import com.hong.common.status.OutboxDeliveryMethod;
import com.hong.hotdealservice.domain.Outbox;
import lombok.Getter;

@Getter
public class OutboxEvent {

    // outboxId
    private final Long outboxId;
    // 발생한 eventType
    private final EventType eventType;
    // KAFKA, FEIGN
    private final OutboxDeliveryMethod deliveryMethod;
    // Json
    private final String payload;

    public OutboxEvent(Outbox outbox) {
        this.outboxId = outbox.getId();
        this.eventType = outbox.getEventType();
        this.deliveryMethod = outbox.getDeliveryMethod();
        this.payload = outbox.getPayload();
    }

}

