package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Attachment;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.AttachmentService;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SysUserService;
import com.jerry.salesmanagement.service.SystemConfigService;
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
@CrossOrigin(exposedHeaders = "Content-Disposition")
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
        try {
            if (file == null || file.isEmpty()) {
                return ApiResponse.badRequest("File is required");
            }

            String uuid = UUID.randomUUID().toString();
            String originalFileName = file.getOriginalFilename();
            String safeFileName = originalFileName == null ? "file" : Paths.get(originalFileName).getFileName().toString();
            String fileExt = extractFileExt(safeFileName);
            Path rootPath = Paths.get(resolveStorageRoot()).toAbsolutePath().normalize();
            Files.createDirectories(rootPath);

            String storedFileName = uuid + "-" + safeFileName;
            Path targetPath = rootPath.resolve(storedFileName).normalize();
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            Attachment attachment = new Attachment();
            attachment.setUuid(uuid);
            attachment.setBusinessType(businessType);
            attachment.setBusinessUuid(businessUuid);
            attachment.setFileName(storedFileName);
            attachment.setOriginalFileName(safeFileName);
            attachment.setFileExt(fileExt);
            attachment.setMimeType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setStoragePath(targetPath.toString());
            attachment.setUploadedBy(resolveCurrentUserId());

            Attachment created = service.create(attachment);
            auditTrailService.record("attachments", "UPLOAD", "attachments", created.getUuid(), "Uploaded attachment file");
            return ApiResponse.success("Attachment uploaded", sanitizeAttachment(created));
        } catch (Exception e) {
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

            Path filePath = Paths.get(attachment.getStoragePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String downloadName = attachment.getOriginalFileName() != null
                    ? attachment.getOriginalFileName()
                    : attachment.getFileName();

            return ResponseEntity.ok()
                    .contentType(parseMediaType(attachment.getMimeType()))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + URLEncoder.encode(downloadName, StandardCharsets.UTF_8)
                    )
                    .body(resource);
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

            Path filePath = Paths.get(attachment.getStoragePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String previewName = attachment.getOriginalFileName() != null
                    ? attachment.getOriginalFileName()
                    : attachment.getFileName();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename*=UTF-8''" + URLEncoder.encode(previewName, StandardCharsets.UTF_8)
                    )
                    .body(resource);
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
            if (attachment != null && attachment.getStoragePath() != null) {
                Files.deleteIfExists(Paths.get(attachment.getStoragePath()));
            }
            service.delete(uuid);
            auditTrailService.record("attachments", "DELETE", "attachments", uuid, "Deleted attachment");
            return ApiResponse.success("Attachment deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete attachment: " + e.getMessage());
        }
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
        if (attachment != null) {
            attachment.setFileName(null);
            attachment.setStoragePath(null);
        }
        return attachment;
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
