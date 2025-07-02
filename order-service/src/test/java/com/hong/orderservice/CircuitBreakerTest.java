package com.hong.orderservice;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.OrderException;
import com.hong.orderservice.client.hotdeal.HotDealServiceClient;
import com.hong.orderservice.service.OrderService;
import com.hong.orderservice.web.dto.OrderProductRequest;
import com.hong.orderservice.web.dto.OrderRequestDto;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
public class CircuitBreakerTest {

    @Autowired
    OrderService orderService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    HotDealServiceClient hotDealServiceClient;

    private Long userId = 1L;

    // CircuitBreaker 각 테스트 케이스 이전에 초기화
    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("custom").reset();
    }

    // Retry, CircuitBreaker 무시, OrderException 생겨야함, 최종 결과 CLOSED
    @Test
    public void Ignore(){
        //given
        OrderRequestDto orderRequestDto = generateOrderRequest();

        Mockito.when(hotDealServiceClient.reserveStock(any()))
                .thenThrow(new OrderException(ErrorCode.ORDER_HOTDEAL_SERVICE_FAILED, "요청 수량보다 재고가 부족합니다. productIds = [1]"));

        //when, then
        for(int i = 1; i <= 11; i++){
            Assertions.assertThatThrownBy(() -> orderService.createOrder(userId, orderRequestDto)).isInstanceOf(OrderException.class);
        }
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("custom");
        Assertions.assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }


    // Retry, CircuitBreaker 대상, 최종 결과 OPEN
    @Test
    public void OPEN(){
        //given
        OrderRequestDto orderRequestDto = generateOrderRequest();

        Response response = generateResponse();
        FeignException feignException = FeignException.errorStatus("HotDealServiceClient#reserveStock", response);
        Mockito.when(hotDealServiceClient.reserveStock(any())).thenThrow(feignException);

        //when, then
        for(int i = 1; i <= 11; i++){
            Assertions.assertThatThrownBy(() -> orderService.createOrder(userId, orderRequestDto));
        }
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("custom");
        Assertions.assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    private OrderRequestDto generateOrderRequest() {
        return new OrderRequestDto("city", "street", "zipCode", List.of(new OrderProductRequest(1L, 1)));
    }

    private Response generateResponse() {
        return Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(Request.create(Request.HttpMethod.POST, "/hotdeal/reserve", Map.of(), null, new RequestTemplate()))
                .headers(Map.of())
                .body("Service Down", StandardCharsets.UTF_8)
                .build();
    }
}
