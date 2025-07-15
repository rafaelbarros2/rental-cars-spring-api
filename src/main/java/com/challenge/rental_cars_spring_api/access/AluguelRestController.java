package com.challenge.rental_cars_spring_api.access;

import com.challenge.rental_cars_spring_api.core.queries.ListarAlugueisQuery; // Importar
import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ListarAlugueisQueryResultItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/alugueis")
@RequiredArgsConstructor
public class AluguelRestController {

    private final ProcessarArquivoAluguelCommand processarArquivoAluguelCommand;
    private final ListarAlugueisQuery listarAlugueisQuery;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Processa um arquivo RTN para registrar aluguéis")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo processado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<String> uploadRtnFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("Arquivo não pode ser vazio.", HttpStatus.BAD_REQUEST);
        }
        if (!file.getOriginalFilename().toLowerCase().endsWith(".rtn")) {
            return new ResponseEntity<>("O arquivo deve ter a extensão .rtn.", HttpStatus.BAD_REQUEST);
        }

        try {
            processarArquivoAluguelCommand.execute(file);
            return new ResponseEntity<>("Arquivo RTN processado com sucesso!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao processar o arquivo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    @Operation(summary = "Lista todos os aluguéis com paginação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de aluguéis retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<Page<ListarAlugueisQueryResultItem>> listarAlugueis(Pageable pageable) {
        Page<ListarAlugueisQueryResultItem> alugueis = listarAlugueisQuery.execute(pageable);
        return ResponseEntity.ok(alugueis);
    }
}