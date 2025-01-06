package com.hong.productservice.repository;

import com.hong.productservice.domain.WishlistProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface wishlistProductRepository extends JpaRepository<WishlistProduct, Long> {


    @Query("SELECT wp " +
            "FROM WishlistProduct wp " +
            "JOIN FETCH wp.product p " +
            "WHERE wp.wishlist.id = :wishlistId AND wp.id < :cursor " +
            "ORDER BY wp.id DESC")
    List<WishlistProduct> findByWishlistIdAndCursor(@Param("wishlistId") Long wishlistId,
                                                    @Param("cursor") Long cursor,
                                                    Pageable pageable);

    @Query("SELECT wp FROM WishlistProduct wp " +
            "JOIN FETCH wp.product p " +
            "WHERE wp.wishlist.id = :wishlistId AND wp.product.id IN :productIds")
    List<WishlistProduct> findByWishlistIdAndProductIds(@Param("wishlistId") Long wishlistId,
                                                        @Param("productIds") List<Long> productIds
    );

}
