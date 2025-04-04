package com.restaurant.creditmanagement.controller;

import com.restaurant.creditmanagement.model.Order;
import com.restaurant.creditmanagement.model.PaymentMethod;
import com.restaurant.creditmanagement.service.CustomerService;
import com.restaurant.creditmanagement.service.OrderService;
import com.restaurant.creditmanagement.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

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

    @PostMapping("/new")
    public String createOrder(@ModelAttribute Order order, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }

        orderService.createOrder(order, adminId);
        return "redirect:/orders"; // This will redirect to list.html
    }
}
