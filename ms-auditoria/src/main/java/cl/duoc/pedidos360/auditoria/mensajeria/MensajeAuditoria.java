package cl.duoc.pedidos360.auditoria.mensajeria;

import java.time.LocalDateTime;

/** Mensaje del topico auditoria.eventos. Lo publican ms-ordenes y ms-pagos. */
public record MensajeAuditoria(String servicio, String tipo, String detalle, LocalDateTime fecha) {
}
