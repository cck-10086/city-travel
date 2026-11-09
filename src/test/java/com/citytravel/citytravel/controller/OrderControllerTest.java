package com.citytravel.citytravel.controller;

import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.Attraction;
import com.citytravel.citytravel.entity.Order;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.AttractionService;
import com.citytravel.citytravel.service.HomestayService;
import com.citytravel.citytravel.service.OrderService;
import com.citytravel.citytravel.service.RouteService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderControllerTest {

    @Test
    void shouldCalculateOrderPriceFromServerSideProduct() {
        OrderService orderService = mock(OrderService.class);
        AttractionService attractionService = mock(AttractionService.class);
        OrderController controller = createController(orderService, attractionService);

        User tourist = new User();
        tourist.setId(3L);
        tourist.setRole("tourist");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", tourist);

        Attraction attraction = new Attraction();
        attraction.setName("麦积山石窟");
        attraction.setPrice(new BigDecimal("80.00"));
        attraction.setStock(10);
        attraction.setStatus(1);
        when(attractionService.getById(7L)).thenReturn(attraction);
        when(attractionService.deductStock(7L, 2)).thenReturn(true);
        when(orderService.generateOrderNo()).thenReturn("ORDER-1");

        Order order = new Order();
        order.setProductType("attraction");
        order.setProductId(7L);
        order.setProductName("伪造名称");
        order.setQuantity(2);
        order.setTotalPrice(new BigDecimal("0.01"));

        Result<Order> result = controller.create(order, session);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(order.getProductName()).isEqualTo("麦积山石窟");
        assertThat(order.getTotalPrice()).isEqualByComparingTo("160.00");
        assertThat(order.getUserId()).isEqualTo(3L);
        verify(orderService).save(order);
    }

    @Test
    void merchantShouldNotConfirmAnotherMerchantsOrder() {
        OrderService orderService = mock(OrderService.class);
        AttractionService attractionService = mock(AttractionService.class);
        OrderController controller = createController(orderService, attractionService);

        User merchant = new User();
        merchant.setId(1L);
        merchant.setRole("merchant");
        merchant.setStatus(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", merchant);

        Order order = new Order();
        order.setId(9L);
        order.setProductType("attraction");
        order.setProductId(7L);
        order.setStatus("paid");
        Attraction attraction = new Attraction();
        attraction.setMerchantId(2L);
        when(orderService.getById(9L)).thenReturn(order);
        when(attractionService.getById(7L)).thenReturn(attraction);

        Result<Order> result = controller.confirmOrder(9L, session);

        assertThat(result.getCode()).isEqualTo(403);
        verify(orderService, never()).updateById(order);
    }

    private OrderController createController(OrderService orderService, AttractionService attractionService) {
        OrderController controller = new OrderController();
        ReflectionTestUtils.setField(controller, "orderService", orderService);
        ReflectionTestUtils.setField(controller, "attractionService", attractionService);
        ReflectionTestUtils.setField(controller, "homestayService", mock(HomestayService.class));
        ReflectionTestUtils.setField(controller, "routeService", mock(RouteService.class));
        return controller;
    }
}
