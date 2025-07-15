package com.challenge.rental_cars_spring_api.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "carro")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class Carro implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "modelo")
    private String modelo;

    @Column(name = "ano")
    private String ano;

    @Column(name = "qtd_passageiros")
    private Integer qtdPassageiros;

    @Column(name = "km")
    private Integer km;

    @Column(name = "fabricante")
    private String fabricante;

    @Column(name = "vlr_diaria")
    private BigDecimal vlrDiaria;
}
