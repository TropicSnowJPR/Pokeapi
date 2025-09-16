package com.pokeapi.papi.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager<T> {
    private final Path path;
    private final Class<T> type;
    private final Yaml yaml;
    private T config;

    public ConfigManager(Path path, Class<T> type) {
        this.path = path;
        this.type = type;

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        // No explicit Constructor -> prevents !!className in YAML
        this.yaml = new Yaml(options);
    }

    public void load() throws IOException {
        if (Files.notExists(path)) {
            config = createDefault();
            save();
        } else {
            try (InputStream in = Files.newInputStream(path)) {
                config = yaml.loadAs(in, type);
            }
        }
    }

    public void save() throws IOException {
        try (Writer out = Files.newBufferedWriter(path)) {
            yaml.dump(config, out);
        }
    }

    public T get() {
        return config;
    }

    private T createDefault() {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create default config", e);
        }
    }
}
