package cl.duoc.pedidos360.ordenes.mensajeria;

import cl.duoc.pedidos360.ordenes.config.KafkaConfig;
import cl.duoc.pedidos360.ordenes.service.OrdenService;
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
public class ConsumidorPagos {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorPagos.class);

    private final OrdenService servicio;
    private final ObjectMapper objectMapper;

    public ConsumidorPagos(OrdenService servicio, ObjectMapper objectMapper) {
        this.servicio = servicio;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.TOPICO_PAGOS, groupId = KafkaConfig.GRUPO)
    public void alResolverseUnPago(@Payload String mensaje,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                                   @Header(KafkaHeaders.OFFSET) long offset) {
        PagoResultado pago;
        try {
            pago = objectMapper.readValue(mensaje, PagoResultado.class);
        } catch (JsonProcessingException e) {
            // Un mensaje que no se puede leer no mejora con reintentos: se registra y se descarta.
            log.error("Mensaje de pago ilegible (particion {}, offset {}): {}", particion, offset, mensaje);
            return;
        }

        if (pago.ordenId() == null || pago.estado() == null) {
            log.error("Mensaje de pago incompleto (particion {}, offset {}): {}", particion, offset, mensaje);
            return;
        }

        log.info("Pago {} de la orden {} llego con estado {} (particion {}, offset {})",
                pago.pagoId(), pago.ordenId(), pago.estado(), particion, offset);
        servicio.aplicarPago(pago);
    }
}
