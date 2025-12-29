package com.appdev.Finance.DTO; // Or your preferred DTO package

import com.appdev.Finance.model.Notification; // Assuming Notification model exists
import java.time.format.DateTimeFormatter;

// This DTO will be sent over WebSockets
public class WebSocketNotificationDTO {
    private Long id;
    private String message;
    private String link;
    private String createdAt;
    private String status;
    private long unreadCount; // Optionally send the new unread count

    private static final DateTimeFormatter DTO_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, hh:mm a");

    // Constructor
    public WebSocketNotificationDTO(Notification notification, long unreadCount) {
        this.id = notification.getId();
        this.message = notification.getMessage();
        this.link = notification.getLink();
        this.createdAt = notification.getCreatedAt() != null ?
                         notification.getCreatedAt().format(DTO_DATE_FORMATTER) : "N/A";
        this.status = notification.getStatus() != null ? notification.getStatus().name() : "UNKNOWN";
        this.unreadCount = unreadCount;
    }

    // Getters (and Setters if needed, though often not for DTOs sent out)
    public Long getId() { return id; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public String getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public long getUnreadCount() { return unreadCount; }
}
