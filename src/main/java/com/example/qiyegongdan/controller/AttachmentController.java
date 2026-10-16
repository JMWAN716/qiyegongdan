package com.example.qiyegongdan.controller;

import com.example.qiyegongdan.entity.TicketAttachment;
import com.example.qiyegongdan.exception.ResourceNotFoundException;
import com.example.qiyegongdan.repository.TicketAttachmentRepository;
import com.example.qiyegongdan.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final TicketAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;

    public AttachmentController(TicketAttachmentRepository attachmentRepository, FileStorageService fileStorageService) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        TicketAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("附件不存在"));
        Resource resource = fileStorageService.loadAsResource(attachment);
        String contentType = attachment.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : attachment.getContentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFilename() + "\"")
                .body(resource);
    }
}