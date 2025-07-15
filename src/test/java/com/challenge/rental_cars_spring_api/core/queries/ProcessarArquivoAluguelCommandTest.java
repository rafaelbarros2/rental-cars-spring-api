package com.challenge.rental_cars_spring_api.core.queries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import com.challenge.rental_cars_spring_api.core.domain.Aluguel;
import com.challenge.rental_cars_spring_api.core.domain.Carro;
import com.challenge.rental_cars_spring_api.core.domain.Cliente;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.AluguelRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.CarroRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ProcessarArquivoAluguelCommandTest {

    @Mock private CarroRepository carroRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private AluguelRepository aluguelRepository;

    @InjectMocks
    private ProcessarArquivoAluguelCommand processador;

    private Carro carroValido;
    private Cliente clienteValido;

    @BeforeEach
    void setUp() {
        carroValido = new Carro();
        carroValido.setId(1L);
        carroValido.setVlrDiaria(BigDecimal.valueOf(100));

        clienteValido = new Cliente();
        clienteValido.setId(1L);
    }

    @Test
    void deveProcessarLinhaValida() throws Exception {
        // Dado: Linha válida de 20 caracteres
        String linha = "01 012022010120220131"; // Carro ID 1, Cliente ID 1, Aluguel: 01/01/2022, Devolução: 31/01/2022
        MultipartFile file = criarArquivo(linha);

        // Configurar mocks
        when(carroRepository.findById(1L)).thenReturn(Optional.of(carroValido));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteValido));

        // Quando: Processar arquivo
        processador.execute(file);

        // Então: Deve salvar 1 aluguel
        verify(aluguelRepository, times(1)).saveAll(anyList());
    }

    @Test
    void deveIgnorarLinhaComTamanhoIncorreto() throws Exception {
        // Dado: Linha inválida (12 caracteres)
        String linhaInvalida = "010120220131";
        MultipartFile file = criarArquivo(linhaInvalida);

        // Quando: Processar arquivo
        processador.execute(file);

        // Então: Não deve salvar nada
        verify(aluguelRepository, never()).save(any());
    }

    @Test
    void deveIgnorarCarroInexistente() throws Exception {
        // Dado: Linha com carro inexistente
        String linha = "99 012022010120220131"; // Carro ID 99
        MultipartFile file = criarArquivo(linha);

        // Configurar mocks
        when(carroRepository.findById(99L)).thenReturn(Optional.empty());

        // Quando: Processar arquivo
        processador.execute(file);

        // Então: Não deve salvar
        verify(aluguelRepository, never()).save(any());
    }

    @Test
    void deveCalcularValorCorretamente() throws Exception {
        // Dado: Linha com período de 10 dias
        String linha = "01 012022010120220110"; // 01/01 a 10/01 (10 dias)
        MultipartFile file = criarArquivo(linha);

        // Configurar mocks
        when(carroRepository.findById(1L)).thenReturn(Optional.of(carroValido));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteValido));

        // Capturar aluguel criado
        List<Aluguel> alugueisSalvos = new ArrayList<>();
        doAnswer(invocation -> {
            alugueisSalvos.addAll(invocation.getArgument(0));
            return null;
        }).when(aluguelRepository).saveAll(anyList());

        // Quando: Processar arquivo
        processador.execute(file);

        // Então: Valor deve ser 100 * 10 = 1000
        assertEquals(1, alugueisSalvos.size());
        assertEquals(BigDecimal.valueOf(1000), alugueisSalvos.get(0).getValor());
    }

    @Test
    void deveProcessarSegmentosCataliticos() throws Exception {
        // Dado: Arquivo com 250 linhas (√250 ≈ 15.8 → segmentos de 16)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            sb.append("01 012022010120220131\n"); // Linha válida
        }
        MultipartFile file = criarArquivo(sb.toString());

        // Configurar mocks
        when(carroRepository.findById(1L)).thenReturn(Optional.of(carroValido));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteValido));

        // Quando: Processar arquivo
        processador.execute(file);

        // Então: Deve salvar em segmentos de ≈16
        // 250 linhas / 16 = 15.6 → 16 segmentos
        verify(aluguelRepository, times(16)).saveAll(anyList());
    }

    @Test
    void deveProcessarGrandeVolumeComCatalise() throws Exception {
        // Dado: 100.000 linhas
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100_000; i++) {
            sb.append("01 012022010120220131\n");
        }
        MultipartFile file = criarArquivo(sb.toString());

        // Configurar mocks
        when(carroRepository.findById(1L)).thenReturn(Optional.of(carroValido));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteValido));

        // Quando: Processar arquivo
        long start = System.currentTimeMillis();
        processador.execute(file);
        long duration = System.currentTimeMillis() - start;

        // Então: Verificar uso de memória (não deve estourar)
        System.out.println("Processado 100.000 linhas em " + duration + "ms");

        // Deve usar segmentação catalítica
        int expectedSegments = (int) Math.ceil(100_000 / Math.sqrt(100_000)); // ≈ 317 segmentos
        verify(aluguelRepository, times(expectedSegments)).saveAll(anyList());
    }

    private MultipartFile criarArquivo(String content) {
        return new MockMultipartFile("file", "test.rtn", "text/plain", content.getBytes());
    }
}