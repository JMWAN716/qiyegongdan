package com.example.qiyegongdan.controller;

import com.example.qiyegongdan.dto.ApiResponse;
import com.example.qiyegongdan.dto.CustomFieldDefDTO;
import com.example.qiyegongdan.dto.TicketTypeDTO;
import com.example.qiyegongdan.service.AuditLogService;
import com.example.qiyegongdan.service.TicketTypeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TicketTypeController {

    private final TicketTypeService typeService;
    private final AuditLogService auditLogService;

    public TicketTypeController(TicketTypeService typeService, AuditLogService auditLogService) {
        this.typeService = typeService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/ticket-types")
    public ApiResponse<List<TicketTypeDTO>> list() {
        return ApiResponse.ok(typeService.listAll());
    }

    @GetMapping("/ticket-types/{id}/fields")
    public ApiResponse<List<CustomFieldDefDTO>> fields(@PathVariable Long id) {
        return ApiResponse.ok(typeService.fieldsOf(id));
    }

    @PostMapping("/ticket-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TicketTypeDTO> create(@RequestBody Map<String, String> body, HttpServletRequest http) {
        TicketTypeDTO dto = typeService.create(body.get("name"), body.get("description"));
        auditLogService.record("工单类型", "创建", "创建类型 " + dto.name(), http);
        return ApiResponse.ok("创建成功", dto);
    }

    @PostMapping("/ticket-types/{id}/fields")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TicketTypeDTO> addField(@PathVariable Long id, @Valid @RequestBody CustomFieldDefDTO def, HttpServletRequest http) {
        TicketTypeDTO dto = typeService.addCustomField(id, def);
        auditLogService.record("工单类型", "添加字段", "类型 id=" + id + " 添加字段 " + def.fieldKey(), http);
        return ApiResponse.ok("添加成功", dto);
    }
}