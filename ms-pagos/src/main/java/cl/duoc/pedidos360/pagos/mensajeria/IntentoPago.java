package cl.duoc.pedidos360.pagos.mensajeria;

import java.time.LocalDateTime;

/** Cada vez que un usuario intenta pagar se publica uno de estos; el consumidor cuenta la rafaga. */
public record IntentoPago(String usuarioOid, Long ordenId, int monto, LocalDateTime fecha) {
}
