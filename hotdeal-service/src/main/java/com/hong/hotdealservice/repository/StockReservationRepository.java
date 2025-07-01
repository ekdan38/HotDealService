package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.domain.StockReservation;
import com.hong.hotdealservice.domain.status.ReserveStatus;
import com.hong.hotdealservice.dto.projection.ProductReservedQuantityDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    @Query("SELECT new com.hong.hotdealservice.dto.projection.ProductReservedQuantityDto" +
            "(sr.productId, COALESCE(SUM(sr.reservedQuantity), 0L)) " +
            "FROM StockReservation sr " +
            "WHERE sr.productId IN :productIds " +
            "AND sr.status = :reserveStatus " +
            "GROUP BY sr.productId")
    List<ProductReservedQuantityDto> sumReservedQuantityByProductId(@Param("productIds") List<Long> productIds,
                                                                    @Param("reserveStatus") ReserveStatus reserveStatus);


    List<StockReservation> findAllByOrderId(String orderId);

}
