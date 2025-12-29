package com.appdev.Finance.Repository;

import com.appdev.Finance.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdAndMonth(Long userId, String month);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'Income' AND t.month = :month")
    Double findTotalIncomeByUserForMonth(@Param("userId") Long userId, @Param("month") String month);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'Expense' AND t.month = :month")
    Double findTotalExpensesByUserForMonth(@Param("userId") Long userId, @Param("month") String month);

    // New method for filtering by user, month, and category (case-insensitive for category)
    List<Transaction> findByUserIdAndMonthAndCategoryIgnoreCase(Long userId, String month, String category);

    // New method to get distinct categories for a user within a specific month
    @Query("SELECT DISTINCT t.category FROM Transaction t WHERE t.user.id = :userId AND t.month = :month ORDER BY t.category ASC")
    List<String> findDistinctCategoriesByUserIdAndMonth(@Param("userId") Long userId, @Param("month") String month);
}