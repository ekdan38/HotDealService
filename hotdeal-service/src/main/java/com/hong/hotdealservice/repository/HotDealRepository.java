package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.domain.HotDeal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HotDealRepository extends JpaRepository<HotDeal, Long> {

    boolean existsByTitle(String title);

    // FetchJoin 으로 쿼리 최적화 하기 위해서 List 로 반환 페이징
    // jpql은 limit 미지원 => pageable 사용 해서 size 적용
    @Query("SELECT h " +
            "FROM HotDeal h " +
            "WHERE h.deleted = false " +
            "AND h.id < :cursor " +
            "AND (:search IS NULL OR h.title LIKE %:search%) " +
            "ORDER BY h.id DESC")
    List<HotDeal> findByCursorAndSearchAndSizeHotDeals(@Param("cursor") Long cursor,
                                                       @Param("search") String search,
                                                       Pageable pageable);

    // hotDealProducts Fetch Join 조회
    @Query("SELECT h " +
            "FROM HotDeal h " +
            "JOIN FETCH h.hotDealProducts hp " +
            "WHERE h.deleted = false " +
            "AND h.id = :hotDealId")
    Optional<HotDeal> findByIdWithHotDealProducts(@Param("hotDealId") Long hotDealId);


    @Query("SELECT h " +
            "FROM HotDeal h " +
            "JOIN FETCH h.hotDealProducts hp " +
            "WHERE h.deleted = false " +
            "AND h.id IN :hotDealIds")
    List<HotDeal> findByIdsWithHotDealProducts(@Param("hotDealIds") List<Long> hotDealIds);

    @Query("SELECT h " +
            "FROM HotDeal h " +
            "WHERE h.deleted = false " +
            "AND h.id IN :hotDealIds")
    List<HotDeal> findByIds(@Param("hotDealIds") List<Long> hotDealIds);

    // hotDeal status 변경 (ACTIVE)
    // 벌크 업데이트
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    @Query("UPDATE HotDeal h SET h.status = 'ACTIVE' " +
            "WHERE h.deleted = false " +
            "AND h.status = 'SCHEDULED' " +
            "AND h.startTime <= :currentTime " +
            "AND h.endTime > :currentTime")
    void updateScheduledToActive(@Param("currentTime") LocalDateTime currentTime);

    // hotDeal status 변경 (EXPIRED)
    // 벌크 업데이트
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    @Query("UPDATE HotDeal h SET h.status = 'EXPIRED', h.expiredAt =:currentTime " +
            "WHERE h.deleted = false " +
            "AND h.status = 'ACTIVE' " +
            "AND h.endTime <= :currentTime")
    void updateScheduledToExpired(@Param("currentTime") LocalDateTime currentTime);

    // hotDeal status 변경 (EXPIRED) 하고 나서 변경된 hotDeal 조회
    @Query("SELECT h " +
            "FROM HotDeal h " +
            "JOIN FETCH h.hotDealProducts hp " +
            "WHERE h.deleted = false " +
            "AND h.status = 'EXPIRED' " +
            "AND h.expiredAt = :now")
    List<HotDeal> findHotDealsByExpiredAtNow(@Param("now") LocalDateTime now);

}
