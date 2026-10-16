package com.example.qiyegongdan.repository;

import com.example.qiyegongdan.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByTicketIdOrderByUploadedAtDesc(Long ticketId);
}