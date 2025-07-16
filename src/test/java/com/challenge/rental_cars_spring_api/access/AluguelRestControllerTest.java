package com.challenge.rental_cars_spring_api.access;

import com.challenge.rental_cars_spring_api.core.queries.ListarAlugueisQuery;
import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ListarAlugueisQueryResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AluguelRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AluguelRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessarArquivoAluguelCommand processarArquivoCommand;

    @MockBean
    private ListarAlugueisQuery listarAlugueisQuery;

    private List<ListarAlugueisQueryResultItem> alugueisExemplo;

    @BeforeEach
    void setUp() {
        LocalDate hoje = LocalDate.now();
        LocalDate devolucao = hoje.plusDays(5);

        alugueisExemplo = List.of(
                new ListarAlugueisQueryResultItem(
                        hoje, "Toyota Corolla", 15000, "João Silva",
                        "+55(11)98765-4321", devolucao, BigDecimal.valueOf(1500.00), "SIM"
                ),
                new ListarAlugueisQueryResultItem(
                        hoje.minusDays(1), "Honda Civic", 25000, "Maria Santos",
                        "+55(11)99876-5432", devolucao.minusDays(1), BigDecimal.valueOf(1200.00), "NÃO"
                )
        );

        when(listarAlugueisQuery.execute(any(Pageable.class), any(), any())).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            return new PageImpl<>(alugueisExemplo, pageable, alugueisExemplo.size());
        });
    }

    @Test
    void deveListarAlugueisComFiltros() throws Exception {
        String dataFiltro = "2025-07-16";
        String modeloFiltro = "Corolla";

        mockMvc.perform(get("/api/alugueis")
                        .param("page", "0")
                        .param("size", "10")
                        .param("dataAluguel", dataFiltro)
                        .param("modeloCarro", modeloFiltro)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));

        verify(listarAlugueisQuery, times(1)).execute(
                any(Pageable.class),
                eq(LocalDate.parse(dataFiltro)),
                eq(modeloFiltro)
        );
    }

    @Test
    void deveRetornarPaginaVaziaQuandoNaoHaAlugueis() throws Exception {
        when(listarAlugueisQuery.execute(any(Pageable.class), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/alugueis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deveTratarErroNaListagem() throws Exception {
        when(listarAlugueisQuery.execute(any(Pageable.class), any(), any()))
                .thenThrow(new RuntimeException("Erro na consulta"));

        mockMvc.perform(get("/api/alugueis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}