package com.appdev.Finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // For file upload
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.appdev.Finance.Service.ActivityLogService;
// TagService removed
import com.appdev.Finance.Service.TransactionService;
import com.appdev.Finance.model.ActivityType;
// Tag model removed
import com.appdev.Finance.model.Transaction;
import com.appdev.Finance.model.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Locale;
import java.util.Set; // Keep for potential non-tag Set usage
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet; // Keep for potential non-tag Set usage
import java.util.stream.Collectors;

@Controller
public class FinanceControllerAPI {

    private static final Logger logger = LoggerFactory.getLogger(FinanceControllerAPI.class);

    @Autowired
    private TransactionService transactionService;

    // TagService removed
    // @Autowired
    // private TagService tagService;

    @Autowired
    private ActivityLogService activityLogService;


    @GetMapping("/transactions")
    public String listTransactions(HttpSession session, Model model,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(name = "type", required = false) String transactionType,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) String month,
                                   @RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate,
                                   @RequestParam(required = false) Double minAmount,
                                   @RequestParam(required = false) Double maxAmount,
                                   // @RequestParam(name = "tags", required = false) Set<String> tagNames, // Removed tagNames
                                   @RequestParam(defaultValue = "transactionDate") String sortField,
                                   @RequestParam(defaultValue = "DESC") String sortOrder) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            logger.warn("User not logged in, redirecting to login from /transactions.");
            return "redirect:/login";
        }
        if (loggedInUser.getId() == null) {
            logger.error("Logged in user has a null ID in /transactions. Invalidating session and redirecting to login.");
            session.invalidate();
            return "redirect:/login";
        }

        List<Transaction> transactions;
        List<String> availableCategories = new ArrayList<>();
        // List<Tag> userTags = new ArrayList<>(); // Removed userTags

        try {
            availableCategories = transactionService.getDistinctCategoriesByUserId(loggedInUser.getId());
            // userTags = tagService.getTagsByUser(loggedInUser); // Removed

            transactions = transactionService.searchTransactions(
                loggedInUser, keyword, transactionType, category, month,
                startDate, endDate, minAmount, maxAmount, 
                sortField, sortOrder
            );

            model.addAttribute("transactions", transactions);
            
            double totalAmount = 0.0;
            if (transactions != null) {
                totalAmount = transactions.stream()
                                        .filter(t -> t.getAmount() != null)
                                        .mapToDouble(Transaction::getAmount)
                                        .sum();
            }
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("keyword", keyword);
            model.addAttribute("selectedType", transactionType);
            model.addAttribute("selectedCategory", category);
            model.addAttribute("selectedMonth", month);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("minAmount", minAmount);
            model.addAttribute("maxAmount", maxAmount);
            // model.addAttribute("selectedTags", tagNames != null ? tagNames : new HashSet<>()); // Removed
            model.addAttribute("sortField", sortField);
            model.addAttribute("sortOrder", sortOrder.toUpperCase());

        } catch (Exception e) {
            logger.error("Error fetching transactions for user ID: {}. Error: {}", loggedInUser.getId(), e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while fetching transactions.");
            model.addAttribute("transactions", List.of());
            model.addAttribute("totalAmount", 0.0);
            model.addAttribute("availableCategories", availableCategories);
            // model.addAttribute("userTags", userTags); // Removed
        }
        model.addAttribute("availableCategories", availableCategories);
        // model.addAttribute("userTags", userTags); // Removed
        model.addAttribute("currentPage", "transactions");

        return "transactions";
    }
    @PostMapping("/transactions")
    public String handleTransactionFiltersViaPost(HttpSession session, Model model,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(name = "type", required = false) String transactionType,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) String month, // Keep month if it's part of POSTable filters
                                   @RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate,
                                   @RequestParam(required = false) Double minAmount,
                                   @RequestParam(required = false) Double maxAmount,
                                   @RequestParam(defaultValue = "transactionDate") String sortField, // Keep sort params if needed for initial POST view
                                   @RequestParam(defaultValue = "DESC") String sortOrder) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            logger.warn("User not logged in, redirecting to login from POST /transactions.");
            return "redirect:/login";
        }
        if (loggedInUser.getId() == null) {
            logger.error("Logged in user has a null ID in POST /transactions. Invalidating session and redirecting to login.");
            session.invalidate();
            return "redirect:/login";
        }

        List<Transaction> transactions;
        List<String> availableCategories = new ArrayList<>();

        try {
            availableCategories = transactionService.getDistinctCategoriesByUserId(loggedInUser.getId());

            // Call your existing service method. Ensure it handles null for month if it's not a filter criteria here.
            transactions = transactionService.searchTransactions(
                loggedInUser, keyword, transactionType, category, month, 
                startDate, endDate, minAmount, maxAmount,
                sortField, sortOrder.toUpperCase()
            );

            model.addAttribute("transactions", transactions);

            double totalAmount = 0.0;
            if (transactions != null) {
                totalAmount = transactions.stream()
                                        .filter(t -> t.getAmount() != null)
                                        .mapToDouble(Transaction::getAmount)
                                        .sum();
            }
            model.addAttribute("totalAmount", totalAmount);

            // Add all parameters back to the model so the form fields can be repopulated
            // and subsequent sort links can use them if they still use GET.
            model.addAttribute("keyword", keyword);
            model.addAttribute("selectedType", transactionType);
            model.addAttribute("selectedCategory", category);
            model.addAttribute("selectedMonth", month); // Or however you manage month display
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("minAmount", minAmount);
            model.addAttribute("maxAmount", maxAmount);
            model.addAttribute("sortField", sortField);
            model.addAttribute("sortOrder", sortOrder.toUpperCase());

        } catch (Exception e) {
            logger.error("Error fetching transactions via POST for user ID: {}. Error: {}", loggedInUser.getId(), e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while fetching transactions.");
            model.addAttribute("transactions", List.of());
            model.addAttribute("totalAmount", 0.0);
        }

        model.addAttribute("availableCategories", availableCategories);
        model.addAttribute("currentPage", "transactions");

        return "transactions"; // Return the same view
    }

    @GetMapping("/transactions/form")
    public String showTransactionForm(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        Transaction transaction = new Transaction();
        model.addAttribute("transaction", transaction);
        model.addAttribute("currentPage", "transactions");
        return "transaction-form";
    }

    @PostMapping("/transactions/add")
    public String addTransaction(
            @ModelAttribute Transaction transaction,
            @RequestParam("transactionDateString") String transactionDateString,
            @RequestParam(name = "fileReceipt", required = false) MultipartFile fileReceipt, // Added for file upload
            HttpSession session,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (loggedInUser.getId() == null) {
            logger.error("Logged in user has a null ID when attempting to add transaction. Invalidating session.");
            session.invalidate();
            redirectAttributes.addFlashAttribute("errorMessage", "User session error. Please log in again.");
            return "redirect:/login";
        }

        if (transaction.getDescription() == null || transaction.getDescription().trim().isEmpty() ||
            transaction.getAmount() == null || transaction.getAmount() <= 0 ||
            transaction.getType() == null || transaction.getType().trim().isEmpty() ||
            transaction.getCategory() == null || transaction.getCategory().trim().isEmpty() ||
            transactionDateString == null || transactionDateString.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "All fields (Description, Amount, Type, Category, Date) are required and amount must be positive.");
            redirectAttributes.addFlashAttribute("transaction", transaction);
            return "redirect:/transactions/form";
        }

        try {
            LocalDate transactionDate = LocalDate.parse(transactionDateString, DateTimeFormatter.ISO_LOCAL_DATE);
            transaction.setTransactionDate(transactionDate);
            transaction.setMonth(transactionDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format for transactionDateString: {}", transactionDateString, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid date format. Please use yyyy-MM-dd.");
            redirectAttributes.addFlashAttribute("transaction", transaction);
            return "redirect:/transactions/form";
        }

        transaction.setUser(loggedInUser);
        transaction.setId(null);

        try {
            Transaction savedTransaction = transactionService.saveTransaction(transaction, fileReceipt); // Pass fileReceipt
            activityLogService.logActivity(loggedInUser, ActivityType.TRANSACTION_ADDED, "Added transaction: " + savedTransaction.getDescription(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Transaction added successfully!");
        } catch (IllegalArgumentException e) {
             logger.warn("Error adding transaction for user {}: {}", loggedInUser.getEmail(), e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             redirectAttributes.addFlashAttribute("transaction", transaction);
             return "redirect:/transactions/form";
        } catch (Exception e) {
            logger.error("Unexpected error saving transaction for user ID: {}. Error: {}", loggedInUser.getId(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while saving the transaction.");
            redirectAttributes.addFlashAttribute("transaction", transaction);
            return "redirect:/transactions/form";
        }

        return "redirect:/transactions";
    }

  
    @GetMapping("/transactions/month/{month}")
    public String getTransactionsByMonth(@PathVariable String month, Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            logger.warn("User not logged in. Redirecting to login page from /transactions/month/{}", month);
            return "redirect:/login";
        }
         if (loggedInUser.getId() == null) {
            logger.error("Logged in user has null ID when fetching transactions by month. Redirecting to login.");
            session.invalidate();
            return "redirect:/login";
        }

        List<Transaction> transactions;
        try {
            logger.debug("Fetching transactions for user ID: {} and month: {}", loggedInUser.getId(), month);
           
            List<Transaction> allUserTransactions = transactionService.getTransactionsByUserId(loggedInUser.getId());
            if (allUserTransactions != null) {
                 transactions = allUserTransactions
                    .stream()
                    .filter(t -> month.equalsIgnoreCase(t.getMonth()))
                    .collect(Collectors.toList());
            } else {
                transactions = List.of();
            }
            logger.info("Found {} transactions for user ID: {} and month: {}", transactions.size(), loggedInUser.getId(), month);

        } catch (Exception e) {
            logger.error("Error fetching transactions for user ID: {} and month: {}", loggedInUser.getId(), month, e);
            model.addAttribute("errorMessage", "An error occurred while fetching transactions for " + month + ".");
            transactions = List.of();
        }

        model.addAttribute("transactions", transactions);
        model.addAttribute("selectedMonth", month);
       
        double totalAmount = 0.0;
        if (transactions != null) {
            totalAmount = transactions.stream()
                                    .filter(t -> t.getAmount() != null)
                                    .mapToDouble(Transaction::getAmount)
                                    .sum();
        }
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("pageContext", "transactionsByMonth");

        return "transactions"; 
    }
}