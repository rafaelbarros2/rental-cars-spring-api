package com.challenge.rental_cars_spring_api.core.queries;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.challenge.rental_cars_spring_api.core.domain.Aluguel;
import com.challenge.rental_cars_spring_api.core.domain.Carro;
import com.challenge.rental_cars_spring_api.core.domain.Cliente;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.AluguelRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.CarroRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.ClienteRepository;
import com.challenge.rental_cars_spring_api.infrastructure.websocket.WebSocketNotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessarArquivoAluguelCommandTest {

    @Mock private CarroRepository carroRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private AluguelRepository aluguelRepository;
    @Mock private EntityManager entityManager; // ✅ Adicionado
    @Mock private PlatformTransactionManager transactionManager; // ✅ Adicionado - esta era a dependência que faltava!
    @Mock private WebSocketNotificationService webSocketNotificationService; // ✅ Adicionado

    @InjectMocks
    private ProcessarArquivoAluguelCommand processador;

    private Carro carroValido;
    private Cliente clienteValido;
    private TransactionStatus mockTransactionStatus; // ✅ Adicionado

    @BeforeEach
    void setUp() {
        carroValido = new Carro();
        carroValido.setId(1L);
        carroValido.setVlrDiaria(BigDecimal.valueOf(100));

        clienteValido = new Cliente();
        clienteValido.setId(1L);

        // ✅ Configurar comportamento do TransactionManager
        mockTransactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(mockTransactionStatus);
        when(mockTransactionStatus.isCompleted()).thenReturn(false);
        doNothing().when(transactionManager).commit(any(TransactionStatus.class));
        doNothing().when(transactionManager).rollback(any(TransactionStatus.class));

        // ✅ Configurar comportamento do EntityManager
        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).clear();

        // ✅ Configurar comportamento do WebSocketNotificationService
        doNothing().when(webSocketNotificationService).notifyProcessingCompletion(any());
    }

    @Test
    void deveProcessarLinhaValida() throws Exception {
        String linhaValida = "01012022010120220131"; // 20 caracteres EXATOS
        MultipartFile file = new MockMultipartFile(
                "file", "test.rtn", "text/plain", linhaValida.getBytes()
        );

        when(carroRepository.findById(1L)).thenReturn(Optional.of(carroValido));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteValido));

        when(aluguelRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Aluguel> argument = invocation.getArgument(0);
            return argument;
        });

        processador.execute(file);

        verify(aluguelRepository, times(1)).saveAll(anyList());
        verify(transactionManager, times(1)).getTransaction(any()); // ✅ Verificar se foi usado
        verify(transactionManager, times(1)).commit(any()); // ✅ Verificar se foi usado

        ArgumentCaptor<List<Aluguel>> captor = ArgumentCaptor.forClass(List.class);
        verify(aluguelRepository).saveAll(captor.capture());

        List<Aluguel> alugueisSalvos = captor.getValue();

        assertNotNull(alugueisSalvos, "Lista de aluguéis não deve ser nula");
        assertEquals(1, alugueisSalvos.size(), "Deveria ter 1 aluguel salvo");

        Aluguel aluguelSalvo = alugueisSalvos.get(0);
        assertEquals(carroValido, aluguelSalvo.getCarro());
        assertEquals(clienteValido, aluguelSalvo.getCliente());
        assertEquals(LocalDate.of(2022, 1, 1), aluguelSalvo.getDataAluguel());
        assertEquals(LocalDate.of(2022, 1, 31), aluguelSalvo.getDataDevolucao());

        long dias = ChronoUnit.DAYS.between(
                aluguelSalvo.getDataAluguel(),
                aluguelSalvo.getDataDevolucao()
        ) + 1;
        BigDecimal valorEsperado = carroValido.getVlrDiaria().multiply(BigDecimal.valueOf(dias));
        assertEquals(valorEsperado, aluguelSalvo.getValor());
    }

    @Test
    void deveIgnorarLinhaComTamanhoIncorreto() throws Exception {
        String linhaInvalida = "010120220131";
        MultipartFile file = criarArquivo(linhaInvalida);
        processador.execute(file);
        verify(aluguelRepository, never()).save(any());
        // ✅ Verificar que o WebSocket foi notificado mesmo com erro
        verify(webSocketNotificationService, times(1)).notifyProcessingCompletion(any());
    }

    @Test
    void deveIgnorarCarroInexistente() throws Exception {
        String linha = "99012022010120220131"; // 20 caracteres (ID 99)
        MultipartFile file = criarArquivo(linha);
        processador.execute(file);

        verify(aluguelRepository, never()).saveAll(anyList());
        verify(carroRepository).findById(99L);
        // ✅ Verificar que o WebSocket foi notificado mesmo com erro
        verify(webSocketNotificationService, times(1)).notifyProcessingCompletion(any());
    }

    @Test
    void deveCalcularValorCorretamente() throws Exception {
        String linha = "01012022010120220110";
        MultipartFile file = criarArquivo(linha);

        when(carroRepository.findById(1L)).thenReturn(Optional.of(carroValido));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteValido));

        when(aluguelRepository.saveAll(anyList())).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        processador.execute(file);

        verify(aluguelRepository, atLeastOnce()).saveAll(anyList());
        verify(transactionManager, atLeastOnce()).getTransaction(any()); // ✅ Verificar transação
        verify(transactionManager, atLeastOnce()).commit(any()); // ✅ Verificar commit

        ArgumentCaptor<List<Aluguel>> captor = ArgumentCaptor.forClass(List.class);
        verify(aluguelRepository).saveAll(captor.capture());

        List<Aluguel> alugueisSalvos = captor.getValue();

        assertEquals(1, alugueisSalvos.size());

        Aluguel aluguel = alugueisSalvos.get(0);
        BigDecimal valorEsperado = BigDecimal.valueOf(1000); // 10 dias * 100

        assertEquals(valorEsperado, aluguel.getValor());
    }

    @Test
    void deveProcessarSegmentosCataliticos() throws Exception {
        int desiredSegmentSize = 6;
        int totalLinhas = 6;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalLinhas; i++) {
            sb.append("01012022010120220131"); // Linha válida
            if (i < totalLinhas - 1) sb.append("\n");
        }

        MultipartFile file = new MockMultipartFile(
                "file", "test.rtn", "text/plain", sb.toString().getBytes()
        );

        when(carroRepository.findById(1L)).thenReturn(Optional.of(carroValido));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteValido));
        when(aluguelRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessarArquivoAluguelCommand spyProcessor = spy(processador);

        spyProcessor.execute(file);

        ArgumentCaptor<List<Aluguel>> captor = ArgumentCaptor.forClass(List.class);
        verify(aluguelRepository, times(1)).saveAll(captor.capture()); // 6/2 = 3 segmentos

        List<List<Aluguel>> allSegments = captor.getAllValues();
        for (List<Aluguel> segment : allSegments) {
            assertEquals(desiredSegmentSize, segment.size(),
                    "Cada segmento deve ter tamanho " + desiredSegmentSize);
        }

        // ✅ Verificar que as transações foram usadas para cada segmento
        verify(transactionManager, times(1)).getTransaction(any());
        verify(transactionManager, times(1)).commit(any());
    }

    @Test
    void deveProcessarGrandeVolumeComCatalise() throws Exception {
        MultipartFile file = criarArquivo("01012022010120220131\n".repeat(100_000));

        when(carroRepository.findById(1L)).thenReturn(Optional.of(carroValido));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteValido));
        when(aluguelRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0)); // ✅ Adicionado

        long start = System.currentTimeMillis();
        processador.execute(file);
        long duration = System.currentTimeMillis() - start;

        System.out.println("Processado 100.000 linhas em " + duration + "ms");

        verify(aluguelRepository, atLeastOnce()).saveAll(anyList());
        verify(entityManager, atLeastOnce()).flush(); // ✅ Corrigido - era aluguelRepository.flush() antes
        verify(entityManager, atLeastOnce()).clear(); // ✅ Adicionado
        verify(transactionManager, atLeastOnce()).getTransaction(any()); // ✅ Verificar transações
        verify(transactionManager, atLeastOnce()).commit(any()); // ✅ Verificar commits
        verify(webSocketNotificationService, times(1)).notifyProcessingCompletion(any()); // ✅ Verificar notificação
    }

    // 🔼 Teste para flush final
    @Test
    void deveSalvarBufferFinal() throws Exception {
        // Arquivo com 2 linhas (menor que tamanho de segmento)
        String linhas = "01012022010120220131\n01012022010120220131";
        MultipartFile file = criarArquivo(linhas);

        when(carroRepository.findById(1L)).thenReturn(Optional.of(carroValido));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteValido));
        when(aluguelRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0)); // ✅ Adicionado

        processador.execute(file);

        verify(aluguelRepository, times(1)).saveAll(anyList());
        verify(transactionManager, times(1)).getTransaction(any()); // ✅ Verificar transação
        verify(transactionManager, times(1)).commit(any()); // ✅ Verificar commit
        verify(webSocketNotificationService, times(1)).notifyProcessingCompletion(any()); // ✅ Verificar notificação
    }

    // ✅ Teste adicional para verificar se as dependências foram injetadas corretamente
    @Test
    void deveVerificarInjecaoDeDependencias() throws Exception {
        assertNotNull(processador, "Processor não deve ser nulo");

        // Verificar via reflection se o transactionManager foi injetado
        Field field = ProcessarArquivoAluguelCommand.class.getDeclaredField("transactionManager");
        field.setAccessible(true);
        Object tm = field.get(processador);

        assertNotNull(tm, "TransactionManager deve estar injetado");
        assertTrue(tm instanceof PlatformTransactionManager, "Deve ser uma instância de PlatformTransactionManager");

        System.out.println("✅ TransactionManager injetado com sucesso: " + tm.getClass().getSimpleName());
    }

    private MultipartFile criarArquivo(String content) {
        return new MockMultipartFile("file", "test.rtn", "text/plain", content.getBytes());
    }
}