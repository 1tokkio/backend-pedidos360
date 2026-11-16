package cl.duoc.pedidos360.ordenes.mensajeria;

import java.time.LocalDateTime;

/** Mensaje que ms-pagos publica en el topico pagos.resultado. */
public record PagoResultado(Long pagoId, Long ordenId, String usuarioOid, Integer monto,
                            String estado, String motivo, String medio, LocalDateTime fecha) {
}
