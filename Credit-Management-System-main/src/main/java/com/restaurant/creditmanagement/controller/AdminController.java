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
import java.util.Date;
import java.util.Optional;

@Controller
public class AdminController {
    @Autowired
    private AdminService adminService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("admin", new Admin());
        return "register";
    }

    @PostMapping("/register")
    public String registerAdmin(@ModelAttribute Admin admin, RedirectAttributes redirectAttributes) {
        // Basic validation
        if (admin.getUsername() == null || admin.getUsername().trim().isEmpty() ||
            admin.getPassword() == null || admin.getPassword().trim().isEmpty() ||
            admin.getName() == null || admin.getName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Name, username and password are required");
            return "redirect:/register";
        }
    
        try {
            // Check if username already exists
            Optional<Admin> existingAdmin = adminService.findByUsername(admin.getUsername());
            if (existingAdmin.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Username already exists");
                return "redirect:/register";
            }
    
            // Set default values
            admin.setActive(true);
            admin.setCreatedAt(new Date());
    
            // Save the admin
            Admin savedAdmin = adminService.registerAdmin(admin);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
            
        } catch (Exception e) {
            e.printStackTrace(); // Add this for debugging
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }

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
                return "redirect:/dashboard";  // This handles the redirection to dashboard
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
