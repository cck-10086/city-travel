package com.citytravel.citytravel.controller;

import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.Attraction;
import com.citytravel.citytravel.entity.Homestay;
import com.citytravel.citytravel.entity.Order;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.AttractionService;
import com.citytravel.citytravel.service.HomestayService;
import com.citytravel.citytravel.service.OrderService;
import com.citytravel.citytravel.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("下单扣库存与超卖防护")
class OrderStockTest {

    private OrderService orderService;
    private AttractionService attractionService;
    private HomestayService homestayService;
    private OrderController controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        attractionService = mock(AttractionService.class);
        homestayService = mock(HomestayService.class);

        controller = new OrderController();
        ReflectionTestUtils.setField(controller, "orderService", orderService);
        ReflectionTestUtils.setField(controller, "attractionService", attractionService);
        ReflectionTestUtils.setField(controller, "homestayService", homestayService);
        ReflectionTestUtils.setField(controller, "routeService", mock(RouteService.class));

        User tourist = new User();
        tourist.setId(3L);
        tourist.setRole("tourist");
        session = new MockHttpSession();
        session.setAttribute("user", tourist);
    }

    private Attraction attraction(int stock) {
        Attraction a = new Attraction();
        a.setName("麦积山石窟");
        a.setPrice(new BigDecimal("80.00"));
        a.setStock(stock);
        a.setStatus(1);
        return a;
    }

    private Order order(String type, Long productId, int quantity) {
        Order o = new Order();
        o.setProductType(type);
        o.setProductId(productId);
        o.setQuantity(quantity);
        return o;
    }

    @Test
    @DisplayName("库存充足时下单成功，且库存被原子扣减一次")
    void shouldDeductStockOnCreate() {
        when(attractionService.getById(7L)).thenReturn(attraction(10));
        when(attractionService.deductStock(7L, 2)).thenReturn(true);
        when(orderService.generateOrderNo()).thenReturn("ORDER-1");

        Result<Order> result = controller.create(order("attraction", 7L, 2), session);

        assertThat(result.getCode()).isEqualTo(200);
        verify(attractionService).deductStock(7L, 2);
        verify(orderService).save(any(Order.class));
    }

    @Test
    @DisplayName("并发把库存抢光时扣减失败，订单不落库（防超卖）")
    void shouldRejectWhenStockDeductFails() {
        // 校验阶段读到还有 10 件，但真正扣减时已被别的请求抢完
        when(attractionService.getById(7L)).thenReturn(attraction(10));
        when(attractionService.deductStock(7L, 2)).thenReturn(false);

        Result<Order> result = controller.create(order("attraction", 7L, 2), session);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("库存不足");
        verify(orderService, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("民宿走同一套扣减逻辑")
    void shouldDeductHomestayStock() {
        Homestay homestay = new Homestay();
        homestay.setName("山景民宿");
        homestay.setPrice(new BigDecimal("200.00"));
        homestay.setStock(3);
        homestay.setStatus(1);
        when(homestayService.getById(9L)).thenReturn(homestay);
        when(homestayService.deductStock(9L, 1)).thenReturn(true);
        when(orderService.generateOrderNo()).thenReturn("ORDER-2");

        Result<Order> result = controller.create(order("homestay", 9L, 1), session);

        assertThat(result.getCode()).isEqualTo(200);
        verify(homestayService).deductStock(9L, 1);
    }

    @Test
    @DisplayName("路线没有库存字段，不参与扣减")
    void shouldSkipStockForRoute() {
        com.citytravel.citytravel.entity.Route route = new com.citytravel.citytravel.entity.Route();
        route.setName("西湖一日游");
        route.setPrice(new BigDecimal("150.00"));
        route.setStatus(1);
        RouteService routeService = mock(RouteService.class);
        when(routeService.getById(11L)).thenReturn(route);
        ReflectionTestUtils.setField(controller, "routeService", routeService);
        when(orderService.generateOrderNo()).thenReturn("ORDER-3");

        Result<Order> result = controller.create(order("route", 11L, 1), session);

        assertThat(result.getCode()).isEqualTo(200);
        verify(attractionService, never()).deductStock(any(), any());
        verify(homestayService, never()).deductStock(any(), any());
    }
}
