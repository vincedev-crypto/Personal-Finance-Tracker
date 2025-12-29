package com.appdev.Finance.Repository;

import com.appdev.Finance.model.ActivityLog;
import com.appdev.Finance.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserOrderByTimestampDesc(User user);
    Page<ActivityLog> findByUserOrderByTimestampDesc(User user, Pageable pageable); // For pagination
}