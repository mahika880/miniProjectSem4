package com.restaurant.creditmanagement.controller;

import com.restaurant.creditmanagement.model.Customer;
import com.restaurant.creditmanagement.model.Order;
import com.restaurant.creditmanagement.model.PaymentMethod;
import com.restaurant.creditmanagement.service.CustomerService;
import com.restaurant.creditmanagement.service.OrderService;
import com.restaurant.creditmanagement.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private MenuItemService menuItemService;

    @GetMapping("/new")
    public String showNewOrderForm(Model model, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }

        model.addAttribute("order", new Order());
        model.addAttribute("customers", customerService.getAllCustomers(adminId));
        model.addAttribute("menuItems", menuItemService.getAllMenuItems(adminId));
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("isEdit", false);
        return "orders/create-order";
    }

    // Remove this mapping since we already have /new
    /*
    @GetMapping("/orders/create")
    public String showCreateOrderForm(...) {
        ...
    }
    */

    @GetMapping("/create")  // Changed from /orders/create to just /create
    public String showCreateOrderForm(@RequestParam(required = false) Long menuItemId, 
                                    Model model, 
                                    HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("customers", customerService.getAllCustomers(adminId));
        model.addAttribute("menuItems", menuItemService.getAllMenuItems(adminId));
        model.addAttribute("selectedMenuItemId", menuItemId);
        return "orders/create-order";
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam("customerId") Long customerId,
                             @RequestParam("menuItemIds[]") List<Long> menuItemIds,
                             @RequestParam("quantities[]") List<Integer> quantities,
                             @RequestParam("paymentMethod") String paymentMethod,
                             @RequestParam("notes") String notes,
                             @RequestParam("tax") BigDecimal tax,
                             @RequestParam("totalAmount") BigDecimal totalAmount,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            Long adminId = (Long) session.getAttribute("adminId");
            if (adminId == null) {
                return "redirect:/login";
            }
    
            System.out.println("Creating order with customer ID: " + customerId); // Debug log
            
            Order order = new Order();
            order.setAdminId(adminId);
            order.setCustomer(customerService.getCustomerById(customerId, adminId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found")));
            order.setPaymentMethod(paymentMethod);
            order.setNotes(notes);
            order.setTax(tax);
            order.setTotalAmount(totalAmount);
            order.setOrderDate(LocalDateTime.now());
            order.setStatus("PENDING");
            
            Order savedOrder = orderService.createOrder(order, menuItemIds, quantities);
            System.out.println("Order saved successfully with ID: " + savedOrder.getId()); // Debug log
            
            redirectAttributes.addFlashAttribute("success", "Order created successfully!");
            return "redirect:/orders/list";
        } catch (Exception e) {
            System.err.println("Error creating order: " + e.getMessage()); // Debug log
            e.printStackTrace(); // Print stack trace for debugging
            redirectAttributes.addFlashAttribute("error", "Failed to create order: " + e.getMessage());
            return "redirect:/orders/new";
        }
    }

    @GetMapping("/list")
    public String listOrders(Model model, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
    
        List<Order> orders = orderService.getOrdersByAdminId(adminId);
        model.addAttribute("orders", orders);
        return "orders/list";
    }
}
