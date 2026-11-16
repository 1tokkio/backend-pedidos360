package cl.duoc.pedidos360.pagos.service;

import cl.duoc.pedidos360.pagos.model.EstadoFraude;
import cl.duoc.pedidos360.pagos.repository.EstadoFraudeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Dos reglas, a proposito simples para poder explicarlas:
 * un monto demasiado alto, y una rafaga de intentos del mismo usuario.
 * La rafaga la cuenta un consumidor de Kafka, no la peticion HTTP, para no frenar el pago.
 */
@Service
public class DetectorFraude {

    private static final Logger log = LoggerFactory.getLogger(DetectorFraude.class);

    private final EstadoFraudeRepository estados;
    private final int montoMaximo;
    private final int intentosMaximos;
    private final int ventanaSegundos;
    private final int bloqueoMinutos;

    public DetectorFraude(EstadoFraudeRepository estados,
                          @Value("${app.fraude.monto-maximo}") int montoMaximo,
                          @Value("${app.fraude.intentos-maximos}") int intentosMaximos,
                          @Value("${app.fraude.ventana-segundos}") int ventanaSegundos,
                          @Value("${app.fraude.bloqueo-minutos}") int bloqueoMinutos) {
        this.estados = estados;
        this.montoMaximo = montoMaximo;
        this.intentosMaximos = intentosMaximos;
        this.ventanaSegundos = ventanaSegundos;
        this.bloqueoMinutos = bloqueoMinutos;
    }

    /** Devuelve el motivo si el usuario esta bloqueado por una rafaga de intentos, o null si puede pagar. */
    public String bloqueo(String usuarioOid) {
        LocalDateTime ahora = LocalDateTime.now();
        return estados.findById(usuarioOid)
                .filter(estado -> estado.estaBloqueado(ahora))
                .map(estado -> "Demasiados intentos de pago seguidos. Bloqueado hasta " + estado.getBloqueadoHasta())
                .orElse(null);
    }

    /** Devuelve el motivo si el monto parece fraude, o null si puede seguir. */
    public String montoSospechoso(int monto) {
        if (monto > montoMaximo) {
            return "El monto " + monto + " supera el maximo permitido de " + montoMaximo;
        }
        return null;
    }

    /** Lo llama el consumidor de Kafka por cada intento de pago. */
    @Transactional
    public void registrarIntento(String usuarioOid, LocalDateTime fecha) {
        EstadoFraude estado = estados.findById(usuarioOid).orElseGet(() -> new EstadoFraude(usuarioOid));

        if (estado.getBloqueadoHasta() != null) {
            if (estado.estaBloqueado(fecha)) {
                return;
            }
            // Ya paso el castigo: el usuario empieza de cero.
            estado.setBloqueadoHasta(null);
            estado.setIntentos(0);
        }

        // Si paso mas tiempo que la ventana desde el ultimo intento, no es una rafaga: se reinicia la cuenta.
        if (estado.getUltimoIntento() != null
                && Duration.between(estado.getUltimoIntento(), fecha).getSeconds() > ventanaSegundos) {
            estado.setIntentos(0);
        }

        estado.setIntentos(estado.getIntentos() + 1);
        estado.setUltimoIntento(fecha);

        if (estado.getIntentos() >= intentosMaximos) {
            estado.setBloqueadoHasta(fecha.plusMinutes(bloqueoMinutos));
            log.error("Posible fraude: el usuario {} hizo {} intentos en menos de {} segundos, bloqueado {} minutos",
                    usuarioOid, estado.getIntentos(), ventanaSegundos, bloqueoMinutos);
        }
        estados.save(estado);
    }
}
