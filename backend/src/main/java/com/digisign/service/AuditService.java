package com.digisign.service;

import com.digisign.domain.AuditLog;
import com.digisign.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(UUID documentId, Long userId, String action, String detail, String ip) {
        AuditLog a = new AuditLog();
        a.setDocumentId(documentId);
        a.setUserId(userId);
        a.setAction(action);
        a.setDetail(detail);
        a.setIpAddress(ip);
        repo.save(a);
    }
}
