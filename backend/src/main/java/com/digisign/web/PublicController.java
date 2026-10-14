package com.digisign.web;

import com.digisign.service.SettingService;
import com.digisign.service.VerificationService;
import com.digisign.web.dto.Dtos.*;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

/** Endpoint tanpa login: verifikasi hasil scan QR dan branding perusahaan. */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final VerificationService verification;
    private final SettingService settings;

    public PublicController(VerificationService verification, SettingService settings) {
        this.verification = verification;
        this.settings = settings;
    }

    @GetMapping("/branding")
    public BrandingDto branding() {
        return new BrandingDto(settings.companyName(), settings.get(SettingService.COMPANY_ADDRESS, ""), settings.hasLogo());
    }

    @GetMapping(value = "/branding/logo", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> logo() {
        byte[] logo = settings.logo();
        if (logo == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5))).body(logo);
    }

    @GetMapping("/verify/{token}")
    public VerificationDto verify(@PathVariable String token) {
        return verification.verify(token);
    }

    @PostMapping(value = "/verify/{token}/check", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IntegrityResultDto check(@PathVariable String token, @RequestPart("file") MultipartFile file) throws IOException {
        return verification.checkFile(token, file.getBytes());
    }

    @GetMapping("/verify/{token}/file")
    public ResponseEntity<byte[]> file(@PathVariable String token) {
        var f = verification.download(token);
        return WebUtil.pdf(f.filename(), f.data(), true);
    }
}
