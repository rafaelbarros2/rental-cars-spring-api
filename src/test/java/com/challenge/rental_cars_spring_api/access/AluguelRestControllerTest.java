package com.challenge.rental_cars_spring_api.access;

import com.challenge.config.SecurityConfig;
import com.challenge.rental_cars_spring_api.core.queries.ListarAlugueisQuery;
import com.challenge.rental_cars_spring_api.core.queries.ListarCarrosQuery;
import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ListarAlugueisQueryResultItem;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ProcessamentoResult;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.AluguelRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.CarroRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.ClienteRepository;
import com.challenge.rental_cars_spring_api.infrastructure.websocket.WebSocketNotificationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AluguelRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AluguelRestControllerTest { // ✅ Mudado para JUnit 5

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessarArquivoAluguelCommand processarArquivoCommand;

    @MockBean
    private ListarAlugueisQuery listarAlugueisQuery;

    @MockBean
    private ListarCarrosQuery listarCarrosQuery; // Se CarrosRestController o utiliza

    @MockBean
    private CarroRepository carroRepository;
    @MockBean private ClienteRepository clienteRepository;
    @MockBean private AluguelRepository aluguelRepository;
    @MockBean private EntityManager entityManager; // ✅ Adicionado
    @MockBean private PlatformTransactionManager transactionManager; // ✅ Adicionado - esta era a dependência que faltava!
    @MockBean private WebSocketNotificationService webSocketNotificationService; // ✅ Adicionado


    private ProcessamentoResult resultadoProcessamento;
    private List<ListarAlugueisQueryResultItem> alugueisExemplo;

    @BeforeEach // ✅ JUnit 5 annotation
    void setUp() {
        // ✅ Configurar resultado de processamento
        resultadoProcessamento = ProcessamentoResult.criar(
                100L, // totalLinhas
                95L,  // sucessos
                5L,   // erros
                List.of() // erros detalhados
        );

        // ✅ Configurar dados de teste
        LocalDate hoje = LocalDate.now();
        LocalDate devolucao = hoje.plusDays(5);
        BigDecimal valorAluguel = BigDecimal.valueOf(1500.00);

        ListarAlugueisQueryResultItem item1 = new ListarAlugueisQueryResultItem(
                hoje,
                "Toyota Corolla",
                15000,
                "João Silva",
                "+55(11)98765-4321",
                devolucao,
                valorAluguel,
                "SIM"
        );

        ListarAlugueisQueryResultItem item2 = new ListarAlugueisQueryResultItem(
                hoje.minusDays(1),
                "Honda Civic",
                25000,
                "Maria Santos",
                "+55(11)99876-5432",
                devolucao.minusDays(1),
                BigDecimal.valueOf(1200.00),
                "NÃO"
        );

        alugueisExemplo = List.of(item1, item2);

        // ✅ Configurar mocks
        when(processarArquivoCommand.execute(any())).thenReturn(resultadoProcessamento);
        when(listarAlugueisQuery.execute(any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            return new PageImpl<>(alugueisExemplo, pageable, alugueisExemplo.size());
        });
    }


    @Test
    void deveListarAlugueis() throws Exception {
        mockMvc.perform(get("/alugueis")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].dataAluguel").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.content[0].modeloCarro").value("Toyota Corolla"))
                .andExpect(jsonPath("$.content[0].kmCarro").value(15000))
                .andExpect(jsonPath("$.content[0].nomeCliente").value("João Silva"))
                .andExpect(jsonPath("$.content[0].telefoneCliente").value("+55(11)98765-4321"))
                .andExpect(jsonPath("$.content[0].dataDevolucao").value(LocalDate.now().plusDays(5).toString()))
                .andExpect(jsonPath("$.content[0].valor").value(1500.00))
                .andExpect(jsonPath("$.content[0].pago").value("SIM"))
                .andExpect(jsonPath("$.content[1].modeloCarro").value("Honda Civic"))
                .andExpect(jsonPath("$.content[1].pago").value("NÃO"));

        verify(listarAlugueisQuery, times(1)).execute(any(Pageable.class));
    }

    @Test
    void deveRetornarPaginaVaziaQuandoNaoHaAlugueis() throws Exception {
        // ✅ Override do mock para este teste específico
        when(listarAlugueisQuery.execute(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
        );

        mockMvc.perform(get("/alugueis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deveUsarPaginacaoPadrao() throws Exception {
        mockMvc.perform(get("/alugueis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10)) // Tamanho padrão
                .andExpect(jsonPath("$.number").value(0)); // Primeira página
    }

    @Test
    void deveTratarErroNoUpload() throws Exception {
        // ✅ Simular erro no processamento
        when(processarArquivoCommand.execute(any())).thenThrow(new RuntimeException("Erro no processamento"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "teste.rtn", "text/plain", "linha_invalida".getBytes()
        );

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());

        verify(processarArquivoCommand, times(1)).execute(any());
    }

    @Test
    void deveTratarErroNaListagem() throws Exception {
        // ✅ Simular erro na listagem
        when(listarAlugueisQuery.execute(any(Pageable.class)))
                .thenThrow(new RuntimeException("Erro na consulta"));

        mockMvc.perform(get("/alugueis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deveAceitarArquivoVazio() throws Exception {
        // ✅ Resultado para arquivo vazio
        ProcessamentoResult resultadoVazio = ProcessamentoResult.criar(0L, 0L, 0L, List.of());
        when(processarArquivoCommand.execute(any())).thenReturn(resultadoVazio);

        MockMultipartFile file = new MockMultipartFile(
                "file", "vazio.rtn", "text/plain", "".getBytes()
        );

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

    }


}