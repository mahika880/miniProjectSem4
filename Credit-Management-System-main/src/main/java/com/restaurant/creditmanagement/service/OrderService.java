package com.restaurant.creditmanagement.service;

import com.restaurant.creditmanagement.model.*;

import com.restaurant.creditmanagement.repository.OrderItemRepository;
import com.restaurant.creditmanagement.repository.OrderRepository;
import com.restaurant.creditmanagement.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private OrderItemRepository orderItemRepository;  // Add this line

    @Transactional
    public Order createOrder(Order order, List<Long> menuItemIds, List<Integer> quantities) {
        // Set creation timestamp
        order.setCreatedAt(LocalDateTime.now());
        
        // Save the order first
        Order savedOrder = orderRepository.save(order);
        
        // Create and save order items
        for (int i = 0; i < menuItemIds.size(); i++) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setMenuItem(menuItemService.getMenuItemById(menuItemIds.get(i)));
            orderItem.setQuantity(quantities.get(i));
            orderItemRepository.save(orderItem);
        }
        
        return savedOrder;
    }

    public List<Order> getOrdersByAdminId(Long adminId) {
        return orderRepository.findByAdminId(adminId);
    }

    public List<Order> getAllOrdersByAdmin(Long adminId) {
        return orderRepository.findByAdminId(adminId);
    }

    public List<Order> getRecentOrders(Long adminId) {
        return orderRepository.findTop5ByAdminIdOrderByOrderDateDesc(adminId);
    }

    public BigDecimal calculateTotalOutstandingCredit(Long adminId) {
        List<Order> creditOrders = orderRepository.findByAdminIdAndPaymentMethod(adminId, "CREDIT");
        return creditOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long countOrdersByAdminId(Long adminId) {
        return orderRepository.countByAdminId(adminId);
    }
}
