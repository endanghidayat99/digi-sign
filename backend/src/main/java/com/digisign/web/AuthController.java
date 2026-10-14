package com.digisign.web;

import com.digisign.domain.AppUser;
import com.digisign.repository.AppUserRepository;
import com.digisign.security.JwtService;
import com.digisign.security.UserPrincipal;
import com.digisign.service.AuditService;
import com.digisign.web.dto.Dtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;

    public AuthController(AppUserRepository users, PasswordEncoder encoder, JwtService jwt, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        AppUser u = users.findByUsernameIgnoreCase(req.username().trim())
                .filter(AppUser::isActive)
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("bad"));
        audit.log(null, u.getId(), "LOGIN", null, WebUtil.clientIp(http));
        return new LoginResponse(jwt.generate(u), jwt.getExpirationMinutes(), UserDto.of(u));
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal UserPrincipal me) {
        return UserDto.of(users.findById(me.id()).orElseThrow());
    }

    @PutMapping("/me")
    @Transactional
    public UserDto updateProfile(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody ProfileRequest req) {
        AppUser u = users.findById(me.id()).orElseThrow();
        u.setFullName(req.fullName());
        u.setPosition(req.position());
        u.setDepartment(req.department());
        u.setEmail(req.email());
        return UserDto.of(users.save(u));
    }

    @PostMapping("/change-password")
    @Transactional
    public void changePassword(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody ChangePasswordRequest req) {
        AppUser u = users.findById(me.id()).orElseThrow();
        if (!encoder.matches(req.oldPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("Password lama salah");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(u);
    }
}
