package com.restaurant.creditmanagement.controller;

import com.restaurant.creditmanagement.model.Customer;
import com.restaurant.creditmanagement.model.Transaction;
import com.restaurant.creditmanagement.repository.CustomerRepository;
import com.restaurant.creditmanagement.repository.TransactionRepository;
import com.restaurant.creditmanagement.service.CustomerService;
// Add this import at the top with other imports
import com.restaurant.creditmanagement.model.TransactionType;
import java.time.LocalDateTime;
import com.restaurant.creditmanagement.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public String listCustomers(Model model, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        
        List<Customer> customers = customerService.getAllCustomers(adminId);
        
        // Calculate statistics with proper BigDecimal comparison
        BigDecimal totalCreditBalance = customers.stream()
                .map(Customer::getCreditBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCustomers = customers.size();
        long customersWithNoBalance = customers.stream()
                .filter(c -> c.getCreditBalance().compareTo(BigDecimal.ZERO) == 0)
                .count();
        long customersWithBalance = customers.stream()
                .filter(c -> c.getCreditBalance().compareTo(BigDecimal.ZERO) > 0)
                .count();

        model.addAttribute("customers", customers);
        model.addAttribute("totalCreditBalance", totalCreditBalance);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("customersWithNoBalance", customersWithNoBalance);
        model.addAttribute("customersWithBalance", customersWithBalance);

        return "customers/list";
    }

    @PostMapping("/new")
    public String addCustomer(@ModelAttribute Customer customer, 
                            RedirectAttributes redirectAttributes,
                            HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        
        try {
            customer.setCreditBalance(BigDecimal.ZERO);
            customerService.createCustomer(customer, adminId);
            redirectAttributes.addFlashAttribute("success", "Customer added successfully!");
            return "redirect:/customers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add customer: " + e.getMessage());
            return "redirect:/customers/new";
        }
    }

    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customers/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        
        Optional<Customer> customerOpt = customerService.getCustomerById(id, adminId);
        if (customerOpt.isPresent()) {
            model.addAttribute("customer", customerOpt.get());
            return "customers/form";
        }
        throw new RuntimeException("Customer not found");
    }

    @PostMapping("/edit/{id}")
    public String updateCustomer(@PathVariable Long id, 
                               @ModelAttribute Customer customer,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        
        try {
            Optional<Customer> existingCustomerOpt = customerService.getCustomerById(id, adminId);
            if (!existingCustomerOpt.isPresent()) {
                throw new RuntimeException("Customer not found");
            }
            
            Customer existingCustomer = existingCustomerOpt.get();
            
            // Update the existing customer's fields
            existingCustomer.setName(customer.getName());
            existingCustomer.setPhone(customer.getPhone());
            existingCustomer.setEmail(customer.getEmail());
            existingCustomer.setAddress(customer.getAddress());
            
            // Handle credit limit changes
            if (customer.getTotalCredit() != null && 
                customer.getTotalCredit().compareTo(existingCustomer.getTotalCredit()) != 0) {
                // Validate that new credit limit is not less than current balance
                if (customer.getTotalCredit().compareTo(existingCustomer.getCreditBalance()) < 0) {
                    redirectAttributes.addFlashAttribute("error", 
                        "New credit limit cannot be less than current balance (₹" + 
                        existingCustomer.getCreditBalance() + ")");
                    return "redirect:/customers/edit/" + id;
                }
                existingCustomer.setTotalCredit(customer.getTotalCredit());
            }
            
            customerService.updateCustomer(existingCustomer, adminId);
            redirectAttributes.addFlashAttribute("success", "Customer updated successfully!");
            return "redirect:/customers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update customer: " + e.getMessage());
            return "redirect:/customers/edit/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.deleteCustomer(id);
            redirectAttributes.addFlashAttribute("success", "Customer deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting customer: " + e.getMessage());
        }
        return "redirect:/customers";
    }

    @PostMapping("/{id}/settle")
    public String settleBalance(@PathVariable Long id,
                              @RequestParam BigDecimal amount,
                              @RequestParam String paymentMethod,
                              @RequestParam(required = false) String notes,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            Long adminId = (Long) session.getAttribute("adminId");
            if (adminId == null) {
                return "redirect:/login";
            }

            Customer customer = customerService.getCustomerById(id, adminId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

            // Validate settlement amount
            if (amount.compareTo(customer.getCreditBalance()) > 0) {
                throw new IllegalArgumentException("Settlement amount cannot exceed outstanding balance");
            }

            // Update customer's credit balance
            BigDecimal newBalance = customer.getCreditBalance().subtract(amount);
            customer.setCreditBalance(newBalance);
            customerService.updateCustomer(customer, adminId);

            // Create transaction record
            // Update this part
            Transaction transaction = new Transaction();
            transaction.setCustomer(customer);
            transaction.setAmount(amount);
            transaction.setType(TransactionType.SETTLEMENT);  // Changed from String to enum
            transaction.setPaymentMethod(paymentMethod);
            transaction.setNotes(notes);
            transaction.setStatus("COMPLETED");
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setAdminId(adminId);
            transactionService.saveTransaction(transaction);

            redirectAttributes.addFlashAttribute("success", 
                "Payment of ₹" + amount + " successfully processed");
            return "redirect:/customers/view/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to process payment: " + e.getMessage());
            return "redirect:/customers/view/" + id;
        }
    }

    @GetMapping("/view/{id}")
    public String viewCustomer(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }

        try {
            Optional<Customer> customerOpt = customerService.getCustomerById(id, adminId);
            if (customerOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Customer not found");
                return "redirect:/customers";
            }

            Customer customer = customerOpt.get();
            List<Transaction> transactions = transactionRepository.findByCustomer_IdOrderByTransactionDateDesc(id);

            model.addAttribute("customer", customer);
            model.addAttribute("transactions", transactions);
            model.addAttribute("creditTrend", 0);
            
            return "customers/view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error loading customer: " + e.getMessage());
            return "redirect:/customers";
        }
    }

    @GetMapping("/search")
    public String searchCustomers(@RequestParam String query, Model model, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        
        List<Customer> customers = customerService.searchCustomers(query, adminId);
        model.addAttribute("customers", customers);
        return "customers/list";
    }

    // Remove the duplicate listCustomers method that starts here
    // The original listCustomers method at the top of the file is the correct one
}
