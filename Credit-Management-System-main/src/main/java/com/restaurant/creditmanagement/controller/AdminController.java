package com.restaurant.creditmanagement.controller;

import com.restaurant.creditmanagement.model.Admin;
import com.restaurant.creditmanagement.repository.AdminRepository;
import com.restaurant.creditmanagement.repository.CustomerRepository;
import com.restaurant.creditmanagement.service.AdminService;
import com.restaurant.creditmanagement.service.CustomerService;
import com.restaurant.creditmanagement.service.DashboardService;
import com.restaurant.creditmanagement.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
public class AdminController {
    @Autowired
    private AdminService adminService;

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("admin", new Admin());
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute Admin admin, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Admin authenticatedAdmin = adminService.authenticate(admin.getUsername(), admin.getPassword());
            if (authenticatedAdmin != null) {
                session.setAttribute("adminId", authenticatedAdmin.getId());
                session.setAttribute("adminUsername", authenticatedAdmin.getUsername());
                return "redirect:/dashboard";
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid username or password");
                return "redirect:/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Login failed: " + e.getMessage());
            return "redirect:/login";
        }
    }
}
