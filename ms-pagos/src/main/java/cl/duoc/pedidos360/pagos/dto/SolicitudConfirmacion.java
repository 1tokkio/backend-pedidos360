package cl.duoc.pedidos360.pagos.dto;

// El token llega a la pagina de retorno como token_ws (pago normal) o TBK_TOKEN (anulado).
public record SolicitudConfirmacion(String token) {
}
