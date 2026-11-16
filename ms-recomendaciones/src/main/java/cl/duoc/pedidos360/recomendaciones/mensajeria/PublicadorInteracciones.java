package cl.duoc.pedidos360.recomendaciones.mensajeria;

import cl.duoc.pedidos360.recomendaciones.config.KafkaConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublicadorInteracciones {

    private static final Logger log = LoggerFactory.getLogger(PublicadorInteracciones.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    public PublicadorInteracciones(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    /** La clave es el usuario: sus interacciones caen en la misma particion y se procesan en orden. */
    public void publicar(InteraccionMensaje mensaje) {
        try {
            String json = objectMapper.writeValueAsString(mensaje);
            kafka.send(KafkaConfig.TOPICO_INTERACCIONES, mensaje.usuarioOid(), json).whenComplete((envio, error) -> {
                if (error != null) {
                    log.error("No se pudo publicar la interaccion del usuario {}: {}", mensaje.usuarioOid(), error.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("No se pudo convertir a JSON la interaccion: {}", e.getMessage());
        }
    }
}
