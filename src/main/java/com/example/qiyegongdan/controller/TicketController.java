package com.example.qiyegongdan.controller;

import com.example.qiyegongdan.dto.*;
import com.example.qiyegongdan.entity.TicketAttachment;
import com.example.qiyegongdan.entity.TicketComment;
import com.example.qiyegongdan.entity.User;
import com.example.qiyegongdan.security.SecurityUtils;
import com.example.qiyegongdan.service.AuditLogService;
import com.example.qiyegongdan.service.FileStorageService;
import com.example.qiyegongdan.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    public TicketController(TicketService ticketService, FileStorageService fileStorageService,
                            AuditLogService auditLogService) {
        this.ticketService = ticketService;
        this.fileStorageService = fileStorageService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ApiResponse<TicketDTO> create(@Valid @RequestBody TicketCreateRequest req, HttpServletRequest http) {
        User user = SecurityUtils.requireCurrentUser();
        TicketDTO dto = ticketService.create(req, user);
        auditLogService.record("工单", "创建工单", "创建工单 " + dto.number(), http);
        return ApiResponse.ok("创建成功", dto);
    }

    @GetMapping
    public ApiResponse<TicketDTO.PageResult> search(TicketQueryRequest req) {
        User user = SecurityUtils.requireCurrentUser();
        return ApiResponse.ok(ticketService.search(req, user));
    }

    @GetMapping("/{id}")
    public ApiResponse<TicketDTO> get(@PathVariable Long id) {
        return ApiResponse.ok(ticketService.get(id));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ApiResponse<TicketDTO> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req, HttpServletRequest http) {
        User user = SecurityUtils.requireCurrentUser();
        ticketService.reassign(id, req, user);
        auditLogService.record("工单", "重新分配", "工单 id=" + id + " 分配给 " + req.assigneeId(), http);
        return ApiResponse.ok("分配成功", ticketService.get(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<TicketDTO> changeStatus(@PathVariable Long id, @Valid @RequestBody TicketUpdateStatusRequest req, HttpServletRequest http) {
        User user = SecurityUtils.requireCurrentUser();
        TicketDTO dto = ticketService.changeStatus(id, req, user);
        auditLogService.record("工单", "状态流转", "工单 " + dto.number() + " 状态 -> " + req.toStatus(), http);
        return ApiResponse.ok("状态已更新", dto);
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<TicketDTO> comment(@PathVariable Long id, @Valid @RequestBody CommentRequest req, HttpServletRequest http) {
        User user = SecurityUtils.requireCurrentUser();
        TicketDTO dto = ticketService.addComment(id, req, user);
        auditLogService.record("工单", "评论", "工单 id=" + id + " 新增评论", http);
        return ApiResponse.ok("评论成功", dto);
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<List<TicketComment>> comments(@PathVariable Long id) {
        return ApiResponse.ok(ticketService.comments(id));
    }

    @GetMapping("/{id}/attachments")
    public ApiResponse<List<TicketAttachment>> attachments(@PathVariable Long id) {
        return ApiResponse.ok(ticketService.attachments(id));
    }

    @PostMapping("/{id}/attachments")
    public ApiResponse<TicketAttachment> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file, HttpServletRequest http) {
        if (file.isEmpty()) {
            throw new com.example.qiyegongdan.exception.BusinessException("文件不能为空");
        }
        User user = SecurityUtils.requireCurrentUser();
        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicket(ticketService.getEntity(id));
        attachment.setUploader(user);
        fileStorageService.store(file, attachment);
        attachment.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        attachment.setSize(file.getSize());
        TicketAttachment saved = ticketService.attach(id, attachment.getFilename(), attachment.getStoragePath(),
                attachment.getContentType(), attachment.getSize(), user);
        auditLogService.record("工单", "上传附件", "工单 id=" + id + " 上传 " + attachment.getFilename(), http);
        return new ApiResponse<>(true, "上传成功", saved);
    }
}