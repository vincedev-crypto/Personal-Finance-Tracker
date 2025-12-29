package com.appdev.Finance;

import com.appdev.Finance.Service.ActivityLogService;
import com.appdev.Finance.Service.UserService;
import com.appdev.Finance.model.User;
import com.appdev.Finance.model.ActivityLog; // Import ActivityLog
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserProfileController {

    private final UserService userService;
    private final ActivityLogService activityLogService; // For displaying activity logs on profile

    @Autowired
    public UserProfileController(UserService userService, ActivityLogService activityLogService) {
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    @GetMapping("/profile")
    public String userProfile(@RequestParam(name = "page", defaultValue = "0") int page, // For pagination
                              HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        // ... (null check) ...
        model.addAttribute("user", loggedInUser);

        int pageSize = 10; // Or make configurable
        Page<com.appdev.Finance.model.ActivityLog> activityPage = activityLogService.getActivitiesForUser(loggedInUser, page, pageSize);
        model.addAttribute("activityLogs", activityPage.getContent());
        model.addAttribute("activityLogCurrentPage", activityPage.getNumber());
        model.addAttribute("activityLogTotalPages", activityPage.getTotalPages());
        model.addAttribute("activityLogHasNext", activityPage.hasNext());
        model.addAttribute("activityLogHasPrevious", activityPage.hasPrevious());
        return "profile";
    }

    @GetMapping("/change-password")
    public String showChangePasswordPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please log in to change your password.");
            return "redirect:/login";
        }
        // Optionally, add an empty form-backing object if needed by the template, though not strictly necessary for this form
        // model.addAttribute("passwordChangeForm", new SomePasswordChangeDTO());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String processChangePassword(@RequestParam("currentPassword") String currentPassword,
                                        @RequestParam("newPassword") String newPassword,
                                        @RequestParam("confirmPassword") String confirmPassword,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes,
                                        HttpServletRequest httpRequest) { // For activity logging

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            // This case should ideally be handled by a security filter if routes are protected
            return "redirect:/login";
        }

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            redirectAttributes.addFlashAttribute("passwordChangeError", "❌ All password fields are required.");
            return "redirect:/change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordChangeError", "❌ New password and confirmation do not match.");
            return "redirect:/change-password";
        }

        boolean changed = userService.changePassword(loggedInUser, currentPassword, newPassword, httpRequest);
        
        if (changed) {
            redirectAttributes.addFlashAttribute("passwordChangeSuccess", "✅ Password updated successfully!");
            return "redirect:/profile"; // Redirect to profile page on success
        } else {
            // userService.changePassword should ideally only return false if currentPassword is wrong
            // Other validation errors should be handled before calling it, or it should throw specific exceptions.
            redirectAttributes.addFlashAttribute("passwordChangeError", "❌ Incorrect current password or update failed.");
            return "redirect:/change-password";
        }
    }
}