package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Attachment;
import com.jerry.salesmanagement.service.AttachmentService;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SysUserService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachmentControllerStorageTests {

    @TempDir
    Path tempDir;

    @Test
    void uploadStoresRelativeFilenameAndCleansUpOnPersistenceFailure() throws Exception {
        Path root = tempDir.resolve("uploads");
        AttachmentService service = mock(AttachmentService.class);
        when(service.create(any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AttachmentController controller = createController(root, service);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../contract.pdf",
                "application/pdf",
                "pdf-bytes".getBytes(StandardCharsets.UTF_8)
        );

        ApiResponse<Attachment> response = controller.upload("contracts", "c-1", file);
        assertEquals(200, response.getCode());
        assertNull(response.getData().getStoragePath());

        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(service).create(captor.capture());
        String savedStoragePath = captor.getValue().getStoragePath();
        assertNotNull(savedStoragePath);
        assertFalse(Paths.get(savedStoragePath).isAbsolute());
        assertTrue(savedStoragePath.endsWith("-contract.pdf"));
        assertTrue(Files.exists(root.resolve(savedStoragePath)));

        // When persistence fails, the newly copied file must be cleaned up.
        AttachmentService failingService = mock(AttachmentService.class);
        when(failingService.create(any(Attachment.class))).thenThrow(new IllegalStateException("db down"));
        AttachmentController failingController = createController(root, failingService);
        long countBefore = Files.list(root).count();
        ApiResponse<Attachment> failedResponse = failingController.upload("contracts", "c-2", file);
        assertEquals(500, failedResponse.getCode());
        assertEquals(countBefore, Files.list(root).count());
    }

    @Test
    void downloadAndPreviewWorkWithRelativeAndMigratedLegacyAbsolutePaths() throws Exception {
        Path root = tempDir.resolve("uploads");
        Files.createDirectories(root);
        Path insideAbsoluteFile = root.resolve("u0-inside.pdf");
        Files.writeString(insideAbsoluteFile, "inside-absolute-content");

        Path relativeFile = root.resolve("u1-doc.pdf");
        Files.writeString(relativeFile, "relative-content");

        Path migratedFile = root.resolve("u2-legacy.pdf");
        Files.writeString(migratedFile, "migrated-content");

        Attachment insideLegacyAttachment = new Attachment();
        insideLegacyAttachment.setUuid("u0");
        insideLegacyAttachment.setFileName("u0-inside.pdf");
        insideLegacyAttachment.setOriginalFileName("inside.pdf");
        insideLegacyAttachment.setFileExt("pdf");
        insideLegacyAttachment.setMimeType("application/pdf");
        insideLegacyAttachment.setStoragePath(insideAbsoluteFile.toAbsolutePath().toString());

        Attachment relativeAttachment = new Attachment();
        relativeAttachment.setUuid("u1");
        relativeAttachment.setFileName("u1-doc.pdf");
        relativeAttachment.setOriginalFileName("doc.pdf");
        relativeAttachment.setFileExt("pdf");
        relativeAttachment.setMimeType("application/pdf");
        relativeAttachment.setStoragePath("u1-doc.pdf");

        Attachment migratedLegacyAttachment = new Attachment();
        migratedLegacyAttachment.setUuid("u2");
        migratedLegacyAttachment.setFileName("u2-legacy.pdf");
        migratedLegacyAttachment.setOriginalFileName("legacy.pdf");
        migratedLegacyAttachment.setFileExt("pdf");
        migratedLegacyAttachment.setMimeType("application/pdf");
        migratedLegacyAttachment.setStoragePath(tempDir.resolve("old-server/u2-legacy.pdf").toAbsolutePath().toString());

        AttachmentService service = mock(AttachmentService.class);
        when(service.getByUuid("u0")).thenReturn(insideLegacyAttachment);
        when(service.getByUuid("u1")).thenReturn(relativeAttachment);
        when(service.getByUuid("u2")).thenReturn(migratedLegacyAttachment);

        AttachmentController controller = createController(root, service);

        ResponseEntity<Resource> insideResponse = controller.download("u0");
        assertEquals(HttpStatus.OK, insideResponse.getStatusCode());
        assertNotNull(insideResponse.getBody());
        assertEquals("inside-absolute-content", insideResponse.getBody().getContentAsString(StandardCharsets.UTF_8));

        ResponseEntity<Resource> downloadResponse = controller.download("u1");
        assertEquals(HttpStatus.OK, downloadResponse.getStatusCode());
        assertNotNull(downloadResponse.getBody());
        assertEquals("relative-content", downloadResponse.getBody().getContentAsString(StandardCharsets.UTF_8));

        ResponseEntity<Resource> previewResponse = controller.previewPdf("u2");
        assertEquals(HttpStatus.OK, previewResponse.getStatusCode());
        assertNotNull(previewResponse.getBody());
        assertEquals("migrated-content", previewResponse.getBody().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void blocksPathTraversalForDownloadPreviewAndDelete() throws Exception {
        Path root = tempDir.resolve("uploads");
        Files.createDirectories(root);
        Path outsideSecret = tempDir.resolve("secret.txt");
        Files.writeString(outsideSecret, "top-secret");

        Attachment relativeTraversal = new Attachment();
        relativeTraversal.setUuid("t1");
        relativeTraversal.setOriginalFileName("secret.txt");
        relativeTraversal.setFileExt("pdf");
        relativeTraversal.setStoragePath("../secret.txt");

        Attachment absoluteOutside = new Attachment();
        absoluteOutside.setUuid("t2");
        absoluteOutside.setOriginalFileName("secret.txt");
        absoluteOutside.setFileExt("pdf");
        absoluteOutside.setStoragePath(outsideSecret.toAbsolutePath().toString());

        AttachmentService service = mock(AttachmentService.class);
        when(service.getByUuid("t1")).thenReturn(relativeTraversal);
        when(service.getByUuid("t2")).thenReturn(absoluteOutside);

        AttachmentController controller = createController(root, service);

        assertEquals(HttpStatus.NOT_FOUND, controller.download("t1").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.previewPdf("t1").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.download("t2").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.previewPdf("t2").getStatusCode());

        assertEquals(500, controller.delete("t1").getCode());
        assertEquals(500, controller.delete("t2").getCode());
        verify(service, never()).delete("t1");
        verify(service, never()).delete("t2");
        assertTrue(Files.exists(outsideSecret));
    }

    @Test
    void deleteRemovesDatabaseRecordBeforeDeletingFile() throws Exception {
        Path root = tempDir.resolve("uploads");
        Files.createDirectories(root);
        Path target = root.resolve("u3-file.txt");
        Files.writeString(target, "keep-until-db-deleted");

        Attachment attachment = new Attachment();
        attachment.setUuid("u3");
        attachment.setStoragePath("u3-file.txt");

        AttachmentService failingDeleteService = mock(AttachmentService.class);
        when(failingDeleteService.getByUuid("u3")).thenReturn(attachment);
        doThrow(new IllegalStateException("db delete failed")).when(failingDeleteService).delete("u3");

        AttachmentController failingController = createController(root, failingDeleteService);
        ApiResponse<Void> failed = failingController.delete("u3");
        assertEquals(500, failed.getCode());
        assertTrue(Files.exists(target));

        AttachmentService okService = mock(AttachmentService.class);
        when(okService.getByUuid("u3")).thenReturn(attachment);
        AttachmentController okController = createController(root, okService);
        ApiResponse<Void> ok = okController.delete("u3");
        assertEquals(200, ok.getCode());
        verify(okService).delete("u3");
        assertFalse(Files.exists(target));
    }

    private AttachmentController createController(Path root, AttachmentService service) {
        SystemConfigService systemConfigService = mock(SystemConfigService.class);
        when(systemConfigService.getString("attachment.storage.root", root.toString())).thenReturn(root.toString());

        AttachmentController controller = new AttachmentController();
        ReflectionTestUtils.setField(controller, "service", service);
        ReflectionTestUtils.setField(controller, "auditTrailService", mock(AuditTrailService.class));
        ReflectionTestUtils.setField(controller, "systemConfigService", systemConfigService);
        ReflectionTestUtils.setField(controller, "sysUserService", mock(SysUserService.class));
        ReflectionTestUtils.setField(controller, "storageRoot", root.toString());
        return controller;
    }
}
