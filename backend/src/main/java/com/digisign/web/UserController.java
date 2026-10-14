package com.digisign.web;

import com.digisign.domain.AppUser;
import com.digisign.repository.AppUserRepository;
import com.digisign.security.UserPrincipal;
import com.digisign.web.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public UserController(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** Daftar user aktif — dipakai semua user untuk memilih penandatangan. */
    @GetMapping("/active")
    public List<UserDto> active() {
        return users.findByActiveTrueOrderByFullNameAsc().stream().map(UserDto::of).toList();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> list() {
        return users.findAllByOrderByFullNameAsc().stream().map(UserDto::of).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserDto create(@Valid @RequestBody UserRequest req) {
        if (users.existsByUsernameIgnoreCase(req.username())) {
            throw ApiException.badRequest("Username sudah digunakan");
        }
        if (req.password() == null || req.password().isBlank()) {
            throw ApiException.badRequest("Password wajib diisi");
        }
        AppUser u = new AppUser();
        u.setUsername(req.username().trim());
        u.setPasswordHash(encoder.encode(req.password()));
        apply(u, req);
        return UserDto.of(users.save(u));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserRequest req,
                          @AuthenticationPrincipal UserPrincipal me) {
        AppUser u = users.findById(id).orElseThrow(() -> ApiException.notFound("User tidak ditemukan"));
        if (!u.getUsername().equalsIgnoreCase(req.username()) && users.existsByUsernameIgnoreCase(req.username())) {
            throw ApiException.badRequest("Username sudah digunakan");
        }
        if (u.getId().equals(me.id()) && (!req.active() || req.role() != AppUser.Role.ADMIN)) {
            throw ApiException.badRequest("Tidak bisa menonaktifkan / menurunkan role akun sendiri");
        }
        u.setUsername(req.username().trim());
        if (req.password() != null && !req.password().isBlank()) {
            u.setPasswordHash(encoder.encode(req.password()));
        }
        apply(u, req);
        return UserDto.of(users.save(u));
    }

    private void apply(AppUser u, UserRequest req) {
        u.setFullName(req.fullName().trim());
        u.setPosition(req.position());
        u.setDepartment(req.department());
        u.setEmployeeNo(req.employeeNo());
        u.setEmail(req.email());
        u.setRole(req.role());
        u.setActive(req.active());
    }
}
