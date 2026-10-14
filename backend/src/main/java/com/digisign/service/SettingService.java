package com.digisign.service;

import com.digisign.domain.AppSetting;
import com.digisign.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

@Service
public class SettingService {

    public static final String COMPANY_NAME = "company.name";
    public static final String COMPANY_ADDRESS = "company.address";
    public static final String QR_LOGO_RATIO = "qr.logo_ratio";
    public static final String LOGO_PATH = "settings/logo.png";

    private final AppSettingRepository repo;
    private final StorageService storage;

    public SettingService(AppSettingRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    public String get(String key, String def) {
        return repo.findById(key).map(AppSetting::getValue).filter(v -> !v.isBlank()).orElse(def);
    }

    public double getDouble(String key, double def) {
        try {
            return Double.parseDouble(get(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Transactional
    public void set(String key, String value) {
        repo.save(new AppSetting(key, value));
    }

    public Map<String, String> all() {
        Map<String, String> m = new TreeMap<>();
        repo.findAll().forEach(s -> m.put(s.getKey(), s.getValue()));
        return m;
    }

    public String companyName() {
        return get(COMPANY_NAME, "Perusahaan");
    }

    public boolean hasLogo() {
        return storage.exists(LOGO_PATH);
    }

    public byte[] logo() {
        return hasLogo() ? storage.read(LOGO_PATH) : null;
    }

    public void saveLogo(byte[] png) {
        storage.write(LOGO_PATH, png);
    }

    public void deleteLogo() {
        storage.delete(LOGO_PATH);
    }
}
