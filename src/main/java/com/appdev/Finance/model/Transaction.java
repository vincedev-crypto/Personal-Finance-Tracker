package com.appdev.Finance.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String type; // Income or Expense

    @Column(nullable = false)
    private String category; // Entertainment, Food, etc.

    @Column(nullable = false)
    private String month; // January, February, etc. Derived from transactionDate

    @Column(nullable = true)
    private LocalDate transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TransactionFile> transactionFiles = new HashSet<>();


    public Transaction() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Set<TransactionFile> getTransactionFiles() {
        return transactionFiles;
    }

    public void setTransactionFiles(Set<TransactionFile> transactionFiles) {
        this.transactionFiles = transactionFiles;
    }

    // Helper methods
    public void addTransactionFile(TransactionFile file) {
        this.transactionFiles.add(file);
        file.setTransaction(this);
    }

    public void removeTransactionFile(TransactionFile file) {
        this.transactionFiles.remove(file);
        file.setTransaction(null);
    }
}