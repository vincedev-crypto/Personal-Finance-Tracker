package com.appdev.Finance.model; // Or com.appdev.Finance.model.enums

public enum ActivityType {
    LOGIN_SUCCESS("User logged in successfully"),
    LOGIN_FAILURE("Failed login attempt"),
    PASSWORD_CHANGE("Password changed successfully"),
    PASSWORD_RESET_REQUEST("Password reset requested"),
    PASSWORD_RESET_SUCCESS("Password reset successfully"),
    EMAIL_VERIFIED("Email verified successfully"),
    USER_REGISTERED("New user registered"),
    BUDGET_UPDATED("Budget updated"),
    TRANSACTION_ADDED("New transaction added"),
    TRANSACTION_UPDATED("Transaction updated"),
    TRANSACTION_DELETED("Transaction deleted");
    // Add more types as needed

    private final String description;

    ActivityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}