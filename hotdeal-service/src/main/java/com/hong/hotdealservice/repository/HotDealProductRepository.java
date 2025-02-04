package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.domain.HotDealProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotDealProductRepository extends JpaRepository<HotDealProduct, Long> {

}
