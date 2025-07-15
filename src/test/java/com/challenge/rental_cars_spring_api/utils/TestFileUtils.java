package com.challenge.rental_cars_spring_api.utils;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestFileUtils {

    public static MultipartFile createMockMultipartFile(String content, String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                content.getBytes()
        );
    }

    public static MultipartFile createMockMultipartFileFromPath(Path path) throws IOException {
        return new MockMultipartFile(
                "file",
                path.getFileName().toString(),
                "text/plain",
                Files.readAllBytes(path)
        );
    }

    public static String generateTempFilePath(String prefix, String suffix) throws IOException {
        Path tempFile = Files.createTempFile(prefix, suffix);
        tempFile.toFile().deleteOnExit();
        return tempFile.toString();
    }
}