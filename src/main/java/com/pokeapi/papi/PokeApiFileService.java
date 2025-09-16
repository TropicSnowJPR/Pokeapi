package com.pokeapi.papi;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PokeApiFileService {

    private final Path root;

    public PokeApiFileService() {
        this.root = Paths.get("C:/Users/public/papiuploads");
        init();
    }

    public void init() {
        try {
            if (Files.exists(root)) {
                return;
            }
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage: " + e.getMessage(), e);
        }
    }

    // Save a file
    public void save(MultipartFile file, String username) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }
        try (InputStream in = file.getInputStream()) {
            Path userroot = Path.of(root.toString() + '\\' + username);
            Files.createDirectories(userroot);
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            if ((ext.equals(".png")) || (ext.equals(".jpg"))) {
                Files.copy(in, userroot.resolve("pfp" + ".png"), StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new IOException("Suspicious file denied!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    // Load a single file as a Spring Resource
    public Resource load(String filename) {
        try {
            Path file = root.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found: " + filename);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file: " + filename, e);
        }
    }

    // List all files
    public List<String> list() {
        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read stored files", e);
        }
    }

    // Delete all files
    public void clear() {
        try {
            FileSystemUtils.deleteRecursively(root);
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not clear storage", e);
        }
    }
}