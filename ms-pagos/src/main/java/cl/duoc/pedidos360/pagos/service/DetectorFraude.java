package cl.duoc.pedidos360.pagos.service;

import cl.duoc.pedidos360.pagos.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Dos reglas, a proposito simples para poder explicarlas: un monto demasiado alto
 * y demasiados intentos del mismo usuario en pocos minutos.
 */
@Service
public class DetectorFraude {

    private final PagoRepository repositorio;
    private final int montoMaximo;
    private final int intentosMaximos;
    private final int ventanaMinutos;

    public DetectorFraude(PagoRepository repositorio,
                          @Value("${app.fraude.monto-maximo}") int montoMaximo,
                          @Value("${app.fraude.intentos-maximos}") int intentosMaximos,
                          @Value("${app.fraude.ventana-minutos}") int ventanaMinutos) {
        this.repositorio = repositorio;
        this.montoMaximo = montoMaximo;
        this.intentosMaximos = intentosMaximos;
        this.ventanaMinutos = ventanaMinutos;
    }

    /** Devuelve el motivo si el pago parece fraude, o null si puede seguir. */
    public String evaluar(String usuarioOid, int monto) {
        if (monto > montoMaximo) {
            return "El monto " + monto + " supera el maximo permitido de " + montoMaximo;
        }

        // El pago actual todavia no se guarda, asi que esto cuenta solo los intentos anteriores.
        LocalDateTime desde = LocalDateTime.now().minusMinutes(ventanaMinutos);
        long intentos = repositorio.countByUsuarioOidAndFechaAfter(usuarioOid, desde);
        if (intentos >= intentosMaximos) {
            return "Ya hizo " + intentos + " intentos de pago en los ultimos " + ventanaMinutos + " minutos";
        }

        return null;
    }
}
