package cl.duoc.pedidos360.ordenes.mensajeria;

import java.time.LocalDateTime;

/** Mensaje que se publica en el topico auditoria.eventos. */
public record EventoAuditoria(String servicio, String tipo, String detalle, LocalDateTime fecha) {
}
