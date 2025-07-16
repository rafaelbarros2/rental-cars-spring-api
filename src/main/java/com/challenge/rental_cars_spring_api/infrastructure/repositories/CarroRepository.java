package com.challenge.rental_cars_spring_api.infrastructure.repositories;

import com.challenge.rental_cars_spring_api.core.domain.Carro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CarroRepository extends JpaRepository<Carro, Long> {

    @Query("SELECT DISTINCT c.modelo FROM Carro c WHERE c.modelo IS NOT NULL AND c.modelo <> ''")
    List<String> findDistinctModelos();
}
