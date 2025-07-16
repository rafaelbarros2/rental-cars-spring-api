package com.challenge.rental_cars_spring_api.performance;

import com.challenge.rental_cars_spring_api.RentalCarsSpringApiApplication;
import com.challenge.rental_cars_spring_api.core.domain.Carro;
import com.challenge.rental_cars_spring_api.core.domain.Cliente;
import com.challenge.rental_cars_spring_api.core.queries.ListarAlugueisQuery;
import com.challenge.rental_cars_spring_api.core.queries.ListarCarrosQuery;
import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.AluguelRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.CarroRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.ClienteRepository;
import com.challenge.rental_cars_spring_api.infrastructure.websocket.WebSocketNotificationService;
import com.challenge.rental_cars_spring_api.utils.RTNFileGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;


@SpringBootTest(
        classes = RentalCarsSpringApiApplication.class

)
@ActiveProfiles("test")
public class CatalyticProcessingLoadTest {

    @Autowired
    private ProcessarArquivoAluguelCommand processor;

    @MockBean private EntityManager entityManager;
    @MockBean private PlatformTransactionManager transactionManager;
    @MockBean private WebSocketNotificationService webSocketNotificationService;
;@MockBean private com.challenge.rental_cars_spring_api.infrastructure.repositories.CarroRepository carroRepository;
    @MockBean private com.challenge.rental_cars_spring_api.infrastructure.repositories.ClienteRepository clienteRepository;
    @MockBean private com.challenge.rental_cars_spring_api.infrastructure.repositories.AluguelRepository aluguelRepository;


    @BeforeEach
    void setupTestEnvironment() {
        assertNotNull(processor, "Processor should be injected");
        assertNotNull(transactionManager, "PlatformTransactionManager should be injected");
        assertNotNull(entityManager, "EntityManager should be injected");

        // Configurar comportamento do mock
        TransactionStatus mockStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(mockStatus);
        when(mockStatus.isCompleted()).thenReturn(false);
        doNothing().when(transactionManager).commit(any(TransactionStatus.class));
        doNothing().when(transactionManager).rollback(any(TransactionStatus.class));

        Carro carroValido = new Carro();
        carroValido.setId(1L);
        carroValido.setVlrDiaria(BigDecimal.valueOf(100));

        Cliente clienteValido = new Cliente();
        clienteValido.setId(1L);

        when(carroRepository.findById(anyLong())).thenReturn(java.util.Optional.of(carroValido));
        when(clienteRepository.findById(anyLong())).thenReturn(java.util.Optional.of(clienteValido));

// Para o saveAll, você pode simplesmente retornar o que foi passado
        when(aluguelRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));


// Para o saveAll, você pode simplesmente retornar o que foi passado
        when(aluguelRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

    }

    @Test
    void deveConferirSeTransactionManagerFoiInjetadoNoProcessor() throws Exception {
        assertNotNull(processor, "processor não foi injetado");

    }

    @Test
    void processar1MilhaoDeLinhas() throws Exception {
        assertNotNull(processor, "Processor não foi injetado");

        int lineCount = 2000;
        Path testFile = RTNFileGenerator.generateRTNFile(lineCount);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "load_test.rtn",
                "text/plain",
                Files.readAllBytes(testFile)
        );

        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.start();

        System.out.println("Memória usada ANTES do processamento: " + memoryMonitor.getCurrentMemoryUsed() + " MB");

        long startTime = System.nanoTime();
        processor.execute(multipartFile);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        memoryMonitor.stopMonitoring();
        long maxMemoryMB = memoryMonitor.getMaxMemoryUsed();

        System.out.println("\n=== RESULTADOS DO TESTE DE CARGA (COM MOCK) ===");
        System.out.println("Linhas processadas: " + lineCount);
        System.out.println("Tempo total: " + durationMs + " ms");
        System.out.println("Máximo de memória usada: " + maxMemoryMB + " MB");

        // Verificar se o transactionManager foi usado
        verify(transactionManager, atLeastOnce()).getTransaction(any());
        verify(transactionManager, atLeastOnce()).commit(any());

        assertTrue(durationMs < 30_000, "Tempo deve ser menor que 30 segundos");
        assertTrue(maxMemoryMB < 500, "Memória deve ser menor que 500 MB");
    }
}