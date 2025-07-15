package com.challenge.rental_cars_spring_api.core.queries.dtos;

import com.challenge.rental_cars_spring_api.core.domain.Aluguel;
import com.challenge.rental_cars_spring_api.core.domain.Carro;
import com.challenge.rental_cars_spring_api.core.domain.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

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
        Objects.requireNonNull(aluguel, "Aluguel não pode ser nulo");

        Carro carro = Objects.requireNonNull(aluguel.getCarro(), "Carro não pode ser nulo");
        Cliente cliente = Objects.requireNonNull(aluguel.getCliente(), "Cliente não pode ser nulo");

        return new ListarAlugueisQueryResultItem(
                Objects.requireNonNull(aluguel.getDataAluguel(), "Data de aluguel não pode ser nula"),
                Objects.requireNonNull(carro.getModelo(), "Modelo do carro não pode ser nulo"),
                Objects.requireNonNull(carro.getKm(), "KM do carro não pode ser nulo"),
                Objects.requireNonNull(cliente.getNome(), "Nome do cliente não pode ser nulo"),
                formatarTelefone(cliente.getTelefone()),
                aluguel.getDataDevolucao(), // Pode ser nulo se ainda não devolvido
                Objects.requireNonNull(aluguel.getValor(), "Valor não pode ser nulo"),
                aluguel.getPago() ? "SIM" : "NAO"
        );
    }

    private static String formatarTelefone(String telefone) {
        if (telefone == null) {
            return null;
        }

        String numeros = telefone.replaceAll("[^0-9]", "");

        if (numeros.length() < 11) {
            return telefone;
        }

        return "+" + numeros.substring(0, 2) +
                "(" + numeros.substring(2, 4) + ")" +
                numeros.substring(4, 9) + "-" +
                numeros.substring(9);
    }

    // Validação adicional no construtor compacto
    public ListarAlugueisQueryResultItem {
        Objects.requireNonNull(dataAluguel, "Data de aluguel não pode ser nula");
        Objects.requireNonNull(modeloCarro, "Modelo do carro não pode ser nulo");
        Objects.requireNonNull(kmCarro, "KM do carro não pode ser nulo");
        Objects.requireNonNull(nomeCliente, "Nome do cliente não pode ser nulo");
        Objects.requireNonNull(valor, "Valor não pode ser nulo");
        Objects.requireNonNull(pago, "Status de pagamento não pode ser nulo");

        if (kmCarro < 0) {
            throw new IllegalArgumentException("KM do carro não pode ser negativo");
        }

        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor não pode ser negativo");
        }
    }
}