package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.domain.RedisStockSaveFail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedisStockSaveFailRepository extends JpaRepository<RedisStockSaveFail, Long> {
}
