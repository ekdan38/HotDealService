package com.hong.orderservice.service;

import com.hong.common.dto.*;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.client.hotdeal.Resilience4JHotDealServiceClient;
import com.hong.orderservice.client.payment.Resilience4JPaymentServiceClient;
import com.hong.orderservice.domain.Delivery;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.OrderProduct;
import com.hong.orderservice.domain.base.Address;
import com.hong.orderservice.domain.outbox.CreatePaymentOutbox;
import com.hong.orderservice.domain.status.DeliveryStatus;
import com.hong.orderservice.domain.status.OrderStatus;
import com.hong.orderservice.dto.OrderPagingResponseDto;
import com.hong.orderservice.dto.OrderProductResponseDto;
import com.hong.orderservice.dto.OrderResponseDto;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.repository.outbox.CreatePaymentOutboxRepository;
import com.hong.orderservice.web.dto.OrderProductRequest;
import com.hong.orderservice.web.dto.OrderRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[OrderServiceImpl]")
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final Resilience4JHotDealServiceClient hotDealServiceClient;
    private final Resilience4JPaymentServiceClient paymentServiceClient;
    private final CreatePaymentOutboxRepository createPaymentOutboxRepository;

    // 주문 생성
    @Transactional
    @Override
    public OrderResponseDto createOrder(Long userId, OrderRequestDto requestDto) {
        String orderId = generateOrderId();

        // 1. product stock 점유 요청 (hotdealService 로 feignClient 요청. 동기 처리)
        ProductReservationResponseDto reserveStockResponse = reserveProductsAndValidate(orderId, userId, requestDto.getProducts());

        // 2. orderProduct, order 생성 및 save
        Order savedOrder = createOrderProductAndOrder(orderId, userId, requestDto, reserveStockResponse);

        // 4. payment 생성 outbox
        saveCreatePaymentOutbox(userId, savedOrder);

        // 3. 응답 Dto 변환
        return convertToCreateResponseDto(savedOrder);
    }

    // 주문 내역 페이징
    @Transactional
    @Override
    public OrderPagingResponseDto getOrders(Long userId, String cursor, int size) {
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
    public OrderResponseDto getOrder(Long userId, String orderId) {
        // 1. Order 조회 및 검증
        Order order = getOrderWithOrderProductsAndDelivery(userId, orderId);

        // 2. 사용자 조회 시점에서 주문, 배송 상태 update
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        updateDeliveryAndOrderStatusForUser(userId, now, List.of(order));

        // 3. 응답 Dto 변환
        return convertToOrderResponse(order, order.getOrderProducts());
    }

    // 주문 취소
    // 결제 완료, 배송 이전, 결제 후 1일 이내 가능
    @Transactional
    @Override
    public OrderResponseDto cancelOrder(Long userId, String orderId) {

        // 1. 주문 조회 및 주문 취소 가능 여부 검증
        Order order = getOrderAndValidateCancelAble(userId, orderId);

        // 2. 결제 취소 요청 paymentService FeignClient 호출
        cancelPaymentRequest(userId, orderId);

        // 3. 재고 복구 요청 hotdealService FeignClient 호출
        restoreStockRequest(userId, orderId);

        // 4. 주문 취소 처리
        order.updateToCancel();

        // 5. 응답 Dto 변환
        return convertToOrderResponse(order);
    }

    // 환불
    // 배송 완료 후 1일 이내 가능
    @Transactional
    @Override
    public OrderResponseDto refundOrder(Long userId, String orderId) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        // 1. 주문 조회 및 환불 가능 여부 검증
        Order order = fetchOrderAndValidateRefundAble(userId, orderId, now);

        // 2. 주문, 배송 환불 요청 처리
        order.updateToRequestRefund(now);

        // 3. 응답 Dto 변환
        return convertToOrderResponse(order);
    }


    // orders 페이징 조회(cursor 기반)
    private List<Order> fetchOrdersByCursor(Long userId, String cursor, int size) {
        LocalDateTime defaultCursor = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
        LocalDateTime cursorDateTime = (cursor != null) ? LocalDateTime.parse(cursor) : defaultCursor;

        PageRequest pageRequest = PageRequest.of(0, size);
        return orderRepository.findOrdersByCursorAndUserIdAndSize(cursorDateTime, userId, pageRequest);
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
                        && o.getDelivery().getStatus() == DeliveryStatus.DELIVERABLE)
                .toList();
        updateDelivering.forEach(o -> o.getDelivery().updateToDelivering(now));

        // 2. 배송 시작 후 1일 경과한 배송 DELIVERED 로 변경
        List<Order> updateToDelivered = page.stream()
                .filter(o -> o.getDelivery().getStatus() == DeliveryStatus.DELIVERING
                        && o.getDelivery().getStartedAt().isBefore(oneDayAgo))
                .toList();
        updateToDelivered.forEach(o -> o.getDelivery().updateToDelivered(now));

        // 3. 환불 처리 후 1일 경과한 된 배송 RETURNED 로 변경 및 재고 복구
        List<Order> updateToReturned = page.stream()
                .filter(o -> o.getStatus() == OrderStatus.RETURN_REQUESTED
                        && o.getDelivery().getStatus() == DeliveryStatus.RETURN_REQUESTED
                        && o.getDelivery().getReturnStartedAt().isBefore(oneDayAgo))
                .toList();
        updateToReturned.forEach(o -> o.updateStatusReturned(now));

        if(!updateToReturned.isEmpty()){
            // 3.1. 결제 취소 요청 paymentService FeignClient 호출
            // 3.2. 재고 복구 요청 hotdealService FeignClient 호출
            updateToReturned.forEach(order -> {
                cancelPaymentRequest(userId, order.getId());
                restoreStockRequest(userId, order.getId());
            });
        }
    }

    // 결제 까지 완료한 주문 조회, 검증 (Fetch Join 으로 orderProducts, delivery 조회)
    private Order getOrderWithOrderProductsAndDelivery(Long userId, String orderId) {
        // 주문 조회 (Fetch Join 으로 orderProducts, delivery 조회)
        return orderRepository.findOrderWithDeliveryAndOpById(orderId, userId).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, userId, orderId);
        });
    }

    private void saveCreatePaymentOutbox(Long userId, Order savedOrder){
        CreatePaymentOutbox outbox = CreatePaymentOutbox.create(savedOrder.getId(), userId, savedOrder.getAmount(), savedOrder.getExpiresAt());
        createPaymentOutboxRepository.save(outbox);
    }

    // order 생성 및 save
    private Order createOrderProductAndOrder(String orderId, Long userId, OrderRequestDto requestDto,
                                            ProductReservationResponseDto reserveStockResponse) {
        // 총 주문 가격 구하기
        List<OrderProduct> orderProducts = new ArrayList<>();

        // hotDealProduct 에서 주문 가격 계산
        BigDecimal amount = reserveStockResponse.getProducts().stream()
                .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getReservedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // OrderProduct 생성
        orderProducts.addAll(reserveStockResponse.getProducts().stream()
                .map(product -> OrderProduct.create(
                        product.getProductId(),
                        product.getProductTitle(),
                        product.getReservedQuantity(),
                        product.getPrice()))
                .toList());

        // Delivery 생성
        Address address = Address.create(requestDto.getCity(), requestDto.getStreet(), requestDto.getZipcode());
        Delivery delivery = Delivery.create(address);

        // Order 생성
        Order order = Order.create(orderId, userId, delivery, orderProducts, amount);
        return orderRepository.save(order);
    }


    // hotDealProduct stock 점유 요청 (hotdealService 로 feignClient 요청)
    private ProductReservationResponseDto reserveProductsAndValidate(String orderId,
                                                                     Long userId,
                                                                     List<OrderProductRequest> products) {
        // 1. 요청 Dto 변환
        List<ProductReservationDto> requestProducts = products.stream()
                .map(p -> new ProductReservationDto(
                        p.getProductId(),
                        p.getQuantity())
                ).toList();
        ProductReservationRequestDto request = new ProductReservationRequestDto(orderId, requestProducts);

        // 2. hotdealService 로 feignClient 요청
        ProductReservationResponseDto reserveStockResponse = hotDealServiceClient.reserveStock(request);

        // 3. 응답이 empty List 라면 circuitBreaker 작동, 예외 처리
        if(reserveStockResponse.isFallback()){
            List<Long> failedProductIds = products.stream().map(OrderProductRequest::getProductId).toList();
            log.error("핫딜 상품 점유 요청을 실패했습니다. userId = {}, orderId = {}, products = {}", userId, orderId, failedProductIds);
            throw new OrderException(ErrorCode.ORDER_HOTDEAL_SERVICE_FAILED);
        }
        return reserveStockResponse;
    }

    private String generateOrderId(){
        return UUID.randomUUID().toString();
    }

    private void restoreStockRequest(Long userId, String orderId) {
        StockRestoreRequestDto stockRestoreRequestDto = new StockRestoreRequestDto(orderId, userId);
        StockRestoreResponseDto stockRestoreResponseDto = hotDealServiceClient.restoreStock(stockRestoreRequestDto);
        if(!stockRestoreResponseDto.isSuccess()){
            log.error("재고 복구를 실패했습니다. userId ={}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_HOTDEAL_RESTORE_STOCK_FAILED, userId, orderId);
        }
    }

    private void cancelPaymentRequest(Long userId, String orderId) {
        PaymentCancelRequestDto requestDto = new PaymentCancelRequestDto(orderId, userId);
        PaymentCancelResponseDto responseDto = paymentServiceClient.cancelPayment(requestDto);
        if(!responseDto.isSuccess()){
            log.error("결제 취소를 실패했습니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_PAYMENT_CANCEL_FAILED, userId, orderId);
        }
    }

    private Order getOrderAndValidateCancelAble(Long userId, String orderId) {
        // 주문 조회 (Fetch Join 으로 orderProducts, delivery 조회)
        Order order = orderRepository.findOrderWithDeliveryAndOpById(orderId, userId).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, userId, orderId);
        });

        // 주문 상태 검증 (PAID)
        if(!order.getStatus().equals(OrderStatus.PAID)){
            log.error("결제 완료된 주문만 환불할 수 있습니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_INVALID_CANCEL, userId, orderId);
        }

        // 주문 취소 가능 조건 검증 (결제 후 1일 이내)
        if(order.getPaidAt().plusDays(1).isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))){
            log.error("환불은 결제 완료 후 1일 이내에 가능합니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_CANCEL_EXPIRED, userId, orderId);
        }
        return order;
    }

    private Order fetchOrderAndValidateRefundAble(Long userId, String orderId, LocalDateTime now) {
        // 주문 조회 (Fetch Join 으로 orderProducts, delivery 조회)
        Order order = orderRepository.findOrderWithDeliveryAndOpById(orderId, userId).orElseThrow(() -> {
            log.debug("요청된 주문이 존재하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            return new OrderException(ErrorCode.ORDER_NOT_FOUND, userId, orderId);
        });

        // 환불 가능 조건 검증 (배송 완료 후 1일 이내 가능)
        Delivery delivery = order.getDelivery();
        if (!delivery.getStatus().equals(DeliveryStatus.DELIVERED)) {
            log.error("환불은 배송 완료된 주문만 가능합니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_INVALID_REFUND, userId, orderId);
        }

        LocalDateTime completedAt = delivery.getCompletedAt();
        if (completedAt == null || completedAt.plusDays(1).isBefore(now)) {
            log.error("환불은 배송 완료 후 1일 이내에만 가능합니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_REFUND_EXPIRED, userId, orderId);
        }
        return order;
    }

    // 주문 내역 페이징 조회 응답 Dto 변환, cursor 지정
    private OrderPagingResponseDto convertToOrderPagingResponse(List<Order> page) {
        List<OrderResponseDto> orderResponseDtos = page.stream().map(this::convertToOrderResponse).toList();
        LocalDateTime nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).getCreatedAt();
        return new OrderPagingResponseDto(nextCursor, orderResponseDtos);
    }

    // 주문 생성 응답
    private OrderResponseDto convertToCreateResponseDto(Order order){
        return new OrderResponseDto(order.getId(), order.getUserId(), order.getAmount());
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
