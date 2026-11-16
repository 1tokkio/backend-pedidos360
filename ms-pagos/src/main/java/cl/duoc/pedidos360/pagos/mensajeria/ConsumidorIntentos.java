package cl.duoc.pedidos360.pagos.mensajeria;

import cl.duoc.pedidos360.pagos.config.KafkaConfig;
import cl.duoc.pedidos360.pagos.service.DetectorFraude;
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
 * Cuenta los intentos de pago de cada usuario. La clave del mensaje es el usuario, asi
 * todos sus intentos caen en la misma particion y se leen en el orden en que ocurrieron.
 */
@Component
public class ConsumidorIntentos {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorIntentos.class);

    private final DetectorFraude detector;
    private final ObjectMapper objectMapper;

    public ConsumidorIntentos(DetectorFraude detector, ObjectMapper objectMapper) {
        this.detector = detector;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.TOPICO_INTENTOS, groupId = KafkaConfig.GRUPO_FRAUDE)
    public void alIntentarPagar(@Payload String mensaje,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                                @Header(KafkaHeaders.OFFSET) long offset) {
        IntentoPago intento;
        try {
            intento = objectMapper.readValue(mensaje, IntentoPago.class);
        } catch (JsonProcessingException e) {
            // Un mensaje ilegible no se reintenta: se registra y se descarta para no bloquear la particion.
            log.error("Intento de pago ilegible (particion {}, offset {}): {}", particion, offset, mensaje);
            return;
        }

        if (intento.usuarioOid() == null || intento.fecha() == null) {
            log.error("Intento de pago incompleto (particion {}, offset {}): {}", particion, offset, mensaje);
            return;
        }

        detector.registrarIntento(intento.usuarioOid(), intento.fecha());
        log.info("Intento de pago de la orden {} contado (particion {}, offset {})", intento.ordenId(), particion, offset);
    }
}
