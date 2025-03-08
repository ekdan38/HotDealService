package com.hong.productservice.repository;

import com.hong.productservice.domain.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist>findByUserId(Long userId);

    // wishlist, wishlistProduct fetch join
    @Query("SELECT w FROM Wishlist w JOIN FETCH w.wishlistProducts WHERE w.userId = :userId")
    Optional<Wishlist> findByUserIdWithProducts(@Param("userId") Long userId);

    // userId로 wishlist 조회 하면서 wishlistProducts, product fetch join
    @Query("SELECT w FROM Wishlist w " +
            "LEFT JOIN FETCH w.wishlistProducts wp " +
            "LEFT JOIN FETCH wp.product " +
            "WHERE w.userId = :userId")
    Optional<Wishlist> findWithProductsByUserId(@Param("userId") Long userId);
}
