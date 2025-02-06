package com.hong.paymentservice.service;

import com.hong.common.dto.*;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.PaymentException;
import com.hong.paymentservice.client.hotDeal.Resilience4JHotDealServiceClient;
import com.hong.paymentservice.client.order.Resilience4JOrderServiceClient;
import com.hong.paymentservice.client.product.Resilience4JProductServiceClient;
import com.hong.paymentservice.domain.Payment;
import com.hong.paymentservice.dto.PaymentEntryResponseDto;
import com.hong.paymentservice.dto.PaymentProcessResponseDto;
import com.hong.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[PaymentServiceImpl]")
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final Resilience4JOrderServiceClient resilience4JOrderServiceClient;
    private final Resilience4JHotDealServiceClient resilience4JHotDealServiceClient;
    private final Resilience4JProductServiceClient resilience4JProductServiceClient;

    // 결제 진입
    @Transactional
    @Override
    public PaymentEntryResponseDto paymentEntry(Long userId, Long orderId) {
        // order FeignClient 조회
        OrderFetchResponseDto orderFetchResponseDto = fetchOrderAndValidate(userId, orderId);

        // hotDealProduct 재고 감소
        decreaseHotDealProductStockAndValidate(userId, orderId, orderFetchResponseDto);

        // product 재고 감소
        // 핫딜 상품에 대한 재고 감소는 성공 했지만, 일반 상품 재고 감소 호출이 실패 하면 성공한 핫딜 상품에 대한 재고 증가 처리
        try{
            decreaseProductStockAndValidate(userId, orderId, orderFetchResponseDto);
        }catch (PaymentException e){
            increaseHotDealProductStockAndValidate(userId, orderId, orderFetchResponseDto);
            throw e;
        }

        // payment 생성, 응답 Dto 반환
        Payment savedPayment = paymentRepository.save(Payment.create(orderId, orderFetchResponseDto.getAmount()));

        return convertToPaymentEntryResponseDto(savedPayment);
    }

    // 결제 수행
    @Override
    public PaymentProcessResponseDto paymentProcess(Long userId, Long paymentId) {
        return null;
    }



    // product 재고 감소
    private List<ProductStockUpdateResponseDto> decreaseProductStockAndValidate(Long userId,
                                                                                Long orderId,
                                                                                OrderFetchResponseDto orderFetchResponseDto) {
        // 요청 dto 에서 hotDealProduct 추출, feignClient 요청 dto 변환
        List<ProductStockUpdateRequestDto> productStockUpdateRequestDtos = converToProductStockUpdateRequestDto(orderFetchResponseDto);

        // productStockUpdateRequestDtos 가 empty 면 feignClient 호출 할 필요 없다.
        if(productStockUpdateRequestDtos.isEmpty()) return new ArrayList<>();

        // hotDealProduct 재고 감소 feignClient 호출
        List<ProductStockUpdateResponseDto> productStockUpdateResponseDtos = resilience4JProductServiceClient.decreaseStock(productStockUpdateRequestDtos);

        // CircuitBreaker OPEN
        if(productStockUpdateResponseDtos.isEmpty()){
            log.debug("상품 재고 감소 호출을 실패했습니다. userId = {}, orderId = {}, products = {}", userId, orderId, productStockUpdateRequestDtos);
            throw new PaymentException(ErrorCode.PAYMENT_DECREASE__PRODUCT_FAILED, userId, orderId, productStockUpdateRequestDtos);
        }
        return productStockUpdateResponseDtos;
    }


    // hotDealProduct 재고 감소
    private List<HotDealProductStockUpdateResponseDto> decreaseHotDealProductStockAndValidate(Long userId,
                                                        Long orderId,
                                                        OrderFetchResponseDto orderFetchResponseDto) {
        // 요청 dto 에서 product 추출, feignClient 요청 dto 변환
        List<HotDealProductStockUpdateRequestDto> hotDealProductStockUpdateRequestDtos = convertToHotDealProductUpdateRequestDto(orderFetchResponseDto);

        // hotDealProductStockUpdateRequestDtos 가 empty 면 feignClient 호출 할 필요 없다.
        if(hotDealProductStockUpdateRequestDtos.isEmpty()) return new ArrayList<>();

        // product 재고 감소 feignClient 호출
        List<HotDealProductStockUpdateResponseDto> hotDealProductStockUpdateResponseDtos = resilience4JHotDealServiceClient.decreaseStock(hotDealProductStockUpdateRequestDtos);

        // CircuitBreaker OPEN
        if(hotDealProductStockUpdateResponseDtos.isEmpty()){
            log.debug("핫딜 상품 재고 감소 호출을 실패했습니다. userId = {}, orderId = {}, hotDealProducts = {}", userId, orderId, hotDealProductStockUpdateRequestDtos);
            throw new PaymentException(ErrorCode.PAYMENT_DECREASE_HOTDEAL_PRODUCT_FAILED, userId, orderId, hotDealProductStockUpdateRequestDtos);
        }
        return hotDealProductStockUpdateResponseDtos;
    }

    // hotDealProduct 재고 증가
    private List<HotDealProductStockUpdateResponseDto> increaseHotDealProductStockAndValidate(Long userId,
                                                                                              Long orderId,
                                                                                              OrderFetchResponseDto orderFetchResponseDto) {
        // 요청 dto 에서 product 추출, feignClient 요청 dto 변환
        List<HotDealProductStockUpdateRequestDto> hotDealProductStockUpdateRequestDtos = convertToHotDealProductUpdateRequestDto(orderFetchResponseDto);

        // hotDealProductStockUpdateRequestDtos 가 empty 면 feignClient 호출 할 필요 없다.
        if(hotDealProductStockUpdateRequestDtos.isEmpty()) return new ArrayList<>();

        // product 재고 감소 feignClient 호출
        List<HotDealProductStockUpdateResponseDto> hotDealProductStockUpdateResponseDtos = resilience4JHotDealServiceClient.increaseStock(hotDealProductStockUpdateRequestDtos);

        // CircuitBreaker OPEN
        if(hotDealProductStockUpdateResponseDtos.isEmpty()){
            log.debug("핫딜 상품 재고 증가 호출을 실패했습니다. userId = {}, orderId = {}, hotDealProducts = {}", userId, orderId, hotDealProductStockUpdateRequestDtos);
            throw new PaymentException(ErrorCode.PAYMENT_INCREASE_HOTDEAL_PRODUCT_FAILED, userId, orderId, hotDealProductStockUpdateRequestDtos);
        }
        return hotDealProductStockUpdateResponseDtos;
    }

    // 요청 dto 에서 product 추출, feignClient 요청 dto 변환
    private List<HotDealProductStockUpdateRequestDto> convertToHotDealProductUpdateRequestDto(OrderFetchResponseDto orderFetchResponseDto) {
        List<HotDealProductStockUpdateRequestDto> hotDealProductStockUpdateRequestDtos =
                orderFetchResponseDto.getHotDealProducts()
                        .stream()
                        .map(hp -> new HotDealProductStockUpdateRequestDto(
                                hp.getHotDealId(),
                                hp.getHotDealProductId(),
                                hp.getQuantity()))
                        .collect(Collectors.toList());
        return hotDealProductStockUpdateRequestDtos;
    }

    // 요청 dto 에서 hotDealProduct 추출, feignClient 요청 dto 변환
    private List<ProductStockUpdateRequestDto> converToProductStockUpdateRequestDto(OrderFetchResponseDto orderFetchResponseDto) {
        List<ProductStockUpdateRequestDto> productStockUpdateRequestDtos =
                orderFetchResponseDto.getProducts()
                        .stream()
                        .map(p -> new ProductStockUpdateRequestDto(
                                p.getProductId(),
                                p.getQuantity()))
                        .collect(Collectors.toList());
        return productStockUpdateRequestDtos;
    }


    // order FeignClient 조회
    private OrderFetchResponseDto fetchOrderAndValidate(Long userId, Long orderId) {
        // order 조회 feignClient 호출
        OrderFetchResponseDto orderFetchResponseDto = resilience4JOrderServiceClient.fetchOrder(new OrderFetchRequestDto(userId, orderId));

        // CircuitBreaker OPEN
        if(orderFetchResponseDto.isEmpty()){
            log.debug("주문 조회 호출을 실패했습니다. userId = {}, orderId = {}", userId, orderId);
            throw new PaymentException(ErrorCode.PAYMENT_FETCH_ORDER_FAILED, userId, orderId);
        }
        return orderFetchResponseDto;
    }

    // paymentEntryResponse Dto 변환
    private PaymentEntryResponseDto convertToPaymentEntryResponseDto(Payment savedPayment) {
        return new PaymentEntryResponseDto(
                savedPayment.getId(),
                savedPayment.getOrderId(),
                savedPayment.getAmount(),
                savedPayment.getExpiresAt());
    }



}
