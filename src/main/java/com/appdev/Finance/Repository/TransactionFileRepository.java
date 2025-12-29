package com.appdev.Finance.Repository;

import com.appdev.Finance.model.TransactionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionFileRepository extends JpaRepository<TransactionFile, Long> {
    Optional<TransactionFile> findByStoredFileName(String storedFileName);
    List<TransactionFile> findByTransactionId(Long transactionId);
}