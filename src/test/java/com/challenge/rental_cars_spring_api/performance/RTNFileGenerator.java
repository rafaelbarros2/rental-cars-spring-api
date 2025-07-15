package com.challenge.rental_cars_spring_api.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class RTNFileGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random random = new Random();

    public static Path generateRTNFile(int lines) throws IOException {
        Path filePath = Files.createTempFile("rtn_test_", ".rtn");
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (int i = 0; i < lines; i++) {
                writer.write(generateRTNLine());
                writer.newLine();
            }
        }
        return filePath;
    }

    public static String generateRTNLine() {
        int carId = 1 + random.nextInt(99);
        int clientId = 1 + random.nextInt(99);

        LocalDate startDate = LocalDate.now()
                .minusDays(random.nextInt(365))
                .plusDays(random.nextInt(30));

        LocalDate endDate = startDate.plusDays(1 + random.nextInt(30));

        return String.format("%02d%02d%s%s",
                carId,
                clientId,
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER));
    }

    public static String generateInvalidRTNLine() {
        // Linha com tamanho incorreto
        return "010120220101"; // Apenas 12 caracteres
    }
}