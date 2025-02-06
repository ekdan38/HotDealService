package com.hong.orderservice.service;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.status.DeliveryStatus;
import com.hong.orderservice.dto.OrderPagingResponseDto;
import com.hong.orderservice.dto.OrderResponseDto;
import com.hong.orderservice.repository.DeliveryRepository;
import com.hong.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[AdminServiceImpl]")
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;

    // 주문 내역 페이징 (userId 기준 가능)
    @Override
    public OrderPagingResponseDto getOrders(Long targetUserId, Long cursor, int size) {
        // Delivery Status ADMIN 조회 시점에서 update
        updateDeliveryAndOrderStatusForAdmin(targetUserId);

        // cursor 가 null 이면 가장 최근 데이터 조회 처리
        if (cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // 페이징 조회
        List<Order> page = orderRepository.findOrdersByCursorAndUserIdAndSize(cursor, targetUserId, pageRequest);

        // 응답 Dto 변환
        List<OrderResponseDto> orderResponseDtos = page.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());

        // nextCursor 지정
        Long nextCursor = orderResponseDtos.isEmpty() ? 0 : orderResponseDtos.get(orderResponseDtos.size() - 1).getOrderId();
        return new OrderPagingResponseDto(nextCursor, orderResponseDtos);
    }


    // 주문 단건 조회
    @Override
    public OrderResponseDto getOrder(Long targetUserId, Long orderId) {
        // Delivery Status ADMIN 조회 시점에서 update
        updateDeliveryAndOrderStatusForAdmin(targetUserId);

        // order 조회
        Order order = getOrderWithOrderProductsAndDelivery(targetUserId, orderId);
        return convertToOrderResponse(order);
    }

    // OrderResponseDto 응답 변환
    private OrderResponseDto convertToOrderResponse(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getDelivery().getDeliveryStatus(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getOrderProducts().stream().map(op -> new OrderResponseDto.OrderProductDto(
                        op.getHotDealId(),
                        op.getProductId(),
                        op.getHotDealProductId(),
                        op.getProductTitle(),
                        op.getQuantity(),
                        op.getPrice()
                )).collect(Collectors.toList())
        );
    }

    // Delivery Status ADMIN 조회 시점에서 update
    private void updateDeliveryAndOrderStatusForAdmin(Long targetUserId) {
        // targetUser == null 이면 전체 주문, 주문 벌크 업데이트 처리
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        if(targetUserId == null) {
            deliveryRepository.bulkUpdatePendingDeliveriesToDelivering(
                    oneDayAgo,
                    now,
                    DeliveryStatus.PENDING,
                    DeliveryStatus.DELIVERING
            );
            deliveryRepository.bulkUpdateDeliveringDeliveriesToDelivered(
                    oneDayAgo,
                    now,
                    DeliveryStatus.DELIVERING,
                    DeliveryStatus.DELIVERED
            );
            deliveryRepository.bulkUpdateDeliveryStatusReturned(
                    now,
                    oneDayAgo
            );
            orderRepository.bulkUpdateOrderStatusToReturned(
                    oneDayAgo
            );

        }
        // targetUser != null 이면 해당 유저의 주문 벌크 업데이트 처리
        else{
            deliveryRepository.bulkUpdatePendingDeliveriesToDeliveringByUserId(
                    targetUserId,
                    oneDayAgo,
                    now,
                    DeliveryStatus.PENDING,
                    DeliveryStatus.DELIVERING
            );
            deliveryRepository.bulkUpdateDeliveringDeliveriesToDeliveredByUserId(
                    targetUserId,
                    oneDayAgo,
                    now,
                    DeliveryStatus.DELIVERING,
                    DeliveryStatus.DELIVERED
            );
            deliveryRepository.bulkUpdateDeliveryStatusReturnedByUserId(
                    targetUserId,
                    now,
                    oneDayAgo
            );
            orderRepository.bulkUpdateOrderStatusToReturnedByUserId(
                    targetUserId,
                    oneDayAgo
            );
        }
    }

    // Fetch Join 으로 order, orderProducts, delivery 조회
    private Order getOrderWithOrderProductsAndDelivery(Long userId, Long orderId) {
        // Fetch Join 으로 orderProducts, delivery 조회
        Order order = orderRepository.findPaidOrderByOrderIdAndUserIdWithOpAndD(orderId, userId);
        // 주문이 존재 하지 않으면
        if (order == null) {
            log.debug("요청된 주문이 존재하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND, userId, orderId);
        }
        return order;
    }
}
