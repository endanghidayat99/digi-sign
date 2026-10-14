package com.digisign.config;

import com.digisign.domain.AppUser;
import com.digisign.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Membuat user admin pertama jika tabel user masih kosong. */
@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public DataInitializer(AppUserRepository users, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() == 0) {
            AppUser admin = new AppUser();
            admin.setUsername(props.admin().username());
            admin.setPasswordHash(encoder.encode(props.admin().password()));
            admin.setFullName(props.admin().fullName());
            admin.setPosition("Administrator Sistem");
            admin.setRole(AppUser.Role.ADMIN);
            users.save(admin);
            log.warn("User admin awal dibuat: username='{}'. SEGERA ganti password setelah login.", admin.getUsername());
        }
    }
}
