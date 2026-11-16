package cl.duoc.pedidos360.kafkaadmin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar deja creados los dos topicos del sistema, con 3 particiones y 3 replicas.
 * Si Kafka no esta disponible el servicio arranca igual y lo deja en el log.
 */
@Component
public class CreadorTopicos implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CreadorTopicos.class);
    private static final long DIA_EN_MS = 24L * 60 * 60 * 1000;

    private final KafkaAdminService servicio;

    public CreadorTopicos(KafkaAdminService servicio) {
        this.servicio = servicio;
    }

    @Override
    public void run(String... args) {
        asegurar("pagos.resultado", 7 * DIA_EN_MS);
        asegurar("auditoria.eventos", 30 * DIA_EN_MS);
    }

    private void asegurar(String nombre, long retencionMs) {
        try {
            if (servicio.existe(nombre)) {
                log.info("El topico {} ya existe", nombre);
                return;
            }
            servicio.crear(nombre, 3, 3, retencionMs, "delete");
            log.info("Topico {} creado con 3 particiones y 3 replicas", nombre);
        } catch (Exception e) {
            log.error("No se pudo asegurar el topico {}: {}", nombre, e.getMessage());
        }
    }
}
