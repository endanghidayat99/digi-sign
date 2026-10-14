package com.digisign.web;

import com.digisign.domain.DocumentType;
import com.digisign.repository.DocumentRepository;
import com.digisign.repository.DocumentTypeRepository;
import com.digisign.service.DocumentNumberService;
import com.digisign.web.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/document-types")
public class DocumentTypeController {

    private final DocumentTypeRepository types;
    private final DocumentRepository documents;

    public DocumentTypeController(DocumentTypeRepository types, DocumentRepository documents) {
        this.types = types;
        this.documents = documents;
    }

    @GetMapping
    public List<DocumentTypeDto> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        var list = activeOnly ? types.findByActiveTrueOrderByNameAsc() : types.findAllByOrderByNameAsc();
        return list.stream().map(this::toDto).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public DocumentTypeDto create(@Valid @RequestBody DocumentTypeRequest req) {
        if (types.existsByCodeIgnoreCase(req.code())) throw ApiException.badRequest("Kode sudah digunakan");
        DocumentType t = new DocumentType();
        apply(t, req);
        return toDto(types.save(t));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public DocumentTypeDto update(@PathVariable Long id, @Valid @RequestBody DocumentTypeRequest req) {
        DocumentType t = types.findById(id).orElseThrow(() -> ApiException.notFound("Jenis dokumen tidak ditemukan"));
        if (!t.getCode().equalsIgnoreCase(req.code()) && types.existsByCodeIgnoreCase(req.code())) {
            throw ApiException.badRequest("Kode sudah digunakan");
        }
        apply(t, req);
        return toDto(types.save(t));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(@PathVariable Long id) {
        if (documents.countByDocumentTypeId(id) > 0) {
            throw ApiException.badRequest("Jenis dokumen sudah dipakai. Nonaktifkan saja, jangan dihapus.");
        }
        types.deleteById(id);
    }

    /** Pratinjau format nomor, mis. untuk form jenis dokumen. */
    @GetMapping("/preview-number")
    public String preview(@RequestParam String format, @RequestParam String code) {
        return DocumentNumberService.format(format, code.toUpperCase(), LocalDate.now(), 1);
    }

    private void apply(DocumentType t, DocumentTypeRequest req) {
        t.setCode(req.code().trim().toUpperCase());
        t.setName(req.name().trim());
        t.setDescription(req.description());
        t.setNumberFormat(req.numberFormat().trim());
        t.setColor(req.color() == null || req.color().isBlank() ? "#1565C0" : req.color());
        t.setActive(req.active());
    }

    private DocumentTypeDto toDto(DocumentType t) {
        return new DocumentTypeDto(t.getId(), t.getCode(), t.getName(), t.getDescription(), t.getNumberFormat(),
                t.getColor(), t.isActive(),
                DocumentNumberService.format(t.getNumberFormat(), t.getCode(), LocalDate.now(), 1),
                documents.countByDocumentTypeId(t.getId()));
    }
}
