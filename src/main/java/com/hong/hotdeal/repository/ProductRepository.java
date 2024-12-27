package com.hong.hotdeal.repository;

import com.hong.hotdeal.domain.Product;
import com.hong.hotdeal.web.dto.response.product.ProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByTitle(String title);

    @Query("SELECT new com.hong.hotdeal.web.dto.response.product.ProductResponseDto(p.id, p.title, p.price, p.stock, c.title) " +
            "FROM Product p " +
            "JOIN CategoryProduct cp ON p.id = cp.product.id " +
            "JOIN Category c ON c.id = cp.category.id " +
            "WHERE p.id < :cursor " +
            "AND (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:search IS NULL OR p.title LIKE %:search%) " +
            "ORDER BY p.id DESC")
    Page<ProductResponseDto> findProductsByCursorAndRequest(@Param("cursor") Long cursor,
                                                            @Param("categoryId") Long categoryId,
                                                            @Param("search") String search,
                                                            Pageable pageable);


}
