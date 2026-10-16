package com.example.qiyegongdan.repository;

import com.example.qiyegongdan.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    Optional<TicketType> findByName(String name);
}