package cl.duoc.pedidos360.pagos.mensajeria;

import java.time.LocalDateTime;

/** Mensaje que se publica en el topico pagos.resultado cuando un pago queda resuelto. */
public record PagoResultado(Long pagoId, Long ordenId, String usuarioOid, Integer monto,
                            String estado, String motivo, String medio, LocalDateTime fecha) {
}
