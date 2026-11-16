package cl.duoc.pedidos360.kafkaadmin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar deja creados los topicos del sistema, con 3 particiones y 3 replicas.
 * Si Kafka no esta disponible el servicio arranca igual y lo deja en el log.
 */
@Component
public class CreadorTopicos implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CreadorTopicos.class);
    private static final long DIA_EN_MS = 24L * 60 * 60 * 1000;
    private static final int INTENTOS = 5;
    private static final long PAUSA_MS = 10_000L;

    private final KafkaAdminService servicio;

    public CreadorTopicos(KafkaAdminService servicio) {
        this.servicio = servicio;
    }

    @Override
    public void run(String... args) {
        asegurar("pagos.resultado", 7 * DIA_EN_MS);
        asegurar("auditoria.eventos", 30 * DIA_EN_MS);
        asegurar("pagos.intentos", DIA_EN_MS);
        asegurar("interacciones.usuario", 7 * DIA_EN_MS);
    }

    /**
     * Al levantar todo junto, el cluster puede tardar en tener los tres brokers listos.
     * Por eso se reintenta unas veces antes de rendirse.
     */
    private void asegurar(String nombre, long retencionMs) {
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                if (servicio.existe(nombre)) {
                    log.info("El topico {} ya existe", nombre);
                    return;
                }
                servicio.crear(nombre, 3, 3, retencionMs, "delete");
                log.info("Topico {} creado con 3 particiones y 3 replicas", nombre);
                return;
            } catch (Exception e) {
                log.warn("No se pudo asegurar el topico {} (intento {} de {}): {}", nombre, intento, INTENTOS, e.getMessage());
                esperar();
            }
        }
        log.error("Me rindo con el topico {}: se puede crear despues desde la API", nombre);
    }

    private void esperar() {
        try {
            Thread.sleep(PAUSA_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
