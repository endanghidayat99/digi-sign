package com.digisign.repository;

import com.digisign.domain.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {
    boolean existsByCodeIgnoreCase(String code);
    List<DocumentType> findAllByOrderByNameAsc();
    List<DocumentType> findByActiveTrueOrderByNameAsc();
}
