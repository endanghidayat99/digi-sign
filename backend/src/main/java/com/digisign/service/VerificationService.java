package com.digisign.service;

import com.digisign.config.AppProperties;
import com.digisign.domain.Document;
import com.digisign.domain.Signature;
import com.digisign.repository.SignatureRepository;
import com.digisign.web.ApiException;
import com.digisign.web.dto.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Verifikasi publik dari hasil scan QR Code. */
@Service
public class VerificationService {

    private final SignatureRepository signatures;
    private final DocumentService documentService;
    private final SettingService settings;
    private final StorageService storage;
    private final AppProperties props;

    public VerificationService(SignatureRepository signatures, DocumentService documentService,
                               SettingService settings, StorageService storage, AppProperties props) {
        this.signatures = signatures;
        this.documentService = documentService;
        this.settings = settings;
        this.storage = storage;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public VerificationDto verify(String token) {
        Signature s = find(token);
        Document d = s.getDocument();
        boolean sealValid = documentService.isSealValid(s);
        boolean cancelled = d.getStatus() == Document.Status.CANCELLED;
        boolean valid = s.getStatus() == Signature.Status.SIGNED && sealValid && !cancelled;

        String message;
        if (!sealValid) {
            message = "Data tanda tangan tidak cocok dengan segel digital. Kemungkinan data telah diubah.";
        } else if (cancelled) {
            message = "Tanda tangan asli, namun dokumen ini telah DIBATALKAN dan tidak berlaku.";
        } else if (d.getStatus() == Document.Status.COMPLETED) {
            message = "Tanda tangan sah. Dokumen telah ditandatangani oleh seluruh penandatangan.";
        } else {
            message = "Tanda tangan sah. Dokumen masih dalam proses penandatanganan pihak lain.";
        }

        List<VerifySignerDto> all = d.getSignatures().stream()
                .sorted(Comparator.comparingInt(Signature::getSignOrder))
                .map(this::signer)
                .toList();

        return new VerificationDto(valid, sealValid, message, settings.companyName(),
                d.getDocumentNumber(), d.getDocumentType().getName(), d.getTitle(), d.getDocumentDate(),
                d.getStatus().name(), d.getPageCount(), d.getCreatedAt(), d.getCurrentHash(),
                signer(s), all, props.publicDownload());
    }

    /** Cocokkan hash file yang diunggah dengan versi-versi dokumen yang tercatat. */
    @Transactional(readOnly = true)
    public IntegrityResultDto checkFile(String token, byte[] file) {
        Document d = find(token).getDocument();
        String hash = CryptoUtil.sha256(file);
        if (hash.equals(d.getCurrentHash())) {
            return new IntegrityResultDto(true, "FINAL",
                    "File identik dengan dokumen versi terakhir yang tercatat di sistem.", hash);
        }
        for (Signature s : d.getSignatures()) {
            if (hash.equals(s.getHashAfter())) {
                return new IntegrityResultDto(true, "AFTER_SIGNATURE_" + s.getSignOrder(),
                        "File identik dengan versi setelah ditandatangani oleh " + s.getSignerName()
                                + " (bukan versi terakhir — ada tanda tangan berikutnya).", hash);
            }
        }
        if (hash.equals(d.getOriginalHash())) {
            return new IntegrityResultDto(true, "ORIGINAL",
                    "File identik dengan dokumen asli sebelum ditandatangani.", hash);
        }
        return new IntegrityResultDto(false, null,
                "File TIDAK cocok dengan dokumen yang tercatat. File mungkin telah diubah setelah ditandatangani.", hash);
    }

    @Transactional(readOnly = true)
    public DocumentService.FileResult download(String token) {
        if (!props.publicDownload()) throw ApiException.forbidden("Unduhan publik tidak diaktifkan");
        Document d = find(token).getDocument();
        if (d.getStatus() == Document.Status.CANCELLED) throw ApiException.forbidden("Dokumen dibatalkan");
        String name = d.getDocumentNumber().replaceAll("[^A-Za-z0-9._-]+", "_") + ".pdf";
        return new DocumentService.FileResult(name, storage.read(d.getCurrentPath()));
    }

    private Signature find(String token) {
        if (token == null || token.length() < 10 || token.length() > 64) {
            throw ApiException.notFound("Kode verifikasi tidak valid");
        }
        return signatures.findByVerificationToken(token)
                .orElseThrow(() -> ApiException.notFound("Tanda tangan tidak ditemukan. Dokumen ini TIDAK terdaftar di sistem."));
    }

    private VerifySignerDto signer(Signature s) {
        return new VerifySignerDto(
                s.getSignerName() != null ? s.getSignerName() : s.getSigner().getFullName(),
                s.getSignerPosition() != null ? s.getSignerPosition() : s.getSigner().getPosition(),
                s.getSignerDepartment() != null ? s.getSignerDepartment() : s.getSigner().getDepartment(),
                s.getRoleLabel(), s.getSignOrder(), s.getStatus().name(), s.getSignedAt());
    }
}
