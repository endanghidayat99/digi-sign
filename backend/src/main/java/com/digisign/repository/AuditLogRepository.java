package com.digisign.repository;

import com.digisign.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);
}
