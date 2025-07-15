package com.challenge.rental_cars_spring_api.core.queries.dtos;

import com.challenge.rental_cars_spring_api.core.domain.Aluguel;
import com.challenge.rental_cars_spring_api.core.domain.Carro;
import com.challenge.rental_cars_spring_api.core.domain.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;


public record ListarAlugueisQueryResultItem(
        LocalDate dataAluguel,
        String modeloCarro,
        Integer kmCarro,
        String nomeCliente,
        String telefoneCliente, // Formato +XX(XX)XXXXX-XXXX
        LocalDate dataDevolucao,
        BigDecimal valor,
        String pago
) {
    public static ListarAlugueisQueryResultItem from(Aluguel aluguel) {
        Carro carro = aluguel.getCarro();
        Cliente cliente = aluguel.getCliente();

        String telefoneFormatado = formatarTelefone(cliente.getTelefone());
        String pagoStatus = aluguel.getPago() ? "SIM" : "NAO";

        return new ListarAlugueisQueryResultItem(
                aluguel.getDataAluguel(),
                carro.getModelo(),
                carro.getKm(),
                cliente.getNome(),
                telefoneFormatado,
                aluguel.getDataDevolucao(),
                aluguel.getValor(),
                pagoStatus
        );
    }

    private static String formatarTelefone(String telefone) {
        if (telefone == null || telefone.length() < 11) {
            return telefone;
        }

        return "+" + telefone.substring(0, 2) + "(" + telefone.substring(2, 4) + ")" + telefone.substring(4, 9) + "-" + telefone.substring(9, 13);
    }
}