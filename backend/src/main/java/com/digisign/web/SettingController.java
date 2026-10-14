package com.digisign.web;

import com.digisign.service.QrCodeService;
import com.digisign.service.SettingService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingController {

    private static final Set<String> EDITABLE = Set.of(
            SettingService.COMPANY_NAME, SettingService.COMPANY_ADDRESS, SettingService.QR_LOGO_RATIO);

    private final SettingService settings;
    private final QrCodeService qr;

    public SettingController(SettingService settings, QrCodeService qr) {
        this.settings = settings;
        this.qr = qr;
    }

    @GetMapping
    public Map<String, Object> get() {
        return Map.of("values", settings.all(), "hasLogo", settings.hasLogo());
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody Map<String, String> values) {
        values.forEach((k, v) -> {
            if (!EDITABLE.contains(k)) throw ApiException.badRequest("Setting tidak dikenal: " + k);
            if (k.equals(SettingService.QR_LOGO_RATIO)) {
                try {
                    double d = Double.parseDouble(v);
                    if (d < 0.1 || d > 0.28) throw ApiException.badRequest("Rasio logo antara 0.10 – 0.28");
                } catch (NumberFormatException e) {
                    throw ApiException.badRequest("Rasio logo harus angka");
                }
            }
            settings.set(k, v == null ? "" : v.trim());
        });
        return get();
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadLogo(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.getSize() > 2 * 1024 * 1024) throw ApiException.badRequest("Logo maksimal 2MB");
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        if (img == null) throw ApiException.badRequest("File logo harus gambar PNG/JPG");
        // normalisasi ke PNG agar konsisten
        settings.saveLogo(qr.toPng(img));
        return get();
    }

    @DeleteMapping("/logo")
    public Map<String, Object> deleteLogo() {
        settings.deleteLogo();
        return get();
    }

    @GetMapping(value = "/qr-preview", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> preview() {
        byte[] png = qr.toPng(qr.generate("https://contoh.perusahaan.co.id/verify/CONTOH-PREVIEW-QR", 400));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(png);
    }
}
