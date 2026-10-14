package com.digisign.service;

import com.digisign.config.AppProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Penyimpanan file di filesystem (volume Docker). Path di DB disimpan relatif terhadap storage-dir. */
@Service
public class StorageService {

    private final Path root;

    public StorageService(AppProperties props) {
        this.root = Path.of(props.storageDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Tidak bisa membuat folder storage " + root, e);
        }
    }

    public void write(String relativePath, byte[] data) {
        Path target = resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Path tmp = Files.createTempFile(target.getParent(), ".upload", ".tmp");
            Files.write(tmp, data);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] read(String relativePath) {
        try {
            return Files.readAllBytes(resolve(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolve(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Path tidak valid");
        }
        return p;
    }
}
