package com.example.qiyegongdan.service;

import com.example.qiyegongdan.entity.TicketAttachment;
import com.example.qiyegongdan.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 附件存储服务：本地文件系统
 */
@Service
public class FileStorageService {

    private Path root;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + root, e);
        }
    }

    public TicketAttachment store(MultipartFile file, TicketAttachment attachment) {
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String safeName = Path.of(original).getFileName().toString();
        String storedName = UUID.randomUUID().toString().replace("-", "") + "_" + safeName;
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("非法文件名");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("附件保存失败");
        }
        attachment.setFilename(safeName);
        attachment.setStoragePath(storedName);
        return attachment;
    }

    public Resource loadAsResource(TicketAttachment attachment) {
        try {
            Path file = root.resolve(attachment.getStoragePath()).normalize();
            if (!file.startsWith(root) || !Files.exists(file)) {
                throw new BusinessException("附件不存在");
            }
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new BusinessException("附件读取失败");
        }
    }
}