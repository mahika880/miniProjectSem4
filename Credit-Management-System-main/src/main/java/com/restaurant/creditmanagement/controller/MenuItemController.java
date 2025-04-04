package com.restaurant.creditmanagement.controller;

import com.restaurant.creditmanagement.model.MenuItem;
import com.restaurant.creditmanagement.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/menu")
public class MenuItemController {
    
    @Autowired
    private MenuItemService menuItemService;

    @GetMapping
    public String listMenuItems(Model model, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        
        List<MenuItem> menuItems = menuItemService.getAllMenuItems(adminId);
        model.addAttribute("menuItems", menuItems);
        return "menu/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("menuItem", new MenuItem());
        return "menu/form";
    }

    @PostMapping("/create")
    public String createMenuItem(@ModelAttribute MenuItem menuItem, 
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        menuItem.setAdminId(adminId);
        menuItemService.saveMenuItem(menuItem);
        redirectAttributes.addFlashAttribute("message", "Menu item created successfully!");
        return "redirect:/menu";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        menuItemService.getMenuItemById(id).ifPresent(menuItem -> 
            model.addAttribute("menuItem", menuItem)
        );
        return "menu/form";
    }

    @PostMapping("/edit/{id}")
    public String updateMenuItem(@PathVariable Long id, @ModelAttribute MenuItem menuItem, 
                               RedirectAttributes redirectAttributes) {
        menuItem.setId(id);
        menuItemService.saveMenuItem(menuItem);
        redirectAttributes.addFlashAttribute("message", "Menu item updated successfully!");
        return "redirect:/menu";
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public String toggleAvailability(@PathVariable Long id) {
        menuItemService.toggleAvailability(id);
        return "success";
    }

    @DeleteMapping("/{id}")
    public String deleteMenuItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        menuItemService.deleteMenuItem(id);
        redirectAttributes.addFlashAttribute("message", "Menu item deleted successfully!");
        return "redirect:/menu";
    }

    @GetMapping("/api/items")
    @ResponseBody
    public List<MenuItem> getMenuItems(@RequestParam(required = false) String category,
                                     HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return List.of(); // Return empty list if not authenticated
        }
        
        if (category != null && !category.isEmpty()) {
            return menuItemService.getMenuItemsByCategory(category);
        }
        return menuItemService.getAllMenuItems(adminId);
    }
}
