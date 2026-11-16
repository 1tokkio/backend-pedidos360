package cl.duoc.pedidos360.recomendaciones.mensajeria;

import cl.duoc.pedidos360.recomendaciones.config.KafkaConfig;
import cl.duoc.pedidos360.recomendaciones.service.RecomendacionService;
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
public class ConsumidorInteracciones {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorInteracciones.class);

    private final RecomendacionService servicio;
    private final ObjectMapper objectMapper;

    public ConsumidorInteracciones(RecomendacionService servicio, ObjectMapper objectMapper) {
        this.servicio = servicio;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.TOPICO_INTERACCIONES, groupId = KafkaConfig.GRUPO)
    public void alInteractuar(@Payload String mensaje,
                              @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                              @Header(KafkaHeaders.OFFSET) long offset) {
        InteraccionMensaje interaccion;
        try {
            interaccion = objectMapper.readValue(mensaje, InteraccionMensaje.class);
        } catch (JsonProcessingException e) {
            // Un mensaje ilegible no se reintenta: se registra y se descarta para no bloquear la particion.
            log.error("Interaccion ilegible (particion {}, offset {}): {}", particion, offset, mensaje);
            return;
        }

        if (interaccion.usuarioOid() == null || interaccion.productoId() == null || interaccion.accion() == null) {
            log.error("Interaccion incompleta (particion {}, offset {}): {}", particion, offset, mensaje);
            return;
        }

        servicio.registrarInteraccion(interaccion);
        log.info("Interaccion {} del producto {} registrada (particion {}, offset {})",
                interaccion.accion(), interaccion.productoId(), particion, offset);
    }
}
