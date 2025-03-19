package com.hong.orderservice.repository;

import com.hong.orderservice.config.JpaConfig;
import com.hong.orderservice.domain.Delivery;
import com.hong.orderservice.domain.Order;
import com.hong.orderservice.domain.OrderProduct;
import com.hong.orderservice.domain.base.Address;
import com.hong.orderservice.domain.status.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class OrderRepositoryUnitTest {

    @Autowired
    OrderRepository orderRepository;

    private OrderProduct createTestHotDealProduct(Long hotDealProductId){
        return OrderProduct.createHotDealProduct(hotDealProductId, "hotDealProduct" + hotDealProductId, 1, 1000);
    }

    private OrderProduct createTestProduct(Long productId){
        return OrderProduct.createProduct(productId, "product" + productId, 1, 2000);
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

    private List<Order> createTestOrdersForPaging(Long userId, List<OrderProduct> orderProducts) {
        int totalAmount = orderProducts.stream()
                .mapToInt(op -> op.getPrice() * op.getQuantity())
                .sum();
        ArrayList<Order> orders = new ArrayList<>();
        for(long i = 1; i <= 5; i++){
            Delivery delivery = Delivery.create(Address.create("city", "street", "100-1"));
            Order order = Order.create(userId, delivery, orderProducts, totalAmount);
            orderRepository.save(order);
            orders.add(order);
        }
        return orders;
    }

    @Test
    @DisplayName("user 의 orderId로 order 단건 조회 (orderProducts, delivery fetch join)")
    public void findOrderWithDeliveryAndOpById(){
        //given
        Long userId = 1L;
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(userId, orderProducts);

        //when
        Optional<Order> optionalOrder = orderRepository.findOrderWithDeliveryAndOpById(order.getId(), userId);

        //then
        assertThat(optionalOrder).isPresent();
        Order foundOrder = optionalOrder.get();
        // order
        assertThat(foundOrder.getUserId()).isEqualTo(order.getUserId());
        assertThat(foundOrder.getAmount()).isEqualTo(order.getAmount());
        assertThat(foundOrder.getStatus()).isEqualTo(order.getStatus());
        assertThat(foundOrder.getPaidAt()).isEqualTo(order.getPaidAt());
        // delivery
        Address address = order.getDelivery().getAddress();
        assertThat(foundOrder.getDelivery()).isNotNull();
        assertThat(foundOrder.getDelivery().getAddress().getCity()).isEqualTo(address.getCity());
        assertThat(foundOrder.getDelivery().getAddress().getStreet()).isEqualTo(address.getStreet());
        assertThat(foundOrder.getDelivery().getAddress().getZipCode()).isEqualTo(address.getZipCode());
        assertThat(foundOrder.getDelivery().getDeliveryStatus()).isEqualTo(order.getDelivery().getDeliveryStatus());

        assertThat(foundOrder.getOrderProducts()).hasSize(orderProducts.size());
        for(int i = 0; i < orderProducts.size(); i++){
            OrderProduct orderProduct = foundOrder.getOrderProducts().get(i);
            OrderProduct expected = order.getOrderProducts().get(i);
            assertThat(orderProduct.getPrice()).isEqualTo(expected.getPrice());
            assertThat(orderProduct.getQuantity()).isEqualTo(expected.getQuantity());
            assertThat(orderProduct.getHotDealProductId()).isEqualTo(expected.getHotDealProductId());
            assertThat(orderProduct.getProductId()).isEqualTo(expected.getProductId());
        }
    }

    @Test
    @DisplayName("user 의 order 페이징 조회 (orderProducts, delivery fetch join)")
    public void findOrdersByCursorAndUserIdAndSize(){
        //given
        Long userId = 1L;
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        List<Order> orders = createTestOrdersForPaging(userId, orderProducts);
        Long cursor = Long.MAX_VALUE;
        int size = 3;
        PageRequest pageRequest = PageRequest.of(0, size);

        //when
        List<Order> result = orderRepository.findOrdersByCursorAndUserIdAndSize(cursor, userId, pageRequest);

        //then
        assertThat(result).hasSize(size);
        assertThat(result.get(0).getId()).isEqualTo(orders.get(orders.size() - 1).getId());
        // 정렬 확인
        for(int i = 1; i < size; i++){
            assertThat(result.get(i - 1).getId()).isGreaterThan(result.get(i).getId());
        }
    }

    @Test
    @DisplayName("환불 처리 후 1일 경과한 order status bulkUpdate")
    public void bulkUpdateOrderStatusToReturned(){
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
        order3.updateStatusReturnRequested(now);
        orderRepository.save(order3);

        //when
        int result = orderRepository.bulkUpdateOrderStatusToReturned(now.minusDays(1));

        //then
        assertThat(result).isEqualTo(2);
        // update 검증
        Order foundOrder1 = orderRepository.findById(order1.getId()).get();
        assertThat(foundOrder1.getStatus()).isEqualTo(OrderStatus.RETURNED);
        Order foundOrder2 = orderRepository.findById(order1.getId()).get();
        assertThat(foundOrder2.getStatus()).isEqualTo(OrderStatus.RETURNED);
    }

    @Test
    @DisplayName("userId, orderI로 order 조회 (delivery fetch join)")
    public void findByIdAndUserIdWithDelivery(){
        //given
        Long userId = 1L;
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(userId, orderProducts);

        //when
        Optional<Order> optionalOrder = orderRepository.findByIdAndUserIdWithDelivery(order.getId(), userId);

        //then
        assertThat(optionalOrder).isPresent();
        Order foundOrder = optionalOrder.get();
        // order
        assertThat(foundOrder.getUserId()).isEqualTo(order.getUserId());
        assertThat(foundOrder.getAmount()).isEqualTo(order.getAmount());
        assertThat(foundOrder.getStatus()).isEqualTo(order.getStatus());
        assertThat(foundOrder.getPaidAt()).isEqualTo(order.getPaidAt());
        // delivery
        Address address = order.getDelivery().getAddress();
        assertThat(foundOrder.getDelivery()).isNotNull();
        assertThat(foundOrder.getDelivery().getAddress().getCity()).isEqualTo(address.getCity());
        assertThat(foundOrder.getDelivery().getAddress().getStreet()).isEqualTo(address.getStreet());
        assertThat(foundOrder.getDelivery().getAddress().getZipCode()).isEqualTo(address.getZipCode());
        assertThat(foundOrder.getDelivery().getDeliveryStatus()).isEqualTo(order.getDelivery().getDeliveryStatus());
    }

    @Test
    @DisplayName("userId, orderI로 order 조회 (orderProducts fetch join)")
    public void findByOrderIdAndUserIdWithOp(){
        //given
        Long userId = 1L;
        ArrayList<OrderProduct> orderProducts = new ArrayList<>();
        OrderProduct orderProduct1 = createTestProduct(1L);
        OrderProduct orderProduct2 = createTestHotDealProduct(1L);
        orderProducts.add(orderProduct1);
        orderProducts.add(orderProduct2);

        Order order = createTestOrder(userId, orderProducts);

        //when
        Optional<Order> optionalOrder = orderRepository.findByOrderIdAndUserIdWithOp(userId, order.getId());

        //then
        assertThat(optionalOrder).isPresent();
        Order foundOrder = optionalOrder.get();
        // order
        assertThat(foundOrder.getUserId()).isEqualTo(order.getUserId());
        assertThat(foundOrder.getAmount()).isEqualTo(order.getAmount());
        assertThat(foundOrder.getStatus()).isEqualTo(order.getStatus());
        assertThat(foundOrder.getPaidAt()).isEqualTo(order.getPaidAt());
        // orderProduct
        assertThat(foundOrder.getOrderProducts()).hasSize(orderProducts.size());
        for(int i = 0; i < orderProducts.size(); i++){
            OrderProduct orderProduct = foundOrder.getOrderProducts().get(i);
            OrderProduct expected = order.getOrderProducts().get(i);
            assertThat(orderProduct.getPrice()).isEqualTo(expected.getPrice());
            assertThat(orderProduct.getQuantity()).isEqualTo(expected.getQuantity());
            assertThat(orderProduct.getHotDealProductId()).isEqualTo(expected.getHotDealProductId());
            assertThat(orderProduct.getProductId()).isEqualTo(expected.getProductId());
        }
    }
}