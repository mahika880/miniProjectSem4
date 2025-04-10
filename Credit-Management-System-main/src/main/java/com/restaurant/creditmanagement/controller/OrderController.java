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
import java.util.Map;

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
    
            Customer customer = customerService.getCustomerById(customerId, adminId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            
            Order order = new Order();
            order.setAdminId(adminId);
            order.setCustomer(customer);
            order.setPaymentMethod(paymentMethod);
            order.setNotes(notes);
            order.setTax(tax);
            order.setTotalAmount(totalAmount);
            order.setOrderDate(LocalDateTime.now());
            order.setStatus("PENDING");
            
            // Update customer's credit balance if payment method is Credit
            if ("Credit".equalsIgnoreCase(paymentMethod)) {
                // Ensure creditBalance is initialized
                BigDecimal currentBalance = customer.getCreditBalance();
                if (currentBalance == null) {
                    currentBalance = BigDecimal.ZERO;
                }
                BigDecimal newBalance = currentBalance.add(totalAmount);
                customer.setCreditBalance(newBalance);
                customerService.updateCustomer(customer, adminId);
                
                // Debug log
                System.out.println("Updated customer credit balance: " + newBalance);
            }
            
            Order savedOrder = orderService.createOrder(order, menuItemIds, quantities);
            
            redirectAttributes.addFlashAttribute("success", "Order created successfully!");
            return "redirect:/orders/list";
        } catch (Exception e) {
            System.err.println("Error creating order: " + e.getMessage());
            e.printStackTrace();
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

    @GetMapping("/view/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "orders/view";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "orders/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateOrder(@PathVariable Long id, 
                            @RequestParam Map<String, String> quantities,
                            RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderQuantities(id, quantities);
            redirectAttributes.addFlashAttribute("success", "Order updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating order: " + e.getMessage());
        }
        return "redirect:/orders/list";
    }

    @PostMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.deleteOrder(id);
            redirectAttributes.addFlashAttribute("success", "Order deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting order: " + e.getMessage());
        }
        return "redirect:/orders/list";
    }
}
