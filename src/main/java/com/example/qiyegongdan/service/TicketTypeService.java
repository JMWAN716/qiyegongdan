package com.example.qiyegongdan.service;

import com.example.qiyegongdan.dto.CustomFieldDefDTO;
import com.example.qiyegongdan.dto.TicketTypeDTO;
import com.example.qiyegongdan.entity.CustomFieldDefinition;
import com.example.qiyegongdan.entity.TicketType;
import com.example.qiyegongdan.exception.BusinessException;
import com.example.qiyegongdan.exception.ResourceNotFoundException;
import com.example.qiyegongdan.repository.CustomFieldDefinitionRepository;
import com.example.qiyegongdan.repository.TicketTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketTypeService {

    private final TicketTypeRepository typeRepository;
    private final CustomFieldDefinitionRepository fieldRepository;

    public TicketTypeService(TicketTypeRepository typeRepository, CustomFieldDefinitionRepository fieldRepository) {
        this.typeRepository = typeRepository;
        this.fieldRepository = fieldRepository;
    }

    public List<TicketTypeDTO> listAll() {
        return typeRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActive()))
                .map(this::toDTO)
                .toList();
    }

    public TicketTypeDTO create(String name, String description) {
        if (typeRepository.findByName(name).isPresent()) {
            throw new BusinessException("工单类型已存在");
        }
        TicketType type = new TicketType();
        type.setName(name);
        type.setDescription(description);
        type = typeRepository.save(type);
        return toDTO(type);
    }

    public TicketTypeDTO addCustomField(Long typeId, CustomFieldDefDTO def) {
        TicketType type = getEntity(typeId);
        boolean dup = fieldRepository.findByTicketTypeIdOrderByIdAsc(typeId).stream()
                .anyMatch(f -> f.getFieldKey().equalsIgnoreCase(def.fieldKey()));
        if (dup) {
            throw new BusinessException("字段标识已存在");
        }
        CustomFieldDefinition field = new CustomFieldDefinition();
        field.setTicketType(type);
        field.setFieldKey(def.fieldKey());
        field.setLabel(def.label());
        field.setDataType(def.dataType());
        field.setRequired(def.required());
        field.setOptions(def.options());
        fieldRepository.save(field);
        return toDTO(type);
    }

    public List<CustomFieldDefDTO> fieldsOf(Long typeId) {
        return fieldRepository.findByTicketTypeIdOrderByIdAsc(typeId).stream()
                .map(this::toFieldDTO)
                .collect(Collectors.toList());
    }

    public TicketType getEntity(Long id) {
        TicketType type = typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工单类型不存在"));
        return type;
    }

    private TicketTypeDTO toDTO(TicketType t) {
        return new TicketTypeDTO(t.getId(), t.getName(), t.getDescription(), t.getActive());
    }

    private CustomFieldDefDTO toFieldDTO(CustomFieldDefinition f) {
        return new CustomFieldDefDTO(f.getId(), f.getFieldKey(), f.getLabel(), f.getDataType(),
                f.getRequired() != null && f.getRequired(), f.getOptions());
    }
}