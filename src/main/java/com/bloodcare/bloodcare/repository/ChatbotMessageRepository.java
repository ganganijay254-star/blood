package com.bloodcare.bloodcare.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bloodcare.bloodcare.entity.ChatbotMessage;
import com.bloodcare.bloodcare.entity.User;
import java.util.List;

@Repository
public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
    List<ChatbotMessage> findByUserOrderByTimestampDesc(User user);
    List<ChatbotMessage> findByUserOrderByTimestampAsc(User user);
    void deleteByUser(User user);
}
