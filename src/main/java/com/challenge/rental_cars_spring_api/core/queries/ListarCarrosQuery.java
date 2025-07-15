package com.challenge.rental_cars_spring_api.core.queries;

import com.challenge.rental_cars_spring_api.core.queries.dtos.ListarCarrosQueryResultItem;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.CarroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListarCarrosQuery {

    private final CarroRepository carroRepository;

    @Transactional(readOnly = true)
    public Page<ListarCarrosQueryResultItem> execute(Pageable pageable) {
        return carroRepository.findAll(pageable)
                .map(ListarCarrosQueryResultItem::from);
    }

    @Transactional(readOnly = true)
    public List<ListarCarrosQueryResultItem> execute() {
        return carroRepository.findAll().stream()
                .map(ListarCarrosQueryResultItem::from)
                .collect(Collectors.toList());
    }
}