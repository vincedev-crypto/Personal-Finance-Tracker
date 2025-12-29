package com.appdev.Finance.Repository;

import com.appdev.Finance.model.User;
import com.appdev.Finance.model.VerificationToken;
import com.appdev.Finance.model.TokenType; // Make sure this import is correct
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // For named parameters in @Query
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List; // Import List
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    // This method is used in UserService
    Optional<VerificationToken> findByTokenAndTokenType(String token, TokenType tokenType);

    // This method is used in UserService
    List<VerificationToken> findAllByUserAndTokenTypeAndUsedFalseAndExpiryDateAfter(User user, TokenType tokenType, LocalDateTime now);

    void deleteByExpiryDateBefore(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.used = true AND t.expiryDate < :referenceDate")
    void deleteUsedAndExpiredTokens(@Param("referenceDate") LocalDateTime referenceDate);

    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.user = :user AND t.tokenType = :tokenType")
    void deleteAllByUserAndTokenType(@Param("user") User user, @Param("tokenType") TokenType tokenType);
}