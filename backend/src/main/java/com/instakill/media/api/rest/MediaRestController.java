package com.instakill.media.api.rest;

import com.instakill.common.error.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MediaRestController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "png", "mp4", "mov");

    private final String basePath;

    public MediaRestController(@Value("${media.base-path}") String basePath) {
        this.basePath = basePath;
    }

    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse uploadMedia(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("File type not allowed");
        }
        String filename = UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
        Path directory = Path.of(basePath).toAbsolutePath();
        Files.createDirectories(directory);
        file.transferTo(directory.resolve(filename));
        return new MediaResponse("/media/" + filename);
    }
}
