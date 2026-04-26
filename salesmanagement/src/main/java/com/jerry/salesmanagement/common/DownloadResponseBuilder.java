package com.jerry.salesmanagement.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class DownloadResponseBuilder {

    private DownloadResponseBuilder() {
    }

    public static ResponseEntity<byte[]> build(String fileName, MediaType mediaType, byte[] content) {
        String resolvedFileName = fileName == null || fileName.isBlank() ? "download" : fileName.trim();
        MediaType resolvedMediaType = mediaType != null ? mediaType : MediaType.APPLICATION_OCTET_STREAM;
        byte[] resolvedContent = content != null ? content : new byte[0];
        String encodedFileName = URLEncoder.encode(resolvedFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(resolvedMediaType)
                .contentLength(resolvedContent.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName
                )
                .body(resolvedContent);
    }
}
