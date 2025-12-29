package com.appdev.Finance.Service;

import com.appdev.Finance.Repository.BudgetRepository;
import com.appdev.Finance.Repository.TransactionRepository;
import com.appdev.Finance.Repository.ReportRepository;
import com.appdev.Finance.model.Budget;
import com.appdev.Finance.model.User;
// Assuming Notification model and NotificationService are in their correct packages

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
// import java.util.HashMap; // Not strictly needed if only using ConcurrentHashMap
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BudgetService {

    private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);

    private final BudgetRepository budgetRepository;
    private final NotificationService notificationService; // Already injected
    private final TransactionRepository transactionRepository;
    private final ReportRepository reportRepository;

    private final Map<String, Long> recentNotifications = new ConcurrentHashMap<>();
    private static final long NOTIFICATION_COOLDOWN_MS = 1000 * 60 * 60 * 24; // 24 hours

    @Autowired
    public BudgetService(BudgetRepository budgetRepository,
                         NotificationService notificationService,
                         TransactionRepository transactionRepository,
                         ReportRepository reportRepository) {
        this.budgetRepository = budgetRepository;
        this.notificationService = notificationService;
        this.transactionRepository = transactionRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public Budget getBudgetForUser(User user) {
        if (user == null || user.getId() == null) {
            logger.warn("getBudgetForUser called with null user or user ID.");
        }
        logger.debug("Fetching budget for user ID: {}", user != null ? user.getId() : "null");
        return budgetRepository.findByUser(user)
                .orElseGet(() -> {
                    logger.info("No budget found for user ID: {}. Returning new default budget (0.00).", user != null ? user.getId() : "null");
                    // Ensure user is not null if creating new Budget here, or handle appropriately
                    return new Budget(BigDecimal.ZERO, user); 
                });
    }

    @Transactional
    public void saveOrUpdateBudget(User user, double amount) {
        if (user == null || user.getId() == null) {
            logger.error("saveOrUpdateBudget called with null user or user ID.");
            throw new IllegalArgumentException("User cannot be null when saving or updating a budget.");
        }
        logger.info("Saving or updating budget for user ID: {} to amount: {}", user.getId(), amount);
        Budget budget = budgetRepository.findByUser(user)
                           .orElse(new Budget(BigDecimal.ZERO, user)); 

        budget.setAmount(amount); 
        budget.setUser(user); 
        budgetRepository.save(budget);
        logger.info("Budget saved/updated for user ID: {}. New amount: {}", user.getId(), budget.getAmount());

        checkBudgetThresholds(user, true);
    }

    public void afterExpenseTransactionUpdate(User user) {
        if (user == null || user.getId() == null) {
            logger.warn("afterExpenseTransactionUpdate called with null user or user ID. Skipping budget check.");
            return;
        }
        logger.debug("After expense update for user ID: {}. Checking budget thresholds.", user.getId());
        checkBudgetThresholds(user, true);
    }

    public void checkBudgetThresholds(User user, boolean isMonthlyBudget) {
        if (user == null || user.getId() == null) {
            logger.warn("checkBudgetThresholds: User or User ID is null. Cannot check budget.");
            return;
        }

        Budget budget = getBudgetForUser(user);

        if (budget == null || budget.getAmount() == null || budget.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            logger.info("checkBudgetThresholds: No active budget (amount > 0) found for user ID: {}. Skipping notification checks.", user.getId());
            return;
        }

        BigDecimal budgetAmount = budget.getAmount();
        BigDecimal expensesBigDecimal;
        String expensePeriodMessage = "";

        logger.debug("checkBudgetThresholds for User ID: {}. Budget Amount: {}", user.getId(), budgetAmount);

        if (isMonthlyBudget) {
            String currentMonth = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            expensePeriodMessage = "for " + currentMonth;
            Double monthlyExpenses = reportRepository.findTotalExpensesByUserForMonth(user.getId(), currentMonth);
            if (monthlyExpenses == null) monthlyExpenses = 0.0;
            expensesBigDecimal = BigDecimal.valueOf(monthlyExpenses);
            logger.debug("User ID: {}. Monthly ({}) Expenses: {}", user.getId(), currentMonth, expensesBigDecimal);
        } else {
            expensePeriodMessage = "overall";
            Double totalExpenses = transactionRepository.findTotalExpensesByUser_Id(user.getId());
            if (totalExpenses == null) totalExpenses = 0.0;
            expensesBigDecimal = BigDecimal.valueOf(totalExpenses);
            logger.debug("User ID: {}. Total Overall Expenses: {}", user.getId(), expensesBigDecimal);
        }

        BigDecimal eightyPercentThreshold = budgetAmount.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ninetyFivePercentThreshold = budgetAmount.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);

        logger.debug("User ID: {}. Thresholds - 80%: {}, 95%: {}, Budget: {}", user.getId(), eightyPercentThreshold, ninetyFivePercentThreshold, budgetAmount);

        String budgetPageLink = "/dashboard";

        if (expensesBigDecimal.compareTo(budgetAmount) >= 0) {
            logger.info("User ID: {}. Condition MET: Expenses ({}) >= Budget ({}). Checking cooldown for EXCEEDED.", user.getId(), expensesBigDecimal, budgetAmount);
            if (canSendNotification(user.getId(), "EXCEEDED")) {
                String message = String.format("⚠️ Warning: You have exceeded %s budget of ₱%,.2f! Current expenses %s: ₱%,.2f.",
                                               (isMonthlyBudget ? "your monthly" : "your"), budgetAmount, expensePeriodMessage, expensesBigDecimal);
                logger.info("User ID: {}. Sending EXCEEDED notification: {}", user.getId(), message);
                // MODIFIED: Call createAndSendNotification for WebSocket push
                notificationService.createAndSendNotification(user, message, budgetPageLink);
                updateNotificationTimestamp(user.getId(), "EXCEEDED");
            } else {
                logger.info("User ID: {}. EXCEEDED notification cooldown active.", user.getId());
            }
        }
        else if (expensesBigDecimal.compareTo(ninetyFivePercentThreshold) >= 0) {
            logger.info("User ID: {}. Condition MET: Expenses ({}) >= 95% Threshold ({}). Checking cooldown for NEAR_95.", user.getId(), expensesBigDecimal, ninetyFivePercentThreshold);
            if (canSendNotification(user.getId(), "NEAR_95")) {
                 String message = String.format("❗ Alert: You have used over 95%% of %s budget (₱%,.2f). Current expenses %s: ₱%,.2f.",
                                               (isMonthlyBudget ? "your monthly" : "your"), budgetAmount, expensePeriodMessage, expensesBigDecimal);
                logger.info("User ID: {}. Sending NEAR_95 notification: {}", user.getId(), message);
                // MODIFIED: Call createAndSendNotification for WebSocket push
                notificationService.createAndSendNotification(user, message, budgetPageLink);
                updateNotificationTimestamp(user.getId(), "NEAR_95");
            } else {
                 logger.info("User ID: {}. NEAR_95 notification cooldown active.", user.getId());
            }
        }
        else if (expensesBigDecimal.compareTo(eightyPercentThreshold) >= 0) {
            logger.info("User ID: {}. Condition MET: Expenses ({}) >= 80% Threshold ({}). Checking cooldown for NEAR_80.", user.getId(), expensesBigDecimal, eightyPercentThreshold);
             if (canSendNotification(user.getId(), "NEAR_80")) {
                String message = String.format("ℹ️ Info: You have used over 80%% of %s budget (₱%,.2f). Current expenses %s: ₱%,.2f.",
                                               (isMonthlyBudget ? "your monthly" : "your"), budgetAmount, expensePeriodMessage, expensesBigDecimal);
                logger.info("User ID: {}. Sending NEAR_80 notification: {}", user.getId(), message);
                // MODIFIED: Call createAndSendNotification for WebSocket push
                notificationService.createAndSendNotification(user, message, budgetPageLink);
                updateNotificationTimestamp(user.getId(), "NEAR_80");
             } else {
                logger.info("User ID: {}. NEAR_80 notification cooldown active.", user.getId());
             }
        } else {
            logger.info("User ID: {}. No budget thresholds met. Expenses: {}, Budget: {}", user.getId(), expensesBigDecimal, budgetAmount);
        }
    }

    private boolean canSendNotification(Long userId, String thresholdType) {
        String key = userId + "_" + thresholdType;
        long lastNotificationTime = recentNotifications.getOrDefault(key, 0L);
        boolean canSend = (System.currentTimeMillis() - lastNotificationTime) > NOTIFICATION_COOLDOWN_MS;
        if (!canSend) {
            logger.debug("Cooldown active for User ID: {}, Threshold Type: {}. Last sent: {}ms ago.", userId, thresholdType, (System.currentTimeMillis() - lastNotificationTime));
        }
        return canSend;
    }

    private void updateNotificationTimestamp(Long userId, String thresholdType) {
        String key = userId + "_" + thresholdType;
        recentNotifications.put(key, System.currentTimeMillis());
        logger.debug("Updated notification timestamp for User ID: {}, Threshold Type: {}.", userId, thresholdType);
    }

    @Transactional
    public void saveBudget(Budget budget) {
       if (budget.getUser() == null) {
           logger.error("Attempted to save budget with null user.");
           throw new IllegalArgumentException("Budget must be associated with a user before saving.");
       }
        budgetRepository.save(budget);
        logger.info("Budget explicitly saved for user ID: {}. Amount: {}", budget.getUser().getId(), budget.getAmount());
    }
}
