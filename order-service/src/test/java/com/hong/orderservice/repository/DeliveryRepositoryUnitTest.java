package com.hong.orderservice.repository;

import com.hong.orderservice.config.JpaConfig;
import com.hong.orderservice.domain.Delivery;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.OrderProduct;
import com.hong.orderservice.domain.base.Address;
import com.hong.orderservice.domain.status.DeliveryStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class DeliveryRepositoryUnitTest {

    @Autowired
    DeliveryRepository deliveryRepository;
    @Autowired
    OrderRepository orderRepository;

    private OrderProduct createTestHotDealProduct(Long hotDealProductId){
        return OrderProduct.createHotDealProduct(hotDealProductId, "hotDealProduct" + hotDealProductId, 1, 1000);
    }

    private OrderProduct createTestProduct(Long productId){
        return OrderProduct.createHotDealProduct(productId, "product" + productId, 1, 2000);
    }

    private Order createTestOrder(Long userId, List<OrderProduct> orderProducts){
        int totalAmount = orderProducts.stream()
                .mapToInt(op -> op.getPrice() * op.getQuantity())
                .sum();
        Delivery delivery = Delivery.create(Address.create("city", "street", "100-1"));
        Order order = Order.create(userId, delivery, orderProducts, totalAmount);
        orderRepository.save(order);
        return order;
    }

    @Test
    @DisplayName("결제 완료 후 1일 경과한 delivery status bulkUpdate")
    public void bulkUpdatePendingDeliveriesToDelivering(){
        //given
        Long userId = 1L;
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        // order1, 2 bulkUpdate 대상
        Order order1 = createTestOrder(userId, orderProducts);
        order1.paymentSuccess(now.minusDays(1));
        orderRepository.save(order1);
        Order order2 = createTestOrder(userId, orderProducts);
        order2.paymentSuccess(now.minusDays(2));
        orderRepository.save(order2);
        Order order3 = createTestOrder(userId, orderProducts);
        order3.paymentSuccess(now);
        orderRepository.save(order3);

        LocalDateTime oneDayAgo = now.minusDays(1);

        //when
        int result = deliveryRepository.bulkUpdatePendingDeliveriesToDelivering(oneDayAgo, now);

        //then
        assertThat(result).isEqualTo(2);
        // update 검증
        Delivery delivery1 = deliveryRepository.findById(order1.getId()).get();
        assertThat(delivery1.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERING);
        assertThat(delivery1.getStartedAt()).isEqualTo(now);
        Delivery delivery2 = deliveryRepository.findById(order1.getId()).get();
        assertThat(delivery2.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERING);
        assertThat(delivery2.getStartedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("배송 후 1일 경과한 delivery status bulkUpdate")
    public void bulkUpdateDeliveringDeliveriesToDelivered(){
        //given
        Long userId = 1L;
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        // order1, 2 bulkUpdate 대상
        Order order1 = createTestOrder(userId, orderProducts);
        order1.paymentSuccess(now.minusDays(2));
        order1.getDelivery().updateToDelivering(now.minusDays(1));
        orderRepository.save(order1);
        Order order2 = createTestOrder(userId, orderProducts);
        order2.paymentSuccess(now.minusDays(3));
        order2.getDelivery().updateToDelivering(now.minusDays(2));
        orderRepository.save(order2);
        Order order3 = createTestOrder(userId, orderProducts);
        order3.getDelivery().updateToDelivering(now);
        orderRepository.save(order3);

        LocalDateTime oneDayAgo = now.minusDays(1);

        //when
        int result = deliveryRepository.bulkUpdateDeliveringDeliveriesToDelivered(oneDayAgo, now);

        //then
        assertThat(result).isEqualTo(2);
        // update 검증
        Delivery delivery1 = deliveryRepository.findById(order1.getId()).get();
        assertThat(delivery1.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery1.getCompletedAt()).isEqualTo(now);
        Delivery delivery2 = deliveryRepository.findById(order1.getId()).get();
        assertThat(delivery2.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery2.getCompletedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("배송 후 1일 경과한 delivery status bulkUpdate")
    public void bulkUpdateDeliveryStatusReturned(){
        //given
        Long userId = 1L;
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        // order1, 2 bulkUpdate 대상
        Order order1 = createTestOrder(userId, orderProducts);
        order1.updateStatusReturnRequested(now.minusDays(1));
        orderRepository.save(order1);
        Order order2 = createTestOrder(userId, orderProducts);
        order2.updateStatusReturnRequested(now.minusDays(2));
        orderRepository.save(order2);
        Order order3 = createTestOrder(userId, orderProducts);
        order3.getDelivery().updateToDelivering(now);
        orderRepository.save(order3);

        LocalDateTime oneDayAgo = now.minusDays(1);

        //when
        int result = deliveryRepository.bulkUpdateDeliveryStatusReturned(oneDayAgo, now);

        //then
        assertThat(result).isEqualTo(2);
        // update 검증
        Delivery delivery1 = deliveryRepository.findById(order1.getId()).get();
        assertThat(delivery1.getDeliveryStatus()).isEqualTo(DeliveryStatus.RETURNED);
        assertThat(delivery1.getReturnCompletedAt()).isEqualTo(now);
        Delivery delivery2 = deliveryRepository.findById(order1.getId()).get();
        assertThat(delivery2.getDeliveryStatus()).isEqualTo(DeliveryStatus.RETURNED);
        assertThat(delivery2.getReturnCompletedAt()).isEqualTo(now);
    }
}