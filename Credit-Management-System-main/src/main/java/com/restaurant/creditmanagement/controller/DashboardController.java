package com.restaurant.creditmanagement.controller;

import com.restaurant.creditmanagement.service.CustomerService;
import com.restaurant.creditmanagement.service.DashboardService;
import com.restaurant.creditmanagement.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;

@Controller
public class DashboardController {
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private CustomerService customerService;

    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }

        // Get total outstanding credit (sum of all credit orders)
        BigDecimal totalOutstandingCredit = orderService.calculateTotalOutstandingCredit(adminId);
        
        model.addAttribute("totalOutstandingCredit", totalOutstandingCredit);
        model.addAttribute("totalOrders", orderService.countOrdersByAdminId(adminId));
        model.addAttribute("totalCustomers", customerService.countCustomersByAdminId(adminId));
        model.addAttribute("recentOrders", orderService.getRecentOrders(adminId));
        model.addAttribute("topCustomers", customerService.getTopCustomers(adminId));
        
        return "dashboard";
    }
}