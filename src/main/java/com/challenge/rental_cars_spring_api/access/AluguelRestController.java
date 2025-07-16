package com.challenge.rental_cars_spring_api.access;

import com.challenge.rental_cars_spring_api.core.queries.ListarAlugueisQuery;
import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ListarAlugueisQueryResultItem;
import com.challenge.rental_cars_spring_api.exception.FileSizeExceededException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.Objects;

@RestController
@RequestMapping("/alugueis")
@RequiredArgsConstructor
public class AluguelRestController {

    private final ProcessarArquivoAluguelCommand processarArquivoAluguelCommand;
    private final ListarAlugueisQuery listarAlugueisQuery;

    // ✅ Valor do application.properties
    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSize;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CrossOrigin("*")
    public ResponseEntity<String> uploadRtnFile(@RequestParam("file") MultipartFile file) {

            if (file.isEmpty()) {

                throw new IllegalArgumentException("Arquivo não pode ser vazio");
            }

            long maxBytes = parseSize(maxFileSize);
            if (file.getSize() > maxBytes) {
                throw new FileSizeExceededException("O tamanho máximo permitido é " + maxFileSize);
            }

            if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".rtn")) {
                throw new IllegalArgumentException("O arquivo deve ter a extensão .rtn");
            }

            processarArquivoAluguelCommand.execute(file);


        return ResponseEntity.ok().body("O processamento pode levar alguns minutos," +
                " mas enquanto isso pode continuar navegando avisaremos quando finalizar");

    }

    private long parseSize(String sizeStr) {
        sizeStr = sizeStr.toUpperCase().trim();
        if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.replace("MB", "")) * 1024 * 1024;
        } else if (sizeStr.endsWith("KB")) {
            return Long.parseLong(sizeStr.replace("KB", "")) * 1024;
        } else {
            return Long.parseLong(sizeStr);
        }
    }

    @GetMapping
    @Operation(summary = "Lista todos os aluguéis com paginação e filtros")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de aluguéis retornada com sucesso.", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ListarAlugueisQueryResultItem.class))}),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)})})
    public ResponseEntity<Page<ListarAlugueisQueryResultItem>> listarAlugueis(
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(name = "dataAluguel", required = false) LocalDate dataAluguel,
            @RequestParam(name = "modeloCarro", required = false) String modeloCarro
    ) {

        Page<ListarAlugueisQueryResultItem> alugueis = listarAlugueisQuery.execute(pageable, dataAluguel, modeloCarro);

        return ResponseEntity.ok(alugueis);
    }

}