package com.digisign.repository;

import com.digisign.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    boolean existsByDocumentNumberIgnoreCase(String documentNumber);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Document d where d.id = :id")
    java.util.Optional<Document> findForUpdate(@Param("id") UUID id);

    long countByDocumentTypeId(Long typeId);

    long countByStatus(Document.Status status);

    @Query("""
            select d from Document d
            where (:typeId is null or d.documentType.id = :typeId)
              and (:status is null or d.status = :status)
              and (:q = '' or lower(d.documentNumber) like concat('%', :q, '%')
                           or lower(d.title) like concat('%', :q, '%'))
              and (:userId is null or d.createdBy.id = :userId
                   or exists (select 1 from Signature s where s.document = d and s.signer.id = :userId))
            """)
    Page<Document> search(@Param("q") String q,
                          @Param("typeId") Long typeId,
                          @Param("status") Document.Status status,
                          @Param("userId") Long userId,
                          Pageable pageable);
}
