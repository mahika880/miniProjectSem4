package com.restaurant.creditmanagement.service;

import com.restaurant.creditmanagement.model.Order;
import com.restaurant.creditmanagement.model.Customer;

import com.restaurant.creditmanagement.repository.OrderRepository;
import com.restaurant.creditmanagement.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    public Order createOrder(Order order, Long adminId) {
        order.setAdminId(adminId);
        order.setOrderDate(LocalDateTime.now());
        // Calculate total amount based on menu items
        BigDecimal totalAmount = calculateTotalAmount(order);
        order.setTotalAmount(totalAmount);
        
        // Update customer's credit balance
        Customer customer = order.getCustomer();
        customer.setCreditBalance(customer.getCreditBalance().add(totalAmount));
        
        return orderRepository.save(order);
    }

    private BigDecimal calculateTotalAmount(Order order) {
        return order.getOrderItems().stream()
            .map(item -> item.getMenuItem().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Autowired
    private CustomerRepository customerRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Order createOrder(Order order) {
        // Update customer credit balance
        Customer customer = order.getCustomer();
        if (customer != null) {
            BigDecimal newBalance = customer.getCreditBalance().subtract(order.getTotalAmount());
            if (newBalance.compareTo(BigDecimal.ZERO) >= 0) {
                customer.setCreditBalance(newBalance);
                customerRepository.save(customer);
                return orderRepository.save(order);
            }
            throw new IllegalStateException("Insufficient credit balance");
        }
        throw new IllegalStateException("Customer not found");
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    public List<Order> getAllOrdersByAdmin(Long adminId) {
        return orderRepository.findByAdminId(adminId);
    }

    public List<Order> getRecentOrders(Long adminId) {
        return orderRepository.findTop5ByAdminIdOrderByOrderDateDesc(adminId);
    }

    public List<Order> getOrdersByCustomerId(Long customerId, Long adminId) {
        return orderRepository.findByCustomerIdAndAdminId(customerId, adminId);
    }


}
