package com.appdev.Finance;

import com.appdev.Finance.DTO.ReportSummaryDTO;
import com.appdev.Finance.Repository.TransactionRepository;
import com.appdev.Finance.Repository.UserRepository;
import com.appdev.Finance.Service.ActivityLogService;
import com.appdev.Finance.Service.BudgetService;
import com.appdev.Finance.Service.ReportService;
import com.appdev.Finance.Service.TransactionService;
import com.appdev.Finance.Service.UserService;
import com.appdev.Finance.model.ActivityType;
import com.appdev.Finance.model.Budget;
import com.appdev.Finance.model.Transaction;
import com.appdev.Finance.model.User;
import com.appdev.Finance.model.CustomUserDetails; // Make sure this is imported
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest; // Import HttpServletRequest
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Import this
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils; // Import for FlashMap
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode; // Import RoundingMode
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Controller
public class DashboardAndLoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;
    private final ActivityLogService activityLogService;
    private final TransactionService transactionService;
    private static final Logger logger = LoggerFactory.getLogger(DashboardAndLoginController.class);

    @Autowired
    public DashboardAndLoginController(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       UserService userService,
                                       TransactionRepository transactionRepository,
                                       BudgetService budgetService,
                                       ReportService reportService,
                                       ObjectMapper objectMapper,
                                       ActivityLogService activityLogService,
                                       TransactionService transactionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
        this.reportService = reportService;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
        this.activityLogService = activityLogService;
        this.transactionService = transactionService;
    }

    @GetMapping("/login")
    public String login(Model model, @RequestParam(value = "error", required = false) String error, @RequestParam(value = "logout", required = false) String logout) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password, or account issue. Please try again or contact support if the problem persists.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal()))) {
            return "redirect:/dashboard"; // Assumes context path is handled by Spring/Tomcat redirects
        }
        return "login";
    }

    /* @PostMapping("/login") method remains commented out as per previous step */

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal CustomUserDetails principalDetails, // Use Spring Security's principal
            @RequestParam(value = "selectedMonth", required = false) String selectedMonthParam,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "type", required = false) String transactionType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(value = "sortField", required = false, defaultValue = "transactionDate") String sortField,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "DESC") String sortOrder,
            HttpSession session, Model model, RedirectAttributes redirectAttributes,
            HttpServletRequest httpRequest) { // <<< MODIFIED: Added HttpServletRequest httpRequest (if it wasn't httpRequest before, ensure it's the one you use for flashMap, if different, add a new HttpServletRequest parameter)

        // **** NEW LINE: Add context path to the model ****
        model.addAttribute("appContextPath", httpRequest.getContextPath());


        User loggedInUser;

        if (principalDetails == null) {
            logger.warn("/dashboard accessed without authenticated principal. Redirecting to login.");
            redirectAttributes.addFlashAttribute("errorMessage", "Your session may have expired. Please log in again.");
            return "redirect:/login";
        }

        Optional<User> userOpt = userService.findByEmail(principalDetails.getUsername());
        if (userOpt.isPresent()) {
            loggedInUser = userOpt.get();
            session.setAttribute("loggedInUser", loggedInUser);
        } else {
            logger.error("/dashboard: User '{}' (from principal) not found in repository. Forcing logout.", principalDetails.getUsername());
            SecurityContextHolder.clearContext(); 
            session.invalidate(); 
            redirectAttributes.addFlashAttribute("errorMessage", "Critical error retrieving your user details. Please log in again.");
            return "redirect:/login";
        }

        if (loggedInUser.getId() == null) {
            logger.error("/dashboard: Logged in user {} has null ID after fetching. Forcing logout.", loggedInUser.getEmail());
            SecurityContextHolder.clearContext();
            session.invalidate();
            redirectAttributes.addFlashAttribute("errorMessage", "User data integrity error. Please log in again.");
            return "redirect:/login";
        }
        
        Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(httpRequest);
        if (flashMap != null) {
            String successMessage = (String) flashMap.get("successMessage");
            if (successMessage != null) model.addAttribute("successMessage", successMessage);

            String generalErrorMessage = (String) flashMap.get("errorMessage"); 
            if (generalErrorMessage != null) model.addAttribute("generalErrorMessage", generalErrorMessage);
        }
        
        String displayMonth;
        if (selectedMonthParam == null || selectedMonthParam.trim().isEmpty()) {
            displayMonth = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        } else {
            displayMonth = selectedMonthParam;
        }
        model.addAttribute("selectedMonth", displayMonth);

        List<Transaction> allUserTransactions = transactionRepository.findByUserId(loggedInUser.getId());
        if (allUserTransactions == null) allUserTransactions = new ArrayList<>();

        Map<String, Double> categoryTotalsForChart = allUserTransactions.stream()
            .filter(t -> "Expense".equalsIgnoreCase(t.getType()) && t.getAmount() != null)
            .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getAmount)));
        Map<String, Double> monthlyTotalsForChart = allUserTransactions.stream()
            .filter(t -> "Expense".equalsIgnoreCase(t.getType()) && t.getAmount() != null)
            .collect(Collectors.groupingBy(Transaction::getMonth, Collectors.summingDouble(Transaction::getAmount)));

        try {
            model.addAttribute("categoryData", objectMapper.writeValueAsString(categoryTotalsForChart));
            model.addAttribute("monthlyData", objectMapper.writeValueAsString(monthlyTotalsForChart));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing chart data for user {}: {}", loggedInUser.getEmail(), e.getMessage());
            model.addAttribute("categoryData", "{}");
            model.addAttribute("monthlyData", "{}");
            model.addAttribute("chartErrorMessage", "Could not load chart data.");
        }

        Budget budget = budgetService.getBudgetForUser(loggedInUser);
        BigDecimal budgetAmount = (budget != null && budget.getAmount() != null) ? budget.getAmount() : BigDecimal.ZERO;

        double totalExpensesForAllTime = allUserTransactions.stream()
            .filter(t -> "Expense".equalsIgnoreCase(t.getType()) && t.getAmount() != null)
            .mapToDouble(Transaction::getAmount).sum();

        int expensePercentage = 0;
        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                 BigDecimal expensePercentageDecimal = BigDecimal.valueOf(totalExpensesForAllTime)
                      .divide(budgetAmount, 4, RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
                 expensePercentage = expensePercentageDecimal.intValue();
            } catch (ArithmeticException e) {
                logger.warn("ArithmeticException during budget percentage calculation for user {}: budgetAmount was likely zero. Setting percentage based on expenses.", loggedInUser.getEmail());
                expensePercentage = (totalExpensesForAllTime > 0) ? 100 : 0;
            }
        }
        expensePercentage = Math.min(Math.max(expensePercentage, 0), 100);

        model.addAttribute("expensePercentage", expensePercentage);
        model.addAttribute("currentBudget", budgetAmount.doubleValue());

        List<Transaction> selectedMonthTransactionsList;
        List<String> availableCategoriesForFilter = new ArrayList<>();

        try {
            String monthToSearch = (startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty()) ? displayMonth : null;

            selectedMonthTransactionsList = transactionService.searchTransactions(
                loggedInUser, keyword, transactionType, category, monthToSearch,
                startDate, endDate, minAmount, maxAmount,
                sortField, sortOrder
            );
            
            availableCategoriesForFilter = transactionService.getDistinctCategoriesByUserId(loggedInUser.getId());

        } catch (Exception e) {
            logger.error("Error fetching dashboard transaction data for user: {}, month: {}, filters. Error: {}", loggedInUser.getEmail(), displayMonth, e.getMessage(), e);
            model.addAttribute("transactionListError", "Could not load transaction data for " + displayMonth);
            selectedMonthTransactionsList = Collections.emptyList();
        }
        
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedType", transactionType);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("minAmount", minAmount);
        model.addAttribute("maxAmount", maxAmount);

        model.addAttribute("sortField", sortField);
        model.addAttribute("sortOrder", sortOrder.toUpperCase());
        model.addAttribute("availableCategoriesForFilter", availableCategoriesForFilter);

        BigDecimal monthIncome = BigDecimal.ZERO;
        BigDecimal monthExpenses = BigDecimal.ZERO;
        if (selectedMonthTransactionsList != null) {
            for (Transaction tx : selectedMonthTransactionsList) {
                if (tx.getAmount() != null) {
                    if ("Income".equalsIgnoreCase(tx.getType())) {
                        monthIncome = monthIncome.add(BigDecimal.valueOf(tx.getAmount()));
                    } else if ("Expense".equalsIgnoreCase(tx.getType())) {
                        monthExpenses = monthExpenses.add(BigDecimal.valueOf(tx.getAmount()));
                    }
                }
            }
        }

        model.addAttribute("selectedMonthTotalIncome", monthIncome);
        model.addAttribute("selectedMonthTotalExpenses", monthExpenses);

        final int MAX_DASHBOARD_TRANSACTIONS = 5;
        List<Transaction> dashboardTransactionView;
        boolean showingLimitedTransactions = false;
        int totalTransactionsForMonthAfterFilter = (selectedMonthTransactionsList != null) ? selectedMonthTransactionsList.size() : 0;

        if (selectedMonthTransactionsList != null) {
            if (selectedMonthTransactionsList.size() > MAX_DASHBOARD_TRANSACTIONS) {
                dashboardTransactionView = selectedMonthTransactionsList.stream().limit(MAX_DASHBOARD_TRANSACTIONS).collect(Collectors.toList());
                showingLimitedTransactions = true;
            } else {
                dashboardTransactionView = selectedMonthTransactionsList;
            }
        } else {
            dashboardTransactionView = Collections.emptyList();
        }

        model.addAttribute("selectedMonthTransactionsList", dashboardTransactionView);
        model.addAttribute("showingLimitedTransactions", showingLimitedTransactions);
        model.addAttribute("totalTransactionsForMonthAfterFilter", totalTransactionsForMonthAfterFilter);

        List<String> monthNames = Arrays.stream(Month.values())
            .map(m -> m.getDisplayName(TextStyle.FULL, Locale.ENGLISH)).collect(Collectors.toList());
        model.addAttribute("availableMonths", monthNames);
        model.addAttribute("currentPage", "dashboard");
        
        return "dashboard";
    }
    @PostMapping("/dashboard")
    public String handleDashboardPost(
            @AuthenticationPrincipal CustomUserDetails principalDetails,
            @RequestParam(value = "selectedMonth", required = false) String selectedMonthParam,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "type", required = false) String transactionType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(value = "sortField", required = false, defaultValue = "transactionDate") String sortField,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "DESC") String sortOrder,
            HttpSession session, Model model, RedirectAttributes redirectAttributes, HttpServletRequest httpRequest) {

        // --- This part is identical to your GET /dashboard method ---
        User loggedInUser;
        if (principalDetails == null) {
            logger.warn("/dashboard (POST) accessed without authenticated principal. Redirecting to login.");
            redirectAttributes.addFlashAttribute("errorMessage", "Your session may have expired. Please log in again.");
            return "redirect:/login";
        }
        Optional<User> userOpt = userService.findByEmail(principalDetails.getUsername());
        if (userOpt.isPresent()) {
            loggedInUser = userOpt.get();
            session.setAttribute("loggedInUser", loggedInUser);
        } else {
            logger.error("/dashboard (POST): User '{}' (from principal) not found. Forcing logout.", principalDetails.getUsername());
            SecurityContextHolder.clearContext();
            session.invalidate();
            redirectAttributes.addFlashAttribute("errorMessage", "Critical error retrieving user details. Please log in again.");
            return "redirect:/login";
        }
        if (loggedInUser.getId() == null) {
            logger.error("/dashboard (POST): Logged in user {} has null ID. Forcing logout.", loggedInUser.getEmail());
            SecurityContextHolder.clearContext();
            session.invalidate();
            redirectAttributes.addFlashAttribute("errorMessage", "User data integrity error. Please log in again.");
            return "redirect:/login";
        }
        model.addAttribute("appContextPath", httpRequest.getContextPath());
        Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(httpRequest);
        if (flashMap != null) {
            String successMessage = (String) flashMap.get("successMessage");
            if (successMessage != null) model.addAttribute("successMessage", successMessage);
            String generalErrorMessage = (String) flashMap.get("errorMessage");
            if (generalErrorMessage != null) model.addAttribute("generalErrorMessage", generalErrorMessage);
        }
        String displayMonth;
        if (selectedMonthParam == null || selectedMonthParam.trim().isEmpty()) {
            displayMonth = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        } else {
            displayMonth = selectedMonthParam;
        }
        model.addAttribute("selectedMonth", displayMonth);
        List<Transaction> allUserTransactions = transactionRepository.findByUserId(loggedInUser.getId());
        if (allUserTransactions == null) allUserTransactions = new ArrayList<>();
        Map<String, Double> categoryTotalsForChart = allUserTransactions.stream()
            .filter(t -> "Expense".equalsIgnoreCase(t.getType()) && t.getAmount() != null)
            .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getAmount)));
        Map<String, Double> monthlyTotalsForChart = allUserTransactions.stream()
            .filter(t -> "Expense".equalsIgnoreCase(t.getType()) && t.getAmount() != null)
            .collect(Collectors.groupingBy(Transaction::getMonth, Collectors.summingDouble(Transaction::getAmount)));
        try {
            model.addAttribute("categoryData", objectMapper.writeValueAsString(categoryTotalsForChart));
            model.addAttribute("monthlyData", objectMapper.writeValueAsString(monthlyTotalsForChart));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing chart data for user {}: {}", loggedInUser.getEmail(), e.getMessage());
            model.addAttribute("categoryData", "{}");
            model.addAttribute("monthlyData", "{}");
            model.addAttribute("chartErrorMessage", "Could not load chart data.");
        }
        Budget budget = budgetService.getBudgetForUser(loggedInUser);
        BigDecimal budgetAmount = (budget != null && budget.getAmount() != null) ? budget.getAmount() : BigDecimal.ZERO;
        double totalExpensesForAllTime = allUserTransactions.stream()
            .filter(t -> "Expense".equalsIgnoreCase(t.getType()) && t.getAmount() != null)
            .mapToDouble(Transaction::getAmount).sum();
        int expensePercentage = 0;
        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                 BigDecimal expensePercentageDecimal = BigDecimal.valueOf(totalExpensesForAllTime)
                      .divide(budgetAmount, 4, RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
                 expensePercentage = expensePercentageDecimal.intValue();
            } catch (ArithmeticException e) {
                logger.warn("ArithmeticException during budget percentage calculation for user {}: budgetAmount was likely zero.", loggedInUser.getEmail());
                expensePercentage = (totalExpensesForAllTime > 0) ? 100 : 0;
            }
        }
        expensePercentage = Math.min(Math.max(expensePercentage, 0), 100);
        model.addAttribute("expensePercentage", expensePercentage);
        model.addAttribute("currentBudget", budgetAmount.doubleValue());
        List<Transaction> selectedMonthTransactionsList;
        List<String> availableCategoriesForFilter = new ArrayList<>();
        try {
            String monthToSearch = (startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty()) ? displayMonth : null;
            selectedMonthTransactionsList = transactionService.searchTransactions(
                loggedInUser, keyword, transactionType, category, monthToSearch,
                startDate, endDate, minAmount, maxAmount,
                sortField, sortOrder.toUpperCase() // Ensure sortOrder is passed correctly
            );
            availableCategoriesForFilter = transactionService.getDistinctCategoriesByUserId(loggedInUser.getId());
        } catch (Exception e) {
            logger.error("Error fetching dashboard transaction data (POST) for user: {}, month: {}, filters. Error: {}", loggedInUser.getEmail(), displayMonth, e.getMessage(), e);
            model.addAttribute("transactionListError", "Could not load transaction data for " + displayMonth);
            selectedMonthTransactionsList = Collections.emptyList();
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedType", transactionType);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("minAmount", minAmount);
        model.addAttribute("maxAmount", maxAmount);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortOrder", sortOrder.toUpperCase());
        model.addAttribute("availableCategoriesForFilter", availableCategoriesForFilter);
        BigDecimal monthIncome = BigDecimal.ZERO;
        BigDecimal monthExpenses = BigDecimal.ZERO;
        if (selectedMonthTransactionsList != null) {
            for (Transaction tx : selectedMonthTransactionsList) {
                if (tx.getAmount() != null) {
                    if ("Income".equalsIgnoreCase(tx.getType())) {
                        monthIncome = monthIncome.add(BigDecimal.valueOf(tx.getAmount()));
                    } else if ("Expense".equalsIgnoreCase(tx.getType())) {
                        monthExpenses = monthExpenses.add(BigDecimal.valueOf(tx.getAmount()));
                    }
                }
            }
        }
        model.addAttribute("selectedMonthTotalIncome", monthIncome);
        model.addAttribute("selectedMonthTotalExpenses", monthExpenses);
        final int MAX_DASHBOARD_TRANSACTIONS = 5;
        List<Transaction> dashboardTransactionView;
        boolean showingLimitedTransactions = false;
        int totalTransactionsForMonthAfterFilter = (selectedMonthTransactionsList != null) ? selectedMonthTransactionsList.size() : 0;
        if (selectedMonthTransactionsList != null) {
            if (selectedMonthTransactionsList.size() > MAX_DASHBOARD_TRANSACTIONS) {
                dashboardTransactionView = selectedMonthTransactionsList.stream().limit(MAX_DASHBOARD_TRANSACTIONS).collect(Collectors.toList());
                showingLimitedTransactions = true;
            } else {
                dashboardTransactionView = selectedMonthTransactionsList;
            }
        } else {
            dashboardTransactionView = Collections.emptyList();
        }
        model.addAttribute("selectedMonthTransactionsList", dashboardTransactionView);
        model.addAttribute("showingLimitedTransactions", showingLimitedTransactions);
        model.addAttribute("totalTransactionsForMonthAfterFilter", totalTransactionsForMonthAfterFilter);
        List<String> monthNames = Arrays.stream(Month.values())
            .map(m -> m.getDisplayName(TextStyle.FULL, Locale.ENGLISH)).collect(Collectors.toList());
        model.addAttribute("availableMonths", monthNames);
        model.addAttribute("currentPage", "dashboard");
        // --- End of identical part ---

        return "dashboard"; // Return the same dashboard view
    }
    
    @GetMapping("/register")
    public String showRegisterPage(Model model) { 
        model.addAttribute("user", new User());
        return "register";
    }
    
    @PostMapping("/user/save")
    public String registerUser(@ModelAttribute User user,
                               @RequestParam("confirmPassword") String confirmPassword,
                               @RequestParam(name = "dataPrivacyCheck", required = false) String dataPrivacyCheck,
                               RedirectAttributes redi,
                               HttpServletRequest httpRequest, Model model) {

        if (user.getEmail() == null || user.getPassword() == null || user.getEmail().isEmpty() || user.getPassword().isEmpty() || confirmPassword.isEmpty()) {
            redi.addFlashAttribute("errorMessage", "❌ All fields are required.");
            return "redirect:/register";
        }
        if (!user.getPassword().equals(confirmPassword)) {
            redi.addFlashAttribute("errorMessage", "❌ Passwords do not match.");
            return "redirect:/register";
        }
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            redi.addFlashAttribute("errorMessage", "❌ Email already exists.");
            return "redirect:/register";
        }
        if (dataPrivacyCheck == null) { 
            redi.addFlashAttribute("errorMessage", "❌ You must agree to the Data Privacy Act to register.");
            return "redirect:/register"; 
        }

        try {
            userService.registerUser(user, httpRequest); 
            redi.addFlashAttribute("successMessage", "✅ Verification email sent! Please check your inbox to activate your account."); 
            return "redirect:/login"; 
        } catch (IllegalStateException e) { 
             redi.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
             return "redirect:/register";
        } catch (Exception e) { 
            logger.error("Error registering user {}: {}", user.getEmail(), e.getMessage(), e);
            redi.addFlashAttribute("errorMessage", "❌ An unexpected error occurred during registration. Please try again.");
            return "redirect:/register";
        }
    }

    @GetMapping("/forgotpassword")
    public String forgotPassword() {
        return "forgotpassword";
    }

    @PostMapping("/user/forgot")
    public String processForgotPassword(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes,
                                        HttpServletRequest httpRequest) {
        redirectAttributes.addFlashAttribute("successMessage", "If an account with that email exists, a password reset link has been sent.");
        try {
            userService.requestPasswordReset(email, httpRequest);
        } catch (Exception e) {
            logger.error("Error processing forgot password for email {}: {}", email, e.getMessage(), e);
            // Still redirect to forgotpassword with a generic message to avoid disclosing account existence.
        }
        return "redirect:/forgotpassword"; 
    }


    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(name = "token", required = false) String token, Model model, RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password reset link is invalid or missing token.");
            return "redirect:/login"; 
        }
        model.addAttribute("token", token);
        return "resetpassword"; 
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       RedirectAttributes redirectAttributes,
                                       HttpServletRequest httpRequest) { 

        if (newPassword == null || newPassword.isEmpty() || !newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match or are empty.");
            redirectAttributes.addAttribute("token", token); // Ensure token is passed back for the redirect
            return "redirect:/resetpassword";
        }
        boolean passwordUpdated = userService.resetPasswordWithToken(token, newPassword, httpRequest); 

        if (passwordUpdated) {
            redirectAttributes.addFlashAttribute("successMessage", "Password has been reset successfully. Please log in.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid or expired password reset link, or password could not be updated. Please request a new link.");
            return "redirect:/login"; 
        }
    }

    @PostMapping("/dashboard/budget/save")
    public String saveDashboardBudget(@RequestParam("amount") double amount,
                                      HttpSession session, RedirectAttributes redirectAttributes,
                                      HttpServletRequest httpRequest) {
        User user = (User) session.getAttribute("loggedInUser"); 

        if (user == null || user.getId() == null) {
             Authentication auth = SecurityContextHolder.getContext().getAuthentication();
             if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
                 CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                 Optional<User> userOpt = userService.findByEmail(userDetails.getUsername());
                 if(userOpt.isPresent()){
                     user = userOpt.get();
                     session.setAttribute("loggedInUser", user);
                 } else {
                     redirectAttributes.addFlashAttribute("errorMessage", "Session error. Please log in again.");
                     return "redirect:/login";
                 }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Please log in again.");
                return "redirect:/login";
            }
        }
        
        if (amount < 0) {
             redirectAttributes.addFlashAttribute("errorMessage", "Budget amount cannot be negative.");
             return "redirect:/dashboard"; 
        }
        try {
            budgetService.saveOrUpdateBudget(user, amount);
            activityLogService.logActivity(user, ActivityType.BUDGET_UPDATED, "Budget updated to: ₱" + String.format("%.2f", amount), httpRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Budget updated successfully!");
        } catch (Exception e) {
            logger.error("Error updating budget for user {}: {}", user.getEmail(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update budget.");
        }
        return "redirect:/dashboard"; 
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes, HttpServletRequest httpRequest) {
        User loggedInUser = (User) session.getAttribute("loggedInUser"); 
        String usernameForLog = "anonymous";

        if (loggedInUser != null && loggedInUser.getEmail() != null) {
            usernameForLog = loggedInUser.getEmail();
        } else {
             Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
             if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                 CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
                 usernameForLog = principal.getUsername();
             }
        }
        
        session.invalidate();
        SecurityContextHolder.clearContext(); 
        
        activityLogService.logActivity(null, ActivityType.USER_REGISTERED, "User " + usernameForLog + " logged out.", httpRequest); // Corrected ActivityType
        
        redirectAttributes.addFlashAttribute("logoutMessage", "You have been logged out successfully.");
        return "redirect:/login?logout=true"; 
    }

    @GetMapping("/about-us")
    public String aboutUsPage(HttpSession session, Model model, HttpServletRequest httpRequest) { // Add HttpServletRequest
        model.addAttribute("currentPage", "about-us");
        model.addAttribute("appContextPath", httpRequest.getContextPath()); // Add this line
        return "about-us";
    }
}