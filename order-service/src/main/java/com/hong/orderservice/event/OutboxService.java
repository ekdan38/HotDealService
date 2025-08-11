package com.hong.orderservice.event;

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

    @Transactional
    public Outbox save(Outbox outbox){
        return outboxRepository.save(outbox);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateToPublished(Long outboxId){
        Optional<Outbox> optionalOutbox = outboxRepository.findById(outboxId);
        if(optionalOutbox.isPresent()){
            Outbox outbox = optionalOutbox.get();
            outbox.updateToPublished();
            outboxRepository.save(outbox);
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
            outboxRepository.save(outbox);
        }
        else{
            log.warn("outboxId = {} FAILED 업데이트 실패", outboxId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateToAborted(Long outboxId){
        Optional<Outbox> optionalOutbox = outboxRepository.findById(outboxId);
        if(optionalOutbox.isPresent()){
            Outbox outbox = optionalOutbox.get();
            outbox.updateToAborted();
            outboxRepository.save(outbox);
        }
        else{
            log.warn("outboxId = {} Aborted 업데이트 실패", outboxId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateToInProgress(Long outboxId){
        Optional<Outbox> optionalOutbox = outboxRepository.findById(outboxId);
        if(optionalOutbox.isPresent()){
            Outbox outbox = optionalOutbox.get();
            outbox.updateToInProgress();
            outboxRepository.save(outbox);
        }
        else{
            log.warn("outboxId = {} IN_PROGRESS 업데이트 실패", outboxId);
        }
    }

}
