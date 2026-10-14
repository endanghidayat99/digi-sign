package com.digisign.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Meneruskan route frontend (Vue Router history mode) ke index.html,
 * mis. /verify/abc, /documents/123 saat di-refresh atau dibuka dari scan QR.
 */
@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/verify/**", "/documents/**", "/inbox/**", "/document-types/**",
            "/users/**", "/settings/**", "/profile/**", "/dashboard/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
