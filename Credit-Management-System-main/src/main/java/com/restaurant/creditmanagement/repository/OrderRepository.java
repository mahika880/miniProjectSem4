package com.restaurant.creditmanagement.repository;

import com.restaurant.creditmanagement.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAdminId(Long adminId);
    Long countByAdminId(Long adminId);
    
    // Add method to get top 5 recent orders
    List<Order> findTop5ByAdminIdOrderByOrderDateDesc(Long adminId);
    List<Order> findByAdminIdAndPaymentMethod(Long adminId, String paymentMethod);
}
