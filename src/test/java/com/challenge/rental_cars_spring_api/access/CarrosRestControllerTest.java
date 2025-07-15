package com.challenge.rental_cars_spring_api.access;

import com.challenge.rental_cars_spring_api.core.queries.ListarCarrosQuery;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ListarCarrosQueryResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class CarrosRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private List<ListarCarrosQueryResultItem> allCarros;
    private Page<ListarCarrosQueryResultItem> pagedCarros;

    @MockBean
    private ListarCarrosQuery listarCarrosQuery;

    @BeforeEach
    void setUp() {
        allCarros = Arrays.asList(
                new ListarCarrosQueryResultItem(1L, "GOL"),
                new ListarCarrosQueryResultItem(2L, "POLO"),
                new ListarCarrosQueryResultItem(3L, "VITRUS"),
                new ListarCarrosQueryResultItem(4L, "JETTA"),
                new ListarCarrosQueryResultItem(5L, "COROLLA")
        );

        // Configura o mock para retornar uma página de resultados
        when(listarCarrosQuery.execute(any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();
            int start = Math.min(page * size, allCarros.size());
            int end = Math.min((page + 1) * size, allCarros.size());

            List<ListarCarrosQueryResultItem> pageContent = allCarros.subList(start, end);
            return new PageImpl<>(pageContent, pageable, allCarros.size());
        });
    }

    @Test
    void shouldListCarsWithPagination() throws Exception {
        mockMvc.perform(get("/carros")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].modelo").value("GOL"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void shouldUseDefaultPaginationWhenNoParams() throws Exception {
        mockMvc.perform(get("/carros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.size").value(10)) // Valor default do controlador
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldReturnEmptyPageWhenNoCars() throws Exception {
        // Sobrescreve o mock para retornar lista vazia
        when(listarCarrosQuery.execute(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(), Pageable.unpaged(), 0)
        );

        mockMvc.perform(get("/carros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldHandleServiceErrors() throws Exception {
        // Configura o mock para lançar exceção
        when(listarCarrosQuery.execute(any(Pageable.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(get("/carros"))
                .andExpect(status().isInternalServerError());
    }
}