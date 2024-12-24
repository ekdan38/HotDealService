package com.hong.hotdeal.repository;

import com.hong.hotdeal.domain.WishlistProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistProductRepository extends JpaRepository<WishlistProduct, Long> {
}
