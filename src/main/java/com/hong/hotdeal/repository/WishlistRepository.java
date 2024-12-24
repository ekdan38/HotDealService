package com.hong.hotdeal.repository;

import com.hong.hotdeal.domain.Product;
import com.hong.hotdeal.domain.Wishlist;
import com.hong.hotdeal.domain.WishlistProduct;
import com.hong.hotdeal.web.dto.response.wishlist.WishlistResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserId(Long userId);

    @Query("SELECT w FROM Wishlist w JOIN FETCH w.wishlistProducts WHERE w.user.id = :userId")
    Optional<Wishlist> findByUserIdWithProducts(@Param("userId") Long userId);


    @Query("SELECT new com.hong.hotdeal.web.dto.response.wishlist.WishlistResponseDto(w.id, wp.id, p.id, p.title, wp.quantity, c.title) " +
            "FROM Wishlist w " +
            "JOIN w.wishlistProducts wp " +
            "JOIN wp.product p " +
            "JOIN CategoryProduct cp ON cp.product.id = p.id " +
            "JOIN cp.category c " +
            "WHERE w.user.id = :userId " +
            "AND wp.id < :cursor " +
            "ORDER BY wp.id DESC")
    Page<WishlistResponseDto> findWishlistProductsByCursor(@Param("userId") Long userId,
                                                           @Param("cursor") Long cursor,
                                                           Pageable pageable);


}
