package com.restaurant.creditmanagement.controller;

import com.restaurant.creditmanagement.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }

        // Add dashboard data
        model.addAttribute("totalCustomers", dashboardService.getTotalCustomers(adminId));
        model.addAttribute("totalOrders", dashboardService.getTotalOrders(adminId));
        model.addAttribute("totalOutstandingCredit", dashboardService.getTotalOutstandingCredit(adminId));
        model.addAttribute("topCustomers", dashboardService.getTopCustomers(adminId));
        model.addAttribute("recentOrders", dashboardService.getRecentOrders(adminId));

        return "dashboard";
    }
}