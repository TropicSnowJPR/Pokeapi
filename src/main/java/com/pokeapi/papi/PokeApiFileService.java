package com.pokeapi.papi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PokeApiFileService {

    private final Path root;
    private final Logger logger = LoggerFactory.getLogger(PokeApiApplication.class);

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
        } catch (Exception e) {
            logger.error("Could not initialize storage: {}", e.getMessage(), e);
        }
    }

    // Save a file
    public void save(MultipartFile file, String username) {
        if (file.isEmpty()) {
            logger.error("Failed to store empty file.");
        }
        try (InputStream in = file.getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            Path userroot = Path.of(root.toString() + '\\' + username);
            Files.createDirectories(userroot);
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".gif") || ext.equals(".bmp") || ext.equals(".webp") || ext.equals(".tiff") || ext.equals(".png")) {
                File outputFile = new File(userroot.toFile(), "pfp.png");
                ImageIO.write(image, "png", outputFile);
                logger.info("New pfp set for {}. Saved in Path: {}", username, userroot);
            } else {
                logger.warn("Suspicious file detected and denied! With {} file extension!", ext); }
        } catch (Exception e) {
            logger.error("Failed to store file: {}", e.getMessage(), e);
        }
    }

    // Load a single file as a Spring Resource
    public Resource load(String filename) {
        try {
            Path file = root.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                logger.warn("File not found: {}", filename);
            }
            return resource;
        } catch (Exception e) {
            logger.error("Error reading file: {}", filename, e);
        }
        return null;
    }

    // List all files
    public List<String> list() {
        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to read stored files", e);
        }
        return null;
    }

    // Delete all files
    public void clear() {
        try {
            FileSystemUtils.deleteRecursively(root);
            Files.createDirectories(root);
        } catch (Exception e) {
            logger.error("Could not clear storage", e);
        }
    }
}