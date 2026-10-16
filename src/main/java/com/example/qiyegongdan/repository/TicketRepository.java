package com.example.qiyegongdan.repository;

import com.example.qiyegongdan.entity.Ticket;
import com.example.qiyegongdan.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Query("select t from Ticket t where t.number like :prefix order by t.number desc")
    List<Ticket> findTopByNumberPrefix(@Param("prefix") String prefix, org.springframework.data.domain.Pageable pageable);

    @Query("select count(t) from Ticket t")
    long totalCount();

    long countByStatus(TicketStatus status);

    long countByPriority(com.example.qiyegongdan.enums.TicketPriority priority);

    long countByStatusAndAssigneeId(TicketStatus status, Long assigneeId);

    long countByStatusInAndAssigneeId(List<TicketStatus> statuses, Long assigneeId);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    long countByAssigneeId(Long assigneeId);

    List<Ticket> findByAssigneeId(Long assigneeId);

    boolean existsByNumber(String number);
}