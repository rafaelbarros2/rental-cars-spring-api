package com.challenge.rental_cars_spring_api.infrastructure.websocket;

import com.challenge.rental_cars_spring_api.core.queries.dtos.ProcessamentoResult;
import com.challenge.rental_cars_spring_api.infrastructure.websocket.WebSocketNotificationService; // Importar do novo pacote
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationServiceImpl implements WebSocketNotificationService {

    private static final String PROCESSING_STATUS_TOPIC = "/topic/processing-status";

    private final SimpMessagingTemplate messagingTemplate;


    @Override
    public void notifyProcessingCompletion(ProcessamentoResult result) {
        messagingTemplate.convertAndSend(PROCESSING_STATUS_TOPIC, result);
    }
}