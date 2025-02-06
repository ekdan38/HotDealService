package com.hong.orderservice.service;

import com.hong.common.dto.OrderFetchRequestDto;
import com.hong.common.dto.OrderFetchResponseDto;
import com.hong.common.dto.orderHotDealProductDto;
import com.hong.common.dto.OrderProductDto;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[OrderApiService]")
@Transactional(readOnly = true)
public class OrderApiService {

    private final OrderRepository orderRepository;

    public OrderFetchResponseDto fetchOrder(OrderFetchRequestDto requestDto) {
        Long orderId = requestDto.getOrderId();
        Long userId = requestDto.getUserId();

        // order 조회, 검증
        Order order = fetchOrderAndValidate(orderId, userId);

        // 주문 상품 추출
        List<orderHotDealProductDto> hotDealProducts = extractHotDealProducts(order);
        List<OrderProductDto> products = extractProducts(order);

        // 결제 가격 연산
        int amount = getAmount(order);

        return new OrderFetchResponseDto(userId, orderId, amount, order.getStatus().name(), hotDealProducts, products);
    }

    // hotDealProducts 추출
    private List<orderHotDealProductDto> extractHotDealProducts(Order order) {
        return  order.getOrderProducts().stream()
                .filter(op -> op.getHotDealId() != null)
                .map(op -> new orderHotDealProductDto(
                        op.getHotDealId(),
                        op.getHotDealProductId(),
                        op.getQuantity()))
                .collect(Collectors.toList());
    }

    // products 추출
    private List<OrderProductDto> extractProducts(Order order) {
        return order.getOrderProducts().stream()
                .map(op -> new OrderProductDto(
                        op.getProductId(),
                        op.getQuantity()))
                .collect(Collectors.toList());
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
        Order order = orderRepository.findOrderWithOrderProductsByUserIdAndOrderId(orderId, userId).orElseThrow(() -> {
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
