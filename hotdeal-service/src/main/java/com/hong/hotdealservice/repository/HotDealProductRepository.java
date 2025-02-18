package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.domain.HotDealProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotDealProductRepository extends JpaRepository<HotDealProduct, Long> {

    // FetchJoin 으로 쿼리 최적화 하기 위해서 List 로 반환 페이징
    // jpql은 limit 미지원 => pageable 사용 해서 size 적용
    @Query("SELECT hp " +
            "FROM HotDealProduct hp " +
            "WHERE hp.hotDeal.id = :hotDealId " +
            "AND hp.id < :cursor " +
            "AND (:search IS NULL OR hp.productTitle LIKE %:search%) " +
            "ORDER BY hp.id DESC")
    List<HotDealProduct> findByCursorAndSearchAndSizeHotDealProducts(@Param("hotDealId") Long hotDealId,
                                                              @Param("cursor") Long cursor,
                                                              @Param("search") String search,
                                                              Pageable pageable);

    @Query("SELECT hp " +
            "FROM HotDealProduct hp " +
            "JOIN FETCH hp.hotDeal " +
            "WHERE hp.id =:hotDealProductId")
    Optional<HotDealProduct> findByIdWithHotDeal(@Param("hotDealProductId") Long hotDealProductId);

    @Query("SELECT hp " +
            "FROM HotDealProduct hp " +
            "WHERE hp.id IN :hotDealProductIds")
    List<HotDealProduct> findByIds(@Param("hotDealProductIds") List<Long>hotDealProductIds);
}
