package cl.duoc.pedidos360.auditoria.mensajeria;

import cl.duoc.pedidos360.auditoria.config.KafkaConfig;
import cl.duoc.pedidos360.auditoria.service.AuditoriaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorAuditoria {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorAuditoria.class);

    private final AuditoriaService servicio;
    private final ObjectMapper objectMapper;

    public ConsumidorAuditoria(AuditoriaService servicio, ObjectMapper objectMapper) {
        this.servicio = servicio;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.TOPICO_AUDITORIA, groupId = KafkaConfig.GRUPO_AUDITORIA)
    public void alLlegarUnEvento(@Payload String mensaje,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                                 @Header(KafkaHeaders.OFFSET) long offset) {
        MensajeAuditoria evento;
        try {
            evento = objectMapper.readValue(mensaje, MensajeAuditoria.class);
        } catch (JsonProcessingException e) {
            // Un mensaje que no se puede leer no mejora con reintentos: se registra y se descarta.
            log.error("Evento de auditoria ilegible (particion {}, offset {}): {}", particion, offset, mensaje);
            return;
        }

        if (evento.servicio() == null || evento.tipo() == null) {
            log.error("Evento de auditoria incompleto (particion {}, offset {}): {}", particion, offset, mensaje);
            return;
        }

        servicio.registrar(evento.servicio(), evento.tipo(), evento.detalle());
        log.info("Auditoria registro {} de {} (particion {}, offset {})",
                evento.tipo(), evento.servicio(), particion, offset);
    }
}
