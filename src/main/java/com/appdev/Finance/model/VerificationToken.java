package com.appdev.Finance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
// Removed import java.util.List; as it's not used here

@Entity
public class VerificationToken {

    private static final int DEFAULT_EXPIRATION_MINUTES = 60 * 24; // 24 hours

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) // Ensure token string is unique
    private String token;

    @ManyToOne(fetch = FetchType.EAGER) // EAGER can be simpler here, LAZY if performance becomes an issue
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING) // Store enum as String
    @Column(nullable = false)
    private TokenType tokenType;  // Added TokenType

    @Column(nullable = false)
    private boolean used = false; // Added used flag, defaults to false

    // No-Arg Constructor (Required by JPA)
    public VerificationToken() {
        this.createdAt = LocalDateTime.now();
    }

    // Constructor for creating a new token with default expiration
    public VerificationToken(String token, User user, TokenType tokenType) {
        this(); // Calls the no-arg constructor to set createdAt
        this.token = token;
        this.user = user;
        this.tokenType = tokenType;
        this.expiryDate = calculateExpiryDate(DEFAULT_EXPIRATION_MINUTES);
    }

    // Constructor allowing custom expiration time
    public VerificationToken(String token, User user, TokenType tokenType, int expirationTimeInMinutes) {
        this(); // Calls the no-arg constructor
        this.token = token;
        this.user = user;
        this.tokenType = tokenType;
        this.expiryDate = calculateExpiryDate(expirationTimeInMinutes);
    }

    private LocalDateTime calculateExpiryDate(int expirationTimeInMinutes) {
        return this.createdAt.plusMinutes(expirationTimeInMinutes);
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public boolean isUsed() { // Correct implementation for isUsed
        return used;
    }

    public void setUsed(boolean used) { // Correct setter for the 'used' field
        this.used = used;
    }

    @Transient // Calculated field, not persisted directly
    public boolean isExpired() { // Correct implementation for isExpired
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    // Removed static methods like save, delete, deleteByExpiryDateBefore etc.
    // These operations belong to the VerificationTokenRepository.
    // Removed setUser(boolean b) as it was incorrect.
}