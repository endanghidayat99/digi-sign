package com.digisign.web.dto;

import com.digisign.domain.AppUser;
import com.digisign.domain.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Kumpulan DTO request/response API. */
public final class Dtos {
    private Dtos() {}

    // ---------- Auth ----------
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record LoginResponse(String token, long expiresInMinutes, UserDto user) {}

    public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank @Size(min = 6) String newPassword) {}

    // ---------- User ----------
    public record UserDto(Long id, String username, String fullName, String position, String department,
                          String employeeNo, String email, String role, boolean active) {
        public static UserDto of(AppUser u) {
            return new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getPosition(), u.getDepartment(),
                    u.getEmployeeNo(), u.getEmail(), u.getRole().name(), u.isActive());
        }
    }

    public record UserRequest(
            @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "hanya huruf, angka, titik, _ dan -") String username,
            @Size(min = 6, message = "minimal 6 karakter") String password,
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 150) String position,
            @Size(max = 150) String department,
            @Size(max = 50) String employeeNo,
            @Email @Size(max = 150) String email,
            @NotNull AppUser.Role role,
            boolean active) {}

    public record ProfileRequest(@NotBlank @Size(max = 150) String fullName, @Size(max = 150) String position,
                                 @Size(max = 150) String department, @Email String email) {}

    // ---------- Document type ----------
    public record DocumentTypeDto(Long id, String code, String name, String description, String numberFormat,
                                  String color, boolean active, String numberExample, long documentCount) {}

    public record DocumentTypeRequest(
            @NotBlank @Size(max = 30) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "hanya huruf, angka, _ dan -") String code,
            @NotBlank @Size(max = 150) String name,
            String description,
            @NotBlank @Size(max = 200) String numberFormat,
            @Size(max = 20) String color,
            boolean active) {}

    public record TypeRef(Long id, String code, String name, String color) {
        public static TypeRef of(DocumentType t) {
            return new TypeRef(t.getId(), t.getCode(), t.getName(), t.getColor());
        }
    }

    // ---------- Document ----------
    public record SignerRequest(@NotNull Long userId, @Size(max = 100) String roleLabel) {}

    public record CreateDocumentRequest(
            @NotNull Long documentTypeId,
            @NotBlank @Size(max = 255) String title,
            String description,
            LocalDate documentDate,
            @Size(max = 150) String documentNumber,
            @NotEmpty(message = "minimal 1 penandatangan") @Valid List<SignerRequest> signers) {}

    public record SignatureDto(UUID id, Long signerId, String signerName, String signerPosition,
                               String signerDepartment, String roleLabel, int signOrder, String status,
                               Instant signedAt, String note, Integer pageNumber, String verificationUrl) {}

    public record DocumentSummaryDto(UUID id, String documentNumber, TypeRef type, String title, String status,
                                     int signedCount, int signerCount, String createdByName, Instant createdAt,
                                     Instant completedAt) {}

    public record DocumentDto(UUID id, String documentNumber, TypeRef type, String title, String description,
                              LocalDate documentDate, String status, String originalFilename, int pageCount,
                              int version, String originalHash, String currentHash, Long createdById,
                              String createdByName, Instant createdAt, Instant completedAt,
                              List<SignatureDto> signatures, UUID mySignatureId, boolean canSign,
                              boolean canManage) {}

    public record SignRequest(
            @Min(1) int page,
            @DecimalMin("0") @DecimalMax("1") double x,
            @DecimalMin("0") @DecimalMax("1") double y,
            @DecimalMin("0.04") @DecimalMax("0.5") double size,
            boolean caption,
            @Size(max = 500) String note) {}

    public record NoteRequest(@Size(max = 500) String note) {}

    public record PendingDto(UUID signatureId, UUID documentId, String documentNumber, TypeRef type, String title,
                             String roleLabel, Instant requestedAt) {}

    public record DashboardDto(long totalDocuments, long inProgress, long completed, long myPending,
                               long signedThisMonth, List<PendingDto> pending) {}

    // ---------- Public verification ----------
    public record BrandingDto(String companyName, String companyAddress, boolean hasLogo) {}

    public record VerifySignerDto(String name, String position, String department, String roleLabel,
                                  int signOrder, String status, Instant signedAt) {}

    public record VerificationDto(boolean valid, boolean sealValid, String message, String companyName,
                                  String documentNumber, String documentType, String documentTitle,
                                  LocalDate documentDate, String documentStatus, int pageCount,
                                  Instant createdAt, String currentHash,
                                  VerifySignerDto signature, List<VerifySignerDto> signers,
                                  boolean publicDownload) {}

    public record IntegrityResultDto(boolean match, String matchedVersion, String message, String uploadedHash) {}
}
