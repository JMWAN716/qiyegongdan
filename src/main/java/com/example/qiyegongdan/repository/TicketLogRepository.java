package com.example.qiyegongdan.repository;

import com.example.qiyegongdan.entity.TicketLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketLogRepository extends JpaRepository<TicketLog, Long> {

    List<TicketLog> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}