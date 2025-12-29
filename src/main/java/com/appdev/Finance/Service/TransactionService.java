package com.appdev.Finance.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
// Set is not used in this version of the file, but can be kept if other methods might use it.
// import java.util.Set;
// import java.util.stream.Collectors; // Not directly used in this version

import com.appdev.Finance.model.User;
import com.appdev.Finance.model.TransactionFile;
import com.appdev.Finance.Repository.TransactionFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.appdev.Finance.Repository.TransactionRepository;
import com.appdev.Finance.model.Transaction;
// TransactionSpecification import is fine
// import com.appdev.Finance.Service.TransactionSpecification;

import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private TransactionFileRepository transactionFileRepository;


    @PostConstruct
    public void init() {
        logger.info("TransactionRepository initialized: {}", transactionRepository);
        logger.info("BudgetService initialized in TransactionService: {}", budgetService);
        logger.info("FileStorageService initialized in TransactionService: {}", fileStorageService);
        logger.info("TransactionFileRepository initialized in TransactionService: {}", transactionFileRepository);
    }

    @Transactional
    public Transaction saveTransaction(Transaction transaction, MultipartFile fileReceipt) {
        if (transaction == null) {
            logger.error("Attempted to save a null transaction.");
            throw new IllegalArgumentException("Transaction cannot be null.");
        }
        if (transaction.getId() != null) {
            logger.error("Attempted to save a transaction that already has an ID. New transactions should not have an ID.");
            // Changed to new transactions to avoid confusion, as updates are not handled here.
            throw new IllegalArgumentException("New transactions should not have an ID. For updates, use a different method.");
        }
        if (transaction.getUser() == null) {
            logger.error("User not set on the transaction object before saving.");
            throw new IllegalStateException("Transaction must be associated with a user.");
        }

        logger.info("Attempting to save new transaction for User ID: {}, Amount: {}, Type: {}, Category: {}, Date: [{}], Month: [{}]",
                transaction.getUser().getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getTransactionDate(),
                transaction.getMonth());

        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("New transaction saved with ID: {}", savedTransaction.getId());

        // Handle file upload
        if (fileReceipt != null && !fileReceipt.isEmpty()) {
            try {
                String storedFileName = fileStorageService.storeFile(fileReceipt);
                TransactionFile transactionFile = new TransactionFile(
                        fileReceipt.getOriginalFilename(),
                        storedFileName,
                        fileReceipt.getContentType(),
                        storedFileName, // Assuming filePath is just the storedFileName relative to base upload dir
                        savedTransaction
                );
                transactionFile.setTransaction(savedTransaction);
                transactionFileRepository.save(transactionFile);

                // If Transaction entity's transactionFiles collection is managed (e.g., with addTransactionFile helper)
                // and uses CascadeType that includes PERSIST or MERGE from Transaction to TransactionFile,
                // you might add it to the collection and save the transaction again, or rely on cascading.
                // savedTransaction.addTransactionFile(transactionFile); // Assuming such method exists and handles bidirectional
                // transactionRepository.save(savedTransaction); // If changes to savedTransaction are made after initial save

                logger.info("Saved file {} for transaction ID {}", storedFileName, savedTransaction.getId());
            } catch (Exception e) {
                logger.error("Could not store file for transaction ID {}: {}", savedTransaction.getId(), e.getMessage(), e);
                // Consider if this error should cause the transaction to roll back
                // For now, it logs the error and the transaction remains saved.
            }
        }


        if ("Expense".equalsIgnoreCase(savedTransaction.getType())) {
            logger.info("Expense transaction saved (ID: {}). Triggering budget check for user ID: {}",
                    savedTransaction.getId(), savedTransaction.getUser().getId());
            try {
                budgetService.afterExpenseTransactionUpdate(savedTransaction.getUser());
            } catch (Exception e) {
                logger.error("Error calling budgetService.afterExpenseTransactionUpdate for user ID {}: {}",
                             savedTransaction.getUser().getId(), e.getMessage(), e);
            }
        }
        return savedTransaction;
    }
    @Transactional(readOnly = true)
    public List<Transaction> searchTransactions(
            User user,
            String keyword,
            String transactionType,
            String category,
            String month,
            String startDate,
            String endDate,
            Double minAmount,
            Double maxAmount,
            // Set<String> tagNames, 
            String sortField,
            String sortOrder) { // <<< REMOVE ", String string" from here

        logger.debug("Searching transactions for user: {} with criteria - keyword: '{}', type: '{}', category: '{}', month: '{}', startDate: '{}', endDate: '{}', minAmt: {}, maxAmt: {}, sort: {} {}",
                user.getEmail(), keyword, transactionType, category, month, startDate, endDate, minAmount, maxAmount, sortField, sortOrder);

        Specification<Transaction> spec = TransactionSpecification.findByCriteria(
                user, keyword, transactionType, category, month, startDate, endDate, minAmount, maxAmount, null
        );

        // Default to DESC if sortOrder is null or empty, or invalid
        Sort.Direction direction = (sortOrder == null || sortOrder.trim().isEmpty() || !"asc".equalsIgnoreCase(sortOrder))
                                   ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Ensure sortField is a valid property of Transaction to prevent HQL injection or errors
        // Added more robust checking for null/empty sortField
        String validSortField = (sortField == null || sortField.trim().isEmpty()) ? "transactionDate" : sortField;
        List<String> allowedSortFields = Arrays.asList("id", "description", "amount", "transactionDate", "type", "category", "month");
        if (!allowedSortFields.contains(validSortField)) {
            logger.warn("Invalid sortField provided: '{}'. Defaulting to 'transactionDate'.", validSortField);
            validSortField = "transactionDate"; // Default to transactionDate if invalid
        }

        Sort sort = Sort.by(direction, validSortField);

        List<Transaction> transactions = transactionRepository.findAll(spec, sort);

        return transactions;
    }
    
    @Transactional(readOnly = true)
    public TransactionFile getTransactionFileByIdAndUser(Long fileId, User user) {
        if (fileId == null || user == null || user.getId() == null) {
            logger.warn("getTransactionFileByIdAndUser called with null fileId or user/userId.");
            return null;
        }
        Optional<TransactionFile> fileOpt = transactionFileRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            TransactionFile tf = fileOpt.get();
            if (tf.getTransaction() != null && tf.getTransaction().getUser() != null &&
                tf.getTransaction().getUser().getId().equals(user.getId())) {
                return tf;
            } else {
                logger.warn("User {} attempted to access file ID {} not owned by them or with missing transaction/user link.", user.getEmail(), fileId);
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByUserId(Long userId) {
        if (userId == null) {
            logger.warn("getTransactionsByUserId called with null userId.");
            return List.of();
        }
        return transactionRepository.findByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Transaction> getTransactionByIdAndUser(Long id, User user) {
        if (id == null || user == null || user.getId() == null) {
            logger.warn("getTransactionByIdAndUser called with null id or user/userId.");
            return Optional.empty();
        }
        return transactionRepository.findById(id)
                .filter(transaction -> transaction.getUser().getId().equals(user.getId()));
    }
    
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        logger.warn("getAllTransactions called - this is not user-specific and might expose all data. Ensure this is intended for admin or specific use cases.");
        return transactionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByUserIdAndMonth(Long userId, String month) {
        if (userId == null || month == null || month.trim().isEmpty()) {
            logger.warn("getTransactionsByUserIdAndMonth called with invalid parameters. UserId: {}, Month: {}", userId, month);
            return List.of();
        }
        logger.debug("Service: Fetching transactions for userId {} and month {}", userId, month);
        return transactionRepository.findByUserIdAndMonthIgnoreCaseOrderByIdDesc(userId, month);
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long id) {
         if (id == null) {
            logger.warn("getTransactionById called with null ID.");
            throw new IllegalArgumentException("Transaction ID cannot be null.");
        }
        return transactionRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Transaction not found with ID: {}", id);
                    return new RuntimeException("Transaction not found with ID: " + id); // Consider a custom, more specific exception
                });
    }

    @Transactional(readOnly = true)
    public double findTotalIncomeByUserId(Long userId) {
        if (userId == null) {
            logger.warn("findTotalIncomeByUserId called with null userId.");
            return 0.0;
        }
        Double income = transactionRepository.findTotalIncomeByUser_Id(userId);
        return (income != null) ? income : 0.0;
    }

    @Transactional(readOnly = true)
    public double findTotalExpensesByUserId(Long userId) {
        if (userId == null) {
            logger.warn("findTotalExpensesByUserId called with null userId.");
            return 0.0;
        }
        Double expenses = transactionRepository.findTotalExpensesByUser_Id(userId);
        return (expenses != null) ? expenses : 0.0;
    }

    @Transactional(readOnly = true)
    public double getTotalIncome() {
        logger.warn("getTotalIncome called - this is not user-specific.");
        Double income = transactionRepository.getTotalIncome();
        return (income != null) ? income : 0.0;
    }

    @Transactional(readOnly = true)
    public double getTotalExpenses() {
        logger.warn("getTotalExpenses called - this is not user-specific.");
        Double expenses = transactionRepository.getTotalExpenses();
        return (expenses != null) ? expenses : 0.0;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByMonth(String month) {
         if (month == null || month.trim().isEmpty()) {
            logger.warn("getTransactionsByMonth called with null or empty month.");
            return List.of();
        }
        logger.warn("getTransactionsByMonth called - this is not user-specific. Prefer getTransactionsByUserIdAndMonth.");
        return transactionRepository.findByMonth(month);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByUserIdAndCategory(Long userId, String category) {
        if (userId == null || category == null || category.trim().isEmpty()) {
             logger.warn("getTransactionsByUserIdAndCategory called with null/empty userId or category.");
            return List.of();
        }
        return transactionRepository.findByUserIdAndCategoryIgnoreCase(userId, category);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctCategoriesByUserId(Long userId) {
        if (userId == null) {
            logger.warn("getDistinctCategoriesByUserId called with null userId.");
            return List.of();
        }
        return transactionRepository.findDistinctCategoriesByUserId(userId);
    }
}