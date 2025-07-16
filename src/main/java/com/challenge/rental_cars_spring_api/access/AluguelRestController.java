package com.challenge.rental_cars_spring_api.access;

import com.challenge.rental_cars_spring_api.core.queries.ListarAlugueisQuery;
import com.challenge.rental_cars_spring_api.core.queries.ProcessarArquivoAluguelCommand;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ListarAlugueisQueryResultItem;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ProcessamentoResult;
import com.challenge.rental_cars_spring_api.exception.FileSizeExceededException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
//    @CrossOrigin(origins = "*")
    public ResponseEntity<Object> uploadRtnFile(@RequestParam("file") MultipartFile file) {

            if (file.isEmpty()) {
                System.out.println("❌ Erro: Arquivo vazio");
                throw new IllegalArgumentException("Arquivo não pode ser vazio");
            }

            long maxBytes = parseSize(maxFileSize);
            if (file.getSize() > maxBytes) {
                System.out.println("❌ Erro: Arquivo muito grande");
                throw new FileSizeExceededException("O tamanho máximo permitido é " + maxFileSize);
            }

            if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".rtn")) {
                System.out.println("❌ Erro: Extensão inválida");
                throw new IllegalArgumentException("O arquivo deve ter a extensão .rtn");
            }


            ProcessamentoResult resultado = processarArquivoAluguelCommand.execute(file);


            Map<String, Object> response = new HashMap<>();
            response.put("filename", file.getOriginalFilename());
            response.put("totalLinhas", resultado.totalLinhas());
            response.put("sucessos", resultado.sucessos());
            response.put("numErros", resultado.numErros());
            response.put("temErros", resultado.temErros());

            if (resultado.temErros()) {
                response.put("message", String.format(
                        "%d linhas processadas com sucesso e %d linhas com erro. Entre em contato para saber mais detalhes.",
                         resultado.sucessos(), resultado.numErros()
                ));
                response.put("errosDetalhados", resultado.errosDetalhados());
            } else {
                response.put("message", "Arquivo RTN processado com sucesso!");
            }
        return ResponseEntity.ok()
                .body(response);
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
    public ResponseEntity<Page<ListarAlugueisQueryResultItem>> listarAlugueis(Pageable pageable) {
        Page<ListarAlugueisQueryResultItem> alugueis = listarAlugueisQuery.execute(pageable);
        return ResponseEntity.ok(alugueis);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Backend funcionando! " + LocalDateTime.now());
    }
}