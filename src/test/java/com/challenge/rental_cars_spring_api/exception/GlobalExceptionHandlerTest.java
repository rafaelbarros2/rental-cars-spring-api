package com.challenge.rental_cars_spring_api.exception;

import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.utils.TestFileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessarArquivoAluguelCommand processador;

    @Test
    void deveLidarComArquivoVazio() throws Exception {
        MockMultipartFile emptyFile = (MockMultipartFile) TestFileUtils.createMockMultipartFile("", "vazio.rtn");

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requisição inválida"))
                .andExpect(jsonPath("$.message").value("Arquivo não pode ser vazio"));
    }

    @Test
    void deveLidarComFormatoInvalido() throws Exception {
        MockMultipartFile invalidFile = (MockMultipartFile) TestFileUtils.createMockMultipartFile(
                "conteúdo", "invalido.txt"
        );

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requisição inválida"))
                .andExpect(jsonPath("$.message").value("O arquivo deve ter a extensão .rtn"));
    }

    @Test
    void deveLidarComTamanhoExcedido() throws Exception {
        // Simular exceção de tamanho excedido
        doThrow(MaxUploadSizeExceededException.class)
                .when(processador).execute(any());

        MockMultipartFile file = (MockMultipartFile) TestFileUtils.createMockMultipartFile(
                "conteúdo", "grande.rtn"
        );

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tamanho de arquivo excedido"))
                .andExpect(jsonPath("$.message").value("O tamanho máximo permitido é 10MB"));
    }

    @Test
    void deveLidarComErroInterno() throws Exception {
        // Simular erro genérico
        doThrow(new RuntimeException("Erro de processamento"))
                .when(processador).execute(any());

        MockMultipartFile file = (MockMultipartFile) TestFileUtils.createMockMultipartFile(
                "01012022010120220131", "valido.rtn"
        );

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Erro interno no servidor"))
                .andExpect(jsonPath("$.message").value("Ocorreu um erro inesperado. Por favor, tente novamente mais tarde."));
    }
}