package com.digisign.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "document")
@Getter
@Setter
public class Document {

    @Id
    private UUID id;

    @Column(name = "document_number", nullable = false, unique = true)
    private String documentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "document_date")
    private LocalDate documentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.IN_PROGRESS;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "original_path", nullable = false)
    private String originalPath;

    @Column(name = "original_hash", nullable = false)
    private String originalHash;

    @Column(name = "current_path", nullable = false)
    private String currentPath;

    @Column(name = "current_hash", nullable = false)
    private String currentHash;

    @Column(nullable = false)
    private int version;

    @Column(name = "page_count", nullable = false)
    private int pageCount = 1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("signOrder ASC")
    private List<Signature> signatures = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum Status { IN_PROGRESS, COMPLETED, REJECTED, CANCELLED }
}
