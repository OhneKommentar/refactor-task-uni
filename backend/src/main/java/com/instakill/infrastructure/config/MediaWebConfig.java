package com.instakill.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class MediaWebConfig implements WebMvcConfigurer {

    private final Path mediaDirectory;

    public MediaWebConfig(@Value("${media.base-path}") String basePath) {
        this.mediaDirectory = Path.of(basePath).toAbsolutePath();
        try {
            Files.createDirectories(mediaDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create media directory " + mediaDirectory, e);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(mediaDirectory.toUri().toString());
    }
}
