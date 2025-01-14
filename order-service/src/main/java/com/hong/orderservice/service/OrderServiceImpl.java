package com.hong.orderservice.service;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.dto.ProductStockDto;
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
import com.hong.orderservice.facade.RedissonLockService;
import com.hong.orderservice.repository.DeliveryRepository;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.web.dto.OrderRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[OrderServiceImpl]")
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final ProductServiceClient productServiceClient;
    private final RedissonLockService redissonLockService;

    // 주문 생성
    @Transactional
    @Override
    public OrderResponseDto createOrder(Long userId, OrderRequestDto requestDto) {
        // 상품 Id 추출
        List<Long> productIds = extractOrderProductIds(requestDto);

        // 획득 한 락 List
        List<RLock> locks = new ArrayList<>();
        try {
            // 락 획득
            locks = acquireLocks(productIds);

            // 주문 상품 조회, 검증, Map 변환
            Map<Long, ProductCommonDto> productMap = fetchAndValidateProducts(productIds, requestDto.getProducts(), userId);

            // 주문 생성, 저장
            Order savedOrder = createAndSaveOrder(userId, requestDto, productMap);

            // 응답 dto 변환
            return convertToOrderResponse(savedOrder);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderException(ErrorCode.ORDER_LOCK_FAILED);
        }finally {
            // 모든 락 해제
            releaseLocks(locks);
        }
    }

    // 주문 내역 페이징
    @Transactional
    @Override
    public OrderPagingResponseDto getOrders(Long userId, Long cursor, int size) {
        // Delivery Status 사용자 조회 시점에서 update
        updateDeliveryStatus();

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


    // 주문 조회
    @Transactional
    @Override
    public OrderResponseDto getOrder(Long userId, Long orderId) {
        // Delivery Status 사용자 조회 시점에서 update
        updateDeliveryStatus();

        // Order 조회
        Order order = getOrderWithOrderProductsAndDelivery(userId, orderId);

        // 응답 Dto 변환
        return convertToOrderResponse(order);
    }

    // 주문 취소
    @Transactional
    @Override
    public OrderResponseDto cancelOrder(Long userId, Long orderId) {
        // 배송중이 되기 전 취소 가능 => DeliveryStatus 가 Pending 일 때만 취소 가능

        // Order 조회
        Order order = getOrderWithOrderProductsAndDelivery(userId, orderId);

        // 취소 하려는 상품 목록
        List<OrderProduct> orderProducts = order.getOrderProducts();
        List<Long> productIds = orderProducts.stream().map(OrderProduct::getProductId)
                .collect(Collectors.toList());

        // 획득 한 락 List
        List<RLock> locks = new ArrayList<>();
        try{
            // 락 획득
            acquireLocks(productIds);

            // order 취소 변경
            List<ProductStockDto> stockIncreaseDto = cancelOrderIfPending(userId, orderId, order, orderProducts);

            // feignClient 로 재고 복구 호출 => CircuitBreaker 적용 필요
            productServiceClient.increaseStock(stockIncreaseDto);

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            // todo 고치기
            throw new OrderException(ErrorCode.ORDER_LOCK_FAILED);
        } finally {
            releaseLocks(locks);
        }
        return convertToOrderResponse(order);
    }


    // 반품
    @Transactional
    @Override
    public OrderResponseDto returnOrder(Long userId, Long orderId) {
        // 배송 완료 후 D + 1일 까지만 반품 가능
        // => Delivery 의 status 가 Delivered 이고, completedDate + 1 이내에 반품 가능

        // order 조회
        Order order = getOrderWithOrderProductsAndDelivery(userId, orderId);

        // DeliveryStatus 가 Delivered, 배송 완료 하루 까지 환불
        returnOrderIfDeliveredAndOneDay(userId, orderId, order);

        return convertToOrderResponse(order);
    }


    // 주문할 상품 Id 추출
    private List<Long> extractOrderProductIds(OrderRequestDto requestDto){
        // 주문 상품 목록
        List<OrderRequestDto.OrderProductRequest> productInfos = requestDto.getProducts();
        // 주문 productId 추출
        return productInfos.stream().map(OrderRequestDto.OrderProductRequest::getProductId).collect(Collectors.toList());
    }

    // 락 획득
    private List<RLock> acquireLocks(List<Long> productIds) throws InterruptedException {
        // 락 객체 목록 생성
        List<RLock> locks = new ArrayList<>();

        // 락 키 목록 생성
        List<String> lockKeys = productIds.stream()
                .map(productId -> "product_lock:" + productId)
                .collect(Collectors.toList());

        for (String lockKey : lockKeys) {
            RLock lock = redissonLockService.tryLock(lockKey, 3L, 10L);
            // tryLock 로직에서 Lock 획득 못하면 예외를 던지지만, 혹시나 null일 상황 대비 로직
            if(lock == null){
                log.error("락 획득 실패 key = {}", lockKey);
                throw new OrderException(ErrorCode.ORDER_LOCK_FAILED);
            }
            locks.add(lock);
        }
        return locks;
    }

    // Product 조회, 검증
    private Map<Long, ProductCommonDto> fetchAndValidateProducts(List<Long> productIds,
                                                                 List<OrderRequestDto.OrderProductRequest> productRequests,
                                                                 Long userId){
        // feignClient 로 Product 조회 => CircuitBreaker 적용 필요
        List<ProductCommonDto> productCommonDtos = productServiceClient.getProductsById(productIds);

        // map 으로 변환
        Map<Long, ProductCommonDto> productMap = productCommonDtos.stream().collect(
                Collectors.toMap(ProductCommonDto::getId, dto -> dto));

        for (OrderRequestDto.OrderProductRequest requestProduct : productRequests) {
            Long productId = requestProduct.getProductId();
            Integer requestQuantity = requestProduct.getQuantity();

            // Map 에 해당 상품이 없으면 조회 실패 (CircuitBreaker 빈 리스트 반환)
            ProductCommonDto productCommonDto = productMap.get(productId);
            if(productCommonDto == null){
                log.error("상품 정보가 없습니다. : userId={}, productId={}", userId, productId);
                throw new OrderException(ErrorCode.ORDER_PRODUCT_NOT_FOUND);
            }

            // 최소 주문 수량 확인
            if (requestQuantity < 1) {
                log.error("상품은 1개 이상 부터 주문 가능 합니다. : userId = {}, requestQuantity = {}", userId, requestQuantity);
                throw new OrderException(ErrorCode.ORDER_QUANTITY_INVALID);
            }

            // 재고 확인
            if (productCommonDto.getStock() < requestQuantity) {
                log.error("상품의 수량이 부족 합니다. : userId = {}, requestProductId = {}, requestQuantity = {}, productStock = {}",
                        userId, requestProduct.getProductId(), requestQuantity, productCommonDto.getStock());
                throw new OrderException(ErrorCode.ORDER_PRODUCT_NO_STOCK);
            }
        }
        return productMap;
    }

    // Order 생성, 저장
    private Order createAndSaveOrder(Long userId,
                                     OrderRequestDto requestDto,
                                     Map<Long, ProductCommonDto> productMap){
        // 주문 상품을 저장할 List
        List<OrderProduct> orderProducts = new ArrayList<>();
        // 재고 감소 요청에 사용할 List
        List<ProductStockDto> stockDecreaseDto = new ArrayList<>();

        for (OrderRequestDto.OrderProductRequest requestProduct : requestDto.getProducts()) {
            Long productId = requestProduct.getProductId();
            Integer requestQuantity = requestProduct.getQuantity();

            // 재고 감소 Dto 생성, List에 Add
            stockDecreaseDto.add(new ProductStockDto(productId, requestQuantity));

            // OrderProduct 생성, List에 Add
            ProductCommonDto productCommonDto = productMap.get(productId);
            orderProducts.add(OrderProduct.create(productId, productCommonDto.getTitle(), requestQuantity, productCommonDto.getPrice()));
        }

        // feignClient 로 재고 감소 호출 => CircuitBreaker 적용 필요
        if(!productServiceClient.decreaseStock(stockDecreaseDto)){
            log.error("재고 감소 호출 실패");
            throw new OrderException(ErrorCode.ORDER_PRODUCT_DECREASE_FAILED);
        }

        // Delivery 생성
        Address address = Address.create(requestDto.getCity(), requestDto.getStreet(), requestDto.getZipCode());
        Delivery delivery = Delivery.create(address);

        // Order 생성, 저장
        // cascade 로 인해 Delivery, OrderProduct 함께 저장 처리
        Order order = Order.create(userId, delivery, orderProducts);
        return orderRepository.save(order);
    }

    // 락 해제
    private void releaseLocks(List<RLock> locks) {
        for (RLock lock : locks) {
            redissonLockService.unLock(lock);
        }
    }

    // OrderResponseDto 응답 변환
    private OrderResponseDto convertToOrderResponse(Order order) {
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

    // Delivery Status 사용자 조회 시점에서 update
    private void updateDeliveryStatus() {
        // 스케쥴링과 별개로 사용자의 관점에서 배송 상태가 변경 되야 한다.
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        // bulk update
        deliveryRepository.updateOrderStatus(oneDayAgo, DeliveryStatus.DELIVERING, DeliveryStatus.PENDING);
        deliveryRepository.updateOrderStatus(twoDaysAgo, DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERING);
    }

    private Order getOrderWithOrderProductsAndDelivery(Long userId, Long orderId) {
        // Fetch Join 으로 orderProducts, delivery 조회
        Order order = orderRepository.findOrderByOrderIdAndUserIdWithOpAndD(orderId, userId);

        // 주문이 존재 하지 않으면
        if (order == null) {
            log.error("해당 주문은 존재 하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    // DeliveryStatus 가 Pending 일 때 주문 취소
    private List<ProductStockDto> cancelOrderIfPending(Long userId, Long orderId, Order order, List<OrderProduct> orderProducts) {
        // product stock 복구 해야 하는 List
        List<ProductStockDto> stockIncreaseDto = new ArrayList<>();
        // 배송 상태 확인 (PENDING 이면)
        if (order.getDelivery().getStatus() == DeliveryStatus.PENDING) {
            // order, delivery Status 를 Cancel 로 변경
            order.cancel();
            // stockIncreaseDto 에 Add
            for (OrderProduct orderProduct : orderProducts) {
                stockIncreaseDto.add(new ProductStockDto(orderProduct.getProductId(), orderProduct.getQuantity()));
            }
        }
        // (PENDING 아니면 취소 불가)
        else {
            log.error("주문 상태가 대기 일때만 주문 취소가 가능 합니다. : userId = {}, orderId = {}, currentDeliveryStatus = {}",
                    userId, orderId, order.getDelivery().getStatus());
            throw new OrderException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }
        return stockIncreaseDto;
    }

    // DeliveryStatus 가 Delivered, 배송 완료 하루 까지 환불
    private void returnOrderIfDeliveredAndOneDay(Long userId, Long orderId, Order order) {
        DeliveryStatus deliveryStatus = order.getDelivery().getStatus();
        LocalDateTime completedAt = order.getDelivery().getCompletedAt();
        // 배송 상태가 Delivered 이고, D + 1 이내 일 때
        if (deliveryStatus == DeliveryStatus.DELIVERED && completedAt.plusDays(1).isBefore(LocalDateTime.now())) {
            // order 상태 변경
            order.returnOrder();

        } else {
            log.error("반품은 배송 완료 상태에서 +1 일까지 가능합니다. userId = {}, orderId = {}, deliveryStatus = {}, completedAt = {}",
                    userId, orderId, deliveryStatus, completedAt);
            throw new OrderException(ErrorCode.ORDER_RETURN_NOT_ALLOWED);
        }
    }
}
