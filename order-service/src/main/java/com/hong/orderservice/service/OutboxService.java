package com.hong.orderservice.service;

import com.hong.orderservice.domain.Outbox;
import com.hong.orderservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[OutboxService]")
public class OutboxService {

    private final OutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateToPublished(Long outboxId){
        Optional<Outbox> optionalOutbox = outboxRepository.findById(outboxId);
        if(optionalOutbox.isPresent()){
            Outbox outbox = optionalOutbox.get();
            outbox.updateToPublished();
        }
        else{
            log.warn("outboxId = {} PUBLISHED 업데이트 실패", outboxId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateToFailed(Long outboxId){
        Optional<Outbox> optionalOutbox = outboxRepository.findById(outboxId);
        if(optionalOutbox.isPresent()){
            Outbox outbox = optionalOutbox.get();
            outbox.updateToFailed();
        }
        else{
            log.warn("outboxId = {} FAILED 업데이트 실패", outboxId);
        }
    }
}
