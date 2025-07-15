package com.challenge.rental_cars_spring_api.core.queries;

import com.challenge.rental_cars_spring_api.core.domain.Aluguel;
import com.challenge.rental_cars_spring_api.core.domain.Carro;
import com.challenge.rental_cars_spring_api.core.domain.Cliente;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.AluguelRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.CarroRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessarArquivoAluguelCommand {

    private static final Logger log = LoggerFactory.getLogger(ProcessarArquivoAluguelCommand.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int LINE_LENGTH = 20;
    private static final int CAR_ID_START = 0;
    private static final int CAR_ID_END = 2;
    private static final int CLIENT_ID_START = 2;
    private static final int CLIENT_ID_END = 4;
    private static final int RENTAL_DATE_START = 4;
    private static final int RENTAL_DATE_END = 12;
    private static final int RETURN_DATE_START = 12;
    private static final int RETURN_DATE_END = 20;

    private final CarroRepository carroRepository;
    private final ClienteRepository clienteRepository;
    private final AluguelRepository aluguelRepository;

    protected int calculateOptimalSegmentSize(int estimatedLines) {
        int sqrtSize = Math.max(1, (int) Math.sqrt(estimatedLines));
        return Math.min(sqrtSize, 100);
    }


    @Transactional
    public void execute(MultipartFile file) {
        if (file.isEmpty()) {
            log.error("Arquivo enviado está vazio");
            throw new IllegalArgumentException("Arquivo vazio");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            int totalLines = 0;
            int successCount = 0;
            int errorCount = 0;

            Map<Long, Carro> carroCache = new HashMap<>();
            Map<Long, Cliente> clienteCache = new HashMap<>();
            List<Aluguel> segmentBuffer = new ArrayList<>();

            int estimatedLines = estimateTotalLines(file);
            int optimalSegmentSize = calculateOptimalSegmentSize(estimatedLines);
            log.info("Iniciando processamento catalítico. Segmentos: √{} ≈ {}",
                    estimatedLines, optimalSegmentSize);

            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                totalLines++;

                try {
                    if (line.length() != LINE_LENGTH) {
                        log.warn("Linha {}: Tamanho inválido ({} caracteres)", lineNumber, line.length());
                        errorCount++;
                        continue;
                    }

                    Aluguel aluguel = processLine(line, lineNumber, carroCache, clienteCache);
                    if (aluguel != null) {
                        segmentBuffer.add(aluguel);
                        successCount++;

                        if (segmentBuffer.size() >= optimalSegmentSize) {
                            saveSegmentCatalytically(new ArrayList<>(segmentBuffer));
                            segmentBuffer.clear();
                        }
                    } else {
                        errorCount++;
                    }
                } catch (Exception e) {
                    log.error("Linha {}: Erro crítico - {}", lineNumber, e.getMessage(), e);
                    errorCount++;
                }
            }

            if (!segmentBuffer.isEmpty()) {
                saveSegmentCatalytically(new ArrayList<>(segmentBuffer));
            }

            log.info("Processamento catalítico concluído! Linhas: {}, Sucessos: {}, Erros: {}",
                    totalLines, successCount, errorCount);

        } catch (Exception e) {
            log.error("Falha catastrófica no processamento: {}", e.getMessage(), e);
            throw new RuntimeException("Erro no processador catalítico", e);
        }
    }

    private void saveSegmentCatalytically(List<Aluguel> segment) {
        if (segment.isEmpty()) {
            log.debug("⏭️ Segmento vazio - ignorando salvamento");
            return;
        }

        log.debug("💾 Salvando segmento com {} aluguéis", segment.size());
        aluguelRepository.saveAll(segment);
        aluguelRepository.flush();
        log.trace(" Segmento salvo com sucesso");
    }

    private Aluguel processLine(String line, int lineNumber,
                                Map<Long, Carro> carroCache,
                                Map<Long, Cliente> clienteCache) {
        try {
            String carIdStr = line.substring(CAR_ID_START, CAR_ID_END).trim();
            String clientIdStr = line.substring(CLIENT_ID_START, CLIENT_ID_END).trim();
            String rentalDateStr = line.substring(RENTAL_DATE_START, RENTAL_DATE_END).trim();
            String returnDateStr = line.substring(RETURN_DATE_START, RETURN_DATE_END).trim();

            Long carroId = parseLong(carIdStr, "Carro", lineNumber);
            Long clienteId = parseLong(clientIdStr, "Cliente", lineNumber);
            LocalDate dataAluguel = parseDate(rentalDateStr, lineNumber);
            LocalDate dataDevolucao = parseDate(returnDateStr, lineNumber);

            if (dataDevolucao.isBefore(dataAluguel)) {
                log.warn("Linha {}: Devolução anterior ao aluguel", lineNumber);
                return null;
            }

            Carro carro = carroCache.computeIfAbsent(carroId, id ->
                    carroRepository.findById(id).orElse(null)
            );

            if (carro == null) {
                log.warn("Linha {}: Carro {} não encontrado", lineNumber, carroId);
                return null;
            }

            Cliente cliente = clienteCache.computeIfAbsent(clienteId, id ->
                    clienteRepository.findById(id).orElse(null)
            );

            if (cliente == null) {
                log.warn("Linha {}: Cliente {} não encontrado", lineNumber, clienteId);
                return null;
            }

            long diasAlugados = ChronoUnit.DAYS.between(dataAluguel, dataDevolucao) + 1;
            BigDecimal valorTotal = carro.getVlrDiaria().multiply(BigDecimal.valueOf(diasAlugados));

            Aluguel aluguel = new Aluguel();
            aluguel.setCarro(carro);
            aluguel.setCliente(cliente);
            aluguel.setDataAluguel(dataAluguel);
            aluguel.setDataDevolucao(dataDevolucao);
            aluguel.setValor(valorTotal);
            aluguel.setPago(false);

            return aluguel;

        } catch (IllegalArgumentException e) {
            log.warn("Linha {}: {}", lineNumber, e.getMessage());
            return null;
        }
    }

    private int estimateTotalLines(MultipartFile file) {
        long fileSize = file.getSize();
        int estimated = (int) (fileSize / LINE_LENGTH);

        return Math.max(100, Math.min(estimated, 1_000_000));
    }

    private Long parseLong(String value, String fieldName, int lineNumber) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("%s ID inválido: '%s'", fieldName, value)
            );
        }
    }


    private LocalDate parseDate(String dateStr, int lineNumber) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Data inválida: '%s'", dateStr)
            );
        }
    }
}