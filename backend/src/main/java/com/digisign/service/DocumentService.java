package com.digisign.service;

import com.digisign.config.AppProperties;
import com.digisign.domain.AppUser;
import com.digisign.domain.Document;
import com.digisign.domain.DocumentType;
import com.digisign.domain.Signature;
import com.digisign.repository.AppUserRepository;
import com.digisign.repository.DocumentRepository;
import com.digisign.repository.DocumentTypeRepository;
import com.digisign.repository.SignatureRepository;
import com.digisign.security.UserPrincipal;
import com.digisign.web.ApiException;
import com.digisign.web.dto.Dtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DocumentService {

    private static final int QR_PX = 600;

    private final DocumentRepository documents;
    private final DocumentTypeRepository types;
    private final SignatureRepository signatures;
    private final AppUserRepository users;
    private final StorageService storage;
    private final PdfStampService pdf;
    private final QrCodeService qr;
    private final DocumentNumberService numbering;
    private final AuditService audit;
    private final AppProperties props;
    private final ZoneId zone;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    public DocumentService(DocumentRepository documents, DocumentTypeRepository types, SignatureRepository signatures,
                           AppUserRepository users, StorageService storage, PdfStampService pdf, QrCodeService qr,
                           DocumentNumberService numbering, AuditService audit, AppProperties props) {
        this.documents = documents;
        this.types = types;
        this.signatures = signatures;
        this.users = users;
        this.storage = storage;
        this.pdf = pdf;
        this.qr = qr;
        this.numbering = numbering;
        this.audit = audit;
        this.props = props;
        this.zone = ZoneId.of(props.timezone());
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public DocumentDto create(CreateDocumentRequest req, byte[] file, String filename, UserPrincipal me, String ip) {
        if (file == null || file.length < 5 || !new String(file, 0, 5).startsWith("%PDF")) {
            throw ApiException.badRequest("File harus berformat PDF");
        }
        int pageCount;
        try {
            pageCount = pdf.inspect(file).pageCount();
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }

        DocumentType type = types.findById(req.documentTypeId())
                .filter(DocumentType::isActive)
                .orElseThrow(() -> ApiException.badRequest("Jenis dokumen tidak ditemukan / nonaktif"));

        String number;
        if (req.documentNumber() != null && !req.documentNumber().isBlank()) {
            number = req.documentNumber().trim();
            if (documents.existsByDocumentNumberIgnoreCase(number)) {
                throw ApiException.badRequest("Nomor dokumen " + number + " sudah digunakan");
            }
        } else {
            do {
                number = numbering.next(type);
            } while (documents.existsByDocumentNumberIgnoreCase(number));
        }

        AppUser creator = users.getReferenceById(me.id());
        UUID id = UUID.randomUUID();
        String basePath = "documents/" + LocalDate.now(zone).getYear() + "/" + id + "/";
        String originalPath = basePath + "v0-original.pdf";
        storage.write(originalPath, file);
        String hash = CryptoUtil.sha256(file);

        Document d = new Document();
        d.setId(id);
        d.setDocumentNumber(number);
        d.setDocumentType(type);
        d.setTitle(req.title().trim());
        d.setDescription(req.description());
        d.setDocumentDate(req.documentDate() != null ? req.documentDate() : LocalDate.now(zone));
        d.setStatus(Document.Status.IN_PROGRESS);
        d.setOriginalFilename(filename == null ? "dokumen.pdf" : filename);
        d.setOriginalPath(originalPath);
        d.setOriginalHash(hash);
        d.setCurrentPath(originalPath);
        d.setCurrentHash(hash);
        d.setVersion(0);
        d.setPageCount(pageCount);
        d.setCreatedBy(creator);

        Set<Long> seen = new HashSet<>();
        int order = 1;
        for (SignerRequest sr : req.signers()) {
            if (!seen.add(sr.userId())) continue;
            AppUser signer = users.findById(sr.userId()).filter(AppUser::isActive)
                    .orElseThrow(() -> ApiException.badRequest("Penandatangan id " + sr.userId() + " tidak ditemukan/nonaktif"));
            Signature s = new Signature();
            s.setId(UUID.randomUUID());
            s.setDocument(d);
            s.setSigner(signer);
            s.setSignOrder(order++);
            s.setRoleLabel(sr.roleLabel());
            s.setStatus(Signature.Status.PENDING);
            d.getSignatures().add(s);
        }
        // persist (bukan save/merge) karena ID di-assign manual
        em.persist(d);
        audit.log(id, me.id(), "CREATE", "Dokumen " + number + " dibuat (" + d.getSignatures().size() + " penandatangan)", ip);
        return toDto(d, me);
    }

    // ------------------------------------------------------------------ sign

    @Transactional
    public DocumentDto sign(UUID documentId, SignRequest req, UserPrincipal me, String ip) {
        Document d = documents.findForUpdate(documentId).orElseThrow(() -> ApiException.notFound("Dokumen tidak ditemukan"));
        if (d.getStatus() != Document.Status.IN_PROGRESS) {
            throw ApiException.badRequest("Dokumen berstatus " + d.getStatus() + ", tidak bisa ditandatangani");
        }
        Signature s = d.getSignatures().stream().filter(x -> x.getSigner().getId().equals(me.id())).findFirst()
                .orElseThrow(() -> ApiException.forbidden("Anda bukan penandatangan dokumen ini"));
        if (s.getStatus() != Signature.Status.PENDING) {
            throw ApiException.badRequest("Anda sudah memproses dokumen ini");
        }
        if (req.page() > d.getPageCount()) {
            throw ApiException.badRequest("Halaman melebihi jumlah halaman dokumen");
        }

        AppUser signer = s.getSigner();
        Instant now = Instant.now();
        String token;
        do {
            token = CryptoUtil.randomToken(18);
        } while (signatures.findByVerificationToken(token).isPresent());

        BufferedImage qrImage = qr.generate(props.verifyUrl(token), QR_PX);
        String[] caption = buildCaption(signer, now);
        byte[] current = storage.read(d.getCurrentPath());
        byte[] stamped = pdf.stamp(current, qrImage,
                new PdfStampService.Placement(req.page(), req.x(), req.y(), req.size(), req.caption()), caption);

        int version = d.getVersion() + 1;
        String path = d.getOriginalPath().replace("v0-original.pdf", "v" + version + "-signed.pdf");
        storage.write(path, stamped);
        String hashAfter = CryptoUtil.sha256(stamped);

        s.setStatus(Signature.Status.SIGNED);
        s.setVerificationToken(token);
        s.setSignerName(signer.getFullName());
        s.setSignerPosition(signer.getPosition());
        s.setSignerDepartment(signer.getDepartment());
        s.setSignedAt(now);
        s.setSignedIp(ip);
        s.setNote(req.note());
        s.setPageNumber(req.page());
        s.setPosX(req.x());
        s.setPosY(req.y());
        s.setQrSize(req.size());
        s.setHashBefore(d.getCurrentHash());
        s.setHashAfter(hashAfter);
        s.setSeal(seal(d, s));

        d.setCurrentPath(path);
        d.setCurrentHash(hashAfter);
        d.setVersion(version);
        if (d.getSignatures().stream().allMatch(x -> x.getStatus() == Signature.Status.SIGNED)) {
            d.setStatus(Document.Status.COMPLETED);
            d.setCompletedAt(now);
        }
        audit.log(d.getId(), me.id(), "SIGN", "Ditandatangani oleh " + signer.getFullName(), ip);
        return toDto(d, me);
    }

    @Transactional
    public DocumentDto reject(UUID documentId, String note, UserPrincipal me, String ip) {
        Document d = documents.findForUpdate(documentId).orElseThrow(() -> ApiException.notFound("Dokumen tidak ditemukan"));
        if (d.getStatus() != Document.Status.IN_PROGRESS) {
            throw ApiException.badRequest("Dokumen tidak dalam proses tanda tangan");
        }
        Signature s = d.getSignatures().stream().filter(x -> x.getSigner().getId().equals(me.id())).findFirst()
                .orElseThrow(() -> ApiException.forbidden("Anda bukan penandatangan dokumen ini"));
        if (s.getStatus() != Signature.Status.PENDING) {
            throw ApiException.badRequest("Anda sudah memproses dokumen ini");
        }
        s.setStatus(Signature.Status.REJECTED);
        s.setNote(note);
        s.setSignerName(s.getSigner().getFullName());
        s.setSignerPosition(s.getSigner().getPosition());
        d.setStatus(Document.Status.REJECTED);
        audit.log(d.getId(), me.id(), "REJECT", note, ip);
        return toDto(d, me);
    }

    @Transactional
    public DocumentDto cancel(UUID documentId, UserPrincipal me, String ip) {
        Document d = documents.findForUpdate(documentId).orElseThrow(() -> ApiException.notFound("Dokumen tidak ditemukan"));
        if (!canManage(d, me)) throw ApiException.forbidden("Hanya pembuat dokumen atau admin");
        if (d.getStatus() == Document.Status.CANCELLED) throw ApiException.badRequest("Dokumen sudah dibatalkan");
        d.setStatus(Document.Status.CANCELLED);
        audit.log(d.getId(), me.id(), "CANCEL", "Dokumen dibatalkan", ip);
        return toDto(d, me);
    }

    // ------------------------------------------------------------------ query

    @Transactional(readOnly = true)
    public Page<DocumentSummaryDto> search(String q, Long typeId, Document.Status status, boolean all,
                                          int page, int size, UserPrincipal me) {
        Long userFilter = (all && me.isAdmin()) ? null : me.id();
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return documents.search(q == null ? "" : q.trim().toLowerCase(), typeId, status, userFilter, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public DocumentDto get(UUID id, UserPrincipal me) {
        Document d = load(id, me);
        return toDto(d, me);
    }

    @Transactional(readOnly = true)
    public FileResult file(UUID id, boolean original, UserPrincipal me) {
        Document d = load(id, me);
        String path = original ? d.getOriginalPath() : d.getCurrentPath();
        return new FileResult(fileName(d, original), storage.read(path));
    }

    @Transactional(readOnly = true)
    public List<PendingDto> pending(UserPrincipal me) {
        return signatures.findPendingForUser(me.id()).stream()
                .map(s -> new PendingDto(s.getId(), s.getDocument().getId(), s.getDocument().getDocumentNumber(),
                        TypeRef.of(s.getDocument().getDocumentType()), s.getDocument().getTitle(), s.getRoleLabel(),
                        s.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardDto dashboard(UserPrincipal me) {
        Instant monthStart = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant();
        List<PendingDto> pending = pending(me);
        return new DashboardDto(documents.count(),
                documents.countByStatus(Document.Status.IN_PROGRESS),
                documents.countByStatus(Document.Status.COMPLETED),
                pending.size(),
                signatures.countByStatusAndSignedAtAfter(Signature.Status.SIGNED, monthStart),
                pending.stream().limit(10).toList());
    }

    public record FileResult(String filename, byte[] data) {}

    // ------------------------------------------------------------------ helpers

    String seal(Document d, Signature s) {
        String payload = String.join("|",
                s.getId().toString(), s.getVerificationToken(), d.getId().toString(), d.getDocumentNumber(),
                String.valueOf(s.getSigner().getId()), s.getSignerName(),
                String.valueOf(s.getSignedAt().toEpochMilli()), s.getHashAfter());
        return CryptoUtil.hmacSha256(props.signatureSecret(), payload);
    }

    boolean isSealValid(Signature s) {
        if (s.getSeal() == null || s.getSignedAt() == null) return false;
        return CryptoUtil.constantTimeEquals(s.getSeal(), seal(s.getDocument(), s));
    }

    private String[] buildCaption(AppUser signer, Instant when) {
        String date = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm z", Locale.forLanguageTag("id"))
                .format(when.atZone(zone));
        List<String> lines = new ArrayList<>();
        lines.add("Ditandatangani secara digital oleh");
        lines.add(signer.getFullName());
        if (signer.getPosition() != null && !signer.getPosition().isBlank()) lines.add(signer.getPosition());
        lines.add(date);
        return lines.toArray(String[]::new);
    }

    private Document load(UUID id, UserPrincipal me) {
        Document d = documents.findById(id).orElseThrow(() -> ApiException.notFound("Dokumen tidak ditemukan"));
        boolean allowed = me.isAdmin() || d.getCreatedBy().getId().equals(me.id())
                || d.getSignatures().stream().anyMatch(s -> s.getSigner().getId().equals(me.id()));
        if (!allowed) throw ApiException.forbidden("Anda tidak memiliki akses ke dokumen ini");
        return d;
    }

    private boolean canManage(Document d, UserPrincipal me) {
        return me.isAdmin() || d.getCreatedBy().getId().equals(me.id());
    }

    private String fileName(Document d, boolean original) {
        String base = d.getDocumentNumber().replaceAll("[^A-Za-z0-9._-]+", "_");
        return base + (original ? "_original.pdf" : (d.getVersion() > 0 ? "_signed.pdf" : ".pdf"));
    }

    private DocumentSummaryDto toSummary(Document d) {
        int signed = (int) d.getSignatures().stream().filter(s -> s.getStatus() == Signature.Status.SIGNED).count();
        return new DocumentSummaryDto(d.getId(), d.getDocumentNumber(), TypeRef.of(d.getDocumentType()), d.getTitle(),
                d.getStatus().name(), signed, d.getSignatures().size(), d.getCreatedBy().getFullName(),
                d.getCreatedAt(), d.getCompletedAt());
    }

    private DocumentDto toDto(Document d, UserPrincipal me) {
        List<SignatureDto> sigs = d.getSignatures().stream()
                .sorted(Comparator.comparingInt(Signature::getSignOrder))
                .map(s -> new SignatureDto(s.getId(), s.getSigner().getId(),
                        s.getSignerName() != null ? s.getSignerName() : s.getSigner().getFullName(),
                        s.getSignerPosition() != null ? s.getSignerPosition() : s.getSigner().getPosition(),
                        s.getSignerDepartment() != null ? s.getSignerDepartment() : s.getSigner().getDepartment(),
                        s.getRoleLabel(), s.getSignOrder(), s.getStatus().name(), s.getSignedAt(), s.getNote(),
                        s.getPageNumber(),
                        s.getVerificationToken() != null ? props.verifyUrl(s.getVerificationToken()) : null))
                .toList();
        Signature mine = d.getSignatures().stream().filter(s -> s.getSigner().getId().equals(me.id())).findFirst().orElse(null);
        boolean canSign = mine != null && mine.getStatus() == Signature.Status.PENDING
                && d.getStatus() == Document.Status.IN_PROGRESS;
        return new DocumentDto(d.getId(), d.getDocumentNumber(), TypeRef.of(d.getDocumentType()), d.getTitle(),
                d.getDescription(), d.getDocumentDate(), d.getStatus().name(), d.getOriginalFilename(),
                d.getPageCount(), d.getVersion(), d.getOriginalHash(), d.getCurrentHash(),
                d.getCreatedBy().getId(), d.getCreatedBy().getFullName(), d.getCreatedAt(), d.getCompletedAt(),
                sigs, mine == null ? null : mine.getId(), canSign, canManage(d, me));
    }
}
