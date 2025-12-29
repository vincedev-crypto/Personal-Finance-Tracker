package com.appdev.Finance.Service;

import com.appdev.Finance.DTO.WebSocketNotificationDTO; // Import the new DTO
import com.appdev.Finance.model.Notification;
import com.appdev.Finance.model.NotificationStatus;
import com.appdev.Finance.model.User;
import com.appdev.Finance.Repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate; // For sending WebSocket messages
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Collections;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // Autowire SimpMessagingTemplate

    @Async // Keep the DB save async if desired
    @Transactional // Ensure the whole operation, including sending message, is managed
    public void createAndSendNotification(User user, String message, String link) {
        if (user == null || user.getId() == null || user.getEmail() == null) { // user.getEmail() is needed for SimpMessagingTemplate.convertAndSendToUser
            logger.warn("Attempted to create notification for null user, user with null ID, or user with null email. Message: {}", message);
            return;
        }
        try {
            Notification notification = new Notification(user, message, link);
            Notification savedNotification = notificationRepository.save(notification);
            logger.info("Notification CREATED & SAVED for user ID {}: '{}', Notification ID: {}", user.getId(), message, savedNotification.getId());

            // After saving, send a WebSocket message to the specific user
            long unreadCount = getUnreadNotificationCount(user); // Get current unread count
            WebSocketNotificationDTO wsMessage = new WebSocketNotificationDTO(savedNotification, unreadCount);

            // The destination is /user/{username}/queue/notifications
            // Spring automatically resolves {username} to the authenticated user's name (which is user.getEmail() in your UserDetails)
            String userDestination = "/queue/notifications"; // The client subscribes to /user/queue/notifications
            messagingTemplate.convertAndSendToUser(user.getEmail(), userDestination, wsMessage);
            
            logger.info("WebSocket notification pushed to user {} at {}: {}", user.getEmail(), userDestination, wsMessage.getMessage());

        } catch (Exception e) {
            logger.error("Error creating and sending notification for user ID {}: {}", user.getId(), message, e);
        }
    }
    
    // Overload for convenience
    public void createAndSendNotification(User user, String message) {
        createAndSendNotification(user, message, null);
    }

    // Your existing methods (getUnreadNotificationsForUser, getAllNotificationsForUser, etc.) remain the same
    // Ensure they have proper null checks for the user object.

    public List<Notification> getUnreadNotificationsForUser(User user) {
        if (user == null || user.getId() == null) {
            logger.warn("getUnreadNotificationsForUser called with null user or user with null ID.");
            return Collections.emptyList();
        }
        logger.debug("Fetching UNREAD notifications for user ID: {}", user.getId());
        List<Notification> notifications = notificationRepository.findByUserAndStatusOrderByCreatedAtDesc(user, NotificationStatus.UNREAD);
        logger.info("Found {} UNREAD notifications for user ID: {}", notifications.size(), user.getId());
        return notifications;
    }

    public List<Notification> getAllNotificationsForUser(User user) {
        if (user == null || user.getId() == null) {
            logger.warn("getAllNotificationsForUser called with null user or user with null ID.");
            return Collections.emptyList();
        }
        logger.debug("Fetching ALL notifications for user ID: {}", user.getId());
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        logger.info("Found {} TOTAL notifications in DB for user ID: {}", notifications.size(), user.getId());
        return notifications;
    }

    public long getUnreadNotificationCount(User user) {
        if (user == null || user.getId() == null) {
            logger.warn("getUnreadNotificationCount called with null user or user with null ID.");
            return 0;
        }
        logger.debug("Counting UNREAD notifications for user ID: {}", user.getId());
        long count = notificationRepository.countByUserAndStatus(user, NotificationStatus.UNREAD);
        logger.info("Unread notification count for user ID {}: {}", user.getId(), count);
        return count;
    }

    @Transactional
    public boolean markAsRead(Long notificationId, User user) {
        if (user == null || user.getId() == null || notificationId == null) {
            logger.warn("markAsRead called with null user/notificationId. User ID: {}, Notif ID: {}", user != null ? user.getId() : "null", notificationId);
            return false;
        }
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        
        if (notification != null && notification.getUser() != null && notification.getUser().getId().equals(user.getId())) {
            boolean wasUnread = notification.getStatus() == NotificationStatus.UNREAD;
            if (wasUnread) {
                notification.setStatus(NotificationStatus.READ);
                notificationRepository.save(notification);
                logger.info("Notification {} marked as read for user ID {}", notificationId, user.getId());
                
                // Optionally, send an update about the new unread count via WebSocket
                // This helps if multiple browser tabs are open for the same user
                long newUnreadCount = getUnreadNotificationCount(user);
                Map<String, Long> countUpdate = Map.of("unreadCount", newUnreadCount);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notification-count-update", countUpdate);
                logger.info("Pushed unread count update ({}) to user {} via WebSocket after marking as read.", newUnreadCount, user.getEmail());
            }
            return true;
        }
        logger.warn("Attempt to mark notification {} as read failed. Not found or not owned by user ID {}", notificationId, user.getId());
        return false;
    }

    @Transactional
    public void markAllAsRead(User user) {
        if (user == null || user.getId() == null) {
            logger.warn("markAllAsRead called with null user or user with null ID.");
            return;
        }
        List<Notification> unreadNotifications = notificationRepository.findByUserAndStatusOrderByCreatedAtDesc(user, NotificationStatus.UNREAD);
        if (!unreadNotifications.isEmpty()) {
            for (Notification notification : unreadNotifications) {
                notification.setStatus(NotificationStatus.READ);
            }
            notificationRepository.saveAll(unreadNotifications);
            logger.info("Marked all {} unread notifications as read for user ID {}", unreadNotifications.size(), user.getId());

            // Send an update about the new unread count via WebSocket
            long newUnreadCount = 0; // All are read
            Map<String, Long> countUpdate = Map.of("unreadCount", newUnreadCount);
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notification-count-update", countUpdate);
            logger.info("Pushed unread count update (0) to user {} via WebSocket after marking all as read.", user.getEmail());

        } else {
            logger.info("No unread notifications to mark as read for user ID {}", user.getId());
        }
    }
}
