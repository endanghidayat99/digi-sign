package com.digisign.repository;

import com.digisign.domain.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SignatureRepository extends JpaRepository<Signature, UUID> {

    Optional<Signature> findByVerificationToken(String token);

    @Query("""
            select s from Signature s join fetch s.document d join fetch d.documentType
            where s.signer.id = :userId and s.status = com.digisign.domain.Signature.Status.PENDING
              and d.status = com.digisign.domain.Document.Status.IN_PROGRESS
            order by s.createdAt asc
            """)
    List<Signature> findPendingForUser(Long userId);

    long countBySignerIdAndStatus(Long signerId, Signature.Status status);

    long countByStatusAndSignedAtAfter(Signature.Status status, Instant after);
}
