package com.hong.orderservice.service;

import com.hong.common.dto.*;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.OrderProduct;
import com.hong.orderservice.domain.status.OrderStatus;
import com.hong.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[OrderApiService]")
@Transactional(readOnly = true)
public class OrderApiService {

    private final OrderRepository orderRepository;

    // Order 조회
    public OrderFetchResponseDto fetchOrder(OrderFetchRequestDto requestDto) {
        // 1. order 조회, 검증
        Order order = fetchOrderAndValidate(requestDto.getOrderId(), requestDto.getUserId());

        // 2. 주문 상품 추출
        // hotDealProduct
        List<orderHotDealProductDto> hotDealProducts = extractHotDealProducts(order);
        // product
        List<OrderProductDto> products = extractProducts(order);

        // 3. 응답 Dto 변환
        return convertToOrderFetchResponse(requestDto, order, hotDealProducts, products);
    }

    // payment 처리 기반 order, delivery update 처리
    @Transactional
    public Boolean updateOrderAndDelivery(OrderUpdateRequestDto requestDto){
        // 1. order 조회 (delivery fetch join) 및 검증
        Order order = fetchOrderWithDeliveryAndValidate(requestDto);

        // 2. 결제 성공 실패 처리(requestDto 로 결제 성공 유무)
        // 결제 성공 처리
        if(requestDto.getIsSuccess()) order.paymentSuccess(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        // 결제 실패 처리
        else order.paymentFailed();

        return true;
    }

    // hotDealProducts 추출
    private List<orderHotDealProductDto> extractHotDealProducts(Order order) {
        return  order.getOrderProducts().stream()
                .filter(op -> op.getHotDealProductId() != null && op.getProductId() == null)
                .map(op -> new orderHotDealProductDto(
                        op.getHotDealProductId(),
                        op.getQuantity()))
                .collect(Collectors.toList());
    }

    // products 추출
    private List<OrderProductDto> extractProducts(Order order) {
        return order.getOrderProducts().stream()
                .filter(op -> op.getProductId() != null && op.getHotDealProductId() == null)
                .map(op -> new OrderProductDto(
                        op.getProductId(),
                        op.getQuantity()))
                .collect(Collectors.toList());
    }

    private OrderFetchResponseDto convertToOrderFetchResponse(OrderFetchRequestDto requestDto,
                                                              Order order,
                                                              List<orderHotDealProductDto> hotDealProducts,
                                                              List<OrderProductDto> products) {
        return new OrderFetchResponseDto(requestDto.getUserId(), requestDto.getOrderId(),
                order.getAmount(), order.getStatus().name(), hotDealProducts, products);
    }

    private Order fetchOrderWithDeliveryAndValidate(OrderUpdateRequestDto requestDto) {
        Order order = orderRepository.findByIdAndUserIdWithDelivery(requestDto.getOrderId(),  requestDto.getUserId()).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. userId = {}, orderId = {}",  requestDto.getUserId(), requestDto.getOrderId());
            return new OrderException(ErrorCode.ORDER_NOT_FOUND,  requestDto.getUserId(), requestDto.getOrderId());
        });
        return order;
    }

    // 결제 가격 연산
    private int getAmount(Order order) {
        List<OrderProduct> orderProducts = order.getOrderProducts();
        int amount = 0;
        for (OrderProduct orderProduct : orderProducts) {
            amount += (orderProduct.getPrice() * orderProduct.getQuantity());
        }
        return amount;
    }

    // order 조회, 검증
    private Order fetchOrderAndValidate(Long orderId, Long userId) {
        Order order = orderRepository.findByOrderIdAndUserIdWithOp(userId, orderId).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, userId, orderId);
        });

        OrderStatus status = order.getStatus();
        if (status != OrderStatus.PENDING_PAYMENT) {
            log.debug("주문이 결제 대기 상태가 아닙니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_NOT_PENDING_PAYMENT, userId, orderId);
        }
        return order;
    }
}
