package com.challenge.rental_cars_spring_api.infrastructure.websocket; // Pacote atualizado


import com.challenge.rental_cars_spring_api.core.queries.dtos.ProcessamentoResult;

public interface WebSocketNotificationService {
    void notifyProcessingCompletion(ProcessamentoResult result);
}