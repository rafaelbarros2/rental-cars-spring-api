package com.challenge.rental_cars_spring_api.exception;

import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.utils.TestFileUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessarArquivoAluguelCommand processador;


    @Test
    void deveLidarComArquivoVazio() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "vazio.rtn",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(emptyFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requisição inválida"))
                .andExpect(jsonPath("$.message").value("Arquivo não pode ser vazio"));
    }

    @Test
    void deveLidarComFormatoInvalido() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "invalido.txt",
                "text/plain",
                "conteúdo".getBytes()
        );

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(invalidFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requisição inválida"))
                .andExpect(jsonPath("$.message").value("O arquivo deve ter a extensão .rtn"));
    }

    @Test
    void deveLidarComTamanhoExcedido() throws Exception {
        // 4. Configuração do limite de tamanho
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "grande.rtn",
                "text/plain",
                new byte[1024 * 1024 * 11]
        );

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tamanho de arquivo excedido"))
                .andExpect(jsonPath("$.message").value("O tamanho máximo permitido é 10MB"));
    }

    @Test
    void deveLidarComErroInterno() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valido.rtn",
                "text/plain",
                "01012022010120220131".getBytes()
        );

        // 6. Simulação de erro no processador
        doThrow(new RuntimeException("Erro de processamento"))
                .when(processador).execute(any(MultipartFile.class));

        mockMvc.perform(multipart("/alugueis/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Erro interno no servidor"))
                .andExpect(jsonPath("$.message").value("Ocorreu um erro inesperado. Por favor, tente novamente mais tarde."));
    }
}