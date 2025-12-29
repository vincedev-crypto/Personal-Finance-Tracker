package com.appdev.Finance.Service;

import com.appdev.Finance.model.ActivityLog;
import com.appdev.Finance.model.ActivityType;
import com.appdev.Finance.model.User;
import com.appdev.Finance.Repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Async // Logging can be done asynchronously
    public void logActivity(User user, ActivityType activityType, String description, String ipAddress, String details) {
        if (user == null) {
            // Handle cases where activity is not user-specific or user is not available
            // For now, we assume user is usually present for most logged activities
            System.err.println("Attempted to log activity with null user for type: " + activityType);
            return;
        }
        ActivityLog log = new ActivityLog(user, activityType, description, ipAddress, details);
        activityLogRepository.save(log);
    }

    public void logActivity(User user, ActivityType activityType, String description, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        logActivity(user, activityType, description, ipAddress, null);
    }
    
    public void logActivity(User user, ActivityType activityType, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        logActivity(user, activityType, activityType.getDescription(), ipAddress, null);
    }


    public List<ActivityLog> getActivitiesForUser(User user) {
        return activityLogRepository.findByUserOrderByTimestampDesc(user);
    }

    public Page<ActivityLog> getActivitiesForUser(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return activityLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "N/A";
        }
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }
}