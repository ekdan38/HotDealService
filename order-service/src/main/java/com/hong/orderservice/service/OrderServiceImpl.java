package com.hong.orderservice.service;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.entity.Address;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.client.ProductServiceClient;
import com.hong.orderservice.domain.Delivery;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.OrderProduct;
import com.hong.orderservice.domain.status.DeliveryStatus;
import com.hong.orderservice.dto.OrderPagingResponseDto;
import com.hong.orderservice.dto.OrderResponseDto;
import com.hong.orderservice.repository.DeliveryRepository;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.web.dto.OrderRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[OrderServiceImpl]")
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService{

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final ProductServiceClient productServiceClient;

    // 주문 생성
    // todo 동시성 문제 고려 해야함 추후 수정 예정
    @Transactional
    @Override
    public OrderResponseDto createOrder(Long userId, OrderRequestDto requestDto) {
        // user 조회 로직 스킵, 멘토링 이후 수정

        // 주문 상품 목록
        List<OrderRequestDto.OrderProductRequest> products = requestDto.getProducts();
        // 주문 상품을 저장할 List
        List<OrderProduct> orderProducts = new ArrayList<>();
        // 주문 상품 조회
        for (OrderRequestDto.OrderProductRequest requestProduct : products) {
            // feignClient 로 product-service Product 에 대한 검증
            ProductCommonDto productCommonDto = productServiceClient.getProductById(requestProduct.getProductId());
            // requestQuantity 가 1 이상 인지 검증
            Integer requestProductQuantity = requestProduct.getQuantity();
            if(requestProductQuantity < 0){
                log.error("상품은 1개 이상 부터 주문 가능 합니다. userId = {}, 요청 quantity = {}", userId, requestProductQuantity);
                throw new OrderException(ErrorCode.ORDER_QUANTITY_INVALID);
            }
            // 재고 확인
            Long productId = productCommonDto.getId();
            Integer productStock = productCommonDto.getStock();
            // 재고 없으면
            if(productStock < requestProductQuantity){
                log.error("상품의 수량이 부족 합니다. userId = {}, 요청 productId = {}, 요청 수량 = {}, 상품 재고 = {}" ,
                        userId, requestProduct.getProductId(), requestProductQuantity, productStock);
                throw new OrderException(ErrorCode.ORDER_PRODUCT_NO_STOCK);
            }
            // 재고 있으면
            // product 의 stock 감소 처리
            productServiceClient.decreaseStock(productId, requestProductQuantity);

            // orderProduct 생성 하고 orderProducts 에 Add
            OrderProduct orderProduct = OrderProduct.create(productId, productCommonDto.getTitle(), requestProductQuantity, productCommonDto.getPrice());
            orderProducts.add(orderProduct);
        }
        // Delivery 생성
        Address address = Address.create(requestDto.getCity(), requestDto.getStreet(), requestDto.getZipCode());
        Delivery delivery = Delivery.create(address);

        // Order 생성
        Order order = Order.create(userId, delivery, orderProducts);
        // Order 저장
        // cascade 로 인해 Delivery, OrderProduct 함께 저장 처리
        Order savedOrder = orderRepository.save(order);

        // 응답 dto 변환
        // orderProducts 사용하면 응답 가능
        List<OrderResponseDto.OrderProductDto> orderProductDtos = orderProducts.stream().map(op -> new OrderResponseDto
                .OrderProductDto(op.getProductId(), op.getProductTitle(), op.getQuantity(), op.getPrice())).collect(Collectors.toList());

        return new OrderResponseDto(
                savedOrder.getId(),
                userId,
                savedOrder.getTotalPrice(),
                savedOrder.getStatus(),
                delivery.getStatus(),
                savedOrder.getCreatedAt(),
                orderProductDtos);
    }

    // 주문 내역 페이징
    @Override
    public OrderPagingResponseDto getOrders(Long userId, Long cursor, int size) {
        // 스케쥴링과 별개로 사용자의 관점에서 배송 상태가 변경 되야 한다.
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        // bulk update
        deliveryRepository.updateOrderStatus(oneDayAgo, DeliveryStatus.DELIVERING.name(), DeliveryStatus.PENDING.name());
        deliveryRepository.updateOrderStatus(twoDaysAgo, DeliveryStatus.DELIVERED.name(), DeliveryStatus.DELIVERING.name());

        // cursor 가 null 이면 가장 최근 데이터 조회 처리
        if(cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // 페이징 조회
        List<Order> page = orderRepository.findOrdersByCursorAndUserIdAndSize(cursor, userId, pageRequest);

        // Dto 변환
        List<OrderResponseDto> orderResponseDtos = page.stream().map(order -> new OrderResponseDto(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getDelivery().getStatus(),
                order.getCreatedAt(),
                order.getOrderProducts().stream().map(op -> new OrderResponseDto.OrderProductDto(
                        op.getProductId(),
                        op.getProductTitle(),
                        op.getQuantity(),
                        op.getPrice()
                )).collect(Collectors.toList())
        )).collect(Collectors.toList());

        // nextCursor 지정
        Long nextCursor = orderResponseDtos.isEmpty() ? 0 : orderResponseDtos.get(orderResponseDtos.size() - 1).getOrderId();

        return new OrderPagingResponseDto(nextCursor, orderResponseDtos);
    }

    // 주문 조회
    @Override
    public OrderResponseDto getOrder(Long userId, Long orderId) {
        // 스케쥴링과 별개로 사용자의 관점에서 배송 상태가 변경 되야 한다.
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        // bulk update
        deliveryRepository.updateOrderStatus(oneDayAgo, DeliveryStatus.DELIVERING.name(), DeliveryStatus.PENDING.name());
        deliveryRepository.updateOrderStatus(twoDaysAgo, DeliveryStatus.DELIVERED.name(), DeliveryStatus.DELIVERING.name());

        // Fetch Join 으로 orderProducts, delivery 조회
        Order order = orderRepository.findOrderByOrderIdAndUserIdWithOpAndD(orderId, userId);

        // 주문이 존재 하지 않으면
        if(order == null){
            log.error("해당 주문은 존재 하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        return new OrderResponseDto(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getDelivery().getStatus(),
                order.getCreatedAt(),
                order.getOrderProducts().stream().map(op -> new OrderResponseDto.OrderProductDto(
                        op.getProductId(),
                        op.getProductTitle(),
                        op.getQuantity(),
                        op.getPrice()
                )).collect(Collectors.toList())
        );
    }

    // 주문 취소
    @Transactional
    @Override
    public OrderResponseDto cancelOrder(Long userId, Long orderId) {
        // 배송중이 되기 전 취소 가능 => DeliveryStatus 가 Pending 일 때만 취소 가능

        // Fetch Join 으로 orderProducts, delivery 조회
        Order order = orderRepository.findOrderByOrderIdAndUserIdWithOpAndD(orderId, userId);

        // 주문이 존재 하지 않으면
        if (order == null) {
            log.error("해당 주문은 존재 하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        DeliveryStatus deliveryStatus = order.getDelivery().getStatus();
        if (deliveryStatus == DeliveryStatus.PENDING) {
            // order, delivery Status 를 Cancel 로 변경
            order.cancel();
            // 상품 재고 복구
            // 취소 하려는 상품 목록
            List<OrderProduct> orderProducts = order.getOrderProducts();
            for (OrderProduct orderProduct : orderProducts) {
                Long productId = orderProduct.getProductId();
                Integer quantity = orderProduct.getQuantity();
                productServiceClient.increaseStock(productId, quantity);
            }
        } else {
            log.error("주문 상태가 대기 일때만 주문 취소가 가능 합니다. userId = {}, orderId = {}, 현재 배송 상태 = {}",
                    userId, orderId, deliveryStatus);
            throw new OrderException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        return new OrderResponseDto(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getDelivery().getStatus(),
                order.getCreatedAt(),
                order.getOrderProducts().stream().map(op -> new OrderResponseDto.OrderProductDto(
                        op.getProductId(),
                        op.getProductTitle(),
                        op.getQuantity(),
                        op.getPrice()
                )).collect(Collectors.toList())
        );
    }

    // 반품
    @Transactional
    @Override
    public OrderResponseDto returnOrder(Long userId, Long orderId) {
        // 배송 완료 후 D + 1일 까지만 반품 가능
        // => Delivery 의 status 가 Delivered 이고, completedDate + 1 이내에 반품 가능

        // Fetch Join 으로 orderProducts, delivery 조회
        Order order = orderRepository.findOrderByOrderIdAndUserIdWithOpAndD(orderId, userId);

        // 주문이 존재 하지 않으면
        if (order == null) {
            log.error("해당 주문은 존재 하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        DeliveryStatus deliveryStatus = order.getDelivery().getStatus();
        LocalDateTime completedAt = order.getDelivery().getCompletedAt();
        // 배송 상태가 Delivered 이고, D + 1 이내 일 때
        if(deliveryStatus == DeliveryStatus.DELIVERED && completedAt.plusDays(1).isBefore(LocalDateTime.now())){
            // order 상태 변경
            order.returnOrder();

        }
        else{
            log.error("반품은 배송 완료 상태에서 +1 일까지 가능합니다. userId = {}, orderId = {}, deliveryStatus = {}, completedAt = {}",
                    userId, orderId, deliveryStatus, completedAt);
            throw new OrderException(ErrorCode.ORDER_RETURN_NOT_ALLOWED);
        }

        return new OrderResponseDto(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getDelivery().getStatus(),
                order.getCreatedAt(),
                order.getOrderProducts().stream().map(op -> new OrderResponseDto.OrderProductDto(
                        op.getProductId(),
                        op.getProductTitle(),
                        op.getQuantity(),
                        op.getPrice()
                )).collect(Collectors.toList())
        );
    }
}
