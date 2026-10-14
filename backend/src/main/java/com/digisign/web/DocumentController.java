package com.digisign.web;

import com.digisign.domain.Document;
import com.digisign.security.UserPrincipal;
import com.digisign.service.DocumentService;
import com.digisign.web.dto.Dtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> search(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(required = false) Long typeId,
                                      @RequestParam(required = false) Document.Status status,
                                      @RequestParam(defaultValue = "false") boolean all,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @AuthenticationPrincipal UserPrincipal me) {
        Page<DocumentSummaryDto> p = service.search(q, typeId, status, all, page, size, me);
        return Map.of("items", p.getContent(), "total", p.getTotalElements(), "page", p.getNumber(), "size", p.getSize());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentDto create(@Valid @RequestPart("data") CreateDocumentRequest data,
                              @RequestPart("file") MultipartFile file,
                              @AuthenticationPrincipal UserPrincipal me,
                              HttpServletRequest http) throws IOException {
        return service.create(data, file.getBytes(), file.getOriginalFilename(), me, WebUtil.clientIp(http));
    }

    @GetMapping("/pending")
    public List<PendingDto> pending(@AuthenticationPrincipal UserPrincipal me) {
        return service.pending(me);
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard(@AuthenticationPrincipal UserPrincipal me) {
        return service.dashboard(me);
    }

    @GetMapping("/{id}")
    public DocumentDto get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal me) {
        return service.get(id, me);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable UUID id,
                                       @RequestParam(defaultValue = "false") boolean original,
                                       @RequestParam(defaultValue = "false") boolean download,
                                       @AuthenticationPrincipal UserPrincipal me) {
        var f = service.file(id, original, me);
        return WebUtil.pdf(f.filename(), f.data(), !download);
    }

    @PostMapping("/{id}/sign")
    public DocumentDto sign(@PathVariable UUID id, @Valid @RequestBody SignRequest req,
                            @AuthenticationPrincipal UserPrincipal me, HttpServletRequest http) {
        return service.sign(id, req, me, WebUtil.clientIp(http));
    }

    @PostMapping("/{id}/reject")
    public DocumentDto reject(@PathVariable UUID id, @Valid @RequestBody NoteRequest req,
                              @AuthenticationPrincipal UserPrincipal me, HttpServletRequest http) {
        return service.reject(id, req.note(), me, WebUtil.clientIp(http));
    }

    @PostMapping("/{id}/cancel")
    public DocumentDto cancel(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal me, HttpServletRequest http) {
        return service.cancel(id, me, WebUtil.clientIp(http));
    }
}
