package com.hong.hotdeal.service;

import com.hong.hotdeal.domain.*;
import com.hong.hotdeal.domain.status.DeliveryStatus;
import com.hong.hotdeal.exception.ErrorCode;
import com.hong.hotdeal.exception.OrderException;
import com.hong.hotdeal.repository.OrderRepository;
import com.hong.hotdeal.web.dto.request.order.OrderRequestDto;
import com.hong.hotdeal.web.dto.response.order.OrderPagingResponseDto;
import com.hong.hotdeal.web.dto.response.order.OrderResponseDto;
import com.hong.hotdeal.web.dto.response.order.OrderSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final UserService userService;
    private final ProductService productService;
    private final OrderRepository orderRepository;

    // 주문
    @Transactional
    public OrderResponseDto createOrder(Long userId, OrderRequestDto requestDto){
        // 유저 조회
        User foundUser = userService.findById(userId);

        // 주문 상품 목록
        List<OrderRequestDto.OrderProductRequest> products = requestDto.getProducts();
        // 주문 상품들 저장할 List
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        // 주문 상품 조회
        for (OrderRequestDto.OrderProductRequest requestProduct : products) {
            Product foundProduct = productService.findById(requestProduct.getProductId());

            // 재고가 남아 있는지 확인
            if(foundProduct.getStock() < requestProduct.getQuantity()){
                throw new OrderException(ErrorCode.ORDER_PRODUCT_NO_STOCK);
            }
            // 재고 있으면 주문 상품 목록에 저장
            OrderProduct orderProduct = OrderProduct.create(foundProduct, requestProduct.getQuantity());
            orderProducts.add(orderProduct);
        }

        // 주문 시작
        Address address = Address.create(requestDto.getCity(), requestDto.getStreet(), requestDto.getZipCode());
        Delivery delivery = Delivery.create(address);
        Order order = Order.create(foundUser, delivery, orderProducts);
        // Cascade로 인해 OderProduct도 저장 됨
        orderRepository.save(order);

        // orderProducts 사용하면 응답 가능
        List<OrderResponseDto.OrderProductDto> productDtos = orderProducts.stream()
                .map(OrderResponseDto.OrderProductDto::new).collect(Collectors.toList());

        return new OrderResponseDto(order.getId(), productDtos, order.getTotalPrice(), delivery.getStatus());
    }

    // 간단한 주문 내역
    public OrderPagingResponseDto getOrders(Long userId, Long cursor, int size){
        PageRequest pageRequest = PageRequest.of(0, size);

        // 조회
        Page<Order> page = orderRepository.findOrdersByCursorAndUserId(cursor, userId, size, pageRequest);
        List<Order> contents = page.getContent();

        List<OrderSummaryResponseDto> orderDtos = contents.stream()
                .map(o -> new OrderSummaryResponseDto(o.getId(), o.getTotalPrice(), o.getStatus(),
                        o.getDelivery().getStatus())).collect(Collectors.toList());

        // cursor 갱신
        Long newCursor = contents.isEmpty() ? null : contents.get(contents.size() - 1).getId();
        return new OrderPagingResponseDto(newCursor, orderDtos);
    }


    // 주문 조회 단건
    public OrderResponseDto getOrder(Long userId, Long orderId){
        // 주문과 관련된 데이터들 fetch join 조회
        Order foundOrder = orderRepository.findOrderWithProductsAndDelivery(orderId, userId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderResponseDto.OrderProductDto> productDtos = foundOrder.getOrderProducts().stream()
                .map(OrderResponseDto.OrderProductDto::new).collect(Collectors.toList());

        return new OrderResponseDto(foundOrder.getId(), productDtos,
                foundOrder.getTotalPrice(), foundOrder.getDelivery().getStatus());
    }

    // 주문 취소
    public void cancelOrder(Long userId, Long orderId){
        Order foundOrder = orderRepository.findOrderWithDelivery(userId, orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        DeliveryStatus deliveryStatus = foundOrder.getDelivery().getStatus();
        if(deliveryStatus == DeliveryStatus.PENDING){
            foundOrder.cancel();
        }
        else{
            throw new OrderException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }
    }

    // 반품
    public void returnOrder(Long userId, Long orderId){
        // 주문 조회
        Order foundOrder = orderRepository.findOrderWithProductsAndDelivery(orderId, userId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        // 배송 상태 확인
        // 배송 완료가 아니면 반품 불가능
        if(foundOrder.getDelivery().getStatus() != DeliveryStatus.DELIVERED){
            throw new OrderException(ErrorCode.ORDER_RETURN_NOT_ALLOWED);
        }
        // 배송 완료 상태에서 D+1 까지만 반품 가능
        if(foundOrder.getDelivery().getCompletedAt().plusDays(1).isBefore(LocalDateTime.now())){
            throw new OrderException(ErrorCode.ORDER_RETURN_PERIOD_EXPIRED);
        }

        // 반품 상태 변경
        foundOrder.getDelivery().updateStatusToReturn();
    }
}
