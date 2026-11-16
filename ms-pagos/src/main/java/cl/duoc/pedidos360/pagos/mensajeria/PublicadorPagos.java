package cl.duoc.pedidos360.pagos.mensajeria;

import cl.duoc.pedidos360.pagos.config.KafkaConfig;
import cl.duoc.pedidos360.pagos.model.Pago;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PublicadorPagos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorPagos.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    public PublicadorPagos(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    /** Publica el resultado del pago y deja constancia en auditoria. */
    public void publicarResultado(Pago pago) {
        PagoResultado resultado = new PagoResultado(
                pago.getId(), pago.getOrdenId(), pago.getUsuarioOid(), pago.getMonto(),
                pago.getEstado(), pago.getMotivo(), pago.getMedio(), pago.getFecha());

        // La clave es la orden: todos los eventos de una orden caen en la misma particion y se leen en orden.
        enviar(KafkaConfig.TOPICO_PAGOS, String.valueOf(pago.getOrdenId()), resultado);

        EventoAuditoria evento = new EventoAuditoria(
                "ms-pagos", "PAGO_" + pago.getEstado(),
                "orden " + pago.getOrdenId() + ", monto " + pago.getMonto() + ": " + pago.getMotivo(),
                LocalDateTime.now());
        enviar(KafkaConfig.TOPICO_AUDITORIA, "ms-pagos", evento);
    }

    /** Avisa que el usuario intento pagar. La clave es el usuario para que sus intentos lleguen en orden. */
    public void publicarIntento(String usuarioOid, Long ordenId, int monto) {
        enviar(KafkaConfig.TOPICO_INTENTOS, usuarioOid, new IntentoPago(usuarioOid, ordenId, monto, LocalDateTime.now()));
    }

    private void enviar(String topico, String clave, Object mensaje) {
        try {
            String json = objectMapper.writeValueAsString(mensaje);
            kafka.send(topico, clave, json).whenComplete((envio, error) -> {
                if (error != null) {
                    log.error("No se pudo publicar en {} con clave {}: {}", topico, clave, error.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("No se pudo convertir a JSON el mensaje para {}: {}", topico, e.getMessage());
        }
    }
}
