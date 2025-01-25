package com.hong.hotdealservice.repository;

import com.hong.common.dto.HotDealProductDto;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotDealRepository extends JpaRepository<HotDeal, Long> {

    boolean existsByTitle(String title);

    // FetchJoin 으로 쿼리 최적화 하기 위해서 List 로 반환 페이징
    // jpql은 limit 미지원 => pageable 사용 해서 size 적용
    @Query("SELECT h " +
            "FROM HotDeal h " +
            "WHERE h.id < :cursor " +
            "AND (:search IS NULL OR h.title LIKE %:search%) " +
            "ORDER BY h.id DESC")
    List<HotDeal> findByCursorAndSearchAndSize(@Param("cursor") Long cursor,
                                               @Param("search") String search,
                                               Pageable pageable);

    // hotDealProducts Fetch Join 조회
    @Query("SELECT h " +
            "FROM HotDeal h " +
            "JOIN FETCH h.hotDealProducts hp " +
            "WHERE h.id = :hotDealId")
    HotDeal findByIdWithHotDealProducts(@Param("hotDealId") Long hotDealId);


    @Query("SELECT h " +
            "FROM HotDeal h " +
            "JOIN FETCH h.hotDealProducts hp " +
            "WHERE h.id IN :hotDealIds")
    List<HotDeal> findByIdsWithHotDealProducts(@Param("hotDealIds") List<Long> hotDealIds);

}
