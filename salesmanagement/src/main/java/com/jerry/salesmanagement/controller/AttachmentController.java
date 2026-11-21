package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Attachment;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.AttachmentService;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SysUserService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    @Autowired
    private AttachmentService service;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private SysUserService sysUserService;

    @Value("${app.attachment.storage-root:uploads}")
    private String storageRoot;

    @GetMapping
    public ApiResponse<List<Attachment>> getAll() {
        try {
            return ApiResponse.success(service.getAll().stream().map(this::sanitizeAttachment).toList());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query attachments: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}")
    public ApiResponse<Attachment> getByUuid(@PathVariable String uuid) {
        try {
            Attachment attachment = service.getByUuid(uuid);
            return attachment != null ? ApiResponse.success(sanitizeAttachment(attachment)) : ApiResponse.notFound("Attachment not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query attachment: " + e.getMessage());
        }
    }

    @GetMapping("/byBusiness")
    public ApiResponse<List<Attachment>> getByBusiness(
            @RequestParam String businessType,
            @RequestParam(required = false) String businessUuid
    ) {
        try {
            return ApiResponse.success(service.getByBusiness(businessType, businessUuid).stream().map(this::sanitizeAttachment).toList());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query business attachments: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Attachment> create(@RequestBody Attachment attachment) {
        return ApiResponse.badRequest("Please create attachments through the upload endpoint");
    }

    @PostMapping("/upload")
    public ApiResponse<Attachment> upload(
            @RequestParam String businessType,
            @RequestParam(required = false) String businessUuid,
            @RequestPart("file") MultipartFile file
    ) {
        Path copiedTarget = null;
        try {
            if (file == null || file.isEmpty()) {
                return ApiResponse.badRequest("File is required");
            }

            String uuid = UUID.randomUUID().toString();
            String safeFileName = sanitizeUploadFileName(file.getOriginalFilename());
            String fileExt = extractFileExt(safeFileName);
            Path rootPath = resolveStorageRootPath();
            Files.createDirectories(rootPath);

            String storedFileName = uuid + "-" + safeFileName;
            Path targetPath = rootPath.resolve(storedFileName).normalize();
            if (!targetPath.startsWith(rootPath)) {
                throw new SecurityException("Invalid attachment storage path");
            }
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            copiedTarget = targetPath;

            Attachment attachment = new Attachment();
            attachment.setUuid(uuid);
            attachment.setBusinessType(businessType);
            attachment.setBusinessUuid(businessUuid);
            attachment.setFileName(storedFileName);
            attachment.setOriginalFileName(safeFileName);
            attachment.setFileExt(fileExt);
            attachment.setMimeType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setStoragePath(storedFileName);
            attachment.setUploadedBy(resolveCurrentUserId());

            Attachment created = service.create(attachment);
            copiedTarget = null;
            auditTrailService.record("attachments", "UPLOAD", "attachments", created.getUuid(), "Uploaded attachment file");
            return ApiResponse.success("Attachment uploaded", sanitizeAttachment(created));
        } catch (Exception e) {
            if (copiedTarget != null) {
                try {
                    Files.deleteIfExists(copiedTarget);
                } catch (Exception ignored) {
                }
            }
            return ApiResponse.error("Failed to upload attachment: " + e.getMessage());
        }
    }

    @GetMapping("/download/{uuid}")
    public ResponseEntity<Resource> download(@PathVariable String uuid) {
        try {
            Attachment attachment = service.getByUuid(uuid);
            if (attachment == null) {
                return ResponseEntity.notFound().build();
            }
            return buildFileResponse(attachment, parseMediaType(attachment.getMimeType()), false);
        } catch (SecurityException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/preview/{uuid}")
    public ResponseEntity<Resource> previewPdf(@PathVariable String uuid) {
        try {
            Attachment attachment = service.getByUuid(uuid);
            if (attachment == null || !isPdf(attachment)) {
                return ResponseEntity.notFound().build();
            }
            return buildFileResponse(attachment, MediaType.APPLICATION_PDF, true);
        } catch (SecurityException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{uuid}")
    public ApiResponse<Attachment> update(@PathVariable String uuid, @RequestBody Attachment attachment) {
        try {
            attachment.setUuid(uuid);
            attachment.setFileName(null);
            attachment.setFileExt(null);
            attachment.setMimeType(null);
            attachment.setFileSize(null);
            attachment.setStoragePath(null);
            attachment.setUploadedBy(null);
            Attachment updated = service.update(attachment);
            auditTrailService.record("attachments", "UPDATE", "attachments", uuid, "Updated attachment metadata");
            return ApiResponse.success("Attachment updated", sanitizeAttachment(updated));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update attachment: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            Attachment attachment = service.getByUuid(uuid);
            Path filePath = attachment != null ? resolveAttachmentPath(attachment) : null;
            service.delete(uuid);
            if (filePath != null) {
                Files.deleteIfExists(filePath);
            }
            auditTrailService.record("attachments", "DELETE", "attachments", uuid, "Deleted attachment");
            return ApiResponse.success("Attachment deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete attachment: " + e.getMessage());
        }
    }

    private ResponseEntity<Resource> buildFileResponse(Attachment attachment, MediaType mediaType, boolean inline)
            throws Exception {
        Path filePath = resolveAttachmentPath(attachment);
        if (filePath == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String displayName = attachment.getOriginalFileName() != null
                ? attachment.getOriginalFileName()
                : attachment.getFileName();
        if (!StringUtils.hasText(displayName)) {
            displayName = filePath.getFileName().toString();
        }
        String dispositionType = inline ? "inline" : "attachment";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        dispositionType + "; filename*=UTF-8''" + URLEncoder.encode(displayName, StandardCharsets.UTF_8)
                )
                .body(resource);
    }

    private Path resolveStorageRootPath() {
        return Paths.get(resolveStorageRoot()).toAbsolutePath().normalize();
    }

    private Path resolveAttachmentPath(Attachment attachment) {
        if (attachment == null || !StringUtils.hasText(attachment.getStoragePath())) {
            return null;
        }
        Path rootPath = resolveStorageRootPath();
        Path storedPath = Paths.get(attachment.getStoragePath().trim());

        if (!storedPath.isAbsolute()) {
            Path resolved = rootPath.resolve(storedPath).normalize();
            if (!resolved.startsWith(rootPath)) {
                throw new SecurityException("Invalid attachment storage path");
            }
            return resolved;
        }

        Path candidate = storedPath.normalize();
        if (candidate.startsWith(rootPath) && Files.exists(candidate)) {
            return candidate;
        }
        Path fileName = candidate.getFileName();
        if (fileName != null) {
            Path migrated = rootPath.resolve(fileName).normalize();
            if (migrated.startsWith(rootPath) && Files.exists(migrated)) {
                return migrated;
            }
        }
        if (candidate.startsWith(rootPath)) {
            return candidate;
        }
        throw new SecurityException("Invalid attachment storage path");
    }

    private String sanitizeUploadFileName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            return "file";
        }
        String normalized = originalFileName.replace('\\', '/');
        String baseName = Paths.get(normalized).getFileName().toString().trim();
        if (!StringUtils.hasText(baseName) || ".".equals(baseName) || "..".equals(baseName)) {
            return "file";
        }
        return baseName;
    }

    private MediaType parseMediaType(String mimeType) {
        try {
            return mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String resolveStorageRoot() {
        return systemConfigService.getString("attachment.storage.root", storageRoot);
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        SysUser currentUser = sysUserService.getByUsername(authentication.getName());
        return currentUser == null ? null : currentUser.getId();
    }

    private boolean isPdf(Attachment attachment) {
        if (attachment == null) {
            return false;
        }
        if (StringUtils.hasText(attachment.getMimeType())
                && "application/pdf".equalsIgnoreCase(attachment.getMimeType().trim())) {
            return true;
        }
        return StringUtils.hasText(attachment.getFileExt())
                && "pdf".equalsIgnoreCase(attachment.getFileExt().trim());
    }

    private Attachment sanitizeAttachment(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        Attachment sanitized = new Attachment();
        BeanUtils.copyProperties(attachment, sanitized);
        sanitized.setFileName(null);
        sanitized.setStoragePath(null);
        return sanitized;
    }

    private String extractFileExt(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
