package com.bloodcare.bloodcare.repository;

import com.bloodcare.bloodcare.entity.Notification;
import com.bloodcare.bloodcare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserOrderByCreatedDateDesc(User user);
    
    List<Notification> findByUserAndIsReadFalseOrderByCreatedDateDesc(User user);
    
    List<Notification> findByUserAndTypeOrderByCreatedDateDesc(User user, String type);
    
    long countByUserAndIsReadFalse(User user);

    void deleteByUser(User user);
}
