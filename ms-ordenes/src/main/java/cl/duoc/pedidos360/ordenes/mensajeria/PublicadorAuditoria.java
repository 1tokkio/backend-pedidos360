package cl.duoc.pedidos360.ordenes.mensajeria;

import cl.duoc.pedidos360.ordenes.config.KafkaConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PublicadorAuditoria {

    private static final Logger log = LoggerFactory.getLogger(PublicadorAuditoria.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    public PublicadorAuditoria(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    public void publicar(String tipo, String detalle) {
        EventoAuditoria evento = new EventoAuditoria("ms-ordenes", tipo, detalle, LocalDateTime.now());
        try {
            String json = objectMapper.writeValueAsString(evento);
            kafka.send(KafkaConfig.TOPICO_AUDITORIA, "ms-ordenes", json).whenComplete((envio, error) -> {
                if (error != null) {
                    log.error("No se pudo publicar el evento {} en auditoria: {}", tipo, error.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("No se pudo convertir a JSON el evento {}: {}", tipo, e.getMessage());
        }
    }
}
