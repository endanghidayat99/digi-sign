package com.digisign.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "signature")
@Getter
@Setter
public class Signature {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signer_id")
    private AppUser signer;

    @Column(name = "sign_order", nullable = false)
    private int signOrder = 1;

    /** Peran penandatangan pada dokumen, mis. "Pihak Pertama", "Tester", "Mengetahui" */
    @Column(name = "role_label")
    private String roleLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "verification_token", unique = true)
    private String verificationToken;

    @Column(name = "signer_name")
    private String signerName;

    @Column(name = "signer_position")
    private String signerPosition;

    @Column(name = "signer_department")
    private String signerDepartment;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "signed_ip")
    private String signedIp;

    private String note;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "pos_x")
    private Double posX;

    @Column(name = "pos_y")
    private Double posY;

    @Column(name = "qr_size")
    private Double qrSize;

    @Column(name = "hash_before")
    private String hashBefore;

    @Column(name = "hash_after")
    private String hashAfter;

    /** HMAC-SHA256 atas data tanda tangan, untuk mendeteksi perubahan data di database */
    private String seal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum Status { PENDING, SIGNED, REJECTED }
}
