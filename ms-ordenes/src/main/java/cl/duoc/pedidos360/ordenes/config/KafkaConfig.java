package cl.duoc.pedidos360.ordenes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    public static final String TOPICO_PAGOS = "pagos.resultado";
    public static final String TOPICO_AUDITORIA = "auditoria.eventos";
    public static final String GRUPO = "ordenes";

    /**
     * Si el procesamiento falla se reintenta 2 veces con 1 segundo de espera (3 intentos en total).
     * Al agotarlos se registra el error y se sigue con el mensaje siguiente, para que un mensaje
     * malo no bloquee la particion.
     */
    @Bean
    public DefaultErrorHandler manejadorDeErrores() {
        return new DefaultErrorHandler(new FixedBackOff(1000L, 2L));
    }
}
