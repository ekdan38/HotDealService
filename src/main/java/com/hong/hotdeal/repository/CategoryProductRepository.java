package com.hong.hotdeal.repository;

import com.hong.hotdeal.domain.CategoryProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryProductRepository extends JpaRepository<CategoryProduct, Long> {
    boolean existsByCategoryIdAndProductId(Long categoryId, Long productId);
    CategoryProduct findByCategoryIdAndProductId(Long categoryId, Long productId);

    @Query("SELECT cp FROM CategoryProduct cp " +
            "JOIN FETCH cp.product p " +
            "JOIN FETCH cp.category c " +
            "WHERE p.id = :productId")
    CategoryProduct findCategoryProductWithProductAndCategory(@Param("productId") Long productId);

}
