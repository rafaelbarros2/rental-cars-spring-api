package com.challenge.rental_cars_spring_api.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "aluguel")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class Aluguel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "carro_id", nullable = false)
    private Carro carro;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "data_aluguel")
    private LocalDate dataAluguel;

    @Column(name = "data_devolucao")
    private LocalDate dataDevolucao;

    @Column(name = "valor")
    private BigDecimal valor;

    @Column(name = "pago")
    private Boolean pago;

    // Construtor para facilitar a criação a partir do arquivo RTN, sem o ID ainda.
    public Aluguel(Carro carro, Cliente cliente, LocalDate dataAluguel, LocalDate dataDevolucao, BigDecimal valor, Boolean pago) {
        this.carro = carro;
        this.cliente = cliente;
        this.dataAluguel = dataAluguel;
        this.dataDevolucao = dataDevolucao;
        this.valor = valor;
        this.pago = pago;
    }
}