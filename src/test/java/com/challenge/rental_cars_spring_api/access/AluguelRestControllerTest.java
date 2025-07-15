package com.challenge.rental_cars_spring_api.access;

import com.challenge.rental_cars_spring_api.core.domain.Aluguel;
import com.challenge.rental_cars_spring_api.core.queries.ListarAlugueisQuery;
import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ListarAlugueisQueryResultItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
public class AluguelRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessarArquivoAluguelCommand processarArquivoCommand;

    @MockBean
    private ListarAlugueisQuery listarAlugueisQuery;

    // Teste de upload
    @Test
    public void deveProcessarUploadComSucesso() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "teste.rtn", "text/plain", "01012022010120220131".getBytes()
        );

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        verify(processarArquivoCommand, times(1)).execute(any());
    }

    // Teste de listagem
    @Test
    public void deveListarAlugueis() throws Exception {
        // Dados de teste
        LocalDate hoje = LocalDate.now();
        LocalDate devolucao = hoje.plusDays(5);
        BigDecimal valorAluguel = BigDecimal.valueOf(1500.00);

        ListarAlugueisQueryResultItem item = new ListarAlugueisQueryResultItem(
                hoje,
                "Toyota Corolla",
                15000,
                "João Silva",
                "+55(11)98765-4321",
                devolucao,
                valorAluguel,
                "SIM"
        );

        PageImpl<ListarAlugueisQueryResultItem> page = new PageImpl<>(List.of(item));

        // ✅ Mock configurado corretamente
        when(listarAlugueisQuery.execute(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/alugueis")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dataAluguel").value(hoje.toString()))
                .andExpect(jsonPath("$.content[0].modeloCarro").value("Toyota Corolla"))
                .andExpect(jsonPath("$.content[0].kmCarro").value(15000))
                .andExpect(jsonPath("$.content[0].nomeCliente").value("João Silva"))
                .andExpect(jsonPath("$.content[0].telefoneCliente").value("+55(11)98765-4321"))
                .andExpect(jsonPath("$.content[0].dataDevolucao").value(devolucao.toString()))
                .andExpect(jsonPath("$.content[0].valor").value(1500.00))
                .andExpect(jsonPath("$.content[0].pago").value("SIM"));

        // ✅ Verifica se o método foi chamado
        verify(listarAlugueisQuery, times(1)).execute(any(Pageable.class));
    }
}
