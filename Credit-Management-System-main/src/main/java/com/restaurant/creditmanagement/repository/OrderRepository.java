package com.restaurant.creditmanagement.repository;

import com.restaurant.creditmanagement.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAdminId(Long adminId);
    List<Order> findByCustomerIdAndAdminId(Long customerId, Long adminId);
    List<Order> findTop5ByAdminIdOrderByOrderDateDesc(Long adminId);

    long countByAdminId(Long adminId);
}
