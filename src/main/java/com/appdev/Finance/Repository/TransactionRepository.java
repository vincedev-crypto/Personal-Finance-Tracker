package com.appdev.Finance.Repository;

import com.appdev.Finance.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph; // Ensure this is imported

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'Income'")
    Double findTotalIncomeByUser_Id(@Param("userId") Long userId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'Expense'")
    Double findTotalExpensesByUser_Id(@Param("userId") Long userId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = 'Income'")
    Double getTotalIncome();

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = 'Expense'")
    Double getTotalExpenses();

    @EntityGraph(attributePaths = {"user", "transactionFiles"})
    List<Transaction> findByMonth(String month);

    @EntityGraph(attributePaths = {"user", "transactionFiles"})
    List<Transaction> findByUserId(Long userId);
  
    List<Transaction> findByUserIdAndCategoryIgnoreCase(Long userId, String category);

    @Query("SELECT DISTINCT t.category FROM Transaction t WHERE t.user.id = :userId ORDER BY t.category ASC")
    List<String> findDistinctCategoriesByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user", "transactionFiles"})
    List<Transaction> findByUserIdAndMonthIgnoreCaseOrderByIdDesc(Long userId, String month);

    /**
     * Re-declares the findAll method from JpaSpecificationExecutor
     * to apply an EntityGraph for eagerly fetching associated entities.
     * This method is used by TransactionService's searchTransactions.
     */
    @Override
    @EntityGraph(attributePaths = {"user", "transactionFiles"})
    List<Transaction> findAll(Specification<Transaction> spec, Sort sort);
    
    /**
     * Re-declares the findAll method (without sort) from JpaSpecificationExecutor
     * to apply an EntityGraph.
     */
    @Override
    @EntityGraph(attributePaths = {"user", "transactionFiles"})
    List<Transaction> findAll(Specification<Transaction> spec);

    /**
     * Re-declares the findAll method with Pageable from JpaSpecificationExecutor/PagingAndSortingRepository
     * to apply an EntityGraph. Useful if you implement pagination for transaction lists showing files.
     */
    @Override
    @EntityGraph(attributePaths = {"user", "transactionFiles"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    // If you directly use findAll(Pageable) without Specification and need files:
    @Override
    @EntityGraph(attributePaths = {"user", "transactionFiles"})
    Page<Transaction> findAll(Pageable pageable);
}