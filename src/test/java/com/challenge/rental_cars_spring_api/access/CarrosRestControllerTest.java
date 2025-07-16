package com.challenge.rental_cars_spring_api.access;

import com.challenge.config.SecurityConfig;
import com.challenge.rental_cars_spring_api.core.queries.ListarAlugueisQuery;
import com.challenge.rental_cars_spring_api.core.queries.ListarCarrosQuery;
import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ListarCarrosQueryResultItem;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.AluguelRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.CarroRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.ClienteRepository;
import com.challenge.rental_cars_spring_api.infrastructure.websocket.WebSocketNotificationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = CarrosRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class CarrosRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListarCarrosQuery listarCarrosQuery;


    @MockBean
    private ProcessarArquivoAluguelCommand processarArquivoCommand;

    @MockBean
    private ListarAlugueisQuery listarAlugueisQuery;

    @MockBean
    private CarroRepository carroRepository;
    @MockBean private ClienteRepository clienteRepository;
    @MockBean private AluguelRepository aluguelRepository;
    @MockBean private EntityManager entityManager; // ✅ Adicionado
    @MockBean private PlatformTransactionManager transactionManager; // ✅ Adicionado - esta era a dependência que faltava!
    @MockBean private WebSocketNotificationService webSocketNotificationService; // ✅ Adicionado


    private List<String> modelosCarrosExemplo; // Ou List<ListarCarrosQueryResultItem> se for o caso

    @BeforeEach
    void setUp() {

        modelosCarrosExemplo = Arrays.asList("Toyota Corolla", "Honda Civic", "Volkswagen Gol");


        when(listarCarrosQuery.execute()).thenReturn(modelosCarrosExemplo);
    }

    @Test
    void deveListarCarrosComSucesso() throws Exception {
        mockMvc.perform(get("/carros") // Assumindo que o controller está mapeado para /carros
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Espera status HTTP 200 OK
                .andExpect(jsonPath("$").isArray()) // Espera um array JSON
                .andExpect(jsonPath("$.length()").value(modelosCarrosExemplo.size())) // Verifica o tamanho
                .andExpect(jsonPath("$[0]").value("Toyota Corolla")) // Verifica o primeiro item
                .andExpect(jsonPath("$[1]").value("Honda Civic"))
                .andExpect(jsonPath("$[2]").value("Volkswagen Gol"));
    }

    @Test
    void deveRetornarVazioQuandoNaoHaCarros() throws Exception {
        // Sobrescreve o mock para este teste, retornando uma lista vazia
        when(listarCarrosQuery.execute()).thenReturn(List.of());

        mockMvc.perform(get("/carros")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deveTratarErroInternoNaListagem() throws Exception {
        // Simula um erro no serviço
        when(listarCarrosQuery.execute()).thenThrow(new RuntimeException("Erro ao buscar carros"));

        mockMvc.perform(get("/carros")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError()); // Espera status 500
    }
}