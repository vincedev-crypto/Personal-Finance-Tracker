package com.appdev.Finance.Repository;

import com.appdev.Finance.model.Notification;
import com.appdev.Finance.model.NotificationStatus;
import com.appdev.Finance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserAndStatusOrderByCreatedAtDesc(User user, NotificationStatus status);
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndStatus(User user, NotificationStatus status);
}