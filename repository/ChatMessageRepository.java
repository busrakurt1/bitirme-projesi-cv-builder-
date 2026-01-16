package com.cvbuilder.repository;

import com.cvbuilder.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(Long userId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.user.id = :userId ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessagesByUserId(@Param("userId") Long userId);
    
    void deleteByUserId(Long userId);
}








