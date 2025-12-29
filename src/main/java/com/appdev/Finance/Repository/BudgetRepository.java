package com.appdev.Finance.Repository;

import com.appdev.Finance.model.Budget;
import com.appdev.Finance.model.User; // Import User
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Add Repository annotation

import java.util.Optional; // Use Optional

@Repository // Add this annotation
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // Method to find a budget associated with a specific user
    // Assumes OneToOne relationship in Budget model
    Optional<Budget> findByUser(User user);

    // Optional: Find by user ID directly if needed
    Optional<Budget> findByUserId(Long userId);

}