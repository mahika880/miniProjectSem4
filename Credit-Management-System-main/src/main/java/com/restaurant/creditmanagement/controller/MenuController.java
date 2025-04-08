package com.restaurant.creditmanagement.controller;

import com.restaurant.creditmanagement.model.MenuItem;
import com.restaurant.creditmanagement.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/menu-items")
public class MenuController {

    @Autowired
    private MenuItemService menuItemService;

    @GetMapping("/add")
    public String showAddMenuForm(Model model) {
        model.addAttribute("menuItem", new MenuItem());
        return "menu/add-menu-item";
    }

    @PostMapping("/add")
    public String addMenuItem(@ModelAttribute MenuItem menuItem, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        
        try {
            menuItem.setAdminId(adminId);
            menuItemService.addMenuItem(menuItem);
            return "redirect:/menu-items/list";
        } catch (Exception e) {
            return "redirect:/menu-items/add?error";
        }
    }

    @GetMapping("/list")
    public String showMenuList(Model model, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        List<MenuItem> menuItems = menuItemService.getAllMenuItems(adminId);
        model.addAttribute("menuItems", menuItems);
        return "menu/list";
    }

    @GetMapping("/categories")
    public String showCategories(Model model) {
        return "menu/categories";
    }
}