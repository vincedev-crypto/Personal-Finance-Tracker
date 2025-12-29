package com.appdev.Finance.Service;

import com.appdev.Finance.model.User;
import com.appdev.Finance.model.VerificationToken;
import com.appdev.Finance.model.TokenType;
import com.appdev.Finance.model.ActivityType; // Ensure this path is correct
import com.appdev.Finance.Repository.UserRepository;
import com.appdev.Finance.Repository.VerificationTokenRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailUtil mailutil;

    @Autowired
    private ActivityLogService activityLogService;

    private static final int TOKEN_EXPIRATION_MINUTES = 5; // Changed to 5 minutes

    @Transactional
    public User registerUser(User user, HttpServletRequest httpRequest) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already in use.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setVerified(false);
        user.setEnabled(false);
        User savedUser = userRepository.saveAndFlush(user);

        invalidateExistingTokens(savedUser, TokenType.EMAIL_VERIFICATION);

        String tokenString = UUID.randomUUID().toString();
        VerificationToken verificationTokenEntity = new VerificationToken(tokenString, savedUser, TokenType.EMAIL_VERIFICATION, TOKEN_EXPIRATION_MINUTES);
        verificationTokenRepository.save(verificationTokenEntity);

        mailutil.sendVerificationEmailViaMailSenderAsync(savedUser.getEmail(), tokenString);
        activityLogService.logActivity(savedUser, ActivityType.USER_REGISTERED, httpRequest);

        return savedUser;
    }

    private void invalidateExistingTokens(User user, TokenType tokenType) {
        List<VerificationToken> existingTokens = verificationTokenRepository.findAllByUserAndTokenTypeAndUsedFalseAndExpiryDateAfter(user, tokenType, LocalDateTime.now());
        for (VerificationToken token : existingTokens) {
            token.setUsed(true);
        }
        if (!existingTokens.isEmpty()) {
            verificationTokenRepository.saveAll(existingTokens);
        }
    }

    @Transactional
    public boolean verifyEmail(String tokenString, HttpServletRequest httpRequest) {
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByTokenAndTokenType(tokenString, TokenType.EMAIL_VERIFICATION);

        if (!tokenOpt.isPresent()) {
            return false;
        }

        VerificationToken verificationToken = tokenOpt.get();
        User user = verificationToken.getUser();

        if (user == null) {
            verificationTokenRepository.delete(verificationToken);
            return false;
        }

        if (verificationToken.isUsed()) {
            activityLogService.logActivity(user, ActivityType.EMAIL_VERIFIED, "Attempt to use already used verification token.", httpRequest);
            return false; // Or true if already verified means success for this flow
        }
        if (verificationToken.isExpired()) {
            activityLogService.logActivity(user, ActivityType.EMAIL_VERIFIED, "Attempt to use expired verification token.", httpRequest);
            // Do not delete here, so the controller can identify it as expired
            return false;
        }

        if (user.isVerified()) {
            activityLogService.logActivity(user, ActivityType.EMAIL_VERIFIED, "Account already verified. Token consumed.", httpRequest);
            verificationToken.setUsed(true);
            verificationTokenRepository.save(verificationToken);
            return true;
        }

        user.setVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        activityLogService.logActivity(user, ActivityType.EMAIL_VERIFIED, "Email successfully verified.", httpRequest);
        return true;
    }

    @Transactional
    public void requestPasswordReset(String email, HttpServletRequest httpRequest) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            invalidateExistingTokens(user, TokenType.PASSWORD_RESET);

            String tokenString = UUID.randomUUID().toString();
            VerificationToken passwordResetTokenEntity = new VerificationToken(tokenString, user, TokenType.PASSWORD_RESET, TOKEN_EXPIRATION_MINUTES);
            verificationTokenRepository.save(passwordResetTokenEntity);

            mailutil.sendResetPasswordEmail(user.getEmail(), tokenString);
            activityLogService.logActivity(user, ActivityType.PASSWORD_RESET_REQUEST, httpRequest);
        } else {
            activityLogService.logActivity(null, ActivityType.PASSWORD_RESET_REQUEST, "Password reset requested for non-existent email: " + email, httpRequest);
        }
    }

    @Transactional
    public boolean resetPasswordWithToken(String tokenString, String newPassword, HttpServletRequest httpRequest) {
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByTokenAndTokenType(tokenString, TokenType.PASSWORD_RESET);

        if (!tokenOpt.isPresent()) {
            return false;
        }

        VerificationToken passwordResetToken = tokenOpt.get();
        User user = passwordResetToken.getUser();

        if (user == null) {
            verificationTokenRepository.delete(passwordResetToken);
            return false;
        }

        if (passwordResetToken.isUsed()) {
            activityLogService.logActivity(user, ActivityType.PASSWORD_RESET_SUCCESS, "Attempt to use already used password reset token (FAIL).", httpRequest);
            return false;
        }
        if (passwordResetToken.isExpired()) {
            activityLogService.logActivity(user, ActivityType.PASSWORD_RESET_SUCCESS, "Attempt to use expired password reset token (FAIL).", httpRequest);
            verificationTokenRepository.delete(passwordResetToken); // Clean up expired
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setEnabled(true);
        user.setVerified(true);
        userRepository.save(user);

        passwordResetToken.setUsed(true);
        verificationTokenRepository.save(passwordResetToken);

        activityLogService.logActivity(user, ActivityType.PASSWORD_RESET_SUCCESS, httpRequest);
        return true;
    }

    // Method to get token details, useful for the controller to check status
    public Optional<VerificationToken> getTokenByStringAndType(String tokenString, TokenType tokenType) {
        return verificationTokenRepository.findByTokenAndTokenType(tokenString, tokenType);
    }

    @Transactional
    public boolean resendVerificationEmail(String email, HttpServletRequest httpRequest) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.isVerified()) {
                // User is already verified
                return false;
            }

            invalidateExistingTokens(user, TokenType.EMAIL_VERIFICATION);

            String tokenString = UUID.randomUUID().toString();
            VerificationToken verificationTokenEntity = new VerificationToken(tokenString, user, TokenType.EMAIL_VERIFICATION, TOKEN_EXPIRATION_MINUTES);
            verificationTokenRepository.save(verificationTokenEntity);

            mailutil.sendVerificationEmailViaMailSenderAsync(user.getEmail(), tokenString);
            activityLogService.logActivity(user, ActivityType.EMAIL_VERIFIED, "Resent verification email.", httpRequest);
            return true;
        } else {
            activityLogService.logActivity(null, ActivityType.EMAIL_VERIFIED, "Attempt to resend verification for non-existent email: " + email, httpRequest);
            return false;
        }
    }


    @Scheduled(cron = "0 0 2 * * ?") // Runs daily at 2 AM
    @Transactional
    public void purgeExpiredAndUsedTokens() {
        System.out.println("Running scheduled task to purge expired and used tokens...");
        LocalDateTime now = LocalDateTime.now();
        verificationTokenRepository.deleteByExpiryDateBefore(now.minusDays(7));
        verificationTokenRepository.deleteUsedAndExpiredTokens(now.minusDays(1));
        System.out.println("Token purge task completed.");
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public boolean changePassword(User loggedInUser, String currentPassword, String newPassword, HttpServletRequest httpRequest) {
        if (loggedInUser == null) {
            return false;
        }
        if (!passwordEncoder.matches(currentPassword, loggedInUser.getPassword())) {
            activityLogService.logActivity(loggedInUser, ActivityType.PASSWORD_CHANGE, "Password change failed: Incorrect current password.", httpRequest);
            return false;
        }
        loggedInUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(loggedInUser);
        activityLogService.logActivity(loggedInUser, ActivityType.PASSWORD_CHANGE, "Password changed successfully.",httpRequest);
        return true;
    }

    public boolean authenticateUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return passwordEncoder.matches(password, user.getPassword()) && user.isVerified() && user.isEnabled();
        }
        return false;
    }

    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
}