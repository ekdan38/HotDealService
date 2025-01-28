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
import com.hong.orderservice.dto.OrderPagingResponseDto;
import com.hong.orderservice.dto.OrderResponseDto;
import com.hong.orderservice.repository.OrderRepository;
import com.hong.orderservice.web.dto.OrderRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        // 핫딜 상품 재고 감소
        List<HotDealProductCommonDto> hotDealProductResponseDtos = hotDealProductFetchAndDecreaseStock(requestDto);

        // 상품 재고 감소
        List<ProductCommonDto> productResponseDtos = productFetchAndDecreaseStock(requestDto, hotDealProductResponseDtos);

        // 주문 생성
        Order savedOrder = createOrder(userId, requestDto, hotDealProductResponseDtos, productResponseDtos);

        return convertToOrderResponse(savedOrder);
    }

    @Override
    public OrderPagingResponseDto getOrders(Long userId, Long cursor, int size) {
        return null;
    }

    @Override
    public OrderResponseDto getOrder(Long userId, Long orderId) {
        return null;
    }

    @Override
    public OrderResponseDto cancelOrder(Long userId, Long orderId) {
        return null;
    }

    @Override
    public OrderResponseDto returnOrder(Long userId, Long orderId) {
        return null;
    }

    // 주문 생성
    private Order createOrder(Long userId, OrderRequestDto requestDto, List<HotDealProductCommonDto> hotDealProductResponseDtos, List<ProductCommonDto> productResponseDtos) {
        List<OrderProduct> orderProducts = new ArrayList<>();
        // OrderProduct 생성
        orderProducts.addAll(hotDealProductResponseDtos.stream()
                .map(product -> OrderProduct.create(
                        product.getProductId(),
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

    // 상품 재고 감소
    private List<ProductCommonDto> productFetchAndDecreaseStock(OrderRequestDto requestDto, List<HotDealProductCommonDto> hotDealProductResponseDtos) {
        // 요청에서 products 추출
        List<ProductCommonDto> productCommonDtos = extractProducts(requestDto, hotDealProductResponseDtos);
        // normalProduct 재고 감소 호출(product 검증, 요청 검사, 재고 감소)
        List<ProductCommonDto> productResponseDtos = resilience4JProductServiceClient.fetchAndDecreaseStock(productCommonDtos);
        if(productResponseDtos.isEmpty()){
            log.debug("상품 재고 감소 호출을 실패했습니다. products = {}", productCommonDtos);

            throw new OrderException(ErrorCode.ORDER_DECREASE_PRODUCT_FAILED, productCommonDtos);
        }
        return productResponseDtos;
    }

    // 핫딜 상품 재고 감소
    private List<HotDealProductCommonDto> hotDealProductFetchAndDecreaseStock(OrderRequestDto requestDto) {
        // 요청에서 hotDealProducts 추출
        List<HotDealProductCommonDto> hotDealProductCommonDtos = extractHotDealProducts(requestDto);
        // hotDealProducts 재고 감소 호출(hotDeal 검증, 요청 검사, 재고 감소)
        List<HotDealProductCommonDto> hotDealProductResponseDtos = resilience4JHotDealServiceClient.fetchAndDecreaseStock(hotDealProductCommonDtos);
        if(hotDealProductResponseDtos.isEmpty()){
            log.debug("핫딜 상품 재고 감소 호출을 실패했습니다. = hotDealProducts = {}", hotDealProductCommonDtos);
            throw new OrderException(ErrorCode.ORDER_DECREASE_HOTDEAL_PRODUCT_FAILED, hotDealProductCommonDtos);
        }
        return hotDealProductResponseDtos;
    }

    // products 추출
    private List<ProductCommonDto> extractProducts(OrderRequestDto requestDto,
                                                   List<HotDealProductCommonDto> hotDealProductResponseDtos){
        List<ProductCommonDto> productCommonDtos = new ArrayList<>();

        productCommonDtos.addAll(hotDealProductResponseDtos.stream()
                .map(hp -> new ProductCommonDto(
                        hp.getProductId(),
                        hp.getQuantity(),
                        true
                )).collect(Collectors.toList()));

        productCommonDtos.addAll(requestDto.getProducts().stream()
                .filter(request -> request.getHotDealId() == null)
                .map(request -> new ProductCommonDto(
                        request.getProductId(),
                        request.getQuantity(),
                        false
                )).collect(Collectors.toList()));
        return productCommonDtos;
    }
    // hotDealProducts 추출
    private List<HotDealProductCommonDto> extractHotDealProducts(OrderRequestDto requestDto) {
        return requestDto.getProducts().stream()
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
                        op.getProductId(),
                        op.getHotDealProductId(),
                        op.getProductTitle(),
                        op.getQuantity(),
                        op.getPrice()
                )).collect(Collectors.toList())
        );
    }
}
