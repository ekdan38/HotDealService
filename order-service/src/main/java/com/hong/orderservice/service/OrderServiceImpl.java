package com.hong.orderservice.service;

import com.hong.common.dto.*;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.client.hotdeal.Resilience4JHotDealServiceClient;
import com.hong.orderservice.client.product.Resilience4JProductServiceClient;
import com.hong.orderservice.domain.Delivery;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.OrderProduct;
import com.hong.orderservice.domain.base.Address;
import com.hong.orderservice.domain.status.DeliveryStatus;
import com.hong.orderservice.domain.status.OrderStatus;
import com.hong.orderservice.dto.OrderPagingResponseDto;
import com.hong.orderservice.dto.OrderProductResponseDto;
import com.hong.orderservice.dto.OrderResponseDto;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.web.dto.OrderProductRequest;
import com.hong.orderservice.web.dto.OrderRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[OrderServiceImpl]")
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final Resilience4JProductServiceClient resilience4JProductServiceClient;
    private final Resilience4JHotDealServiceClient resilience4JHotDealServiceClient;


    // 주문 생성
    @Transactional
    @Override
    public OrderResponseDto createOrder(Long userId, OrderRequestDto requestDto) {
        // 1. hotDealProducts 조회 및 검증
        List<HotDealProductStockCheckResponseDto> hotDealProductStockCheckResponseDto = fetchHotDealProductAndValidate(userId, requestDto.getProducts());

        // 2. products 조회 및 검증
        List<ProductStockCheckResponseDto> productStockCheckResponseDtos = fetchProductAndValidate(userId, requestDto.getProducts());

        // 3. order, orderProduct 생성
        Order savedOrder = createOrder(userId, requestDto, hotDealProductStockCheckResponseDto, productStockCheckResponseDtos);

        // 4. 응답 Dto 변환
        return convertToOrderResponse(savedOrder, savedOrder.getOrderProducts());
    }

    // 주문 내역 페이징
    @Transactional
    @Override
    public OrderPagingResponseDto getOrders(Long userId, Long cursor, int size) {
        // 1. orders 페이징 조회(cursor 기반)
        List<Order> page = fetchOrdersByCursor(userId, cursor, size);

        // 2. 사용자 조회 시점에서 주문, 배송 상태 update
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        updateDeliveryAndOrderStatusForUser(userId, now, page);

        // 3. 응답 Dto 변환, cursor 지정
        return convertToOrderPagingResponse(page);
    }

    // 주문 단건 조회
    @Transactional
    @Override
    public OrderResponseDto getOrder(Long userId, Long orderId) {
        // 1. Order 조회 및 검증
        Order order = getOrderWithOrderProductsAndDelivery(userId, orderId);

        // 2. 사용자 조회 시점에서 주문, 배송 상태 update
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        updateDeliveryAndOrderStatusForUser(userId, now, List.of(order));

        // 3. 응답 Dto 변환
        return convertToOrderResponse(order, order.getOrderProducts());
    }

    // 주문 취소
    @Transactional
    @Override
    public OrderResponseDto cancelOrder(Long userId, Long orderId) {
        // 1. order 조회 및 취소 가능 여부 검증
        Order order = fetchOrderAndValidateCancelAble(userId, orderId);

        // 2. orderProduct 중 hotDealProduct 재고 증가 요청
        increaseHotDealProductStockAndValidate(userId, orderId, order);

        // 3. orderProduct 중 product 재고 증가 요청
        // (hotDealProduct 에 대한 재고 증가는 성공 했지만, product 재고 증가 호출이 실패 하면 성공한 hotDealProduct 대한 재고 감소 처리)
        try {
            increaseProductStockAndValidate(userId, orderId, order);
        } catch (OrderException e) {
            decreaseHotDealProductStockAndValidate(userId, orderId, order);
            throw e;
        }
        // 4. order, delivery 주문 취소 처리(Cancel)
        order.updateStatusToCancel();

        // 5. 응답 Dto 변환
        return convertToOrderResponse(order);
    }

    // 환불
    @Transactional
    @Override
    public OrderResponseDto returnOrder(Long userId, Long orderId) {
        // 1. order 조회 및 환불 가능 검증
        Order order = fetchOrderAndValidateReturn(userId, orderId);

        // 2. 주문, 배송 반품 처리 (ReturnRequested)
        order.updateStatusReturnRequested(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        // 3. 응답 Dto 변환
        return convertToOrderResponse(order);
    }

    // order 조회, 반품 가능 검증
    private Order fetchOrderAndValidateReturn(Long userId, Long orderId) {
        Order order = getOrderWithOrderProductsAndDelivery(userId, orderId);

        Delivery delivery = order.getDelivery();
        DeliveryStatus deliveryStatus = delivery.getDeliveryStatus();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime oneDayAgo = now.minusDays(1);

        // 배송 완료 후 1일 이내 까지 반품 가능
        if (!(deliveryStatus.equals(DeliveryStatus.DELIVERED)
                && delivery.getCompletedAt() != null
                && delivery.getCompletedAt().isAfter(oneDayAgo))) {
            log.debug("환불은 배송 완료 후 하루 이내에 가능합니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_RETURN_EXPIRED, userId, orderId);
        }
        return order;
    }

    // orders 페이징 조회(cursor 기반)
    private List<Order> fetchOrdersByCursor(Long userId, Long cursor, int size) {
        cursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        PageRequest pageRequest = PageRequest.of(0, size);
        List<Order> page = orderRepository.findOrdersByCursorAndUserIdAndSize(cursor, userId, pageRequest);
        return page;
    }

    // Delivery Status 사용자 조회 시점에서 update
    void updateDeliveryAndOrderStatusForUser(Long userId, LocalDateTime now, List<Order> page) {

        // 스케쥴링과 별개로 사용자의 관점에서 배송 상태가 변경 되야 한다.
        LocalDateTime oneDayAgo = now.minusDays(1);

        // 1. 결제 완료 후 1일 경과한 배송 DELIVERING 으로 변경
        List<Order> updateDelivering = page.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID
                        && o.getPaidAt() != null
                        && o.getPaidAt().isBefore(oneDayAgo)
                        && o.getDelivery().getDeliveryStatus() == DeliveryStatus.DELIVERABLE)
                .toList();
        updateDelivering.forEach(o -> o.getDelivery().updateToDelivering(now));

        // 2. 배송 시작 후 1일 경과한 배송 DELIVERED 로 변경
        List<Order> updateToDelivered = page.stream()
                .filter(o -> o.getDelivery().getDeliveryStatus() == DeliveryStatus.DELIVERING
                        && o.getDelivery().getStartedAt().isBefore(oneDayAgo))
                .toList();
        updateToDelivered.forEach(o -> o.getDelivery().updateToDelivered(now));

        // 3. 환불 처리 후 1일 경과한 된 배송 RETURNED 로 변경 및 재고 복구
        List<Order> updateToReturned = page.stream()
                .filter(o -> o.getStatus() == OrderStatus.RETURN_REQUESTED
                        && o.getDelivery().getDeliveryStatus() == DeliveryStatus.RETURN_REQUESTED
                        && o.getDelivery().getReturnStartedAt().isBefore(now))
                .toList();
        updateToReturned.forEach(o -> o.updateStatusReturned(now));

        // 재고 복구
        updateToReturned.forEach(o -> {
            increaseHotDealProductStockAndValidate(userId, o.getId(), o);
            increaseProductStockAndValidate(userId, o.getId(), o);
        });
    }

    // 결제 까지 완료한 주문 조회, 검증 (Fetch Join 으로 orderProducts, delivery 조회)
    private Order getOrderWithOrderProductsAndDelivery(Long userId, Long orderId) {
        // 주문 조회 (Fetch Join 으로 orderProducts, delivery 조회)
        return orderRepository.findOrderWithDeliveryAndOpById(orderId, userId).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, userId, orderId);
        });
    }


    // order 조회, 취소 가능 검증
    private Order fetchOrderAndValidateCancelAble(Long userId, Long orderId) {
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
        // 총 주문 가격 구하기
        List<OrderProduct> orderProducts = new ArrayList<>();

        // hotDealProduct 에서 주문 가격 계산
        int hotDealProductAmount = hotDealProductStockCheckResponseDtos.stream()
                .mapToInt(product -> product.getRequestedQuantity() * product.getHotDealPrice())
                .sum();
        // product 에서 주문 가격 계산
        int productAmount = productStockCheckResponseDtos.stream()
                .mapToInt(product -> product.getRequestedQuantity() * product.getPrice())
                .sum();

        // 총 주문 가격
        int amount = hotDealProductAmount + productAmount;

        // OrderProduct 생성
        // hotDealProduct 에 대한 주문 생성
        orderProducts.addAll(hotDealProductStockCheckResponseDtos.stream()
                .map(product -> OrderProduct.createHotDealProduct(
                        product.getHotDealProductId(),
                        product.getProductTitle(),
                        product.getRequestedQuantity(),
                        product.getHotDealPrice()))
                .toList());

        // product 에 대한 주문 생성
        orderProducts.addAll(productStockCheckResponseDtos.stream()
                .map(product -> OrderProduct.createProduct(
                        product.getProductId(),
                        product.getTitle(),
                        product.getRequestedQuantity(),
                        product.getPrice()))
                .toList());

        // Delivery 생성
        Address address = Address.create(requestDto.getCity(), requestDto.getStreet(), requestDto.getZipCode());
        Delivery delivery = Delivery.create(address);
        // Order 생성
        Order order = Order.create(userId, delivery, orderProducts, amount);
        return orderRepository.save(order);
    }


    // 상품 조회
    private List<ProductStockCheckResponseDto> fetchProductAndValidate(Long userId, List<OrderProductRequest> products) {
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
    private List<HotDealProductStockCheckResponseDto> fetchHotDealProductAndValidate(Long userId, List<OrderProductRequest> products) {
        // 요청에서 hotDealProducts 추출
        List<HotDealProductStockCheckRequestDto> hotDealProductStockCheckRequestDtos = convertToHotDealProductStockCheckRequestDto(products);

        // hotDealProduct 없으면 empty List 반환 => hotDealService 로 요청 보낼 필요 없음
        if (hotDealProductStockCheckRequestDtos.isEmpty()) return new ArrayList<>();

        // hotDealProducts 조회 호출(hotDeal 검증, 재고 확인, 요청 검사)
        List<HotDealProductStockCheckResponseDto> hotDealProductStockCheckResponseDtos = resilience4JHotDealServiceClient.fetchProductsAndValidateStock(hotDealProductStockCheckRequestDtos);
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
        if (productStockUpdateRequestDtos.isEmpty()) return new ArrayList<>();

        // product 재고 증가 feignClient 호출
        List<ProductStockUpdateResponseDto> productStockUpdateResponseDtos = resilience4JProductServiceClient.increaseStock(productStockUpdateRequestDtos);

        // circuitBreaker OPEN
        if (productStockUpdateResponseDtos.isEmpty()) {
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
        if (hotDealProductStockUpdateRequestDtos.isEmpty()) return new ArrayList<>();

        // hotDealProduct 재고 증가 feignClient 호출
        List<HotDealProductStockUpdateResponseDto> hotDealProductStockUpdateResponseDtos = resilience4JHotDealServiceClient.increaseStock(hotDealProductStockUpdateRequestDtos);

        // circuitBreaker OPEN
        if (hotDealProductStockUpdateResponseDtos.isEmpty()) {
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
        if (hotDealProductStockUpdateRequestDtos.isEmpty()) return new ArrayList<>();

        // hotDealProduct 재고 감소 feignClient 호출
        List<HotDealProductStockUpdateResponseDto> hotDealProductStockUpdateResponseDtos = resilience4JHotDealServiceClient.decreaseStock(hotDealProductStockUpdateRequestDtos);

        // circuitBreaker OPEN
        if (hotDealProductStockUpdateResponseDtos.isEmpty()) {
            log.debug("핫딜 상품 감소 호출을 실패했습니다. userId = {}, orderId = {}, hotDealProducts = {}", userId, orderId, hotDealProductStockUpdateResponseDtos);
            throw new OrderException(ErrorCode.ORDER_DECREASE_HOTDEAL_PRODUCT_FAILED, userId, orderId, hotDealProductStockUpdateResponseDtos);
        }
        return hotDealProductStockUpdateResponseDtos;
    }


    // products 에서 일반 상품 조회 Dto 변환
    private List<ProductStockCheckRequestDto> convertToProductStockCheckRequestDto(List<OrderProductRequest> products) {
        return products.stream()
                .filter(product -> product.getHotDealProductId() == null && product.getProductId() != null)
                .map(product -> new ProductStockCheckRequestDto(
                        product.getProductId(),
                        product.getQuantity()))
                .collect(toList());
    }

    // products 에서 핫딜 상품 조회 Dto 변환
    private List<HotDealProductStockCheckRequestDto> convertToHotDealProductStockCheckRequestDto(List<OrderProductRequest> products) {
        return products.stream()
                .filter(request -> request.getProductId() == null && request.getHotDealProductId() != null)
                .map(request -> new HotDealProductStockCheckRequestDto(
                        request.getHotDealProductId(),
                        request.getQuantity()))
                .collect(toList());
    }

    // 요청 dto 에서 product 추출, feignClient 요청 dto 변환
    private List<ProductStockUpdateRequestDto> convertToProductStockUpdateDto(Order order) {
        return order.getOrderProducts().stream()
                .filter(op -> op.getHotDealProductId() == null && op.getProductId() != null)
                .map(op -> new ProductStockUpdateRequestDto(
                        op.getProductId(),
                        op.getQuantity()
                ))
                .collect(toList());
    }

    // 요청 dto 에서 hotDealProduct 추출, feignClient 요청 dto 변환
    private List<HotDealProductStockUpdateRequestDto> convertToHotDealProductStockUpdateDto(Order order) {
        return order.getOrderProducts().stream()
                .filter(op -> op.getProductId() == null && op.getHotDealProductId() != null)
                .map(op -> new HotDealProductStockUpdateRequestDto(
                        op.getHotDealProductId(),
                        op.getQuantity()))
                .collect(toList());
    }

    // 주문 내역 페이징 조회 응답 Dto 변환, cursor 지정
    private OrderPagingResponseDto convertToOrderPagingResponse(List<Order> page) {
        List<OrderResponseDto> orderResponseDtos = page.stream().map(this::convertToOrderResponse).toList();
        Long nextCursor = orderResponseDtos.isEmpty() ? 0 : orderResponseDtos.get(orderResponseDtos.size() - 1).getOrderId();
        return new OrderPagingResponseDto(nextCursor, orderResponseDtos);
    }

    // OrderResponseDto 응답 변환
    private OrderResponseDto convertToOrderResponse(Order order) {
        return new OrderResponseDto(order);
    }

    private OrderResponseDto convertToOrderResponse(Order order, List<OrderProduct> orderProducts) {
        List<OrderProductResponseDto> orderProductResponseDtos = orderProducts
                .stream()
                .map(OrderProductResponseDto::new)
                .collect(toList());
        return new OrderResponseDto(order, orderProductResponseDtos);
    }
}
