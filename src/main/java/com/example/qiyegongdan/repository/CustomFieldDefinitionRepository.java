package com.example.qiyegongdan.repository;

import com.example.qiyegongdan.entity.CustomFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, Long> {

    List<CustomFieldDefinition> findByTicketTypeIdOrderByIdAsc(Long typeId);
}