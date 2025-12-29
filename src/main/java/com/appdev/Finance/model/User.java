package com.appdev.Finance.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "user") // Good practice to explicitly name the table
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean enabled = false;

    // REMOVED: verificationToken
    // REMOVED: resetPasswordToken
    // REMOVED: resetPasswordTokenExpiry

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    // Consider adding a bidirectional relationship to VerificationToken if needed, though often not necessary
    // @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<VerificationToken> verificationTokens;


    public User() {}

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.verified = false; // Initial state
        this.enabled = false;  // Initial state
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) { // Ensure createdAt is only set once
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters & Setters (excluding the removed token fields)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // getPassword() is part of UserDetails

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // setCreatedAt is usually not needed if managed by @PrePersist or DB default

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) { // Corrected parameter name from 'i' to 'verified'
        this.verified = verified;
    }

    // isEnabled() is part of UserDetails

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Getters and Setters for removed token fields should be deleted.

    public List<Transaction> getTransactions() {
        // Potential infinite recursion fix:
        // return this.transactions; // Directly return the field
        // Or if you intended specific logic:
        // For now, let's assume direct return is fine. If there was custom logic, it needs review.
        // The original code had `return getTransactions();` which is a recursive call.
        return this.transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    // This getType() method seems out of place for a User entity.
    // If it's not used or intended for something specific, it can be removed.
    // For now, I'll keep it as per your provided code but comment it out.
    /*
    public Object getType() {
        // TODO Auto-generated method stub
        return null;
    }
    */

    // UserDetails methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // Or map roles/permissions if you add them
    }

    @Override
    public String getPassword() { // Already have this getter
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Or implement logic if accounts can expire
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Or implement logic for account locking
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Or implement logic if credentials can expire
    }

    @Override
    public boolean isEnabled() { // Already have this getter
        return enabled;
    }
}