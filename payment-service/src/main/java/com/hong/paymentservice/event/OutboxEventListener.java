package com.hong.paymentservice.event;

import com.hong.common.status.OutboxDeliveryMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "[OutboxEventListener]")
public class OutboxEventListener {
    private final KafkaMessageProducer kafkaMessageProducer;

    @Async("EventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSendEvent(OutboxEvent event) {
        OutboxDeliveryMethod deliveryMethod = event.getDeliveryMethod();

        // Feign
        if(deliveryMethod.equals(OutboxDeliveryMethod.FEIGN)){
            //ExternalServiceInvoker 로 처리 요청
            return;
        }
        // Kafka
        else if(deliveryMethod.equals(OutboxDeliveryMethod.KAFKA)){
            // KafkaMessageProducer 로 처리 요청
            kafkaMessageProducer.produceMessage(event);
        }
    }
}
