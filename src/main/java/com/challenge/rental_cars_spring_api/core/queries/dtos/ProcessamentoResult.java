package com.challenge.rental_cars_spring_api.core.queries.dtos;

import java.math.BigDecimal;
import java.util.List;

public record ProcessamentoResult(
        long totalLinhas,
        long sucessos,
        long numErros,
        List<ErroLinha> errosDetalhados,
        boolean temErros
) {

    public record ErroLinha(
            long linha,
            String mensagem,
            String tipo
    ) {}

    public static ProcessamentoResult criar(long totalLinhas, long sucessos, long numErros, List<ErroLinha> erros) {
        return new ProcessamentoResult(totalLinhas, sucessos, numErros, erros, numErros > 0);
    }

    public static ErroLinha criarErro(long linha, String mensagem, String tipo) {
        return new ErroLinha(linha, mensagem, tipo);
    }
}
