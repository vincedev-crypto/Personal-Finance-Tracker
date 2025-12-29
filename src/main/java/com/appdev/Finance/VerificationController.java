package com.appdev.Finance;

import com.appdev.Finance.Service.UserService;
import com.appdev.Finance.model.TokenType;
import com.appdev.Finance.model.VerificationToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping; // No longer needed if removing the form-based resend
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

@Controller
public class VerificationController {

    private final UserService userService;

    @Autowired
    public VerificationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/verify")
    public String verifyToken(@RequestParam(name = "token", required = false) String tokenString,
                              HttpServletRequest httpRequest,
                              RedirectAttributes redirectAttributes) {

        if (tokenString == null || tokenString.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid or missing verification token. If your link has expired, check your email for an option to resend or register again.");
        } else {
            Optional<VerificationToken> tokenOpt = userService.getTokenByStringAndType(tokenString, TokenType.EMAIL_VERIFICATION);

            if (!tokenOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid verification token. It might have been used, does not exist, or has expired. Please check your email for an option to resend, or register again.");
            } else {
                VerificationToken verificationToken = tokenOpt.get();
                if (verificationToken.isUsed()) {
                    redirectAttributes.addFlashAttribute("infoMessage", "This verification link has already been used. Your email should be verified. Please try logging in.");
                } else if (verificationToken.isExpired()) {
                    String userEmailForResend = (verificationToken.getUser() != null) ? verificationToken.getUser().getEmail() : null;
                    redirectAttributes.addFlashAttribute("errorMessage", "This verification link has expired (valid for 5 minutes). Please use the 'Request a New Verification Link' option from your original verification email, or register again.");
                    // We don't automatically show a resend option on login page anymore based on this path,
                    // as the user is expected to use the link in the email.
                } else {
                    boolean isVerified = userService.verifyEmail(tokenString, httpRequest);
                    if (isVerified) {
                        redirectAttributes.addFlashAttribute("successMessage", "Your email has been verified successfully! You can now log in.");
                    } else {
                        redirectAttributes.addFlashAttribute("errorMessage", "Verification failed. The link may be invalid or an internal error occurred. Please use the 'Request a New Verification Link' option from your original verification email or register again.");
                    }
                }
            }
        }
        return "redirect:/login";
    }

    // New GET mapping to handle the direct resend link from the email
    @GetMapping("/resend-verification-directly")
    public String resendVerificationDirectly(@RequestParam("email") String email,
                                             RedirectAttributes redirectAttributes,
                                             HttpServletRequest httpRequest) {
        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email address is required to resend verification.");
            return "redirect:/login";
        }

        boolean emailSent = userService.resendVerificationEmail(email, httpRequest);

        if (emailSent) {
            redirectAttributes.addFlashAttribute("successMessage", "A new 5-minute verification link has been sent to " + email + ". Please check your inbox.");
        } else {
            // This could mean user not found or already verified.
            redirectAttributes.addFlashAttribute("infoMessage", "If your email " + email + " is registered and not yet verified, a new link has been sent. If already verified, please try logging in.");
        }
        return "redirect:/login";
    }

    // The /resend-verification-link page and its POST mapping can now be removed
    // if you only want the direct resend from email.
    // If you choose to remove them, also remove resend-verification-link.html.

    /*
    // REMOVE or COMMENT OUT:
    @GetMapping("/resend-verification-link")
    public String showResendVerificationPage(@RequestParam(name = "email", required = false) String email, Model model) {
        // ...
        return "resend-verification-link";
    }

    @PostMapping("/resend-verification-link")
    public String processResendVerificationLink(@RequestParam("email") String email,
                                                RedirectAttributes redirectAttributes,
                                                HttpServletRequest httpRequest) {
        // ...
        return "redirect:/login";
    }
    */
}