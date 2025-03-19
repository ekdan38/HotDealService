package com.hong.productservice.repository;

import com.hong.productservice.domain.Product;
import com.hong.productservice.dto.product.ProductResponseDto;
import com.hong.productservice.dto.product.ProductStockProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByTitle(String title);


     //jpql은 limit 미지원 => pageable 사용 해서 size 적용
    @Query("SELECT new com.hong.productservice.dto.product.ProductResponseDto(p.id, p.title, p.price) " +
            "FROM Product p " +
            "LEFT JOIN p.categoryProducts cp " +
            "LEFT JOIN cp.category c " +
            "WHERE p.id < :cursor " +
            "AND (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:search IS NULL OR p.title LIKE %:search%) " +
            "ORDER BY p.id DESC")
    List<ProductResponseDto> findProductsByCursorAndCategoryIdAndSearchAndSize(@Param("cursor") Long cursor,
                                                                               @Param("categoryId") Long categoryId,
                                                                               @Param("search") String search,
                                                                               Pageable pageable);

    // FetchJoin 으로 쿼리 최적화
    @Query("SELECT p " +
            "FROM Product p " +
            "JOIN FETCH p.categoryProducts cp " +
            "JOIN FETCH cp.category c " +
            "WHERE p.id = :productId")
    Optional<Product> findProductByProductIdWithCategoryProducts(@Param("productId") Long productId);

    @Query("SELECT p " +
            "FROM Product p " +
            "WHERE p.id IN :productIds")
    List<Product> findByIds(@Param("productIds") List<Long> productIds);

    @Query("SELECT DISTINCT p " +
            "FROM Product p " +
            "JOIN FETCH p.categoryProducts cp " +
            "JOIN FETCH cp.category c " +
            "WHERE p.id IN :productIds")
    List<Product> findByIdsWithCategory(@Param("productIds") List<Long> productIds);

    @Query("SELECT new com.hong.productservice.dto.product.ProductStockProjection(p.id, p.stock) " +
            "FROM Product p " +
            "WHERE p.id IN :productIds")
    List<ProductStockProjection>findStockByProductIds(@Param("productIds") List<Long> productIds);
}