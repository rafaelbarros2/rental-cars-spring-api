package com.challenge.rental_cars_spring_api.performance;

import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.utils.MemoryMonitor;
import com.challenge.rental_cars_spring_api.utils.RTNFileGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class CatalyticProcessingLoadTest {

    @Autowired
    private ProcessarArquivoAluguelCommand processor;

    @Test
    void processar1MilhaoDeLinhas() throws Exception {
        // Configurar
        int lineCount = 1_000_000;
        Path testFile = RTNFileGenerator.generateRTNFile(lineCount);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "load_test.rtn",
                "text/plain",
                Files.readAllBytes(testFile)
        );

        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.start();

        long startTime = System.nanoTime();
        processor.execute(multipartFile);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        memoryMonitor.stopMonitoring();
        long maxMemoryMB = memoryMonitor.getMaxMemoryUsed();

        System.out.println("\n=== RESULTADOS DO TESTE DE CARGA ===");
        System.out.println("Linhas processadas: " + lineCount);
        System.out.println("Tempo total: " + durationMs + " ms");
        System.out.println("Máximo de memória usada: " + maxMemoryMB + " MB");

        assertTrue(durationMs < 30_000, "Tempo deve ser menor que 30 segundos");
        assertTrue(maxMemoryMB < 100, "Memória deve ser menor que 100 MB");
    }
}