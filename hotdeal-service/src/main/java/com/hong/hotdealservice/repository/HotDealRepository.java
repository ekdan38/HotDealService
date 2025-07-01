package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.dto.projection.HotDealSimpleDto;
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

    @Query("SELECT new com.hong.hotdealservice.dto.projection.HotDealSimpleDto" +
            "(h.id, h.title, h.description, h.status, h.startTime, h.endTime) " +
            "FROM HotDeal h " +
            "WHERE h.deleted = false " +
            "AND h.id < :cursor " +
            "AND (:search IS NULL OR h.title LIKE %:search%) " +
            "ORDER BY h.id DESC")
    List<HotDealSimpleDto> findByCursorAndSearchAndSizeHotDeals(@Param("cursor") Long cursor,
                                                                @Param("search") String search,
                                                                Pageable pageable);


    // 단건 조회
    @Query("SELECT new com.hong.hotdealservice.dto.projection.HotDealSimpleDto" +
            "(h.id, h.title, h.description, h.status, h.startTime, h.endTime) " +
            "FROM HotDeal h " +
            "WHERE h.deleted = false " +
            "AND h.id = :hotDealId")
    Optional<HotDealSimpleDto> findHotDealById(@Param("hotDealId") Long hotDealId);

    // hotDealProducts Fetch Join 조회
    @Query("SELECT h " +
            "FROM HotDeal h " +
            "JOIN FETCH h.hotDealProducts hp " +
            "WHERE h.deleted = false " +
            "AND h.id = :hotDealId")
    Optional<HotDeal> findByIdWithHotDealProducts(@Param("hotDealId") Long hotDealId);

    @Query("SELECT new com.hong.hotdealservice.dto.projection.HotDealSimpleDto" +
            "(h.id, h.title, h.description, h.status, h.startTime, h.endTime) " +
            "FROM HotDeal h " +
            "WHERE h.deleted = false " +
            "AND h.id IN :hotDealIds")
    List<HotDealSimpleDto> findByIds(@Param("hotDealIds") List<Long> hotDealIds);


    @Query("SELECT h FROM HotDeal h " +
            "JOIN FETCH h.hotDealProducts " +
            "WHERE h.endTime BETWEEN :fiveMinutesAgo AND :now " +
            "AND h.isStockSynced = false")
    List<HotDeal> findRecentlyEndedHotDeals(@Param("fiveMinutesAgo") LocalDateTime fiveMinutesAgo,
                                            @Param("now") LocalDateTime now);


    // hotDeal status 변경 (ACTIVE)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE HotDeal h SET h.status = 'ACTIVE' " +
            "WHERE h.deleted = false " +
            "AND h.status = 'SCHEDULED' " +
            "AND h.startTime <= :currentTime " +
            "AND h.endTime > :currentTime")
    int updateScheduledToActive(@Param("currentTime") LocalDateTime currentTime);

    // hotDeal status 변경 (EXPIRED)
    // 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE HotDeal h SET h.status = 'EXPIRED', h.deletedAt =:currentTime " +
            "WHERE h.deleted = false " +
            "AND h.status = 'ACTIVE' " +
            "AND h.endTime <= :currentTime")
    int updateScheduledToExpired(@Param("currentTime") LocalDateTime currentTime);


}
