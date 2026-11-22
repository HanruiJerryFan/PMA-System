package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.AttachmentMapper;
import com.jerry.salesmanagement.pojo.Attachment;
import com.jerry.salesmanagement.service.AttachmentService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentServiceImpl.class);
    private static final Set<String> BUSINESS_TYPES = Set.of(
            "projects",
            "project-lists",
            "finance-vouchers",
            "contracts",
            "customers",
            "warehouse-documents",
            "other"
    );

    @Autowired
    private AttachmentMapper mapper;

    @Autowired
    private CustomerService customerService;

    @Autowired(required = false)
    private SystemConfigService systemConfigService;

    @Value("${app.attachment.storage-root:uploads}")
    private String storageRoot = "uploads";

    @Override
    public Attachment getByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Override
    public List<Attachment> getAll() {
        return mapper.selectAll();
    }

    @Override
    public List<Attachment> getByBusiness(String businessType, String businessUuid) {
        return mapper.selectByBusiness(businessType, businessUuid);
    }

    @Override
    @Transactional
    public Attachment create(Attachment attachment) {
        validateAttachment(attachment);
        if (!StringUtils.hasText(attachment.getUuid())) {
            attachment.setUuid(UUID.randomUUID().toString());
        }
        mapper.insert(attachment);
        touchCustomerAttachment(attachment);
        return attachment;
    }

    @Override
    @Transactional
    public Attachment update(Attachment attachment) {
        Attachment existing = mapper.selectByUuid(attachment.getUuid());
        if (existing != null) {
            if (!StringUtils.hasText(attachment.getBusinessType())) {
                attachment.setBusinessType(existing.getBusinessType());
            }
            if (!StringUtils.hasText(attachment.getBusinessUuid())) {
                attachment.setBusinessUuid(existing.getBusinessUuid());
            }
            if (!StringUtils.hasText(attachment.getFileName())) {
                attachment.setFileName(existing.getFileName());
            }
            if (!StringUtils.hasText(attachment.getOriginalFileName())) {
                attachment.setOriginalFileName(existing.getOriginalFileName());
            }
            if (!StringUtils.hasText(attachment.getFileExt())) {
                attachment.setFileExt(existing.getFileExt());
            }
            if (!StringUtils.hasText(attachment.getMimeType())) {
                attachment.setMimeType(existing.getMimeType());
            }
            if (attachment.getFileSize() == null) {
                attachment.setFileSize(existing.getFileSize());
            }
            if (!StringUtils.hasText(attachment.getStoragePath())) {
                attachment.setStoragePath(existing.getStoragePath());
            }
            if (attachment.getUploadedBy() == null) {
                attachment.setUploadedBy(existing.getUploadedBy());
            }
        }
        validateAttachment(attachment);
        mapper.updateByUuid(attachment);
        touchCustomerAttachment(attachment);
        return attachment;
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        mapper.deleteByUuid(uuid);
    }

    @Override
    @Transactional
    public void deleteByBusiness(String businessType, String businessUuid) {
        if (!StringUtils.hasText(businessType) || !StringUtils.hasText(businessUuid)) {
            return;
        }
        List<Attachment> attachments = mapper.selectByBusiness(businessType.trim(), businessUuid.trim());
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        mapper.deleteByBusiness(businessType.trim(), businessUuid.trim());
        List<Path> pathsToDelete = new ArrayList<>();
        for (Attachment attachment : attachments) {
            Path resolved = resolvePhysicalPathSafely(attachment);
            if (resolved != null) {
                pathsToDelete.add(resolved);
            }
        }
        if (pathsToDelete.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFilesQuietly(pathsToDelete);
                }
            });
        } else {
            deleteFilesQuietly(pathsToDelete);
        }
    }

    private void deleteFilesQuietly(List<Path> paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                log.warn("Failed to remove orphaned attachment file {}: {}", path, e.getMessage());
            }
        }
    }

    private Path resolvePhysicalPathSafely(Attachment attachment) {
        if (attachment == null || !StringUtils.hasText(attachment.getStoragePath())) {
            return null;
        }
        try {
            String rawStoragePath = attachment.getStoragePath().trim();
            if (containsParentTraversalSegment(rawStoragePath)) {
                return null;
            }
            String root = systemConfigService != null
                    ? systemConfigService.getString("attachment.storage.root", storageRoot)
                    : storageRoot;
            if (!StringUtils.hasText(root)) {
                root = "uploads";
            }
            Path rootPath = Paths.get(root).toAbsolutePath().normalize();
            if (isWindowsDrivePath(rawStoragePath) && File.separatorChar != '\\') {
                String normalized = rawStoragePath.replace('\\', '/');
                int lastSlash = normalized.lastIndexOf('/');
                String fileName = lastSlash >= 0 ? normalized.substring(lastSlash + 1).trim() : normalized;
                if (!StringUtils.hasText(fileName) || ".".equals(fileName) || "..".equals(fileName)) {
                    return null;
                }
                Path migrated = rootPath.resolve(fileName).normalize();
                return migrated.startsWith(rootPath) ? migrated : null;
            }
            Path storedPath = Paths.get(rawStoragePath);
            Path resolved = storedPath.isAbsolute() ? storedPath.normalize() : rootPath.resolve(storedPath).normalize();
            return resolved.startsWith(rootPath) ? resolved : null;
        } catch (Exception e) {
            return null;
        }
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

    private void validateAttachment(Attachment attachment) {
        if (!StringUtils.hasText(attachment.getBusinessType())
                || !BUSINESS_TYPES.contains(attachment.getBusinessType())) {
            throw new IllegalArgumentException("Business type is invalid");
        }
        if (!StringUtils.hasText(attachment.getFileName())) {
            throw new IllegalArgumentException("File name is required");
        }
        if (!StringUtils.hasText(attachment.getStoragePath())) {
            throw new IllegalArgumentException("Storage path is required");
        }
        if (attachment.getFileSize() != null && attachment.getFileSize() < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
    }

    private void touchCustomerAttachment(Attachment attachment) {
        if ("customers".equals(attachment.getBusinessType()) && StringUtils.hasText(attachment.getBusinessUuid())) {
            customerService.touchActivity(attachment.getBusinessUuid());
        }
    }
}
