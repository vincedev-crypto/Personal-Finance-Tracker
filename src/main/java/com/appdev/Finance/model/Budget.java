package com.appdev.Finance.model;

import jakarta.persistence.*;
import java.math.BigDecimal; // Use BigDecimal for currency

@Entity
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Use standard ID generation
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2) // Use BigDecimal for precision
    private BigDecimal amount; // Renamed from static field

    @OneToOne // Assumes one budget per user. Use @ManyToOne if a user can have multiple/historical budgets.
    @JoinColumn(name = "user_id", nullable = false, unique = true) // Ensure one budget per user if OneToOne
    private User user;

    // Constructors
    public Budget() {
        this.amount = BigDecimal.ZERO; // Default to zero
    }

    public Budget(BigDecimal amount, User user) {
        this.amount = amount;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        // Ensure amount is not null, return ZERO if it is (optional, depends on DB constraints)
        return (this.amount == null) ? BigDecimal.ZERO : this.amount;
    }

    // Accept double for convenience, but store as BigDecimal
    public void setAmount(double amount) {
        this.amount = BigDecimal.valueOf(amount);
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}