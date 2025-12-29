package com.appdev.Finance;

import com.appdev.Finance.Service.NotificationService;
import com.appdev.Finance.Service.UserService; // Import UserService
import com.appdev.Finance.model.CustomUserDetails; // Import CustomUserDetails
import com.appdev.Finance.model.Notification;
import com.appdev.Finance.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Import
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationApiController.class);
    private static final DateTimeFormatter DTO_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    @Autowired
    private NotificationService notificationService;

    @Autowired // Autowire UserService
    private UserService userService;

    private static class NotificationDTO {
        public Long id;
        public String message;
        public String link;
        public String createdAt;
        public String status;

        public NotificationDTO(Notification notification) {
            this.id = notification.getId();
            this.message = notification.getMessage();
            this.link = notification.getLink();
            this.createdAt = notification.getCreatedAt() != null ?
                             notification.getCreatedAt().format(DTO_DATE_FORMATTER) : "N/A";
            this.status = notification.getStatus() != null ? notification.getStatus().name() : "UNKNOWN";
        }
    }

    private User getLoggedInUserFromPrincipal(CustomUserDetails principalDetails) {
        if (principalDetails == null) return null;
        Optional<User> userOpt = userService.findByEmail(principalDetails.getUsername());
        return userOpt.orElse(null);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadNotificationCount(@AuthenticationPrincipal CustomUserDetails principalDetails) {
        User loggedInUser = getLoggedInUserFromPrincipal(principalDetails);
        if (loggedInUser == null) {
            logger.warn("[API GET /unread-count] Unauthorized access attempt or user not found from principal.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated or not found", "status", 401));
        }
        logger.info("[API GET /unread-count] User: {}", loggedInUser.getEmail());
        try {
            long count = notificationService.getUnreadNotificationCount(loggedInUser);
            Map<String, Object> response = new HashMap<>();
            response.put("unreadCount", count);
            logger.debug("[API GET /unread-count] User: {}, Unread count: {}", loggedInUser.getEmail(), count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[API GET /unread-count] Error for user {}: {}", loggedInUser.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to retrieve unread count", "status", 500));
        }
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal CustomUserDetails principalDetails,
                                              @RequestParam(defaultValue = "7") int size) {
        User loggedInUser = getLoggedInUserFromPrincipal(principalDetails);
        if (loggedInUser == null) {
            logger.warn("[API GET /notifications] Unauthorized access attempt or user not found from principal.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated or not found", "status", 401));
        }
        logger.info("[API GET /notifications] Request for user: {}, size: {}", loggedInUser.getEmail(), size);

        try {
            List<Notification> notifications = notificationService.getAllNotificationsForUser(loggedInUser);
            if (notifications == null) {
                 notifications = Collections.emptyList();
                 logger.warn("[API GET /notifications] NotificationService returned null list for user: {}", loggedInUser.getEmail());
            }
            List<NotificationDTO> notificationDTOs = notifications.stream()
                                                                 .limit(size)
                                                                 .map(NotificationDTO::new)
                                                                 .collect(Collectors.toList());
            logger.info("[API GET /notifications] User: {}, Found {} total notifications, returning {} DTOs.",
                        loggedInUser.getEmail(), notifications.size(), notificationDTOs.size());
            return ResponseEntity.ok(notificationDTOs);
        } catch (Exception e) {
            logger.error("[API GET /notifications] Error fetching notifications for user {}: {}", loggedInUser.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch notifications", "status", 500));
        }
    }

    @PostMapping("/mark-as-read/{id}")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principalDetails) {
        User loggedInUser = getLoggedInUserFromPrincipal(principalDetails);
        if (loggedInUser == null) {
            logger.warn("[API POST /mark-as-read/{}] Unauthorized access attempt or user not found from principal.", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated or not found", "status", 401));
        }
        logger.info("[API POST /mark-as-read/{}] Request for user: {}", id, loggedInUser.getEmail());
        try {
            boolean success = notificationService.markAsRead(id, loggedInUser);
            if (success) {
                logger.info("[API POST /mark-as-read/{}] Successfully marked as read for user: {}", id, loggedInUser.getEmail());
                // Return the new unread count
                long newUnreadCount = notificationService.getUnreadNotificationCount(loggedInUser);
                return ResponseEntity.ok().body(Map.of("message", "Notification marked as read", "unreadCount", newUnreadCount));
            } else {
                logger.warn("[API POST /mark-as-read/{}] Failed to mark as read for user {} (not found or not owner).", id, loggedInUser.getEmail());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Notification not found or not owned by user", "status", 404));
            }
        } catch (Exception e) {
            logger.error("[API POST /mark-as-read/{}] Error for user {}: {}", id, loggedInUser.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error marking notification as read", "status", 500));
        }
    }

    @PostMapping("/mark-all-as-read")
    public ResponseEntity<?> markAllNotificationsAsRead(@AuthenticationPrincipal CustomUserDetails principalDetails) {
        User loggedInUser = getLoggedInUserFromPrincipal(principalDetails);
        if (loggedInUser == null) {
            logger.warn("[API POST /mark-all-as-read] Unauthorized access attempt or user not found from principal.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated or not found", "status", 401));
        }
        logger.info("[API POST /mark-all-as-read] Request for user: {}", loggedInUser.getEmail());
        try {
            notificationService.markAllAsRead(loggedInUser);
            logger.info("[API POST /mark-all-as-read] Successfully marked all as read for user: {}", loggedInUser.getEmail());
             // Return the new unread count (which should be 0)
            long newUnreadCount = notificationService.getUnreadNotificationCount(loggedInUser);
            return ResponseEntity.ok().body(Map.of("message", "All notifications marked as read", "unreadCount", newUnreadCount));
        } catch (Exception e) {
            logger.error("[API POST /mark-all-as-read] Error for user {}: {}", loggedInUser.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error marking all notifications as read", "status", 500));
        }
    }
}