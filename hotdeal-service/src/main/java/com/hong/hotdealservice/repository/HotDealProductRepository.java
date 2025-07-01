package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.projection.ProductSimpleDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotDealProductRepository extends JpaRepository<HotDealProduct, Long> {

    @Query("SELECT new com.hong.hotdealservice.dto.projection.ProductSimpleDto" +
            "(hp.id, hp.hotDeal.id, hp.price, hp.title)" +
            "FROM HotDealProduct hp " +
            "WHERE hp.hotDeal.id = :hotDealId " +
            "AND hp.hotDeal.status = 'ACTIVE' " +
            "AND hp.hotDeal.deleted = false " +
            "AND hp.id < :cursor " +
            "AND (:search IS NULL OR hp.title LIKE %:search%) " +
            "ORDER BY hp.id DESC")
    List<ProductSimpleDto> findByCursorAndSearchAndSizeHotDealProducts(@Param("hotDealId") Long hotDealId,
                                                                       @Param("cursor") Long cursor,
                                                                       @Param("search") String search,
                                                                       Pageable pageable);

    @Query("SELECT new com.hong.hotdealservice.dto.projection.ProductSimpleDto" +
            "(hp.id, hp.hotDeal.id, hp.price, hp.title)" +
            "FROM HotDealProduct hp " +
            "WHERE hp.id = :productId " +
            "AND hp.hotDeal.status = 'ACTIVE' " +
            "AND hp.hotDeal.deleted = false")
    Optional<ProductSimpleDto> findActiveProductById(@Param("productId") Long productId);


    List<HotDealProduct> findByIdIn(List<Long> hotDealProductIds);

    @Query("SELECT hp " +
            "FROM HotDealProduct hp " +
            "JOIN FETCH hp.hotDeal " +
            "WHERE hp.id IN :ids")
    List<HotDealProduct> findByIdsWithHotdeal(@Param("ids") List<Long> ids);

    boolean existsByTitle(String title);
}
