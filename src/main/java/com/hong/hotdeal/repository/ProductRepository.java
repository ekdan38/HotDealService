package com.hong.hotdeal.repository;

import com.hong.hotdeal.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByTitle(String title);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN CategoryProduct cp ON p.id = cp.product.id " +
            "LEFT JOIN Category c ON c.id = cp.category.id " +
            "WHERE p.id < :cursor " +
            "AND (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:search IS NULL OR p.title LIKE %:search%) " +
            "ORDER BY p.id DESC")
    Page<Product> findProductsByCursorAndRequest(@Param("cursor") Long cursor,
                                                 @Param("categoryId") Long categoryId,
                                                 @Param("search") String search,
                                                 Pageable pageable);

}
