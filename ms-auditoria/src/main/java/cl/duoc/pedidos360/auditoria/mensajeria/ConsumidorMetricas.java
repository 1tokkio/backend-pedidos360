package cl.duoc.pedidos360.auditoria.mensajeria;

import cl.duoc.pedidos360.auditoria.config.KafkaConfig;
import cl.duoc.pedidos360.auditoria.service.MetricasService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Lee el mismo topico que ms-ordenes, pero en otro grupo: por eso recibe todos los
 * pagos y no se los quita a ordenes. Cada pago termina en DynamoDB, y de ahi el
 * Stream avisa a la Lambda que lo manda por WebSocket al panel.
 */
@Component
public class ConsumidorMetricas {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorMetricas.class);

    private final MetricasService metricas;
    private final ObjectMapper objectMapper;

    public ConsumidorMetricas(MetricasService metricas, ObjectMapper objectMapper) {
        this.metricas = metricas;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.TOPICO_PAGOS, groupId = KafkaConfig.GRUPO_METRICAS)
    public void alResolverseUnPago(@Payload String mensaje,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                                   @Header(KafkaHeaders.OFFSET) long offset) {
        PagoResultado pago;
        try {
            pago = objectMapper.readValue(mensaje, PagoResultado.class);
        } catch (JsonProcessingException e) {
            log.error("Pago ilegible para metricas (particion {}, offset {}): {}", particion, offset, mensaje);
            return;
        }

        if (pago.pagoId() == null || pago.estado() == null) {
            log.error("Pago incompleto para metricas (particion {}, offset {}): {}", particion, offset, mensaje);
            return;
        }

        metricas.guardarPago(pago);
        log.info("Metrica del pago {} guardada en DynamoDB (particion {}, offset {})", pago.pagoId(), particion, offset);
    }
}
