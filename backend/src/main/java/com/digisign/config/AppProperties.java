package com.digisign.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String publicBaseUrl,
        String storageDir,
        String timezone,
        Jwt jwt,
        String signatureSecret,
        boolean publicDownload,
        Admin admin
) {
    public record Jwt(String secret, long expirationMinutes) {}
    public record Admin(String username, String password, String fullName) {}

    public String verifyUrl(String token) {
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/verify/" + token;
    }
}
