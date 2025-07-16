package com.challenge.rental_cars_spring_api.core.queries;

import com.challenge.rental_cars_spring_api.core.domain.Aluguel;
import com.challenge.rental_cars_spring_api.core.domain.Carro;
import com.challenge.rental_cars_spring_api.core.domain.Cliente;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ProcessamentoResult;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.AluguelRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.CarroRepository;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.ClienteRepository;
import com.challenge.rental_cars_spring_api.infrastructure.websocket.WebSocketNotificationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;



    @Service
    @RequiredArgsConstructor
    public class ProcessarArquivoAluguelCommand {

        private static final Logger log = LoggerFactory.getLogger(ProcessarArquivoAluguelCommand.class);
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
        private static final int BATCH_SIZE = 1000;
        private static final int MAX_DETAILED_ERRORS_TO_COLLECT = 1000; // Limite para coletar erros detalhados

        private final CarroRepository carroRepository;
        private final ClienteRepository clienteRepository;
        private final AluguelRepository aluguelRepository;
        private final EntityManager entityManager;
        private final PlatformTransactionManager transactionManager;
        private final WebSocketNotificationService webSocketNotificationService;

        private final Map<Long, Carro> carroCache = new HashMap<>();
        private final Map<Long, Cliente> clienteCache = new HashMap<>();

        public ProcessamentoResult execute(MultipartFile file) {
            List<Aluguel> alugueisBatch = new ArrayList<>();
            List<ProcessamentoResult.ErroLinha> errosDetalhados = new ArrayList<>(); // Esta lista será limitada
            long totalLinhas = 0; // Voltou para long
            long sucessos = 0; // Voltou para long
            long numErrosContados = 0; // Reintroduzido para contar todos os erros
            ProcessamentoResult finalResult;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    totalLinhas++;
                    if (line.length() != 20) {
                        String mensagemErro = String.format("Formato inválido (tamanho %d, esperado 20 caracteres). Linha: '%s'", line.length(), line);
                        log.warn("Linha {}: {}", totalLinhas, mensagemErro);
                        if (errosDetalhados.size() < MAX_DETAILED_ERRORS_TO_COLLECT) { // Condicional para coletar
                            errosDetalhados.add(ProcessamentoResult.criarErro((int) totalLinhas, mensagemErro, "FORMATO_INVALIDO")); // totalLinhas cast para int
                        }
                        numErrosContados++; // Sempre incrementa o contador total de erros
                        continue;
                    }

                    try {
                        Long carroId = Long.parseLong(line.substring(0, 2).trim());
                        Long clienteId = Long.parseLong(line.substring(2, 4).trim());
                        LocalDate dataAluguel = LocalDate.parse(line.substring(4, 12).trim(), DATE_FORMATTER);
                        LocalDate dataDevolucao = LocalDate.parse(line.substring(12, 20).trim(), DATE_FORMATTER);

                        Carro carro = carroCache.get(carroId);
                        if (carro == null) {
                            Optional<Carro> carroOpt = carroRepository.findById(carroId);
                            if (carroOpt.isEmpty()) {
                                String mensagemErro = String.format("Carro com ID %d não encontrado.", carroId);
                                log.warn("Linha {}: {}", totalLinhas, mensagemErro);
                                if (errosDetalhados.size() < MAX_DETAILED_ERRORS_TO_COLLECT) {
                                    errosDetalhados.add(ProcessamentoResult.criarErro((int) totalLinhas, mensagemErro, "CARRO_NAO_ENCONTRADO"));
                                }
                                numErrosContados++;
                                continue;
                            }
                            carro = carroOpt.get();
                            carroCache.put(carroId, carro);
                        }

                        Cliente cliente = clienteCache.get(clienteId);
                        if (cliente == null) {
                            Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
                            if (clienteOpt.isEmpty()) {
                                String mensagemErro = String.format("Cliente com ID %d não encontrado.", clienteId);
                                log.warn("Linha {}: {}", totalLinhas, mensagemErro);
                                if (errosDetalhados.size() < MAX_DETAILED_ERRORS_TO_COLLECT) {
                                    errosDetalhados.add(ProcessamentoResult.criarErro((int) totalLinhas, mensagemErro, "CLIENTE_NAO_ENCONTRADO"));
                                }
                                numErrosContados++;
                                continue;
                            }
                            cliente = clienteOpt.get();
                            clienteCache.put(clienteId, cliente);
                        }

                        long diasAlugados = ChronoUnit.DAYS.between(dataAluguel, dataDevolucao) + 1;
                        if (diasAlugados <= 0) {
                            String mensagemErro = String.format("Data de devolução (%s) não é posterior ou igual à data de aluguel (%s).", dataDevolucao, dataAluguel);
                            log.warn("Linha {}: {}", totalLinhas, mensagemErro);
                            if (errosDetalhados.size() < MAX_DETAILED_ERRORS_TO_COLLECT) {
                                errosDetalhados.add(ProcessamentoResult.criarErro((int) totalLinhas, mensagemErro, "DATA_INVALIDA"));
                            }
                            numErrosContados++;
                            continue;
                        }
                        BigDecimal valorDiaria = carro.getVlrDiaria();
                        BigDecimal valorTotal = valorDiaria.multiply(BigDecimal.valueOf(diasAlugados));

                        Aluguel novoAluguel = new Aluguel(carro, cliente, dataAluguel, dataDevolucao, valorTotal, false);
                        alugueisBatch.add(novoAluguel);

                        if (alugueisBatch.size() >= BATCH_SIZE) {
                            processBatch(alugueisBatch);
                            sucessos += alugueisBatch.size();
                            alugueisBatch.clear();
                        }

                        log.info("Linha {}: Dados de aluguel para Carro ID {} e Cliente ID {} adicionados ao lote.", totalLinhas, carroId, clienteId);

                    } catch (NumberFormatException e) {
                        String mensagemErro = String.format("Erro de formato numérico: '%s'. Detalhe: %s", line, e.getMessage());
                        log.error("Linha {}: {}", totalLinhas, mensagemErro);
                        if (errosDetalhados.size() < MAX_DETAILED_ERRORS_TO_COLLECT) {
                            errosDetalhados.add(ProcessamentoResult.criarErro((int) totalLinhas, mensagemErro, "ERRO_NUMERICO"));
                        }
                        numErrosContados++;
                    } catch (java.time.format.DateTimeParseException e) {
                        String mensagemErro = String.format("Erro de formato de data: '%s'. Detalhe: %s", line, e.getMessage());
                        log.error("Linha {}: {}", totalLinhas, mensagemErro);
                        if (errosDetalhados.size() < MAX_DETAILED_ERRORS_TO_COLLECT) {
                            errosDetalhados.add(ProcessamentoResult.criarErro((int) totalLinhas, mensagemErro, "ERRO_DATA"));
                        }
                        numErrosContados++;
                    } catch (Exception e) {
                        String mensagemErro = String.format("Linha %d: Erro inesperado ao processar: '%s'. Detalhe: %s", totalLinhas, line, e.getMessage());
                        log.error("Linha {}: {}", totalLinhas, mensagemErro, e);
                        if (errosDetalhados.size() < MAX_DETAILED_ERRORS_TO_COLLECT) {
                            errosDetalhados.add(ProcessamentoResult.criarErro((int) totalLinhas, mensagemErro, "ERRO_INESPERADO"));
                        }
                        numErrosContados++;
                    }
                }

                if (!alugueisBatch.isEmpty()) {
                    processBatch(alugueisBatch);
                    sucessos += alugueisBatch.size();
                }

            } catch (Exception e) {
                log.error("Erro fatal ao ler o arquivo RTN: {}", e.getMessage(), e);
                numErrosContados++;
                errosDetalhados.add(ProcessamentoResult.criarErro(0L, "Erro fatal na leitura do arquivo: " + e.getMessage(), "ERRO_LEITURA_ARQUIVO"));
                finalResult = ProcessamentoResult.criar(totalLinhas, sucessos, numErrosContados, errosDetalhados);
                webSocketNotificationService.notifyProcessingCompletion(finalResult);
                throw new RuntimeException("Falha ao processar o arquivo RTN", e);
            }

            finalResult = ProcessamentoResult.criar(totalLinhas, sucessos, numErrosContados, errosDetalhados);
            webSocketNotificationService.notifyProcessingCompletion(finalResult);
            return finalResult;
        }

        private void processBatch(List<Aluguel> batch) {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            TransactionStatus status = transactionManager.getTransaction(def);

            try {
                aluguelRepository.saveAll(batch);
                entityManager.flush();
                entityManager.clear();
                transactionManager.commit(status);
                log.info("Lote de {} aluguéis processado e commitado com sucesso.", batch.size());
            } catch (Exception e) {
                transactionManager.rollback(status);
                log.error("Erro ao processar lote de aluguéis: {}", e.getMessage(), e);
                throw new RuntimeException("Falha ao persistir lote de aluguéis", e);
            }
        }


}