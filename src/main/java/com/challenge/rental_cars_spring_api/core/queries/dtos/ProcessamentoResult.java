package com.challenge.rental_cars_spring_api.core.queries.dtos;

import java.util.List;

public record ProcessamentoResult(
        int totalLinhas,
        int sucessos,
        int numErros,
        List<ErroLinha> errosDetalhados,
        boolean temErros
) {

    public record ErroLinha(
            int linha,
            String mensagem,
            String tipo
    ) {}

    // Método de conveniência para criar resultado
    public static ProcessamentoResult criar(int totalLinhas, int sucessos, int numErros, List<ErroLinha> erros) {
        return new ProcessamentoResult(totalLinhas, sucessos, numErros, erros, numErros > 0);
    }

    // Método para adicionar erro facilmente
    public static ErroLinha criarErro(int linha, String mensagem, String tipo) {
        return new ErroLinha(linha, mensagem, tipo);
    }
}
