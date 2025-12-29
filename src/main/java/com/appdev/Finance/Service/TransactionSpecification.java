package com.appdev.Finance.Service;

import com.appdev.Finance.model.Transaction;
import com.appdev.Finance.model.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set; // Keep for potential future use, but tagNames parameter is removed

public class TransactionSpecification {

    public static Specification<Transaction> findByCriteria(
            User user,
            String keyword,
            String transactionType,
            String category,
            String month,
            String startDateStr,
            String endDateStr,
            Double minAmount,
            Double maxAmount,
            Set<String> ignoredTagNames) { // Parameter kept for signature consistency, but not used for tags

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("user"), user));

            if (StringUtils.hasText(keyword)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + keyword.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(transactionType)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("type")), transactionType.toLowerCase()));
            }

            if (StringUtils.hasText(category)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.toLowerCase()));
            }

            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            boolean dateRangeProvided = false;

            if (StringUtils.hasText(startDateStr)) {
                try {
                    LocalDate startDate = LocalDate.parse(startDateStr, formatter);
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
                    dateRangeProvided = true;
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid start date format: " + startDateStr + ". Expected yyyy-MM-dd.");
                }
            }

            if (StringUtils.hasText(endDateStr)) {
                try {
                    LocalDate endDate = LocalDate.parse(endDateStr, formatter);
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), endDate));
                    dateRangeProvided = true;
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid end date format: " + endDateStr + ". Expected yyyy-MM-dd.");
                }
            }

            if (!dateRangeProvided && StringUtils.hasText(month)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("month")), month.toLowerCase()));
            }

            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }

            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            // Tag filtering logic is removed.
            // The 'ignoredTagNames' parameter is present but not used.

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}