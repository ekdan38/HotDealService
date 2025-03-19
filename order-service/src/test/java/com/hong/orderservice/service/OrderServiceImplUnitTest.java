package com.hong.orderservice.service;

import com.hong.common.dto.*;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplUnitTest {

    @InjectMocks
    OrderServiceImpl orderService;
    @Mock
    OrderRepository orderRepository;
    @Mock
    Resilience4JHotDealServiceClient resilience4JHotDealServiceClient;
    @Mock
    Resilience4JProductServiceClient resilience4JProductServiceClient;

    private Long userId = 1L;

    private OrderProduct createTestHotDealProduct(Long orderProductId, Long hotDealProductId){
        OrderProduct orderProduct = OrderProduct.createHotDealProduct(hotDealProductId, "hotDealProduct" + hotDealProductId, 1, 1000);
        ReflectionTestUtils.setField(orderProduct, "id", orderProductId);
        return orderProduct;
    }

    private OrderProduct createTestProduct(Long orderProductId, Long productId){
        OrderProduct orderProduct = OrderProduct.createProduct(productId, "product" + productId, 1, 1000);
        ReflectionTestUtils.setField(orderProduct, "id", orderProductId);
        return orderProduct;
    }

    private Order createTestOrder(Long orderId, List<OrderProduct> orderProducts){
        int totalAmount = orderProducts.stream()
                .mapToInt(op -> op.getPrice() * op.getQuantity())
                .sum();
        Delivery delivery = Delivery.create(Address.create("city", "street", "100-1"));
        Order order = Order.create(userId, delivery, orderProducts, totalAmount);
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private List<Order> createTestOrdersForPaging(Long userId) {
        ArrayList<Order> orders = new ArrayList<>();
        for(long i = 1; i <= 5; i++){
            Delivery delivery = Delivery.create(Address.create("city", "street", "100-1"));
            Order order = Order.create(userId, delivery, List.of(), 10000);
            ReflectionTestUtils.setField(order, "id", i);
            orders.add(order);
        }
        return orders;
    }

    @Test
    @DisplayName("order 생성_성공")
    public void createOrder_success(){
        //given
        // request
        ArrayList<OrderProductRequest> orderProductRequests = new ArrayList<>();
        // productRequest
        OrderProductRequest orderProductRequest1 = new OrderProductRequest(1L, null, 1);
        orderProductRequests.add(orderProductRequest1);
        // hotDealProductRequest
        OrderProductRequest orderProductRequest2 = new OrderProductRequest(null, 1L, 1);
        orderProductRequests.add(orderProductRequest2);

        OrderRequestDto request = new OrderRequestDto("city", "street", "100-1", orderProductRequests);

        // 1. hotDealProducts 조회 및 재고 확인 feignClient Mock
        HotDealProductStockCheckRequestDto hotDealProductStockCheckRequestDto =
                new HotDealProductStockCheckRequestDto(orderProductRequest2.getHotDealProductId(), orderProductRequest2.getQuantity());

        HotDealProductStockCheckResponseDto hotDealProductStockCheckResponseDto =
                new HotDealProductStockCheckResponseDto(
                        hotDealProductStockCheckRequestDto.getHotDealProductId(),
                        "hotDealProduct1",
                        hotDealProductStockCheckRequestDto.getQuantity(),
                        1000);

        when(resilience4JHotDealServiceClient.fetchProductsAndValidateStock(List.of(hotDealProductStockCheckRequestDto)))
                .thenReturn(List.of(hotDealProductStockCheckResponseDto));

        // 2. product 조회 및 재고 확인 feignClient Mock
        ProductStockCheckRequestDto productStockCheckRequestDto =
                new ProductStockCheckRequestDto(orderProductRequest1.getProductId(), orderProductRequest1.getQuantity());

        ProductStockCheckResponseDto productStockCheckResponseDto = new ProductStockCheckResponseDto(
                orderProductRequest1.getProductId(),
                "product1",
                orderProductRequest1.getQuantity(),
                1000);

        when(resilience4JProductServiceClient.fetchProducts(List.of(productStockCheckRequestDto)))
                .thenReturn(List.of(productStockCheckResponseDto));

        // 3. order save Mock
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L, orderProductRequest1.getProductId());
        OrderProduct orderProduct2 = createTestHotDealProduct(2L, orderProductRequest2.getHotDealProductId());
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        //when
        OrderResponseDto result = orderService.createOrder(userId, request);

        //then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTotalPrice()).isEqualTo(2000);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(result.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);

        // orderProducts
        assertThat(result.getOrderProducts()).hasSize(2);

        List<OrderProductResponseDto> products = result.getOrderProducts()
                .stream()
                .filter(op -> op.getProductId() != null)
                .toList();
        assertThat(products.get(0).getProductId()).isEqualTo(1L);
        assertThat(products.get(0).getQuantity()).isEqualTo(1);
        assertThat(products.get(0).getPrice()).isEqualTo(1000);
        assertThat(products.get(0).getProductTitle()).isEqualTo("product1");

        List<OrderProductResponseDto> hotDealProducts = result.getOrderProducts()
                .stream()
                .filter(op -> op.getHotDealProductId() != null)
                .toList();
        assertThat(hotDealProducts.get(0).getHotDealProductId()).isEqualTo(1L);
        assertThat(hotDealProducts.get(0).getQuantity()).isEqualTo(1);
        assertThat(hotDealProducts.get(0).getPrice()).isEqualTo(1000);
        assertThat(hotDealProducts.get(0).getProductTitle()).isEqualTo("hotDealProduct1");
    }

    @Test
    @DisplayName("order 생성_실패_hotDealProduct 조회 및 재고 검증 요청 실패")
    public void createOrder_failure_failToFetchHotDealProducts(){
        //given
        // request
        ArrayList<OrderProductRequest> orderProductRequests = new ArrayList<>();
        // productRequest
        OrderProductRequest orderProductRequest1 = new OrderProductRequest(1L, null, 1);
        orderProductRequests.add(orderProductRequest1);
        // hotDealProductRequest
        OrderProductRequest orderProductRequest2 = new OrderProductRequest(null, 1L, 1);
        orderProductRequests.add(orderProductRequest2);

        OrderRequestDto request = new OrderRequestDto("city", "street", "100-1", orderProductRequests);

        // 1. hotDealProducts 조회 및 재고 확인 feignClient Mock
        HotDealProductStockCheckRequestDto hotDealProductStockCheckRequestDto =
                new HotDealProductStockCheckRequestDto(orderProductRequest2.getHotDealProductId(), orderProductRequest2.getQuantity());

        when(resilience4JHotDealServiceClient.fetchProductsAndValidateStock(List.of(hotDealProductStockCheckRequestDto)))
                .thenReturn(List.of());

        // when && then
        assertThatThrownBy(() -> orderService.createOrder(userId, request)).isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("order 생성_실패_product 조회 및 재고 검증 요청 실패")
    public void createOrder_failure_failToFetchProducts(){
        //given
        // request
        ArrayList<OrderProductRequest> orderProductRequests = new ArrayList<>();
        // productRequest
        OrderProductRequest orderProductRequest1 = new OrderProductRequest(1L, null, 1);
        orderProductRequests.add(orderProductRequest1);
        // hotDealProductRequest
        OrderProductRequest orderProductRequest2 = new OrderProductRequest(null, 1L, 1);
        orderProductRequests.add(orderProductRequest2);

        OrderRequestDto request = new OrderRequestDto("city", "street", "100-1", orderProductRequests);

        // 1. hotDealProducts 조회 및 재고 확인 feignClient Mock
        HotDealProductStockCheckRequestDto hotDealProductStockCheckRequestDto =
                new HotDealProductStockCheckRequestDto(orderProductRequest2.getHotDealProductId(), orderProductRequest2.getQuantity());

        HotDealProductStockCheckResponseDto hotDealProductStockCheckResponseDto =
                new HotDealProductStockCheckResponseDto(
                        hotDealProductStockCheckRequestDto.getHotDealProductId(),
                        "hotDealProduct1",
                        hotDealProductStockCheckRequestDto.getQuantity(),
                        1000);

        when(resilience4JHotDealServiceClient.fetchProductsAndValidateStock(List.of(hotDealProductStockCheckRequestDto)))
                .thenReturn(List.of(hotDealProductStockCheckResponseDto));

        // 2. product 조회 및 재고 확인 feignClient Mock
        ProductStockCheckRequestDto productStockCheckRequestDto =
                new ProductStockCheckRequestDto(orderProductRequest1.getProductId(), orderProductRequest1.getQuantity());

        when(resilience4JProductServiceClient.fetchProducts(List.of(productStockCheckRequestDto)))
                .thenReturn(List.of());

        // when && then
        assertThatThrownBy(() -> orderService.createOrder(userId, request)).isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("user 조회 시점에 결제 후 1일 경과한 delivery status DELIVERING 으로 변경")
    public void updateDeliveryAndOrderStatusForUser_toDELIVERING(){
        //given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        Order order = createTestOrder(1L, List.of());

        order.paymentSuccess(now.minusDays(1).minusMinutes(1));

        //when
        orderService.updateDeliveryAndOrderStatusForUser(userId, now, List.of(order));

        //then
        assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERING);
        assertThat(order.getDelivery().getStartedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("user 조회 시점에 배송 시작 후 1일 경과한 delivery status DELIVERED 으로 변경")
    public void updateDeliveryAndOrderStatusForUser_toDELIVERED(){
        //given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        Order order = createTestOrder(1L, List.of());

        order.getDelivery().updateToDelivering(now.minusDays(1).minusMinutes(1));

        //when
        orderService.updateDeliveryAndOrderStatusForUser(userId, now, List.of(order));

        //then
        assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(order.getDelivery().getCompletedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("user 조회 시점에 환불 처리 후 1일 경과한 delivery status RETURNED 으로 변경 및 재고 복구")
    public void updateDeliveryAndOrderStatusForUser_toRETURN_RETURNED(){
        //given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestHotDealProduct(1L, 1L);
        OrderProduct orderProduct2 = createTestProduct(2L, 1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);
        order.updateStatusReturnRequested(now.minusDays(1).minusMinutes(1));

        HotDealProductStockUpdateRequestDto hotDealProductIncreaseRequest =
                new HotDealProductStockUpdateRequestDto(orderProduct1.getHotDealProductId(), orderProduct1.getQuantity());
        HotDealProductStockUpdateResponseDto hotDealProductIncreaseResponse =
                new HotDealProductStockUpdateResponseDto(orderProduct1.getHotDealProductId(), orderProduct1.getProductTitle(), orderProduct1.getQuantity());
        when(resilience4JHotDealServiceClient.increaseStock(List.of(hotDealProductIncreaseRequest)))
                .thenReturn(List.of(hotDealProductIncreaseResponse));

        ProductStockUpdateRequestDto productIncreaseRequest =
                new ProductStockUpdateRequestDto(orderProduct2.getProductId(), orderProduct2.getQuantity());
        ProductStockUpdateResponseDto productIncreaseResponse =
                new ProductStockUpdateResponseDto(orderProduct2.getProductId(), orderProduct2.getProductTitle(),
                        orderProduct2.getPrice(), orderProduct2.getQuantity());
        when(resilience4JProductServiceClient.increaseStock(List.of(productIncreaseRequest)))
                .thenReturn(List.of(productIncreaseResponse));

        //when
        orderService.updateDeliveryAndOrderStatusForUser(userId, now, List.of(order));

        //then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURNED);
        assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.RETURNED);
        assertThat(order.getDelivery().getReturnCompletedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("order 커서 기반 페이징 조회")
    public void getOrders(){
        //given
        List<Order> orders = createTestOrdersForPaging(userId);
        Long cursor = 10L;
        int size = 3;

        List<Order> expectPagingResult = orders
                .stream()
                .skip(orders.size() - size)
                .toList()
                .reversed();
        when(orderRepository.findOrdersByCursorAndUserIdAndSize(eq(cursor), eq(userId), any())).thenReturn(expectPagingResult);

        //when
        OrderPagingResponseDto result = orderService.getOrders(userId, cursor, size);

        //then
        assertThat(result.getCursor()).isEqualTo(3L);
        assertThat(result.getOrders()).hasSize(size);
        result.getOrders().forEach(o -> {
            assertThat(o.getOrderId()).isNotNull();
            assertThat(o.getUserId()).isEqualTo(userId);
            assertThat(o.getTotalPrice()).isEqualTo(10000);
        });
    }

    @Test
    @DisplayName("order 단건 조회_성공")
    public void getOrder_success(){
        //given
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L, 1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(2L, 1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);

        when(orderRepository.findOrderWithDeliveryAndOpById(order.getId(), userId)).thenReturn(Optional.of(order));

        //when
        OrderResponseDto result = orderService.getOrder(userId, order.getId());

        //then
        assertThat(result.getOrderId()).isEqualTo(order.getId());
        assertThat(result.getUserId()).isEqualTo(order.getUserId());
        assertThat(result.getTotalPrice()).isEqualTo(order.getAmount());
        assertThat(result.getOrderStatus()).isEqualTo(order.getStatus());
        assertThat(result.getDeliveryStatus()).isEqualTo(order.getDelivery().getDeliveryStatus());

        assertThat(result.getOrderProducts()).hasSize(2);
        assertThat(result.getOrderProducts().get(0).getProductTitle()).isEqualTo(orderProduct1.getProductTitle());
        assertThat(result.getOrderProducts().get(0).getQuantity()).isEqualTo(orderProduct1.getQuantity());
        assertThat(result.getOrderProducts().get(0).getPrice()).isEqualTo(orderProduct1.getPrice());
        assertThat(result.getOrderProducts().get(1).getProductTitle()).isEqualTo(orderProduct2.getProductTitle());
        assertThat(result.getOrderProducts().get(1).getQuantity()).isEqualTo(orderProduct2.getQuantity());
        assertThat(result.getOrderProducts().get(1).getPrice()).isEqualTo(orderProduct2.getPrice());
    }

    @Test
    @DisplayName("order 단건 조회_실패_존재 하지 않는 order")
    public void getOrder_failure_notFoundOrder(){
        //given
        Long orderId = 1L;

        when(orderRepository.findOrderWithDeliveryAndOpById(orderId, userId)).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> orderService.getOrder(userId, orderId)).isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("order 취소_성공")
    public void cancelOrder_success(){
        //given
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L, 1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(2L, 1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        order.paymentSuccess(now);

        when(orderRepository.findPaidOrderByOrderIdAndUserIdWithOpAndD(order.getId(), userId)).thenReturn(Optional.of(order));

        // order 취소 하면서 재고 복구 Mock
        // hotDealProduct
        HotDealProductStockUpdateRequestDto hotDealProductIncreaseRequest =
                new HotDealProductStockUpdateRequestDto(orderProduct2.getHotDealProductId(), orderProduct2.getQuantity());
        HotDealProductStockUpdateResponseDto hotDealProductIncreaseResponse =
                new HotDealProductStockUpdateResponseDto(orderProduct2.getHotDealProductId(), orderProduct2.getProductTitle(), orderProduct2.getQuantity());
        when(resilience4JHotDealServiceClient.increaseStock(List.of(hotDealProductIncreaseRequest)))
                .thenReturn(List.of(hotDealProductIncreaseResponse));

        // product
        ProductStockUpdateRequestDto productIncreaseRequest =
                new ProductStockUpdateRequestDto(orderProduct1.getProductId(), orderProduct1.getQuantity());
        ProductStockUpdateResponseDto productIncreaseResponse =
                new ProductStockUpdateResponseDto(orderProduct1.getProductId(), orderProduct1.getProductTitle(), orderProduct1.getPrice(), orderProduct1.getQuantity());
        when(resilience4JProductServiceClient.increaseStock(List.of(productIncreaseRequest)))
                .thenReturn(List.of(productIncreaseResponse));

        //when
        OrderResponseDto result = orderService.cancelOrder(userId, order.getId());

        //then
        assertThat(result.getOrderId()).isEqualTo(order.getId());
        assertThat(result.getUserId()).isEqualTo(order.getUserId());
        assertThat(result.getTotalPrice()).isEqualTo(order.getAmount());
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCEL);
        assertThat(result.getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCEL);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCEL);
        assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCEL);
    }

    @Test
    @DisplayName("order 취소_실패_존재 하지 않는 order")
    public void cancelOrder_failure_notFoundOrder(){
        //given
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L, 1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(2L, 1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        order.paymentSuccess(now);

        when(orderRepository.findPaidOrderByOrderIdAndUserIdWithOpAndD(order.getId(), userId)).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> orderService.cancelOrder(userId, order.getId())).isInstanceOf(OrderException.class);
    }


    @Test
    @DisplayName("order 취소_실패_주문 가능 기간 지남")
    public void cancelOrder_failure_afterOneDay(){
        //given
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L, 1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(2L, 1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        order.paymentSuccess(now);
        order.getDelivery().updateToDelivering(now);

        when(orderRepository.findPaidOrderByOrderIdAndUserIdWithOpAndD(order.getId(), userId)).thenReturn(Optional.of(order));

        //when && then
        assertThatThrownBy(() -> orderService.cancelOrder(userId, order.getId())).isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("order 취소_hotDealProduct 재고 복구 요청 실패")
    public void cancelOrder_failure_failToIncreaseHotDealProduct(){
        //given
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L, 1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(2L, 1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        order.paymentSuccess(now);

        when(orderRepository.findPaidOrderByOrderIdAndUserIdWithOpAndD(order.getId(), userId)).thenReturn(Optional.of(order));

        // order 취소 하면서 재고 복구 Mock
        // hotDealProduct
        HotDealProductStockUpdateRequestDto hotDealProductIncreaseRequest =
                new HotDealProductStockUpdateRequestDto(orderProduct2.getHotDealProductId(), orderProduct2.getQuantity());
        when(resilience4JHotDealServiceClient.increaseStock(List.of(hotDealProductIncreaseRequest)))
                .thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> orderService.cancelOrder(userId, order.getId())).isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("order 취소_실패_product 재고 복구 요청 실패")
    public void cancelOrder_failure_failToIncreaseProduct(){
        //given
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L, 1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(2L, 1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        order.paymentSuccess(now);

        when(orderRepository.findPaidOrderByOrderIdAndUserIdWithOpAndD(order.getId(), userId)).thenReturn(Optional.of(order));

        // order 취소 하면서 재고 복구 Mock
        // hotDealProduct
        HotDealProductStockUpdateRequestDto hotDealProductIncreaseRequest =
                new HotDealProductStockUpdateRequestDto(orderProduct2.getHotDealProductId(), orderProduct2.getQuantity());
        HotDealProductStockUpdateResponseDto hotDealProductIncreaseResponse =
                new HotDealProductStockUpdateResponseDto(orderProduct2.getHotDealProductId(), orderProduct2.getProductTitle(), orderProduct2.getQuantity());
        when(resilience4JHotDealServiceClient.increaseStock(List.of(hotDealProductIncreaseRequest)))
                .thenReturn(List.of(hotDealProductIncreaseResponse));

        // product
        ProductStockUpdateRequestDto productIncreaseRequest =
                new ProductStockUpdateRequestDto(orderProduct1.getProductId(), orderProduct1.getQuantity());
        when(resilience4JProductServiceClient.increaseStock(List.of(productIncreaseRequest)))
                .thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> orderService.cancelOrder(userId, order.getId())).isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("order 환불_성공")
    public void returnOrder_success(){
        //given
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L, 1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(2L, 1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2).plusDays(1).truncatedTo(ChronoUnit.MILLIS);
        order.paymentSuccess(twoDaysAgo);
        order.getDelivery().updateToDelivered(twoDaysAgo.plusDays(1));

        when(orderRepository.findOrderWithDeliveryAndOpById(userId, order.getId())).thenReturn(Optional.of(order));

        //when
        OrderResponseDto result = orderService.returnOrder(userId, order.getId());

        //then
        assertThat(result.getOrderId()).isEqualTo(order.getId());
        assertThat(result.getUserId()).isEqualTo(order.getId());
        assertThat(result.getTotalPrice()).isEqualTo(order.getAmount());
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
        assertThat(result.getDeliveryStatus()).isEqualTo(DeliveryStatus.RETURN_REQUESTED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
        assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.RETURN_REQUESTED);
        assertThat(order.getDelivery().getReturnStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("order 환불_실패_존재 하지 않는 order")
    public void returnOrder_failure_notFoundOrder(){
        //given
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L, 1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(2L, 1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2).minusMinutes(1).truncatedTo(ChronoUnit.MILLIS);
        order.paymentSuccess(twoDaysAgo);
        order.getDelivery().updateToDelivered(twoDaysAgo.plusDays(1));

        when(orderRepository.findOrderWithDeliveryAndOpById(userId, order.getId())).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> orderService.returnOrder(userId, order.getId())).isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("order 환불_실패_환불 기간 지남")
    public void returnOrder_failure_afterDay(){
        //given
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L, 1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(2L, 1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(1L, orderProducts);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(3).minusMinutes(1).truncatedTo(ChronoUnit.MILLIS);
        order.paymentSuccess(twoDaysAgo);
        order.getDelivery().updateToDelivered(twoDaysAgo.plusDays(2));

        when(orderRepository.findOrderWithDeliveryAndOpById(userId, order.getId())).thenReturn(Optional.of(order));

        //when && then
        assertThatThrownBy(() -> orderService.returnOrder(userId, order.getId())).isInstanceOf(OrderException.class);
    }

}