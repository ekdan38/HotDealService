package com.hong.orderservice.service;

import com.hong.common.dto.*;
import com.hong.common.entity.Address;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.client.hotdeal.Resilience4JHotDealServiceClient;
import com.hong.orderservice.client.product.Resilience4JProductServiceClient;
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
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final Resilience4JProductServiceClient resilience4JProductServiceClient;
    private final Resilience4JHotDealServiceClient resilience4JHotDealServiceClient;


    // 주문 생성
    @Transactional
    @Override
    public OrderResponseDto createOrder(Long userId, OrderRequestDto requestDto) {
        // hotDealProducts 조회
        List<HotDealProductStockCheckResponseDto> hotDealProductStockCheckResponseDto = fetchHotDealProductAndValidate(userId, requestDto.getProducts());

        // products 조회
        List<ProductStockCheckResponseDto> productStockCheckResponseDtos = fetchProductAndValidate(userId, requestDto.getProducts());

        // 주문 생성
        Order savedOrder = createOrder(userId, requestDto, hotDealProductStockCheckResponseDto, productStockCheckResponseDtos);

        return convertToOrderResponse(savedOrder);
    }

    // 주문 내역 페이징
    @Transactional
    @Override
    public OrderPagingResponseDto getOrders(Long userId, Long cursor, int size) {
        updateDeliveryAndOrderStatusForUser(userId);
        // cursor 가 null 이면 가장 최근 데이터 조회 처리
        if (cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // 페이징 조회
        List<Order> page = orderRepository.findOrdersByCursorAndUserIdAndSize(cursor, userId, pageRequest);

        // 응답 Dto 변환
        List<OrderResponseDto> orderResponseDtos = page.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());

        // nextCursor 지정
        Long nextCursor = orderResponseDtos.isEmpty() ? 0 : orderResponseDtos.get(orderResponseDtos.size() - 1).getOrderId();
        return new OrderPagingResponseDto(nextCursor, orderResponseDtos);
    }

    // 주문 단건 조회
    @Transactional
    @Override
    public OrderResponseDto getOrder(Long userId, Long orderId) {
        updateDeliveryAndOrderStatusForUser(userId);
        // Order 조회
        Order order = getOrderWithOrderProductsAndDelivery(userId, orderId);
        // 응답 Dto 변환
        return convertToOrderResponse(order);
    }

    // 주문 취소
    @Transactional
    @Override
    public OrderResponseDto cancelOrder(Long userId, Long orderId) {
        // order 조회, 취소 가능 검증
        Order order = fetchOrderAndValidateCancel(userId, orderId);

        // 주문 상품 중 핫딜 상품 재고 증가 처리
        increaseHotDealProductStockAndValidate(userId, orderId, order);

        // 주문 상품 중 일반 상품 재고 증가 처리
        // 핫딜 상품에 대한 재고 증가는 성공 했지만, 일반 상품 재고 증가 호출이 실패 하면 성공한 핫딜 상품에 대한 재고 감소 처리
        try{
            increaseProductStockAndValidate(userId, orderId, order);
        }catch (OrderException e){
            decreaseHotDealProductStockAndValidate(userId, orderId, order);
            throw e;
        }
        // 주문, 배송 상태 변경 (Cancel)
        order.updateStatusToCancel();

        return convertToOrderResponse(order);
    }


    // 반품
    @Transactional
    @Override
    public OrderResponseDto returnOrder(Long userId, Long orderId) {
        // order 조회, 환불 가능 검증
        Order order = fetchOrderAndValidateReturn(userId, orderId);

        // 주문, 배송 상태 변경 (ReturnRequested)
        order.updateStatusReturnRequested();

        return convertToOrderResponse(order);
    }

    // order 조회, 반품 가능 검증
    private Order fetchOrderAndValidateReturn(Long userId, Long orderId) {
        Order order = getOrderWithOrderProductsAndDelivery(userId, orderId);

        Delivery delivery = order.getDelivery();
        DeliveryStatus deliveryStatus = delivery.getDeliveryStatus();
        LocalDateTime completedAt = delivery.getCompletedAt();
        LocalDateTime now = LocalDateTime.now();

        // 배송 완료 후 1일 이내 까지 반품 가능
        if (!(deliveryStatus.equals(DeliveryStatus.DELIVERED) && completedAt != null && now.isBefore(completedAt.plusDays(1)))) {
            log.debug("환불은 배송 완료 후 하루 이내에 가능합니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_RETURN_EXPIRED, userId, orderId);
        }
        return order;
    }


    // Delivery Status 사용자 조회 시점에서 update
    private void updateDeliveryAndOrderStatusForUser(Long userId) {
        List<Long> orderIds = orderRepository.findOrdersByUserId(userId);

        // 스케쥴링과 별개로 사용자의 관점에서 배송 상태가 변경 되야 한다.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        // 결제 완료, 주문 후 1일 경과한 배송 DELIVERING 로 상태 변경
        deliveryRepository.bulkUpdatePendingDeliveriesToDeliveringByUserId(
                userId,
                oneDayAgo,
                now,
                DeliveryStatus.PENDING,
                DeliveryStatus.DELIVERING
        );

        // 결제 완료, 배송 시작 후 1일 경과한 배송 DELIVERED 로 상태 변경
        deliveryRepository.bulkUpdateDeliveringDeliveriesToDeliveredByUserId(
                userId,
                oneDayAgo,
                now,
                DeliveryStatus.DELIVERING,
                DeliveryStatus.DELIVERED
        );

        // 배송 상태 반품 처리
        deliveryRepository.bulkUpdateDeliveryStatusReturnedByUserId(
                userId,
                now,
                oneDayAgo
        );

        // 주문 상태 배송 처리
        orderRepository.bulkUpdateOrderStatusToReturnedByUserId(
                userId,
                oneDayAgo
        );
    }

    // 결제 까지 완료한 주문 조회, 검증 (Fetch Join 으로 orderProducts, delivery 조회)
    private Order getOrderWithOrderProductsAndDelivery(Long userId, Long orderId) {
        // 결제 까지 완료한 주문 조회 (Fetch Join 으로 orderProducts, delivery 조회)
       return orderRepository.findPaidOrderByOrderIdAndUserIdWithOpAndD(orderId, userId).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, userId, orderId);
        });
    }


    // order 조회, 취소 가능 검증
    private Order fetchOrderAndValidateCancel(Long userId, Long orderId) {
        // 결제 까지 완료한 주문 조회 (Fetch Join 으로 orderProducts, delivery 조회)
        Order order = orderRepository.findPaidOrderByOrderIdAndUserIdWithOpAndD(orderId, userId).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, userId, orderId);
        });

        // 주문 취소 가능 한지 확인 (주문 후 1일 이내 가능 => 배송 상태 DELIVERABLE 일때 가능)
        DeliveryStatus deliveryStatus = order.getDelivery().getDeliveryStatus();
        if (!deliveryStatus.equals(DeliveryStatus.DELIVERABLE)) {
            log.debug("주문 취소는 주문 후 하루 이내 가능합니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_CANCEL_EXPIRED, userId, orderId);
        }
        return order;
    }

    // 주문 생성
    private Order createOrder(Long userId, OrderRequestDto requestDto,
                              List<HotDealProductStockCheckResponseDto> hotDealProductStockCheckResponseDtos,
                              List<ProductStockCheckResponseDto> productStockCheckResponseDtos) {
        List<OrderProduct> orderProducts = new ArrayList<>();
        // OrderProduct 생성
        // hotDealProduct 에 대한 주문 생성
        orderProducts.addAll(hotDealProductStockCheckResponseDtos.stream()
                .map(product -> OrderProduct.create(
                        product.getProductId(),
                        product.getHotDealId(),
                        product.getHotDealProductId(),
                        product.getProductTitle(),
                        product.getRequestQuantity(),
                        product.getHotDealPrice()))
                .collect(Collectors.toList()));

        // product 에 대한 주문 생성
        orderProducts.addAll(productStockCheckResponseDtos.stream()
                .map(product -> OrderProduct.create(
                        product.getProductId(),
                        product.getTitle(),
                        product.getQuantity(),
                        product.getPrice()))
                .collect(Collectors.toList()));

        // Delivery 생성
        Address address = Address.create(requestDto.getCity(), requestDto.getStreet(), requestDto.getZipCode());
        Delivery delivery = Delivery.create(address);
        // Order 생성
        Order order = Order.create(userId, delivery, orderProducts);
        return orderRepository.save(order);
    }

    // 상품 조회
    private List<ProductStockCheckResponseDto> fetchProductAndValidate(Long userId, List<OrderRequestDto.OrderProductRequest> products) {
        // 요청에서 products 추출
        List<ProductStockCheckRequestDto> productStockCheckRequestDtos = convertToProductStockCheckRequestDto(products);

        // product 없으면 empty List 반환 => productService 로 요청 보낼 필요 없음
        if (productStockCheckRequestDtos.isEmpty()) return new ArrayList<>();

        // products 조회 호출(product 검증, 재고 확인, 요청 검사)
        List<ProductStockCheckResponseDto> productStockCheckResponseDtos = resilience4JProductServiceClient.fetchProducts(productStockCheckRequestDtos);

        if (productStockCheckResponseDtos.isEmpty()) {
            log.debug("상품 조회 호출을 실패했습니다. userId = {}, products = {}", userId, productStockCheckRequestDtos);
            throw new OrderException(ErrorCode.ORDER_FETCH_PRODUCT_FAILED, userId, productStockCheckRequestDtos);
        }
        return productStockCheckResponseDtos;
    }

    // 핫딜 상품 조회
    private List<HotDealProductStockCheckResponseDto> fetchHotDealProductAndValidate(Long userId, List<OrderRequestDto.OrderProductRequest> products) {
        // 요청에서 hotDealProducts 추출
        List<HotDealProductStockCheckRequestDto> hotDealProductStockCheckRequestDtos = convertToHotDealProductStockCheckRequestDto(products);

        // hotDealProduct 없으면 empty List 반환 => hotDealService 로 요청 보낼 필요 없음
        if (hotDealProductStockCheckRequestDtos.isEmpty()) return new ArrayList<>();

        // hotDealProducts 조회 호출(hotDeal 검증, 재고 확인, 요청 검사)
        List<HotDealProductStockCheckResponseDto> hotDealProductStockCheckResponseDtos = resilience4JHotDealServiceClient.fetchProducts(hotDealProductStockCheckRequestDtos);
        if (hotDealProductStockCheckResponseDtos.isEmpty()) {
            log.debug("핫딜 상품 조회 호출을 실패했습니다. userId = {}, hotDealProducts = {}", userId, hotDealProductStockCheckRequestDtos);
            throw new OrderException(ErrorCode.ORDER_FETCH_HOTDEAL_PRODUCT_FAILED, userId, hotDealProductStockCheckRequestDtos);
        }
        return hotDealProductStockCheckResponseDtos;
    }

    // 주문 상품 중 일반 상품 재고 증가 처리
    private List<ProductStockUpdateResponseDto> increaseProductStockAndValidate(Long userId, Long orderId, Order order) {
        // 요청 dto 에서 product 추출, feignClient 요청 dto 변환
        List<ProductStockUpdateRequestDto> productStockUpdateRequestDtos = convertToProductStockUpdateDto(order);

        // product 없으면 empty List 반환 => productService 로 요청 보낼 필요 없음
        if(productStockUpdateRequestDtos.isEmpty()) return new ArrayList<>();

        // product 재고 증가 feignClient 호출
        List<ProductStockUpdateResponseDto> productStockUpdateResponseDtos = resilience4JProductServiceClient.increaseStock(productStockUpdateRequestDtos);

        // circuitBreaker OPEN
        if(productStockUpdateResponseDtos.isEmpty()){
            log.debug("상품 증가 호출을 실패했습니다. userId = {}, orderId = {}, products = {}", userId, orderId, productStockUpdateResponseDtos);
            throw new OrderException(ErrorCode.ORDER_INCREASE_PRODUCT_FAILED, userId, orderId, productStockUpdateResponseDtos);
        }
        return productStockUpdateResponseDtos;
    }


    // 주문 상품 중 핫딜 상품 재고 증가 처리
    private List<HotDealProductStockUpdateResponseDto> increaseHotDealProductStockAndValidate(Long userId, Long orderId, Order order) {
        // 요청 dto 에서 hotDealProduct 추출, feignClient 요청 dto 변환
        List<HotDealProductStockUpdateRequestDto> hotDealProductStockUpdateRequestDtos = convertToHotDealProductStockUpdateDto(order);

        // hotDealProduct 없으면 empty List 반환 => hotDealService 로 요청 보낼 필요 없음
        if(hotDealProductStockUpdateRequestDtos.isEmpty()) return new ArrayList<>();

        // hotDealProduct 재고 증가 feignClient 호출
        List<HotDealProductStockUpdateResponseDto> hotDealProductStockUpdateResponseDtos = resilience4JHotDealServiceClient.increaseStock(hotDealProductStockUpdateRequestDtos);

        // circuitBreaker OPEN
        if(hotDealProductStockUpdateResponseDtos.isEmpty()){
            log.debug("핫딜 상품 증가 호출을 실패했습니다. userId = {}, orderId = {}, hotDealProducts = {}", userId, orderId, hotDealProductStockUpdateResponseDtos);
            throw new OrderException(ErrorCode.ORDER_INCREASE_HOTDEAL_PRODUCT_FAILED, userId, orderId, hotDealProductStockUpdateResponseDtos);
        }
        return hotDealProductStockUpdateResponseDtos;
    }

    // 주문 상품 중 핫딜 상품 재고 감소 처리
    private List<HotDealProductStockUpdateResponseDto> decreaseHotDealProductStockAndValidate(Long userId, Long orderId, Order order) {
        // 요청 dto 에서 hotDealProduct 추출, feignClient 요청 dto 변환
        List<HotDealProductStockUpdateRequestDto> hotDealProductStockUpdateRequestDtos = convertToHotDealProductStockUpdateDto(order);

        // hotDealProduct 없으면 empty List 반환 => hotDealService 로 요청 보낼 필요 없음
        if(hotDealProductStockUpdateRequestDtos.isEmpty()) return new ArrayList<>();

        // hotDealProduct 재고 감소 feignClient 호출
        List<HotDealProductStockUpdateResponseDto> hotDealProductStockUpdateResponseDtos = resilience4JHotDealServiceClient.decreaseStock(hotDealProductStockUpdateRequestDtos);

        // circuitBreaker OPEN
        if(hotDealProductStockUpdateResponseDtos.isEmpty()){
            log.debug("핫딜 상품 감소 호출을 실패했습니다. userId = {}, orderId = {}, hotDealProducts = {}", userId, orderId, hotDealProductStockUpdateResponseDtos);
            throw new OrderException(ErrorCode.ORDER_DECREASE_HOTDEAL_PRODUCT_FAILED, userId, orderId, hotDealProductStockUpdateResponseDtos);
        }
        return hotDealProductStockUpdateResponseDtos;
    }


    // products 에서 일반 상품 조회 Dto 변환
    private List<ProductStockCheckRequestDto> convertToProductStockCheckRequestDto(List<OrderRequestDto.OrderProductRequest> products) {
        return products.stream()
                .filter(product -> product.getHotDealId() == null)
                .map(product -> new ProductStockCheckRequestDto(
                        product.getProductId(),
                        product.getQuantity()))
                .collect(Collectors.toList());
    }

    // products 에서 핫딜 상품 조회 Dto 변환
    private List<HotDealProductStockCheckRequestDto> convertToHotDealProductStockCheckRequestDto(List<OrderRequestDto.OrderProductRequest> products) {
        return products.stream()
                .filter(request -> request.getHotDealId() != null)
                .map(request -> new HotDealProductStockCheckRequestDto(
                        request.getHotDealId(),
                        request.getHotDealProductId(),
                        request.getQuantity()))
                .collect(Collectors.toList());
    }

    // 요청 dto 에서 product 추출, feignClient 요청 dto 변환
    private List<ProductStockUpdateRequestDto> convertToProductStockUpdateDto(Order order) {
        return order.getOrderProducts().stream()
                .filter(op -> op.getHotDealProductId() == null)
                .map(op -> new ProductStockUpdateRequestDto(
                        op.getProductId(),
                        op.getQuantity()
                ))
                .collect(Collectors.toList());
    }

    // 요청 dto 에서 hotDealProduct 추출, feignClient 요청 dto 변환
    private List<HotDealProductStockUpdateRequestDto> convertToHotDealProductStockUpdateDto(Order order) {
        List<HotDealProductStockUpdateRequestDto> hotDealProductStockUpdateRequestDtos = order.getOrderProducts().stream()
                .filter(op -> op.getHotDealProductId() != null)
                .map(op -> new HotDealProductStockUpdateRequestDto(
                        op.getHotDealId(),
                        op.getHotDealProductId(),
                        op.getQuantity()))
                .collect(Collectors.toList());
        return hotDealProductStockUpdateRequestDtos;
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
                        op.getProductId(),
                        op.getHotDealId(),
                        op.getHotDealProductId(),
                        op.getProductTitle(),
                        op.getQuantity(),
                        op.getPrice()
                )).collect(Collectors.toList())
        );
    }
}
