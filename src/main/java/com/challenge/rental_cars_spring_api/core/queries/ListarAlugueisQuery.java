package com.challenge.rental_cars_spring_api.core.queries;

import com.challenge.rental_cars_spring_api.core.domain.Aluguel;
import com.challenge.rental_cars_spring_api.core.queries.dtos.ListarAlugueisQueryResultItem;
import com.challenge.rental_cars_spring_api.infrastructure.repositories.AluguelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarAlugueisQuery {

    private final AluguelRepository aluguelRepository;

    @Transactional(readOnly = true)
    public Page<ListarAlugueisQueryResultItem> execute(Pageable pageable, LocalDate dataAluguel, String modelo) {

        Specification<Aluguel> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (dataAluguel != null) {
                predicates.add(criteriaBuilder.equal(root.get("dataAluguel"), dataAluguel));
            }
            if (modelo != null && !modelo.trim().isEmpty()) {
                     predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("carro").get("modelo")), "%" + modelo.toLowerCase() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Aluguel> alugueisPage = aluguelRepository.findAll(spec, pageable);

        return alugueisPage.map(ListarAlugueisQueryResultItem::from);
    }
}