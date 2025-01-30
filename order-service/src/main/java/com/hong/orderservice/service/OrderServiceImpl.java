package com.hong.orderservice.service;

import com.hong.common.dto.HotDealProductCommonDto;
import com.hong.common.dto.ProductCommonDto;
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
        // 핫딜 상품 재고 감소
        List<HotDealProductCommonDto> hotDealProductResponseDtos = hotDealProductFetchAndDecreaseStock(userId, requestDto.getProducts());

        // 상품 재고 감소(상품 재고 감소 실패시 hotDealProduct 재고 증가 처리)
        List<ProductCommonDto> productResponseDtos = productFetchAndDecreaseStock(userId, requestDto.getProducts(), hotDealProductResponseDtos);

        // 주문 생성
        Order savedOrder = createOrder(userId, requestDto, hotDealProductResponseDtos, productResponseDtos);

        return convertToOrderResponse(savedOrder);
    }

    // 주문 내역 페이징
    @Override
    public OrderPagingResponseDto getOrders(Long userId, Long cursor, int size) {
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
    @Override
    public OrderResponseDto getOrder(Long userId, Long orderId) {
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

        // 주문 상품 중 핫딜 상품 추출
        List<OrderRequestDto.OrderProductRequest> hotDealProducts = extractHotDealProductsFromOrder(order);

        // 핫딜 상품에 대한 재고 증가
        hotDealProductFetchAndIncreaseStock(userId, hotDealProducts);

        // 주문 상품 중 일반 상품 추출
        List<OrderRequestDto.OrderProductRequest> products = extractProductsFromOrder(order);

        // 상품 재고 증가(상품 재고 증가 실패시 hotDealProduct 재고 증가 처리)
        productFetchAndIncreaseStock(userId, products, null);

        // 주문, 배송 상태 변경 (Cancel)
        order.updateStatusToCancel();

        return convertToOrderResponse(order);
    }

    // 반품
    @Override
    public OrderResponseDto returnOrder(Long userId, Long orderId) {
        // order 조회, 환불 가능 검증
        Order order = fetchOrderAndValidateReturn(userId, orderId);
        // 주문 상품 중 핫딜 상품 추출
        List<OrderRequestDto.OrderProductRequest> hotDealProducts = extractHotDealProductsFromOrder(order);

        // 핫딜 상품에 대한 재고 증가
        hotDealProductFetchAndIncreaseStock(userId, hotDealProducts);

        // 주문 상품 중 일반 상품 추출
        List<OrderRequestDto.OrderProductRequest> products = extractProductsFromOrder(order);

        // 상품 재고 증가(상품 재고 증가 실패시 hotDealProduct 재고 증가 처리)
        productFetchAndIncreaseStock(userId, products, null);

        // 주문, 배송 상태 변경 (ReturnRequested)
        order.updateStatusReturnRequested();

        return convertToOrderResponse(order);
    }

    // order 조회, 환불 가능 검증
    private Order fetchOrderAndValidateReturn(Long userId, Long orderId) {
        Order order = getOrderWithOrderProductsAndDelivery(userId, orderId);

        Delivery delivery = order.getDelivery();
        DeliveryStatus deliveryStatus = delivery.getStatus();
        LocalDateTime completedAt = delivery.getCompletedAt();
        LocalDateTime now = LocalDateTime.now();

        // 배송 완료 후 1일 이내 까지 반품 가능
        if(!(deliveryStatus.equals(DeliveryStatus.DELIVERED) && completedAt != null && now.isBefore(completedAt.plusDays(1)))){
            log.debug("환불은 배송 완료 후 하루 이내에 가능합니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_RETURN_EXPIRED, userId, orderId);
        }
        return order;
    }


    // Delivery Status 사용자 조회 시점에서 update
    private void updateDeliveryStatus() {
        // 스케쥴링과 별개로 사용자의 관점에서 배송 상태가 변경 되야 한다.
        LocalDateTime now = LocalDateTime.now();
        // 주문 후 1일 경과한 배송 DELIVERING 로 상태 변경
        deliveryRepository.updatePendingDeliveriesToDelivering(
                now.minusDays(1),
                DeliveryStatus.DELIVERING,
                LocalDateTime.now(),
                DeliveryStatus.PENDING
        );
        log.info("Scheduler DELIVERING 로 상태 변경");

        // 주문 후 2일 경과한 배송 DELIVERED 로 상태 변경
        deliveryRepository.updatePendingDeliveriesToDelivering(
                now.minusDays(2),
                DeliveryStatus.DELIVERING,
                LocalDateTime.now(),
                DeliveryStatus.DELIVERED
        );
        log.info("Scheduler DELIVERED 로 상태 변경");
    }

    // Fetch Join 으로 order, orderProducts, delivery 조회
    private Order getOrderWithOrderProductsAndDelivery(Long userId, Long orderId) {
        // Fetch Join 으로 orderProducts, delivery 조회
        Order order = orderRepository.findOrderByOrderIdAndUserIdWithOpAndD(orderId, userId);
        // 주문이 존재 하지 않으면
        if (order == null) {
            log.debug("요청된 주문이 존재하지 않습니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND, userId, orderId);
        }
        return order;
    }

    // 주문 상품 중 일반 상품 추출
    private List<OrderRequestDto.OrderProductRequest> extractProductsFromOrder(Order order) {
        List<OrderRequestDto.OrderProductRequest> products = order.getOrderProducts().stream()
                .filter(op -> op.getHotDealProductId() == null)
                .map(op -> new OrderRequestDto.OrderProductRequest(
                        op.getProductId(),
                        op.getQuantity()))
                .collect(Collectors.toList());
        return products;
    }

    // 주문 상품 중 핫딜 상품 추출
    private List<OrderRequestDto.OrderProductRequest> extractHotDealProductsFromOrder(Order order) {
        return order.getOrderProducts().stream()
                .filter(op -> op.getHotDealProductId() != null)
                .map(op -> new OrderRequestDto.OrderProductRequest(
                        op.getProductId(),
                        op.getHotDealId(),
                        op.getHotDealProductId(),
                        op.getQuantity()))
                .collect(Collectors.toList());
    }

    // order 조회, 취소 가능 검증
    private Order fetchOrderAndValidateCancel(Long userId, Long orderId) {
        // Order 조회
        Order order = getOrderWithOrderProductsAndDelivery(userId, orderId);

        // 주문 취소 가능 한지 확인 (주문 후 1일 이내 가능 => 배송 상태 PENDING 일때 가능)
        //todo 주문 완료 후, 시간 체크 로직 추가 작성
        DeliveryStatus deliveryStatus = order.getDelivery().getStatus();
        if(!deliveryStatus.equals(DeliveryStatus.PENDING)){
            log.debug("주문 취소는 주문 후 하루 이내 가능합니다. userId = {}, orderId = {}", userId, orderId);
            throw new OrderException(ErrorCode.ORDER_CANCEL_EXPIRED, userId, orderId);
        }
        return order;
    }

    // 주문 생성
    private Order createOrder(Long userId, OrderRequestDto requestDto, List<HotDealProductCommonDto> hotDealProductResponseDtos, List<ProductCommonDto> productResponseDtos) {
        List<OrderProduct> orderProducts = new ArrayList<>();
        // OrderProduct 생성
        orderProducts.addAll(hotDealProductResponseDtos.stream()
                .map(product -> OrderProduct.create(
                        product.getProductId(),
                        product.getHotDealId(),
                        product.getHotDealProductId(),
                        product.getProductTitle(),
                        product.getQuantity(),
                        product.getHotDealPrice()))
                .collect(Collectors.toList()));

        orderProducts.addAll(productResponseDtos.stream()
                .filter(product -> !product.getIsHotDealProduct())
                .map(product -> OrderProduct.create(
                        product.getId(),
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

    // 상품 재고 감소(상품 재고 감소 실패시 hotDealProduct 재고 증가 처리)
    private List<ProductCommonDto> productFetchAndDecreaseStock(Long userId, List<OrderRequestDto.OrderProductRequest> products,
                                                                List<HotDealProductCommonDto> hotDealProductResponseDtos) {
        // 요청에서 products 추출
        List<ProductCommonDto> productCommonDtos = extractProductsFromOrder(products, hotDealProductResponseDtos);
        // normalProduct 재고 감소 호출(product 검증, 요청 검사, 재고 감소)
        List<ProductCommonDto> productResponseDtos = resilience4JProductServiceClient.fetchAndDecreaseStock(productCommonDtos);
        if(productResponseDtos.isEmpty()){
            log.debug("상품 재고 감소 호출을 실패했습니다. userId = {}, products = {}", userId, productCommonDtos);
            // 상품에 대한 재고 감소 실패시 hotDealProduct 재고 증가 처리
            hotDealProductFetchAndIncreaseStock(userId,products);
            throw new OrderException(ErrorCode.ORDER_DECREASE_PRODUCT_FAILED, userId, productCommonDtos);
        }
        return productResponseDtos;
    }

    // 상품 재고 증가
    private List<ProductCommonDto> productFetchAndIncreaseStock(Long userId, List<OrderRequestDto.OrderProductRequest> products,
                                                                List<HotDealProductCommonDto> hotDealProductResponseDtos) {
        // 요청에서 products 추출
        List<ProductCommonDto> productCommonDtos = extractProductsFromOrder(products, hotDealProductResponseDtos);
        // normalProduct 재고 증가 호출(product 검증, 요청 검사, 재고 증가)
        List<ProductCommonDto> productResponseDtos = resilience4JProductServiceClient.fetchAndIncreaseStock(productCommonDtos);
        if(productResponseDtos.isEmpty()){
            log.debug("상품 재고 증가 호출을 실패했습니다. userId = {}, products = {}", productCommonDtos);
            // 상품에 대한 재고 감소 실패시 hotDealProduct 재고 감소 처리
            hotDealProductFetchAndDecreaseStock(userId, products);
            throw new OrderException(ErrorCode.ORDER_INCREASE_PRODUCT_FAILED, userId, productCommonDtos);
        }
        return productResponseDtos;
    }


    // 핫딜 상품 재고 감소
    private List<HotDealProductCommonDto> hotDealProductFetchAndDecreaseStock(Long userId, List<OrderRequestDto.OrderProductRequest> products) {
        // 요청에서 hotDealProducts 추출
        List<HotDealProductCommonDto> hotDealProductCommonDtos = extractHotDealProductsFromOrder(products);
        // hotDealProduct 없으면 empty List 반환
        if(hotDealProductCommonDtos.isEmpty()) return new ArrayList<>();

        // hotDealProducts 재고 감소 호출(hotDeal 검증, 요청 검사, 재고 감소)
        List<HotDealProductCommonDto> hotDealProductResponseDtos = resilience4JHotDealServiceClient.fetchAndDecreaseStock(hotDealProductCommonDtos);
        if(hotDealProductResponseDtos.isEmpty()){
            log.debug("핫딜 상품 재고 감소 호출을 실패했습니다. userId = {}, hotDealProducts = {}", userId, hotDealProductCommonDtos);
            throw new OrderException(ErrorCode.ORDER_DECREASE_HOTDEAL_PRODUCT_FAILED, userId, hotDealProductCommonDtos);
        }
        return hotDealProductResponseDtos;
    }

    // 핫딜 상품 재고 증가
    private List<HotDealProductCommonDto> hotDealProductFetchAndIncreaseStock(Long userId, List<OrderRequestDto.OrderProductRequest> products) {
        // 요청에서 hotDealProducts 추출
        List<HotDealProductCommonDto> hotDealProductCommonDtos = extractHotDealProductsFromOrder(products);
        // hotDealProduct 없으면 empty List 반환
        if(hotDealProductCommonDtos.isEmpty()) return new ArrayList<>();

        // hotDealProducts 재고 증가 호출(hotDeal 검증, 요청 검사, 재고 증가)
        List<HotDealProductCommonDto> hotDealProductResponseDtos = resilience4JHotDealServiceClient.fetchAndIncreaseStock(hotDealProductCommonDtos);
        if(hotDealProductResponseDtos.isEmpty()){
            log.debug("핫딜 상품 재고 증가 호출을 실패했습니다. userId = {}, hotDealProducts = {}", userId, hotDealProductCommonDtos);
            throw new OrderException(ErrorCode.ORDER_INCREASE_HOTDEAL_PRODUCT_FAILED, userId, hotDealProductCommonDtos);
        }
        return hotDealProductResponseDtos;
    }

    // products 추출
    private List<ProductCommonDto> extractProductsFromOrder(List<OrderRequestDto.OrderProductRequest> products,
                                                            List<HotDealProductCommonDto> hotDealProductResponseDtos){
        List<ProductCommonDto> productCommonDtos = new ArrayList<>();

        productCommonDtos.addAll(hotDealProductResponseDtos.stream()
                .map(hp -> new ProductCommonDto(
                        hp.getProductId(),
                        hp.getQuantity(),
                        true
                )).collect(Collectors.toList()));

        productCommonDtos.addAll(products.stream()
                .filter(request -> request.getHotDealId() == null)
                .map(request -> new ProductCommonDto(
                        request.getProductId(),
                        request.getQuantity(),
                        false
                )).collect(Collectors.toList()));
        return productCommonDtos;
    }
    // hotDealProducts 추출
    private List<HotDealProductCommonDto> extractHotDealProductsFromOrder(List<OrderRequestDto.OrderProductRequest> products) {
        return products.stream()
                .filter(request -> request.getHotDealId() != null)
                .map(request -> new HotDealProductCommonDto(
                        request.getHotDealId(),
                        request.getHotDealProductId(),
                        request.getQuantity()))
                .collect(Collectors.toList());
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
                        op.getHotDealId(),
                        op.getProductId(),
                        op.getHotDealProductId(),
                        op.getProductTitle(),
                        op.getQuantity(),
                        op.getPrice()
                )).collect(Collectors.toList())
        );
    }
}
