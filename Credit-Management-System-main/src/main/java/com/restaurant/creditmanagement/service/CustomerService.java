package com.restaurant.creditmanagement.service;

import com.restaurant.creditmanagement.model.Customer;
import com.restaurant.creditmanagement.model.Transaction;
import com.restaurant.creditmanagement.model.TransactionType;
import com.restaurant.creditmanagement.repository.CustomerRepository;
import com.restaurant.creditmanagement.repository.TransactionRepository;
import com.restaurant.creditmanagement.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void deleteCustomer(Long id) {
        Optional<Customer> customerOpt = customerRepository.findById(id);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            
            // Check if customer has outstanding balance
            if (customer.getCreditBalance().compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("Cannot delete customer " + customer.getName() + 
                    " because they have an outstanding balance of ₹" + customer.getCreditBalance());
            }
            
            // Clear relationships
            customer.getTransactions().clear();
            customer.getOrders().clear();
            
            // Delete the customer
            customerRepository.delete(customer);
            
            // Reset sequence to the next available ID
            Long maxId = (Long) entityManager.createQuery("SELECT COALESCE(MAX(c.id), 0) FROM Customer c")
                .getSingleResult();
            
            if (maxId == 0) {
                // If no customers exist, reset to 1
                entityManager.createNativeQuery("ALTER SEQUENCE customer_sequence RESTART WITH 1")
                    .executeUpdate();
            } else {
                // Find the next available ID
                List<Long> gaps = entityManager.createQuery(
                    "SELECT m.id + 1 FROM (SELECT c.id, LEAD(c.id) OVER (ORDER BY c.id) AS next_id " +
                    "FROM Customer c) m WHERE m.next_id - m.id > 1 AND m.id + 1 <= :maxId", Long.class)
                    .setParameter("maxId", maxId)
                    .getResultList();
                
                Long nextId = gaps.isEmpty() ? maxId + 1 : gaps.get(0);
                entityManager.createNativeQuery("ALTER SEQUENCE customer_sequence RESTART WITH :nextId")
                    .setParameter("nextId", nextId)
                    .executeUpdate();
            }
            
            logger.info("Successfully deleted customer ID: {}", id);
        } else {
            throw new RuntimeException("Customer not found");
        }
    }

    public List<Customer> getAllCustomers(Long adminId) {
        return customerRepository.findByAdminId(adminId);
    }

    @Transactional
    public Customer createCustomer(Customer customer, Long adminId) {
        customer.setAdminId(adminId);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Customer customer, Long adminId) {
        if (!customerRepository.existsById(customer.getId())) {
            throw new RuntimeException("Customer not found");
        }
        customer.setAdminId(adminId);
        return customerRepository.save(customer);
    }

    public List<Customer> getTopCustomers(Long adminId) {
        return customerRepository.findTop5ByAdminIdOrderByCreditBalanceDesc(adminId);
    }

    public List<Customer> searchCustomers(String query, Long adminId) {
        return customerRepository.searchByName(query, adminId);
    }

    // Remove the duplicate method:
    // public List<Customer> getTopCustomers() {
    //     return customerRepository.findTop5ByOrderByCreditBalanceDesc();
    // }

    public Optional<Customer> getCustomerById(Long id, Long adminId) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isPresent() && customer.get().getAdminId().equals(adminId)) {
            return customer;
        }
        return Optional.empty();
    }

    @Transactional
    public void settleBalance(Long customerId, BigDecimal settlementAmount, Long adminId) {
        Optional<Customer> customerOpt = getCustomerById(customerId, adminId);
        if (!customerOpt.isPresent()) {
            throw new RuntimeException("Customer not found");
        }

        Customer customer = customerOpt.get();
        if (customer.getCreditBalance().compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Customer has no outstanding balance to settle");
        }

        // Validate settlement amount
        if (settlementAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Settlement amount must be greater than zero");
        }
        if (settlementAmount.compareTo(customer.getCreditBalance()) > 0) {
            throw new RuntimeException("Settlement amount cannot be greater than the outstanding balance");
        }

        // Create settlement transaction
        Transaction settlement = new Transaction();
        settlement.setCustomer(customer);
        settlement.setAmount(settlementAmount);
        settlement.setType(TransactionType.SETTLEMENT);
        settlement.setDescription("Partial balance settlement");
        settlement.setTransactionDate(LocalDateTime.now());

        transactionRepository.save(settlement);

        // Update customer's credit balance
        customer.setCreditBalance(customer.getCreditBalance().subtract(settlementAmount));
        customerRepository.save(customer);
    }

    @Transactional
    public void settleBalance(Long customerId, Long adminId) {
        Optional<Customer> customerOpt = getCustomerById(customerId, adminId);
        if (!customerOpt.isPresent()) {
            throw new RuntimeException("Customer not found");
        }

        Customer customer = customerOpt.get();
        settleBalance(customerId, customer.getCreditBalance(), adminId);
    }

    public Long countCustomersByAdminId(Long adminId) {
        return customerRepository.countByAdminId(adminId);
    }
}
