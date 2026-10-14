package com.digisign.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

public final class WebUtil {
    private WebUtil() {}

    /** IP klien (server.forward-headers-strategy=framework sudah menangani X-Forwarded-For dari reverse proxy). */
    public static String clientIp(HttpServletRequest req) {
        return req.getRemoteAddr();
    }

    public static ResponseEntity<byte[]> pdf(String filename, byte[] data, boolean inline) {
        ContentDisposition cd = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(filename, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(data);
    }
}
