package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Attachment;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.AttachmentService;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.SysUserService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private static final Logger log = LoggerFactory.getLogger(AttachmentController.class);
    private static final int MAX_SAFE_FILENAME_LENGTH = 218;

    @Autowired
    private AttachmentService service;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private CurrentUserService currentUserService;

    @Value("${app.attachment.storage-root:uploads}")
    private String storageRoot;

    @GetMapping
    public ApiResponse<List<Attachment>> getAll() {
        try {
            return ApiResponse.success(service.getAll().stream()
                    .filter(att -> att != null && canAccessBusinessType(att.getBusinessType()))
                    .map(this::sanitizeAttachment)
                    .toList());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query attachments: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}")
    public ApiResponse<Attachment> getByUuid(@PathVariable String uuid) {
        try {
            Attachment attachment = service.getByUuid(uuid);
            if (attachment == null) {
                return ApiResponse.notFound("Attachment not found");
            }
            requireBusinessTypeAccess(attachment.getBusinessType());
            return ApiResponse.success(sanitizeAttachment(attachment));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.error("Failed to query attachment: " + e.getMessage());
        }
    }

    @GetMapping("/byBusiness")
    public ApiResponse<List<Attachment>> getByBusiness(
            @RequestParam String businessType,
            @RequestParam(required = false) String businessUuid
    ) {
        requireBusinessTypeAccess(businessType);
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
        requireBusinessTypeAccess(businessType);
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
            if (!canAccessBusinessType(attachment.getBusinessType())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return buildFileResponse(attachment, MediaType.APPLICATION_OCTET_STREAM, false);
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
            if (!canAccessBusinessType(attachment.getBusinessType())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Path filePath = resolveAttachmentPath(attachment);
            if (filePath == null || !Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            if (!hasPdfMagicBytes(filePath)) {
                return ResponseEntity.badRequest().build();
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
            Attachment existing = service.getByUuid(uuid);
            if (existing == null) {
                return ApiResponse.notFound("Attachment not found");
            }
            requireBusinessTypeAccess(existing.getBusinessType());
            if (attachment != null && StringUtils.hasText(attachment.getBusinessType())) {
                requireBusinessTypeAccess(attachment.getBusinessType());
            }
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
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.error("Failed to update attachment: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            Attachment attachment = service.getByUuid(uuid);
            if (attachment != null) {
                requireBusinessTypeAccess(attachment.getBusinessType());
            }
            Path filePath = attachment != null ? resolveAttachmentPath(attachment) : null;
            service.delete(uuid);
            auditTrailService.record("attachments", "DELETE", "attachments", uuid, "Deleted attachment");
            if (filePath != null) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (Exception e) {
                    log.warn("Failed to remove attachment file {}: {}", filePath, e.getMessage());
                }
            }
            return ApiResponse.success("Attachment deleted", null);
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete attachment: " + e.getMessage());
        }
    }

    private void requireBusinessTypeAccess(String businessType) {
        if (!canAccessBusinessType(businessType)) {
            throw new AccessDeniedException("没有访问该业务模块附件的权限");
        }
    }

    private boolean canAccessBusinessType(String businessType) {
        if (currentUserService == null) {
            return false;
        }
        if (currentUserService.isSystemUser()) {
            return true;
        }
        if (!StringUtils.hasText(businessType) || "other".equalsIgnoreCase(businessType.trim())) {
            return true;
        }
        return switch (businessType.trim().toLowerCase(Locale.ROOT)) {
            case "projects" -> currentUserService.hasAnyAuthority("project.access", "project.manage");
            case "project-lists" -> currentUserService.hasAnyAuthority(
                    "project.access", "project.manage", "project.list.entry", "project.list.audit");
            case "contracts" -> currentUserService.hasAnyAuthority("contract.access", "contract.manage");
            case "customers" -> currentUserService.hasAnyAuthority("customer.access", "customer.manage");
            case "warehouse-documents" -> currentUserService.hasAnyAuthority(
                    "inventory.access", "inventory.manage", "inventory.warehouse-doc.entry", "inventory.warehouse-doc.audit");
            case "finance-vouchers" -> currentUserService.hasAnyAuthority(
                    "finance.access", "finance.manage", "finance.voucher.entry", "finance.voucher.audit");
            default -> currentUserService.hasAnyAuthority("attachment.manage");
        };
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
                .header("X-Content-Type-Options", "nosniff")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        dispositionType + "; filename*=UTF-8''" + URLEncoder.encode(displayName, StandardCharsets.UTF_8).replace("+", "%20")
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
        String rawStoragePath = attachment.getStoragePath().trim();
        Path rootPath = resolveStorageRootPath();

        if (isWindowsDrivePath(rawStoragePath)) {
            return resolveLegacyAbsolutePath(rawStoragePath, rootPath);
        }

        Path storedPath = Paths.get(rawStoragePath);
        if (!storedPath.isAbsolute()) {
            Path resolved = rootPath.resolve(storedPath).normalize();
            if (!resolved.startsWith(rootPath)) {
                throw new SecurityException("Invalid attachment storage path");
            }
            return resolved;
        }

        return resolveLegacyAbsolutePath(rawStoragePath, rootPath);
    }

    private Path resolveLegacyAbsolutePath(String rawStoragePath, Path rootPath) {
        if (containsParentTraversalSegment(rawStoragePath)) {
            throw new SecurityException("Invalid attachment storage path");
        }
        if (!isWindowsDrivePath(rawStoragePath) || File.separatorChar == '\\') {
            Path candidate = Paths.get(rawStoragePath).normalize();
            if (candidate.startsWith(rootPath) && Files.exists(candidate)) {
                return candidate;
            }
        }
        String normalized = rawStoragePath.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? normalized.substring(lastSlash + 1).trim() : normalized.trim();
        if (StringUtils.hasText(fileName) && !".".equals(fileName) && !"..".equals(fileName)) {
            Path migrated = rootPath.resolve(fileName).normalize();
            if (migrated.startsWith(rootPath) && Files.exists(migrated)) {
                return migrated;
            }
        }
        if (!isWindowsDrivePath(rawStoragePath) || File.separatorChar == '\\') {
            Path candidate = Paths.get(rawStoragePath).normalize();
            if (candidate.startsWith(rootPath)) {
                return candidate;
            }
        }
        throw new SecurityException("Invalid attachment storage path");
    }

    private boolean isWindowsDrivePath(String path) {
        return path.length() >= 3
                && Character.isLetter(path.charAt(0))
                && path.charAt(1) == ':'
                && (path.charAt(2) == '\\' || path.charAt(2) == '/');
    }

    private boolean containsParentTraversalSegment(String path) {
        for (String segment : path.replace('\\', '/').split("/")) {
            if ("..".equals(segment.trim())) {
                return true;
            }
        }
        return false;
    }

    private String sanitizeUploadFileName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            return "file";
        }
        String normalized = originalFileName.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String baseName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        StringBuilder sb = new StringBuilder(baseName.length());
        for (int i = 0; i < baseName.length(); i++) {
            char c = baseName.charAt(i);
            if (c < 0x20 || c == 0x7F || c == '<' || c == '>' || c == ':' || c == '"'
                    || c == '/' || c == '\\' || c == '|' || c == '?' || c == '*') {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        String cleaned = sb.toString().trim();
        while (cleaned.endsWith(".") || cleaned.endsWith(" ")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        if (!StringUtils.hasText(cleaned) || ".".equals(cleaned) || "..".equals(cleaned)) {
            return "file";
        }
        if (cleaned.length() <= MAX_SAFE_FILENAME_LENGTH) {
            return cleaned;
        }
        int lastDot = cleaned.lastIndexOf('.');
        if (lastDot > 0 && lastDot < cleaned.length() - 1) {
            String ext = cleaned.substring(lastDot);
            if (ext.length() <= 32) {
                int maxPrefixLength = MAX_SAFE_FILENAME_LENGTH - ext.length();
                if (maxPrefixLength > 0) {
                    return cleaned.substring(0, maxPrefixLength) + ext;
                }
            }
        }
        return cleaned.substring(0, MAX_SAFE_FILENAME_LENGTH);
    }

    private boolean hasPdfMagicBytes(Path filePath) {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            byte[] header = inputStream.readNBytes(5);
            return header.length == 5 && "%PDF-".equals(new String(header, StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
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
